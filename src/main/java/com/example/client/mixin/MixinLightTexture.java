package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import net.spectre.client.module.ModuleRegistry;
import net.spectre.client.module.impl.render.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lightmap overrides (No Render darkness, Fullbright, X-ray).
 * <p>
 * 1.21.11: there is no {@code LightmapRenderState}; {@link LightTexture#updateLightTexture(float)}
 * reads the options and writes the uniform block directly, so the same values are substituted there.
 * Option reads in that method, in order: hideLightningFlash (0), darknessEffectScale (1), gamma (2).
 */
@Mixin(LightTexture.class)
public abstract class MixinLightTexture {
    @Shadow private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void spectre$forceUpdate(float partialTick, CallbackInfo ci) {
        var fullbright = ModuleRegistry.get(FullbrightModule.class);
        if (fullbright != null && fullbright.isEnabled() || XrayModule.active != null) this.updateLightTexture = true;
    }

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 1))
    private Object spectre$darkness(Object scale) {
        return NoRenderModule.hideBlindnessDarkness() || XrayModule.active != null ? Double.valueOf(0.0) : scale;
    }

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
    private Object spectre$brightness(Object gamma) {
        if (XrayModule.active != null) return Double.valueOf(1.0);
        var fullbright = ModuleRegistry.get(FullbrightModule.class);
        return fullbright != null && fullbright.isEnabled() ? Double.valueOf(fullbright.gamma()) : gamma;
    }

    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 0))
    private Vector3fc spectre$ambient(Vector3fc ambient) {
        return XrayModule.active != null ? new Vector3f(1.0F) : ambient;
    }
}
