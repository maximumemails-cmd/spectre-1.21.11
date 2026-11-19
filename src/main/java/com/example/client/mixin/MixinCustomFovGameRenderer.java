package com.example.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.spectre.client.module.impl.render.CustomFovModule;
import net.spectre.client.module.impl.render.ZoomModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11: field of view is computed by {@code GameRenderer.getFov(Camera, float, boolean)}; the boolean
 * selects the world FOV (true) or the hand/HUD FOV (false), which 26.x split into
 * {@code Camera.calculateFov} and {@code Camera.calculateHudFov}.
 */
@Mixin(GameRenderer.class)
public abstract class MixinCustomFovGameRenderer {
    @Shadow public abstract boolean isPanoramicMode();

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void spectre$customFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValueF();
        var module = CustomFovModule.active;
        if (useFovSetting) {
            if (isPanoramicMode()) return;
            cir.setReturnValue(ZoomModule.fov(module == null ? original : module.fov()));
        } else {
            float fov = module != null && !isPanoramicMode() ? module.fov() : original;
            CustomFovModule.hudFovApplied(original, fov);
            cir.setReturnValue(fov);
        }
    }
}
