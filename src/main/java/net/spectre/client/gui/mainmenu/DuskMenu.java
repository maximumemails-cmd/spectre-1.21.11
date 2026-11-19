package net.spectre.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import java.util.List;
import net.spectre.client.gui.SpectreDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DuskMenu {
   private static final float SUN_R = 150.0F;
   private static final float HEAD_SIZE = 1.7F;
   private static final float NAME_GAP = 26.0F;
   private static final float ITEMS_GAP = 34.0F;
   private static final float ROW_H = 56.0F;
   private static final float QUIT_GAP = 12.0F;
   private static final float BUTTON_W = 320.0F;
   private static final float BUTTON_H = 46.0F;
   private static final float ITEM_TRACK = 2.0F;
   private static final List<MenuAction> ICONS = List.of(MenuAction.ACCOUNTS, MenuAction.LANGUAGE, MenuAction.ACCESSIBILITY, MenuAction.SPECTRE, MenuAction.MODS);
   private static final float ICON_R = 9.0F;
   private static final float ICON_STEP = 34.0F;
   private static final float ICON_MARGIN = 26.0F;
   private static final int ITEM = 0xFFE9E6F5;
   private static final float ITEM_ALPHA = 0.85F;
   private static final int ICON = -4676216;

   private DuskMenu() {
   }

   public static DuskMenu.Layout layout(float width, float height, float s) {
      float sunR = 150.0F * s;
      float nameH = 20.0F * s;
      float items = MenuAction.MAIN.size() * 56.0F * s + 12.0F * s;
      float column = 2.0F * sunR + 26.0F * s + nameH + 34.0F * s + items;
      float top = Math.max(48.0F * s, DuskScene.horizon(height) - column);
      float sunY = top + sunR;
      float nameY = sunY + sunR + 26.0F * s;
      return new DuskMenu.Layout(width * 0.5F, sunY, sunR, nameY, nameY + nameH + 34.0F * s);
   }

   public static void render(MenuContext c) {
      ImDrawList dl = c.bg();
      float sw = c.width();
      float sh = c.height();
      float s = c.scale();
      DuskMenu.Layout l = layout(sw, sh, s);
      DuskScene.draw(dl, sw, sh, s, c.time(), DuskScene.horizon(sh), l.cx(), l.sunY(), l.sunR(), c.lookX(), c.lookY());
      DuskEmblem.draw(c, l.cx(), l.sunY(), l.sunR() * 1.7F, l.nameY());
      float y = l.itemsY();

      for (int i = 0; i < MenuAction.MAIN.size(); i++) {
         MenuAction action = MenuAction.MAIN.get(i);
         if (action == MenuAction.QUIT) {
            y += 12.0F * s;
         }

         item(c, action, i, l.cx(), y, c.intro(0.4F + i * 0.05F));
         y += 56.0F * s;
      }

      float a = c.intro(0.7F);
      float iconY = sh - 26.0F * s;
      float iconX = sw - 26.0F * s - (ICONS.size() - 1) * 34.0F * s;

      for (int i = 0; i < ICONS.size(); i++) {
         icon(c, ICONS.get(i), iconX + i * 34.0F * s, iconY, a);
      }
   }

   private static void item(MenuContext c, MenuAction action, int index, float cx, float y, float a) {
      ImDrawList dl = c.bg();
      ImFont font = c.itemFont();
      float s = c.scale();
      float w = 320.0F * s;
      float h = 46.0F * s;
      float x = cx - w * 0.5F;
      boolean enabled = c.enabled(action);
      float k = a > 0.5F ? c.control(action, x, y, w, h) : c.hover(action, false);
      // Tactile press: the pill sinks while held and springs back with a slight overshoot.
      boolean held = enabled && k > 0.5F && imgui.ImGui.isMouseDown(0);
      float press = Math.max(0.0F, net.spectre.client.gui.ui.Anim.spring(net.spectre.client.gui.ui.Anim.key(0x4D50, action.ordinal()), held ? 1.0F : 0.0F, net.spectre.client.gui.ui.Anim.TACTILE));
      float inset = 3.0F * s * press;
      x += inset;
      y += inset * 0.5F;
      w -= inset * 2.0F;
      h -= inset;
      if (enabled && k > 0.01F) {
         net.spectre.client.gui.ui.GhostPaint.glow(dl, x, y, w, h, h * 0.5F, 10.0F * s * k, net.spectre.client.gui.ui.SpectralTokens.alpha(net.spectre.client.gui.ui.SpectralTokens.accent(), 0.22F * k * a * net.spectre.client.gui.ui.SpectralTokens.glow()));
      }

      DuskPill.frame(c, x, y, w, h, k, DuskEmblem.buttonPass(c, index), a);
      net.spectre.client.gui.ui.GhostPaint.drawRipple(dl, net.spectre.client.gui.ui.Anim.key(0x4D4E, action.ordinal()), x, y, w, h, net.spectre.client.gui.ui.SpectralTokens.accent());
      String label = action.label();
      float track = 2.0F * s;
      float ty = y + (h - c.textHeight(font)) * 0.5F + (1.0F - a) * 8.0F * s;
      float tx = cx - c.spacedWidth(font, label, track) * 0.5F;
      int color = enabled ? SpectreDraw.mix(0xFFE9E6F5, 0xFFE9E6F5, k) : 0xFF8C86A6;
      float alpha = (action == MenuAction.QUIT ? 0.75F + 0.25F * k : 0.85F + 0.14999998F * k) * a;
      if (enabled) {
         DuskPill.halo(c, font, tx, ty, track, k * a, label);
      }

      c.spacedText(dl, font, tx + s, ty + 1.5F * s, track, -16777216, 0.55F * a, label);
      c.spacedText(dl, font, tx, ty, track, color, alpha, label);
   }

   private static void icon(MenuContext c, MenuAction action, float cx, float cy, float a) {
      float s = c.scale();
      float r = 9.0F * s;
      float reach = 34.0F * s * 0.5F;
      float k = a > 0.5F ? c.control(action, cx - reach, cy - reach, reach * 2.0F, reach * 2.0F) : c.hover(action, false);
      MenuIcons.draw(c.bg(), action, cx, cy, r, SpectreDraw.col(SpectreDraw.mix(-4676216, 0xFFE9E6F5, k), a), Math.max(1.2F, 1.5F * s));
      if (k > 0.5F) {
         String name = action == MenuAction.ACCOUNTS ? action.label() + "  ·  " + c.displayName() : action.label();
         c.tooltip(name, Math.min(cx, c.width() - c.textWidth(c.tagFont(), name) * 0.5F - 18.0F * s), cy - reach - c.textHeight(c.tagFont()) - 14.0F * s);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Layout(float cx, float sunY, float sunR, float nameY, float itemsY) {
   }
}
