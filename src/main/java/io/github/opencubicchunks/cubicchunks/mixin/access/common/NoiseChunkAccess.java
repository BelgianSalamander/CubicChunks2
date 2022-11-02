package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccess {
    @Invoker("<init>")
    static NoiseChunk invokeInit(
        int i, int j, int k, NoiseRouter noiseRouter, int l, int m,
        DensityFunctions.BeardifierOrMarker beardifierOrMarker, NoiseGeneratorSettings noiseGeneratorSettings,
        Aquifer.FluidPicker fluidPicker, Blender blender
    ) {
        throw new AssertionError();
    }
}
