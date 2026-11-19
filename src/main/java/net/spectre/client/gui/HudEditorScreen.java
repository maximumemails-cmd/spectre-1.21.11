package net.spectre.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class HudEditorScreen extends Screen {
   public HudEditorScreen() {
      super(Component.literal("SPECTRE HUD Layout"));
   }

   @Override
   public void removed() {
      super.removed();
      HudLayoutStore.getInstance().save();
   }

   @Override
   public void onClose() {
      HudLayoutStore.getInstance().save();
      Minecraft.getInstance().setScreen(null);
   }

   @Override
   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
