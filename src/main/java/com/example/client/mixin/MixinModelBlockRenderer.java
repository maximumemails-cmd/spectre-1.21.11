package com.example.client.mixin;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.spectre.client.module.impl.render.XrayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * X-ray block transparency for the vanilla (non-Indigo) block renderer.
 * <p>
 * 1.21.11: there is no {@code QuadInstance}; the per-quad alpha is the {@code putBulkData} alpha argument.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class MixinModelBlockRenderer {
    @ModifyArg(method = "putQuadData", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"), index = 6)
    private float spectre$alpha(float alpha) {
        return XrayModule.active != null ? alpha * XrayModule.CURRENT_ALPHA.get()[0] : alpha;
    }
}
