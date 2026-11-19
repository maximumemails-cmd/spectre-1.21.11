package com.example.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.spectre.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class MixinJumpCirclesLevelRenderer {
   // 1.21.11: queue the circles with the entity submissions (26.x used the block-destroy submit phase).
   @Inject(method = "submitEntities", at = @At("HEAD"))
   private void spectre$jumpCircles(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector collector, CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.renderLevel(levelRenderState.cameraRenderState, collector);
      }
   }
}
