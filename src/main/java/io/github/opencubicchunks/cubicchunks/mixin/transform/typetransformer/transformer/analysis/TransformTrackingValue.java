package io.github.opencubicchunks.cubicchunks.mixin.transform.typetransformer.transformer.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.github.opencubicchunks.cubicchunks.mixin.transform.typetransformer.transformer.config.TransformType;
import io.github.opencubicchunks.cubicchunks.mixin.transform.util.ASMUtil;
import io.github.opencubicchunks.cubicchunks.mixin.transform.util.AncestorHashMap;
import io.github.opencubicchunks.cubicchunks.mixin.transform.util.FieldID;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

/**
 * A value that infers its transform type and tracks the instructions that created it, consumed it and
 * the fields it comes from
 */
public class TransformTrackingValue implements Value {
    final Set<UnresolvedMethodTransform> possibleTransformChecks = new HashSet<>(); //Used to track possible transform checks

    private final @Nullable Type type;
    private final Set<AbstractInsnNode> source;
    private final Set<Integer> localVars; //Used uniquely for parameters
    private final Set<AbstractInsnNode> consumers = new HashSet<>(); //Any instruction which "consumes" this value
    private final Set<FieldSource> fieldSources = new HashSet<>(); //Used for field detecting which field this value comes from. For now only tracks instance fields (i.e not static)
    private final AncestorHashMap<FieldID, TransformTrackingValue> pseudoValues;

    private final List<TransformTrackingValue> mergedFrom = new ArrayList<>(2);
    private final List<TransformTrackingValue> mergedTo = new ArrayList<>(1);

    private final TransformSubtype transform;

    private final Set<TransformTrackingValue> valuesWithSameType = new HashSet<>();

    public TransformTrackingValue(@Nullable Type type, AncestorHashMap<FieldID, TransformTrackingValue> fieldPseudoValues) {
        this.type = type;
        this.source = new HashSet<>();
        this.localVars = new HashSet<>();
        this.pseudoValues = fieldPseudoValues;
        this.transform = TransformSubtype.createDefault();

        this.transform.getTransformTypePtr().addTrackingValue(this);
        this.transform.setSubType(TransformSubtype.getSubType(type));
    }

    public TransformTrackingValue(@Nullable Type type, int localVar, AncestorHashMap<FieldID, TransformTrackingValue> fieldPseudoValues) {
        this.type = type;
        this.source = new HashSet<>();
        this.localVars = new HashSet<>();
        localVars.add(localVar);
        this.pseudoValues = fieldPseudoValues;
        this.transform = TransformSubtype.createDefault();

        this.transform.getTransformTypePtr().addTrackingValue(this);
        this.transform.setSubType(TransformSubtype.getSubType(type));
    }

    public TransformTrackingValue(@Nullable Type type, AbstractInsnNode source, AncestorHashMap<FieldID, TransformTrackingValue> fieldPseudoValues) {
        this(type, fieldPseudoValues);
        this.source.add(source);
    }

    public TransformTrackingValue(@Nullable Type type, AbstractInsnNode insn, int var, TransformSubtype transform, AncestorHashMap<FieldID, TransformTrackingValue> fieldPseudoValues) {
        this.type = type;
        this.source = new HashSet<>();
        this.source.add(insn);
        this.localVars = new HashSet<>();
        this.localVars.add(var);
        this.transform = transform;
        this.pseudoValues = fieldPseudoValues;

        this.transform.getTransformTypePtr().addTrackingValue(this);
        this.transform.setSubType(TransformSubtype.getSubType(type));
    }

    public TransformTrackingValue(@Nullable Type type, Set<AbstractInsnNode> source, Set<Integer> localVars, TransformSubtype transform,
                                  AncestorHashMap<FieldID, TransformTrackingValue> fieldPseudoValues) {
        this.type = type;
        this.source = source;
        this.localVars = localVars;
        this.transform = transform;
        this.pseudoValues = fieldPseudoValues;

        this.transform.getTransformTypePtr().addTrackingValue(this);
        this.transform.setSubType(TransformSubtype.getSubType(type));
    }

    public TransformTrackingValue merge(TransformTrackingValue other) {
        if (transform.getTransformType() != null && other.transform.getTransformType() != null && transform.getTransformType() != other.transform.getTransformType()) {
            throw new RuntimeException("Merging incompatible values. (Different transform types had already been assigned)");
        }

        setSameType(this, other);

        TransformTrackingValue newValue = new TransformTrackingValue(
            type,
            union(source, other.source),
            union(localVars, other.localVars),
            transform,
            pseudoValues
        );

        newValue.mergedFrom.add(this);
        newValue.mergedFrom.add(other);

        this.mergedTo.add(newValue);
        other.mergedTo.add(newValue);

        return newValue;
    }

    public @Nullable TransformType getTransformType() {
        return transform.getTransformType();
    }

    public void setTransformType(TransformType transformType) {
        if (this.transform.getTransformType() != null && transformType != this.transform.getTransformType()) {
            throw new RuntimeException("Transform subType already set");
        }

        if (this.transform.getTransformType() == transformType) {
            return;
        }

        Type rawType = this.transform.getRawType(transformType);
        int dimension = ASMUtil.getDimensions(this.type) - ASMUtil.getDimensions(rawType);
        this.transform.setArrayDimensionality(dimension);

        this.transform.getTransformTypePtr().setValue(transformType);
    }

