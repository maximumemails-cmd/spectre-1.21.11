package com.example.client.mixin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.spectre.client.module.impl.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: rain splash particles are spawned by {@code WeatherEffectRenderer.tickRainParticles}. */
@Mixin(net.minecraft.client.renderer.WeatherEffectRenderer.class)
public abstract class MixinNoRenderRainParticles {
    @Inject(method="tickRainParticles",at=@At("HEAD"),cancellable=true)
    private void spectre$rain(CallbackInfo ci){if(NoRenderModule.hideWeather())ci.cancel();}
}
