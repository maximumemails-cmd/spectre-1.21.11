package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.spectre.client.module.impl.misc.FreecamModule;
import net.spectre.client.module.impl.render.FreelookModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam/Freelook hooks into vanilla terrain culling. Skipped by {@code SpectreMixinPlugin}
 * when Sodium is installed, because Sodium overwrites both target methods.
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class MixinVanillaCullingLevelRenderer {
   @ModifyExpressionValue(method = "cullTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;smartCull:Z"))
   private boolean spectre$smartCull(boolean original) {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      return (FreecamModule.active != null || camera.isDetached() && FreelookModule.throughBlocks()) ? false : original;
   }

   @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
   private void spectre$freecamEntitiesReady(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (FreecamModule.active != null) {
         cir.setReturnValue(true);
      }
   }
}
