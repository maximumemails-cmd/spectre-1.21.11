package net.spectre.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21.11 compatibility: Minecraft 26.x sends attacks as a dedicated {@code ServerboundAttackPacket};
 * 1.21.11 encodes them as the ATTACK action of {@link ServerboundInteractPacket}.
 */
@Environment(EnvType.CLIENT)
public final class AttackPackets {
   private AttackPackets() {
   }

   public static boolean isAttack(Packet<?> packet) {
      if (!(packet instanceof ServerboundInteractPacket interact)) {
         return false;
      }
      boolean[] attack = new boolean[1];
      interact.dispatch(new ServerboundInteractPacket.Handler() {
         @Override
         public void onInteraction(InteractionHand hand) {
         }

         @Override
         public void onInteraction(InteractionHand hand, Vec3 location) {
         }

         @Override
         public void onAttack() {
            attack[0] = true;
         }
      });
      return attack[0];
   }
}
