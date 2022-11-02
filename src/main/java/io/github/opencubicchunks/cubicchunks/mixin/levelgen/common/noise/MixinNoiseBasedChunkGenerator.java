package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.noise;

import java.util.OptionalInt;
import java.util.function.Predicate;

import io.github.opencubicchunks.cc_core.utils.Coords;
import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.levelgen.CubicNoiseBasedChunkGenerator;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.NoiseCube;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NoiseBasedChunkGenerator.class)
public class MixinNoiseBasedChunkGenerator implements CubicNoiseBasedChunkGenerator {
    private boolean isCubic = false;

    @Override
    public void setCubic() {
        this.isCubic = true;
    }

    // TODO: also change decoration seed

    // MC uses the top of the world for accessing biome, use cube center
    @Redirect(method = "spawnOriginalMobs", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/WorldGenRegion;getMaxBuildHeight()I"))
    private int yPosInCubeForMobSpawn(WorldGenRegion instance) {
        if (instance instanceof CubeWorldGenRegion region) {
            return Coords.cubeToCenterBlock(region.getMainCubeY());
        } else {
            return instance.getMaxBuildHeight();
        }
    }

    // MC uses corner of a chunk for accessing biomes here. This works but will always access unchached biomes for interpolation
    // use the center of the chunk instead
    @Redirect(method = "spawnOriginalMobs", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getWorldPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos yPosInCubeForMobSpawn(ChunkPos instance, WorldGenRegion level) {
        if (level instanceof CubeWorldGenRegion) {
            return instance.getBlockAt(8, 0, 8);
        } else {
            return instance.getWorldPosition();
        }
    }

    @Redirect(
        method = "fillFromNoise",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;min(II)I"
        )
    )
    private int returnLevelValueMin(int a, int b) {
        if (this.isCubic) {
            return b;
        } else {
            return Math.min(a, b);
        }
    }

    @Redirect(
        method = "fillFromNoise",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"
        )
    )
    private int returnLevelValueMax(int a, int b) {
        if (this.isCubic) {
            return b;
        } else {
            return Math.max(a, b);
        }
    }

    @Redirect(
        method = "iterateNoiseColumn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;forColumn(IIIILnet/minecraft/world/level/levelgen/NoiseRouter;Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/NoiseChunk;"
        )
    )
    private NoiseChunk setCubic(int i, int j, int k, int l, NoiseRouter noiseRouter, NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker) {
        NoiseChunk result = NoiseChunk.forColumn(i, j, k, l, noiseRouter, noiseGeneratorSettings, fluidPicker);
        ((NoiseCube) result).setCubic(this.isCubic);
        return result;
    }
}
