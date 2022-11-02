package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import io.github.opencubicchunks.cc_core.api.CubePos;
import io.github.opencubicchunks.cc_core.world.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.levelgen.chunk.NoiseAndSurfaceBuilderHelper;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.NoiseCube;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SurfaceSystem.class)
public abstract class MixinSurfaceSystem {
    private CubePos currCube;

    @Redirect(method = "buildSurface",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/dimension/DimensionType;WAY_BELOW_MIN_Y:I")
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/BlockColumn;getBlock(I)Lnet/minecraft/world/level/block/state/BlockState;"
        ))
    private BlockState modifyLoop(BlockColumn instance, int yPos,
                                  BiomeManager biomeManager,
                                  Registry<Biome> registry,
                                  boolean bl,
                                  WorldGenerationContext worldGenerationContext,
                                  ChunkAccess chunkAccess) {
        if (((CubicLevelHeightAccessor) chunkAccess).isCubic() && yPos < chunkAccess.getMinBuildHeight()) {
            return Blocks.STONE.defaultBlockState();
        }
        return instance.getBlock(yPos);
    }

    @Redirect(
        method = "buildSurface",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
        )
    )
    private int getHeight(ChunkAccess chunkAccess, Heightmap.Types heightmap, int x, int z, BiomeManager biomeManager, Registry<Biome> registry, boolean bl,
                          WorldGenerationContext worldGenerationContext, final ChunkAccess chunkAccess2, NoiseChunk noiseChunk, SurfaceRules.RuleSource ruleSource) {
        if (((CubicLevelHeightAccessor) chunkAccess).isCubic()) {
            int height = chunkAccess.getHeight(heightmap, x, z);

            if (height < chunkAccess.getMinBuildHeight()) {
                return ((NoiseCube) noiseChunk).getPreliminarySurfaceForY(
                    ChunkPos.asLong(
                        QuartPos.fromBlock(x),
                        QuartPos.fromBlock(z)
                    ),
                    chunkAccess.getMinBuildHeight()
                );
            } else {
                return height;
            }
        }
        return chunkAccess.getHeight(heightmap, x, z);
    }

    @Inject(
        method = "buildSurface",
        at = @At("HEAD")
    )
    private void saveChunkAccess(BiomeManager biomeManager, Registry<Biome> registry, boolean bl, WorldGenerationContext worldGenerationContext, ChunkAccess chunkAccess,
                                 NoiseChunk noiseChunk, SurfaceRules.RuleSource ruleSource, CallbackInfo ci) {
        if (chunkAccess instanceof CubeAccess) {
            this.currCube = ((CubeAccess) chunkAccess).getCubePos();
        } else if (chunkAccess instanceof NoiseAndSurfaceBuilderHelper) {
            this.currCube = ((NoiseAndSurfaceBuilderHelper) chunkAccess).getDelegateByIndex(0).getCubePos();
        }
    }

    @Redirect(
        method = "frozenOceanExtension",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"
        )
    )
    private int clampUpperBound(int a, int b) {
        int value = Math.max(a, b);

        if (currCube == null) {
            return value;
        }

        int minHeight = currCube.minCubeY();
        int maxHeight = currCube.maxCubeY();

        return Mth.clamp(value, minHeight, maxHeight);
    }

    @ModifyArg(
        method = "buildSurface",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/SurfaceSystem;frozenOceanExtension(ILnet/minecraft/world/level/biome/Biome;"
                + "Lnet/minecraft/world/level/chunk/BlockColumn;Lnet/minecraft/core/BlockPos$MutableBlockPos;III)V"
        ),
        index = 0
    )
    private int modifyFrozenOceanExtension(int value) {
        if (currCube == null) {
            return value;
        }

        int minHeight = currCube.minCubeY();
        int maxHeight = currCube.maxCubeY();

        return Mth.clamp(value, minHeight, maxHeight);
    }
}
