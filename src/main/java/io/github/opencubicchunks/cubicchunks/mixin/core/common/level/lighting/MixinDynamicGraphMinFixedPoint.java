package io.github.opencubicchunks.cubicchunks.mixin.core.common.level.lighting;

import io.github.opencubicchunks.cubicchunks.debug.light.LightEngineTracker;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DynamicGraphMinFixedPoint.class)
public abstract class MixinDynamicGraphMinFixedPoint implements LightEngineTracker.Access {
    @Shadow protected abstract void checkNeighbor(long sourceId, long targetId, int level, boolean decrease);

    @Shadow
    protected abstract void checkEdge(long sourceId, long id, int level, boolean decrease);

    //Light Engine Tracker
    @Inject(method = "runUpdates", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lit/unimi/dsi/fastutil/longs/Long2ByteMap;remove(J)B"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void storeUpdate(int i, CallbackInfoReturnable<Integer> cir, LongLinkedOpenHashSet longLinkedOpenHashSet, long pos, int currLevel, int newLevel) {
        if (this.getTracker() != null) {
            this.getTracker().writeRunUpdate(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos), currLevel, newLevel);
        }
    }

    @Inject(method = "enqueue", at = @At("HEAD"))
    private void recordEnqueue(long l, int i, int j, CallbackInfo ci) {
        if (this.getTracker() != null) {
            this.getTracker().writeEnqueue(
                BlockPos.getX(l),
                BlockPos.getY(l),
                BlockPos.getZ(l),
                i
            );
        }
    }

    @Inject(method = "checkEdge(JJIIIZ)V", at = @At("HEAD"))
    private void recordCheckEdge(long sourceId, long targetId, int newComputedLevel, int currentlyStoredLevel, int currentlyQueuedLevel, boolean decrease, CallbackInfo ci) {
        if (this.getTracker() != null) {
            this.getTracker().writeCheckEdge(
                BlockPos.getX(sourceId),
                BlockPos.getY(sourceId),
                BlockPos.getZ(sourceId),
                BlockPos.getX(targetId),
                BlockPos.getY(targetId),
                BlockPos.getZ(targetId),
                currentlyStoredLevel,
                currentlyQueuedLevel,
                newComputedLevel,
                decrease
            );
        }
    }

    @Override
    @Nullable
    public LightEngineTracker getTracker() {
        return null;
    }

    @Override
    public void setTracker(LightEngineTracker tracker) {
        throw new UnsupportedOperationException();
    }
}
