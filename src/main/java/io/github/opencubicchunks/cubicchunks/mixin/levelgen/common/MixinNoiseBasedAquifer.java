package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.NoiseCube;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class MixinNoiseBasedAquifer {
    @Shadow protected abstract Aquifer.FluidStatus computeFluid(int x, int y, int z);

    @Redirect(
        method = "computeFluid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;preliminarySurfaceLevel(II)I"
        )
    )
    private int calculateCubicLevel(NoiseChunk instance, int i, int j, int x, int y, int z) {
        return ((NoiseCube) instance).isCubic() ?
            ((NoiseCube) instance).getPreliminarySurfaceForY(ChunkPos.asLong(QuartPos.fromBlock(i), QuartPos.fromBlock(j)), y) :
            instance.preliminarySurfaceLevel(x, z);
    }

    @Redirect(
        method = "getAquiferStatus",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/Aquifer$NoiseBasedAquifer;computeFluid(III)Lnet/minecraft/world/level/levelgen/Aquifer$FluidStatus;"
        )
    )
    private Aquifer.FluidStatus calculateCubicFluidStatus(Aquifer.NoiseBasedAquifer instance, int x, int y, int z) {
        Aquifer.FluidStatus res = this.computeFluid(x, y, z);

        if (!res.at(200).is(Blocks.AIR)) {
            //Breakpoint
            this.computeFluid(x, y, z);
        }

        return res;
    }
}
