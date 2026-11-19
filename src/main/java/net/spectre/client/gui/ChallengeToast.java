package net.spectre.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class ChallengeToast implements Toast {
   private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
   private static final int TITLE_COLOR = -30465;
   private static final int MESSAGE_COLOR = -1;
   private static final int TEXT_X = 30;
   private static final int TEXT_PAD = 4;
   private static final int MAX_WIDTH = 240;
   private static final long DISPLAY_MILLIS = 5000L;
   private final Object token;
   private final ItemStack icon;
   private final boolean sound;
   private Component title;
   private Component message;
   private int width;
   private int count = 1;
   private boolean changed;
   private boolean hiding;
   private long changedAt;
   private Visibility wanted = Visibility.HIDE;

   public ChallengeToast(ItemStack icon, Component title, Component message, boolean sound) {
      this(NO_TOKEN, icon, title, message, sound);
   }

   private ChallengeToast(Object token, ItemStack icon, Component title, Component message, boolean sound) {
      this.token = token;
      this.icon = icon.copy();
      this.icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      this.title = title;
      this.message = message;
      this.sound = sound;
      this.fit();
   }

   public static void addOrUpdate(ItemStack icon, String title, Component message, boolean sound) {
      addOrUpdate(icon, title, null, message, sound);
   }

   public static void addOrUpdate(ItemStack icon, String one, String many, Component message, boolean sound) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.isSameThread()) {
         mc.execute(() -> addOrUpdate(icon, one, many, message, sound));
      } else {
         ToastManager toasts = mc.getToastManager();
         ChallengeToast toast = toasts.getToast(ChallengeToast.class, one);
         if (toast == null) {
            toasts.addToast(new ChallengeToast(one, icon, Component.literal(one), message, sound));
         } else {
            toast.count++;
            if (many != null) {
               toast.title = Component.literal(toast.count + " " + many);
            }

            toast.message = message;
            toast.changed = true;
            toast.fit();
         }
      }
   }

   private void fit() {
      Font font = Minecraft.getInstance().font;
      int text = Math.max(font.width(this.title), font.width(this.message));
      this.width = Mth.clamp(30 + text + 4, 160, 240);
   }

   @Override
   public Object getToken() {
      return this.token;
   }

   @Override
   public SoundEvent getSoundEvent() {
      return this.sound ? SoundEvents.NOTE_BLOCK_BELL.value() : null;
   }

   @Override
   public Visibility getWantedVisibility() {
      return this.wanted;
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public void update(ToastManager toasts, long visibleFor) {
      if (!this.hiding) {
         if (this.changed) {
            this.changedAt = visibleFor;
            this.changed = false;
         }

         this.hiding = visibleFor - this.changedAt >= 5000.0 * toasts.getNotificationDisplayTimeMultiplier();
         this.wanted = this.hiding ? Visibility.HIDE : Visibility.SHOW;
      }
   }

   @Override
   public void onFinishedRendering() {
      if (this.changed) {
         this.hiding = false;
         Minecraft.getInstance().getToastManager().addToast(this);
      }
   }

   @Override
   public void render(GuiGraphics graphics, Font font, long visibleFor) {
      int half = this.width / 2;
      drawBackground(graphics, 0, 0, half);
      drawBackground(graphics, 160 - (this.width - half), half, this.width - half);
      boolean single = this.message.getString().isEmpty();
      this.drawText(graphics, font, this.title, single ? 12 : 7, -30465);
      if (!single) {
         this.drawText(graphics, font, this.message, 18, -1);
      }

      graphics.renderFakeItem(this.icon, 8, 8);
   }

   private static void drawBackground(GuiGraphics graphics, int u, int x, int w) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 160, 32, u, 0, x, 0, w, 32);
   }

   private void drawText(GuiGraphics graphics, Font font, Component text, int y, int color) {
      int room = this.width - 30 - 4;
      int textWidth = font.width(text);
      if (textWidth <= room) {
         graphics.drawString(font, text, 30, y, color, false);
      } else {
         graphics.pose().pushMatrix();
         graphics.pose().translate(30.0F, y);
         graphics.pose().scale((float)room / textWidth, 1.0F);
         graphics.drawString(font, text, 0, 0, color, false);
         graphics.pose().popMatrix();
      }
   }
}
