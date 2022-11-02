package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import java.util.Map;
import java.util.function.Supplier;

import io.github.opencubicchunks.cubicchunks.levelgen.chunk.NoiseAndSurfaceBuilderHelper;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.NoiseChunkAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.NoiseRouterDataAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.NoiseCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunk.class)
public abstract class MixinNoiseChunk implements NoiseCube {
    private static final int SURFACE_SEARCH_UP = 64;
    private static final int SURFACE_SEARCH_DOWN = 64;

    @Shadow @Final int cellCountY;

    private final Long2ObjectMap<int[]> preliminarySurfaceForY = new Long2ObjectOpenHashMap<>();
    private int minY;
    private boolean cubic;

    @Shadow @Final private NoiseSettings noiseSettings;

    @Shadow @Final private Map<DensityFunction, DensityFunction> wrapped;

    @Shadow @Final private DensityFunction initialDensityNoJaggedness;

    @Shadow protected abstract DensityFunction wrapNew(DensityFunction densityFunction);

    @Override
    public void setMinY(int minY) {
        this.minY = minY;
    }

    @Override
    public void setCubic(boolean cubic) {
        this.cubic = cubic;
    }

    @Override
    public boolean isCubic() {
        return this.cubic;
    }

    @Override
    public int getPreliminarySurfaceForY(long quart, int y) {
        int[] column = preliminarySurfaceForY.computeIfAbsent(quart, this::computeSurfaceColumn);

        //TODO: Remove the clamp, this should not go out of bounds
        int idx = Mth.clamp((y - minY) / noiseSettings.getCellHeight(), 0, column.length - 1);

        return column[idx];
    }

    /**
      Calculates density at a point and determine if that block will be air (Taken from NoiseRouterData)
     */
    private boolean isAir(int x, int y, int z) {
        double noise = this.initialDensityNoJaggedness.compute(
            new DensityFunction.SinglePointContext(x, y, z)
        ) - 0.703125;
        double clamped = Mth.clamp(noise, -64, 64);

        return NoiseRouterDataAccess.invokeApplySlide(this.noiseSettings, clamped, y) <= 0.390625;
    }

    private int[] computeSurfaceColumn(long chunk) {
        /*
         In vanilla, this method scans from the top of a chunk to the bottom, finding the first value where the density of the noise
         function represents a solid block. In CC, we cannot scan the entirety of a chunk, so we scan within the cube and a few blocks
         up and down to determine where the surfaces are.

         On top of that, we allow for multiple surfaces within a single cube, hence the array.
         */
        int x = QuartPos.toBlock(ChunkPos.getX(chunk));
        int z = QuartPos.toBlock(ChunkPos.getZ(chunk));

        int[] highestSurfaces = new int[this.cellCountY];
        boolean[] isAir = new boolean[this.cellCountY];

        //First we find what cells in the column are air
        for (int cellYIdx = 0; cellYIdx < this.cellCountY; cellYIdx++) {
            int cellY = cellYIdx * this.noiseSettings.getCellHeight() + this.minY;
            isAir[cellYIdx] = isAir(x, cellY, z);
        }

        //If the bottom cell in the column is air, then we try to find the highest surface below the column that is solid
        if (isAir[0]) {
            highestSurfaces[0] = Integer.MIN_VALUE; //If we find nothing in our scanning range, then we say that the surface is at Integer.MIN_VALUE
            //Scan downwards
            for (int cellYDown = -1; cellYDown >= -SURFACE_SEARCH_DOWN / noiseSettings.getCellHeight(); cellYDown--) {
                int cellY = cellYDown * this.noiseSettings.getCellHeight() + this.minY;

                if (!isAir(x, cellY, z)) {
                    //We have found the highest surface below the column
                    highestSurfaces[0] = cellY;
                    break;
                }
            }
        }

        //Similarly, if the top cell in the column is solid, we try to find the first surface above it.
        if (!isAir[this.cellCountY - 1]) {
            highestSurfaces[this.cellCountY - 1] = Integer.MAX_VALUE;
            //Scan upwards
            for (int cellYUp = 1; cellYUp <= SURFACE_SEARCH_UP / noiseSettings.getCellHeight(); cellYUp++) {
                int cellY = cellYUp * this.noiseSettings.getCellHeight() + this.minY;

                if (isAir(x, cellY, z)) {
                    //We have found the first surface above the column. We subtract the cell height because
                    //the actual surface is that last solid cell, which is just below the first air cell
                    highestSurfaces[this.cellCountY - 1] = cellY - noiseSettings.getCellHeight();
                    break;
                }
            }
        }

        //Then we find all the transitions from solid to air.
        for (int i = 1; i < this.cellCountY; i++) {
            if (!isAir[i - 1] && isAir[i]) {
                //We have found a transition from solid to air. Therefore, the surface level of both these cells is the solid cell.
                int highestSurface = this.minY + (i - 1) * this.noiseSettings.getCellHeight();
                highestSurfaces[i - 1] = highestSurface;
                highestSurfaces[i] = highestSurface;
            }
        }

        //We spread the surface information upwards. If a cell is air, then the surface level is the same as that of the cell below it.
        for (int i = 1; i < this.cellCountY; i++) {
            if (isAir[i]) {
                highestSurfaces[i] = highestSurfaces[i - 1];
            }
        }

        //Similarly, we spread the surface information downwards. If a cell is solid, then the surface level is the same as that of the cell above it.
        for (int i = this.cellCountY - 2; i >= 0; i--) {
            if (!isAir[i]) {
                highestSurfaces[i] = highestSurfaces[i + 1];
            }
        }

        return highestSurfaces;
    }

