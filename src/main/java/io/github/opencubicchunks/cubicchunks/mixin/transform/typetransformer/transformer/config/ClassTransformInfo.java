package io.github.opencubicchunks.cubicchunks.mixin.transform.typetransformer.transformer.config;

import java.util.Map;

import io.github.opencubicchunks.cubicchunks.mixin.transform.util.MethodID;

public class ClassTransformInfo {
    private final Map<MethodID, Map<Integer, TransformType>> typeHints;
    private final Map<String, ConstructorReplacer> constructorReplacers;
    private final boolean inPlace;

    public ClassTransformInfo(Map<MethodID, Map<Integer, TransformType>> typeHints, Map<String, ConstructorReplacer> constructorReplacers, boolean inPlace) {
        this.typeHints = typeHints;
        this.constructorReplacers = constructorReplacers;
        this.inPlace = inPlace;
    }

    public Map<MethodID, Map<Integer, TransformType>> getTypeHints() {
        return typeHints;
    }

    public Map<String, ConstructorReplacer> getConstructorReplacers() {
        return constructorReplacers;
    }

    public boolean isInPlace() {
        return inPlace;
    }
}
