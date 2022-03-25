package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelLightEngine.class)
public interface LevelLightEngineAccess {
    @Accessor
    LayerLightEngine getBlockEngine();

    @Accessor
    LayerLightEngine getSkyEngine();
}