    @Redirect(method = "forChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private static int forceCubeMaxBounds(int a, int b, ChunkAccess chunkAccess, NoiseRouter noiseRouter, Supplier<DensityFunctions.BeardifierOrMarker> supplier,
                                        NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender) {
        if (chunkAccess instanceof ProtoCube || chunkAccess instanceof NoiseAndSurfaceBuilderHelper) {
            return b;
        }
       return Math.max(a, b);
    }

    @Redirect(method = "forChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private static int forceCubeMinBounds(int a, int b, ChunkAccess chunkAccess, NoiseRouter noiseRouter, Supplier<DensityFunctions.BeardifierOrMarker> supplier,
                                        NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender) {
        if (chunkAccess instanceof ProtoCube || chunkAccess instanceof NoiseAndSurfaceBuilderHelper) {
            return b;
        } else {
            return Math.min(a, b);
        }
    }

    @Redirect(method = "forChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;intFloorDiv(II)I", ordinal = 1))
    private static int useCubeCellSize(int a, int b, ChunkAccess chunkAccess, NoiseRouter noiseRouter, Supplier<DensityFunctions.BeardifierOrMarker> supplier,
                                          NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender) {
        if (chunkAccess instanceof ProtoCube || chunkAccess instanceof NoiseAndSurfaceBuilderHelper) {
            return b;
        } else {
            return chunkAccess.getHeight() / noiseGeneratorSettings.noiseSettings().getCellHeight();
        }
    }

    @Redirect(
        method = "forChunk",
        at = @At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/NoiseChunk"
        )
    )
    private static NoiseChunk setMinY(int i, int j, int k, NoiseRouter noiseRouter, int l, int m, DensityFunctions.BeardifierOrMarker beardifierOrMarker,
                                      NoiseGeneratorSettings noiseGeneratorSettings, Aquifer.FluidPicker fluidPicker, Blender blender, ChunkAccess chunkAccess) {
        NoiseChunk noiseChunk = NoiseChunkAccess.invokeInit(i, j, k, noiseRouter, l, m, beardifierOrMarker, noiseGeneratorSettings, fluidPicker, blender);
        ((NoiseCube) noiseChunk).setMinY(chunkAccess.getMinBuildHeight());
        ((NoiseCube) noiseChunk).setCubic(chunkAccess instanceof ProtoCube || chunkAccess instanceof NoiseAndSurfaceBuilderHelper);
        return noiseChunk;
    }

    @Redirect(
        method = "forColumn",
        at = @At(
            value = "NEW",
            target = "net/minecraft/world/level/levelgen/NoiseChunk"
        )
    )
    private static NoiseChunk setMinY(
            int i,
            int j,
            int k,
            NoiseRouter noiseRouter,
            int l,
            int m,
            DensityFunctions.BeardifierOrMarker beardifierOrMarker,
            NoiseGeneratorSettings noiseGeneratorSettings,
            Aquifer.FluidPicker fluidPicker,
            Blender blender,
            int x, int z, int minCellY
    ) {
        NoiseChunk noiseChunk = NoiseChunkAccess.invokeInit(i, j, k, noiseRouter, l, m, beardifierOrMarker, noiseGeneratorSettings, fluidPicker, blender);
        ((NoiseCube) noiseChunk).setMinY(minCellY * noiseGeneratorSettings.noiseSettings().getCellHeight());
        // "isCubic" is set in MixinNoiseBasedChunkGenerator (That is the only place forColumn is called) because there is not enough context in this method to determine that
        return noiseChunk;
    }
}
