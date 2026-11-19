package com.example.client.mixin;

import net.spectre.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(ServerboundInteractPacket.class)
public abstract class MixinSneakServerboundInteractPacket {
   // 1.21.11: every interaction (including attacks) goes through this private constructor.
   @ModifyVariable(method = "<init>(IZLnet/minecraft/network/protocol/game/ServerboundInteractPacket$Action;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private static boolean spectre$packetSneak(boolean usingSecondaryAction) {
      if (usingSecondaryAction) {
         return true;
      } else {
         SneakModule sneak = SneakModule.active;
         return sneak != null && sneak.doPacket();
      }
   }
}
