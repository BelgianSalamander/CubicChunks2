package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.NoiseCube;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {"net.minecraft.world.level.levelgen.SurfaceRules$Context"})
public class MixinSurfaceRulesContext {
    @Shadow @Final ChunkAccess chunk;

    @Redirect(
        method = "getMinSurfaceLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;preliminarySurfaceLevel(II)I"
        )
    )
    private int preliminarySurfaceLevel(NoiseChunk noiseChunk, int x, int z) {
        if (this.chunk instanceof CubeAccess) {
            return ((NoiseCube) noiseChunk).getPreliminarySurfaceForY(
                ChunkPos.asLong(
                    QuartPos.fromBlock(x),
                    QuartPos.fromBlock(z)
                ),
                chunk.getMinBuildHeight()
            );
        }

        return noiseChunk.preliminarySurfaceLevel(x, z);
    }
}