    public void updateType(@Nullable TransformType oldType, TransformType newType) {
        //Set appropriate array dimensions
        Set<TransformTrackingValue> copy = new HashSet<>(valuesWithSameType);
        valuesWithSameType.clear(); //To prevent infinite recursion

        for (TransformTrackingValue value : copy) {
            value.setTransformType(newType);
        }

        Type rawType = this.transform.getRawType(newType);
        int dimension = ASMUtil.getDimensions(this.type) - ASMUtil.getDimensions(rawType);
        this.transform.setArrayDimensionality(dimension);

        for (UnresolvedMethodTransform check : possibleTransformChecks) {
            int validity = check.check();
            if (validity == -1) {
                check.reject();
            } else if (validity == 1) {
                check.accept();
            }
        }

        if (fieldSources.size() > 0) {
            for (FieldSource fieldSource : fieldSources) {
                //System.out.println("Field " + source.root() + " is now " + newType);
                FieldID id = new FieldID(Type.getObjectType(fieldSource.classNode()), fieldSource.fieldName(), Type.getType(fieldSource.fieldDesc()));
                if (pseudoValues.containsKey(id)) {
                    TransformTrackingValue value = pseudoValues.get(id);
                    //value.transform.setArrayDimensionality(source.arrayDepth());
                    value.setTransformType(newType);
                }
            }
        }
    }

    public void addFieldSource(FieldSource fieldSource) {
        fieldSources.add(fieldSource);
    }

    public void addFieldSources(Set<FieldSource> sources) {
        this.fieldSources.addAll(sources);
    }

    public Set<FieldSource> getFieldSources() {
        return fieldSources;
    }

    public void addPossibleTransformCheck(UnresolvedMethodTransform transformCheck) {
        possibleTransformChecks.add(transformCheck);
    }

    @Override
    public int getSize() {
        return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransformTrackingValue that = (TransformTrackingValue) o;
        return Objects.equals(type, that.type) && Objects.equals(source, that.source) && Objects
            .equals(consumers, that.consumers);
    }

    @Override public int hashCode() {
        return Objects.hash(type, source, localVars, consumers, transform);
    }

    public static <T> Set<T> union(Set<T> first, Set<T> second) {
        Set<T> union = new HashSet<>(first);
        union.addAll(second);
        return union;
    }

    public Type getType() {
        return type;
    }

    public Set<AbstractInsnNode> getSource() {
        return source;
    }

    public Set<Integer> getLocalVars() {
        return localVars;
    }

    public Set<AbstractInsnNode> getConsumers() {
        return consumers;
    }

    public void consumeBy(AbstractInsnNode consumer) {
        consumers.add(consumer);
    }

    public Set<TransformTrackingValue> getAllRelatedValues() {
        Set<TransformTrackingValue> relatedValues = new ObjectOpenCustomHashSet<>(Util.identityStrategy());

        Set<TransformTrackingValue> newValues = new ObjectOpenCustomHashSet<>(Util.identityStrategy());
        newValues.addAll(mergedTo);
        newValues.add(this);

        while (!newValues.isEmpty()) {
            Set<TransformTrackingValue> nextValues = new ObjectOpenCustomHashSet<>(Util.identityStrategy());
            for (TransformTrackingValue value : newValues) {
                relatedValues.add(value);
                nextValues.addAll(value.mergedFrom);
                nextValues.addAll(value.mergedTo);
            }
            nextValues.removeAll(relatedValues);
            nextValues.removeAll(newValues);
            newValues = nextValues;
        }

        return relatedValues;
    }

    public static void setSameType(TransformTrackingValue first, TransformTrackingValue second) {
        if (first.type == null || second.type == null) {
            //System.err.println("WARNING: Attempted to set same subType on null subType");
            return;
        }

        if (first.getTransformType() == null && second.getTransformType() == null) {
            first.valuesWithSameType.add(second);
            second.valuesWithSameType.add(first);
            return;
        }

        if (first.getTransformType() != null && second.getTransformType() != null && first.getTransformType() != second.getTransformType()) {
            throw new RuntimeException("Merging incompatible values. (Different types had already been assigned)");
        }

        if (first.getTransformType() != null) {
            second.getTransformTypeRef().setValue(first.getTransformType());
        } else if (second.getTransformType() != null) {
            first.getTransformTypeRef().setValue(second.getTransformType());
        }
    }

    public TransformTypePtr getTransformTypeRef() {
        return transform.getTransformTypePtr();
    }

    @Override
    public String toString() {
        if (type == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(type.toString());

        if (transform.getTransformType() != null) {
            sb.append(" (").append(transform).append(")");
        }

        if (fieldSources.size() > 0) {
            sb.append(" (from ");
            int i = 0;
            for (FieldSource fieldSource : fieldSources) {
                sb.append(fieldSource.toString());
                if (i < fieldSources.size() - 1) {
                    sb.append(", ");
                }
                i++;
            }
            sb.append(")");
        }

        return sb.toString();
    }

    public TransformSubtype getTransform() {
        return transform;
    }

    public int getTransformedSize() {
        if (transform.getTransformType() == null) {
            return getSize();
        } else {
            return transform.getTransformedSize();
        }
    }

    public List<Type> transformedTypes() {
        return this.transform.transformedTypes(this.type);
    }
}
