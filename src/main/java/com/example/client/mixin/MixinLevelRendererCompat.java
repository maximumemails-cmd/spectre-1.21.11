package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.spectre.client.module.impl.misc.FreecamModule;
import net.spectre.client.module.impl.render.FreelookModule;
import net.spectre.client.module.impl.render.NoRenderModule;
import net.spectre.client.util.RenderMatrices;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.11 replacements for hooks that 26.x Spectre placed on render-state extraction
 * ({@code Camera.render(CameraRenderState, float)}).
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRendererCompat {
   @Inject(method = "renderLevel", at = @At("HEAD"))
   private void spectre$captureMatrices(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
      Matrix4f viewRotation, Matrix4f projection, Matrix4f cullProjection, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
      RenderMatrices.capture(viewRotation, projection);
   }

   @Inject(method = "doesMobEffectBlockSky", at = @At("HEAD"), cancellable = true)
   private void spectre$skyThroughBlindness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         cir.setReturnValue(false);
      }
   }

}
