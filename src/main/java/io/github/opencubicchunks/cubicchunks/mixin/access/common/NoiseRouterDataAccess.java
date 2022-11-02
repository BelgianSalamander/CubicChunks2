package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface NoiseRouterDataAccess {
    @Invoker
    static double invokeApplySlide(NoiseSettings noiseSettings, double d, double e) {
        throw new AssertionError();
    }
}
