package net.spectre.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;
import java.util.List;
import java.util.Locale;
import net.spectre.client.gui.ui.Anim;
import net.spectre.client.gui.ui.GhostPaint;
import net.spectre.client.gui.ui.SpectralTokens;
import net.spectre.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SpectreWidgets {
   private static final float POPUP_MAX_H = 280.0F;

   private SpectreWidgets() {
   }

   public static float s(float v) {
      return v * ImGuiManager.getInstance().uiScale();
   }

   public static float animate(int key, boolean on, float speed) {
      return ease(key, on ? 1.0F : 0.0F, speed);
   }

   /** Under-damped spring (slight overshoot) for presses, toggles and indicators. */
   public static float tactile(int key, boolean on) {
      return Anim.spring(Anim.key(0x5350, key), on ? 1.0F : 0.0F, Anim.TACTILE);
   }

   public static float tactileVelocity(int key) {
      return Anim.velocity(Anim.key(0x5350, key));
   }

   private static float ease(int key, float target, float speed) {
      return Anim.spring(Anim.key(0x5357, key), target, speed >= 14.0F ? Anim.SNAPPY : Anim.GENTLE);
   }

   public static void text(ImDrawList dl, ImFont font, float x, float y, int color, String text) {
      text(dl, font, x, y, color, text, false);
   }

   public static void text(ImDrawList dl, ImFont font, float x, float y, int color, String text, boolean guard) {
      int size = font != null ? Math.round(font.getFontSize()) : 0;
      HudChrome.text(dl, font, size, x, y, c(color), text, guard);
   }

   public static float textWidth(ImFont font, String text) {
      return font != null ? font.calcTextSizeAX(font.getFontSize(), Float.MAX_VALUE, 0.0F, text) : ImGui.calcTextSize(text).x;
   }

   public static float lineHeight(ImFont font) {
      return font != null ? font.getFontSize() : ImGui.getTextLineHeight();
   }

   public static String ellipsize(ImFont font, String text, float maxW) {
      if (textWidth(font, text) <= maxW) {
         return text;
      } else {
         int end = text.length();

         while (end > 0 && textWidth(font, text.substring(0, end) + "...") > maxW) {
            end--;
         }

         return text.substring(0, end).stripTrailing() + "...";
      }
   }

   public static int c(int color) {
      return ImGui.getColorU32i(color);
   }

   public static int withAlpha(int color, float alpha) {
      int a = Math.round((color >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, alpha)));
      return a << 24 | color & 16777215;
   }

   public static void drawSwitch(ImDrawList dl, float x, float y, float w, float h, float t) {
      drawSwitch(dl, x, y, w, h, t, 0.0F);
   }

   /**
    * Spectral switch. {@code t} may overshoot 0..1 (spring); {@code velocity} squashes and stretches
    * the knob along the direction of travel.
    */
   public static void drawSwitch(ImDrawList dl, float x, float y, float w, float h, float t, float velocity) {
      float r = h * 0.5F;
      float on = Math.max(0.0F, Math.min(1.0F, t));
      if (on > 0.01F) {
         GhostPaint.ecto(dl, x, y, w, h, r, on);
      }

      dl.addRectFilled(x, y, x + w, y + h, c(SpectralTokens.SWITCH_OFF), r);
      if (on > 0.01F) {
         GhostPaint.gradient(dl, x, y, w, h, r, c(SpectralTokens.alpha(SpectralTokens.accent(), on)), c(SpectralTokens.alpha(SpectralTokens.accentDeep(), on)));
      }

      dl.addRect(x + 0.5F, y + 0.5F, x + w - 0.5F, y + h - 0.5F, c(SpectralTokens.alpha(SpectralTokens.LINE_STRONG, 1.0F - 0.6F * on)), r, 0, 1.0F);
      float knobR = r - s(2.5F);
      float cx = x + r + (w - 2.0F * r) * t;
      float stretch = Math.min(0.45F, Math.abs(velocity) * 0.012F);
      float kw = knobR * (1.0F + stretch);
      float kh = knobR * (1.0F - stretch * 0.45F);
      float cy = y + r;
      GhostPaint.shadow(dl, cx - kw, cy - kh, kw * 2.0F, kh * 2.0F, kh, s(4.0F), s(1.0F), c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.7F)));
      dl.addRectFilled(cx - kw, cy - kh, cx + kw, cy + kh, c(0xFFFAF0F2), kh);
      if (on > 0.05F) {
         dl.addCircleFilled(cx, cy, knobR * 0.32F * on, c(SpectralTokens.alpha(SpectralTokens.accent(), on)), 16);
      }
   }

   public static boolean sliderFloat(String id, float[] value, float min, float max, float width) {
      return sliderFloat(id, value, min, max, width, null);
   }

   public static boolean sliderFloat(String id, float[] value, float min, float max, float width, SpectreWidgets.Snap snap) {
      float hitH = s(20.0F);
      float sx = ImGui.getCursorScreenPosX();
      float cy = ImGui.getCursorScreenPosY() + hitH * 0.5F;
      ImGui.invisibleButton(id, width, hitH);
      boolean hovered = ImGui.isItemHovered();
      boolean held = ImGui.isItemActive();
      int itemId = ImGui.getItemID();
      float focus = animate(itemId, hovered || held, 16.0F);
      float grab = tactile(itemId, held);
      boolean changed = false;
      float inset = s(8.0F);
      float x0 = sx + inset;
      float x1 = sx + width - inset;
      if (held) {
         float t = Math.max(0.0F, Math.min(1.0F, (ImGui.getIO().getMousePosX() - x0) / Math.max(1.0F, x1 - x0)));
         float nv = min + t * (max - min);
         if (snap != null) {
            nv = snap.apply(nv);
         }

         if (nv != value[0]) {
            value[0] = nv;
            changed = true;
            Anim.kick(Anim.key(0x534B, itemId), 9.0F);
         }
      }

      // The drawn fill follows the value with a spring; the stored value is exact.
      float target = frac(value[0], min, max);
      float tx = Anim.spring(Anim.key(0x5356, itemId), target, held ? Anim.SNAPPY : Anim.TACTILE);
      float tick = Anim.spring(Anim.key(0x534B, itemId), 0.0F, Anim.TACTILE);
      ImDrawList dl = ImGui.getWindowDrawList();
      float trackH = s(4.0F) + s(1.5F) * focus;
      float fillX = x0 + Math.max(0.0F, Math.min(1.0F, tx)) * (x1 - x0);
      dl.addRectFilled(x0, cy - trackH * 0.5F, x1, cy + trackH * 0.5F, c(SpectralTokens.SWITCH_OFF), trackH);
      if (fillX > x0 + 0.5F) {
         GhostPaint.glow(dl, x0, cy - trackH * 0.5F, fillX - x0, trackH, trackH * 0.5F, s(5.0F) * (0.4F + 0.6F * focus),
            c(SpectralTokens.alpha(SpectralTokens.accent(), 0.35F * SpectralTokens.glow())));
         dl.addRectFilledMultiColor(x0, cy - trackH * 0.5F, fillX, cy + trackH * 0.5F,
            c(SpectralTokens.accentDeep()), c(SpectralTokens.accent()), c(SpectralTokens.accent()), c(SpectralTokens.accentDeep()));
      }

      float knobR = s(6.5F) + s(2.0F) * focus + s(1.5F) * Math.max(0.0F, grab) + s(0.6F) * Math.abs(tick);
      if (focus > 0.01F) {
         GhostPaint.orb(dl, fillX, cy, knobR + s(9.0F) * focus, c(SpectralTokens.alpha(SpectralTokens.accent(), 0.3F * focus * SpectralTokens.glow())));
      }

      GhostPaint.shadow(dl, fillX - knobR, cy - knobR, knobR * 2.0F, knobR * 2.0F, knobR, s(5.0F), s(1.5F), c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.75F)));
      dl.addCircleFilled(fillX, cy, knobR, c(0xFFFAF0F2), 24);
      dl.addCircleFilled(fillX, cy, knobR * 0.38F, c(SpectralTokens.accent()), 16);
      return changed;
   }

   public static boolean chipButton(String id, String label, float w, float h, boolean active) {
      float sx = ImGui.getCursorScreenPosX();
      float sy = ImGui.getCursorScreenPosY();
      boolean clicked = ImGui.invisibleButton(id, w, h);
      boolean hovered = ImGui.isItemHovered();
      boolean held = ImGui.isItemActive();
      int itemId = ImGui.getItemID();
      float hover = animate(itemId, hovered, 18.0F);
      float press = tactile(itemId, held);
      long key = Anim.key(0x5352, itemId);
      if (ImGui.isItemClicked(0)) {
         GhostPaint.ripple(key, ImGui.getIO().getMousePosX(), ImGui.getIO().getMousePosY());
      }

      ImDrawList dl = ImGui.getWindowDrawList();
      float inset = s(1.2F) * Math.max(0.0F, press);
      float x = sx + inset;
      float y = sy + inset - s(1.0F) * hover * (1.0F - Math.max(0.0F, Math.min(1.0F, press)));
      float bw = w - inset * 2.0F;
      float bh = h - inset * 2.0F;
      float r = s(8.0F);
      GhostPaint.shadow(dl, x, y, bw, bh, r, s(4.0F) + s(4.0F) * hover, s(1.0F) + s(1.5F) * hover, c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.6F)));
      if (active) {
         GhostPaint.ecto(dl, x, y, bw, bh, r, 0.55F + 0.45F * hover);
         GhostPaint.gradient(dl, x, y, bw, bh, r, c(SpectralTokens.mix(SpectralTokens.accent(), 0xFFFFFFFF, 0.12F * hover)), c(SpectralTokens.accentDeep()));
      } else {
         GhostPaint.gradient(dl, x, y, bw, bh, r, c(SpectralTokens.mix(SpectralTokens.RAISED, SpectralTokens.RAISED_HI, 0.4F + 0.6F * hover)), c(SpectralTokens.RAISED));
      }

      GhostPaint.rim(dl, x, y, bw, bh, r, c(SpectralTokens.alpha(SpectralTokens.RIM, active ? 0.9F : 0.5F + 0.5F * hover)));
      dl.addRect(x + 0.5F, y + 0.5F, x + bw - 0.5F, y + bh - 0.5F, c(active ? SpectralTokens.alpha(SpectralTokens.accent(), 0.6F) : SpectralTokens.LINE), r, 0, 1.0F);
      GhostPaint.drawRipple(dl, key, x, y, bw, bh, c(0xFFFFFFFF));
      ImFont font = ImGui.getFont();
      float tw = textWidth(font, label);
      text(dl, font, x + (bw - tw) * 0.5F, y + (bh - lineHeight(font)) * 0.5F, active ? SpectralTokens.abgr(0xFF07140F) : SpectralTokens.TEXT, label);
      return clicked;
   }

   /** Filled accent call-to-action button with glow, lift, press and ripple. */
   public static boolean primaryButton(String id, String label, float w, float h) {
      return chipButton(id, label, w, h, true);
   }

   public static boolean iconButton(String id, float size) {
      float sx = ImGui.getCursorScreenPosX();
      float sy = ImGui.getCursorScreenPosY();
      boolean clicked = ImGui.invisibleButton(id, size, size);
      int itemId = ImGui.getItemID();
      float hover = animate(itemId, ImGui.isItemHovered(), 18.0F);
      float press = Math.max(0.0F, tactile(itemId, ImGui.isItemActive()));
      ImDrawList dl = ImGui.getWindowDrawList();
      float r = size * 0.5F * (1.0F - 0.08F * press);
      float cx = sx + size * 0.5F;
      float cy = sy + size * 0.5F;
      if (hover > 0.01F) {
         GhostPaint.orb(dl, cx, cy, r + s(6.0F) * hover, c(SpectralTokens.alpha(SpectralTokens.accent(), 0.18F * hover * SpectralTokens.glow())));
      }

      dl.addCircleFilled(cx, cy, r, c(SpectralTokens.mix(SpectralTokens.RAISED, SpectralTokens.RAISED_HI, hover)), 32);
      dl.addCircle(cx, cy, r - 0.5F, c(SpectralTokens.alpha(SpectralTokens.LINE_STRONG, 0.6F + 0.4F * hover)), 32, 1.0F);
      return clicked;
   }

   public static int segmented(String id, String[] labels, int selected, float w, float h) {
      float localX = ImGui.getCursorPosX();
      float localY = ImGui.getCursorPosY();
      float bx = ImGui.getCursorScreenPosX();
      float by = ImGui.getCursorScreenPosY();
      float inner = s(3.0F);
      float r = s(8.0F);
      float segW = (w - inner * 2.0F) / labels.length;
      float pillX = Anim.spring(Anim.key(0x5347, ImGui.getID(id)), inner + selected * segW, Anim.TACTILE);
      ImDrawList dl = ImGui.getWindowDrawList();
      dl.addRectFilled(bx, by, bx + w, by + h, c(SpectralTokens.PANEL_2), r);
      dl.addRect(bx + 0.5F, by + 0.5F, bx + w - 0.5F, by + h - 0.5F, c(SpectralTokens.LINE), r, 0, 1.0F);
      float px = bx + pillX;
      GhostPaint.ecto(dl, px, by + inner, segW, h - inner * 2.0F, r - inner, 0.6F);
      GhostPaint.gradient(dl, px, by + inner, segW, h - inner * 2.0F, r - inner, c(SpectralTokens.accent()), c(SpectralTokens.accentDeep()));
      GhostPaint.rim(dl, px, by + inner, segW, h - inner * 2.0F, r - inner, c(SpectralTokens.RIM));
      ImFont font = ImGui.getFont();
      int clicked = -1;

      for (int i = 0; i < labels.length; i++) {
         ImGui.setCursorPos(localX + inner + i * segW, localY + inner);
         if (ImGui.invisibleButton(id + i, segW, h - inner * 2.0F)) {
            clicked = i;
         }

         float hover = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
         float centre = bx + inner + i * segW + segW * 0.5F;
         float cover = Math.max(0.0F, 1.0F - Math.abs(centre - (px + segW * 0.5F)) / (segW * 0.5F));
         int color = SpectralTokens.mix(SpectralTokens.mix(SpectralTokens.MUTED, SpectralTokens.TEXT, hover), SpectralTokens.abgr(0xFF07140F), cover);
         float tw = textWidth(font, labels[i]);
         text(dl, font, centre - tw * 0.5F, by + (h - lineHeight(font)) * 0.5F, color, labels[i]);
      }

      return clicked;
   }

   public static int dropdown(String id, String current, List<String> options, float w, float h) {
      return dropdown(id, current, current, options, w, h, null);
   }

   public static int dropdown(String id, String current, String label, List<String> options, float w, float h, ImString query) {
      String popupId = id + "_popup";
      float sx = ImGui.getCursorScreenPosX();
      float sy = ImGui.getCursorScreenPosY();
      if (ImGui.invisibleButton(id, w, h)) {
         ImGui.openPopup(popupId);
         if (query != null) {
            query.set("");
         }
      }

      boolean open = ImGui.isPopupOpen(popupId);
      float hover = animate(ImGui.getItemID(), ImGui.isItemHovered() || open, 18.0F);
      ImDrawList dl = ImGui.getWindowDrawList();
      float r = s(8.0F);
      if (open) {
         GhostPaint.ecto(dl, sx, sy, w, h, r, 0.5F);
      }

      GhostPaint.gradient(dl, sx, sy, w, h, r, c(SpectralTokens.mix(SpectralTokens.RAISED, SpectralTokens.RAISED_HI, 0.3F + 0.7F * hover)), c(SpectralTokens.RAISED));
      GhostPaint.rim(dl, sx, sy, w, h, r, c(SpectralTokens.alpha(SpectralTokens.RIM, 0.5F + 0.5F * hover)));
      if (open) {
         dl.addRect(sx, sy, sx + w, sy + h, c(withAlpha(GuiPalette.accent(), 0.8F)), r, 0, s(1.2F));
      }

      ImFont font = ImGui.getFont();
      text(dl, font, sx + s(10.0F), sy + (h - lineHeight(font)) * 0.5F, current.isEmpty() ? -5863796 : -661783, ellipsize(font, label, w - s(34.0F)));
      chevron(dl, sx + w - s(14.0F), sy + h * 0.5F, s(4.0F), -5863796, open);
      int picked = -1;
      float popupAnim = animate(ImGui.getID(popupId), open, 22.0F);
      float itemH = s(30.0F);
      float roomBelow = ImGui.getIO().getDisplaySizeY() - (sy + h);
      float slide = s(6.0F) * (1.0F - popupAnim);
      if (roomBelow < s(290.0F) && sy > roomBelow) {
         ImGui.setNextWindowPos(sx, sy - s(4.0F) + slide, 1, 0.0F, 1.0F);
      } else {
         ImGui.setNextWindowPos(sx, sy + h + s(4.0F) - slide);
      }

      ImGui.setNextWindowSizeConstraints(w, 0.0F, w, s(280.0F));
      ImGui.pushStyleVar(0, Math.max(0.05F, popupAnim));
      ImGui.pushStyleVar(2, s(4.0F), s(4.0F));
      ImGui.pushStyleVar(14, 0.0F, s(2.0F));
      if (ImGui.beginPopup(popupId, 8)) {
         float rowW = w - s(8.0F);
         String needle = "";
         if (query != null) {
            if (ImGui.isWindowAppearing()) {
               ImGui.setKeyboardFocusHere();
            }

            ImGui.pushStyleVar(11, s(8.0F), s(6.0F));
            ImGui.setNextItemWidth(rowW);
            ImGui.inputTextWithHint("##search", "Search...", query);
            boolean enter = submittedWithEnter();
            ImGui.popStyleVar();
            needle = query.get().trim().toLowerCase(Locale.ROOT);
            if (enter) {
               for (int i = 0; i < options.size() && picked < 0; i++) {
                  if (matches(options.get(i), needle)) {
                     picked = i;
                  }
               }

               if (picked >= 0) {
                  ImGui.closeCurrentPopup();
               }
            }
         }

         ImDrawList pdl = ImGui.getWindowDrawList();
         boolean any = false;

         for (int ix = 0; ix < options.size(); ix++) {
            String option = options.get(ix);
            if (matches(option, needle)) {
               any = true;
               float rx = ImGui.getCursorScreenPosX();
               float ry = ImGui.getCursorScreenPosY();
               if (ImGui.invisibleButton("##opt" + ix, rowW, itemH)) {
                  picked = ix;
                  ImGui.closeCurrentPopup();
               }

               boolean selected = option.equals(current);
               float rowHover = animate(ImGui.getItemID(), ImGui.isItemHovered(), 20.0F);
               if (rowHover > 0.01F) {
                  pdl.addRectFilled(rx, ry, rx + rowW, ry + itemH, c(withAlpha(-12443866, rowHover)), s(6.0F));
               }

               text(
                  pdl,
                  font,
                  rx + s(10.0F),
                  ry + (itemH - lineHeight(font)) * 0.5F,
                  selected ? GuiPalette.accent() : -661783,
                  ellipsize(font, option, rowW - s(34.0F))
               );
               if (selected) {
                  check(pdl, rx + rowW - s(14.0F), ry + itemH * 0.5F, s(5.0F), GuiPalette.accent());
               }
            }
         }

         if (!any) {
            float rxx = ImGui.getCursorScreenPosX();
            float ryx = ImGui.getCursorScreenPosY();
            ImGui.dummy(rowW, itemH);
            text(pdl, font, rxx + s(10.0F), ryx + (itemH - lineHeight(font)) * 0.5F, -5863796, options.isEmpty() ? "Nothing to pick yet" : "No matches");
         }

         ImGui.endPopup();
      }

      ImGui.popStyleVar(3);
      return picked;
   }

   private static boolean matches(String option, String needle) {
      return needle.isEmpty() || option.toLowerCase(Locale.ROOT).contains(needle);
   }

   public static boolean submittedWithEnter() {
      return ImGui.isItemDeactivated() && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
   }

   public static void chevron(ImDrawList dl, float cx, float cy, float r, int color, boolean up) {
      float dy = up ? -r * 0.5F : r * 0.5F;
      dl.addLine(cx - r, cy - dy, cx, cy + dy, c(color), s(1.6F));
      dl.addLine(cx, cy + dy, cx + r, cy - dy, c(color), s(1.6F));
   }

   public static void chevronLeft(ImDrawList dl, float cx, float cy, float r, int color) {
      dl.addLine(cx + r * 0.5F, cy - r, cx - r * 0.5F, cy, c(color), s(2.0F));
      dl.addLine(cx - r * 0.5F, cy, cx + r * 0.5F, cy + r, c(color), s(2.0F));
   }

   public static void chevronRight(ImDrawList dl, float cx, float cy, float r, int color) {
      dl.addLine(cx - r * 0.5F, cy - r, cx + r * 0.5F, cy, c(color), s(2.0F));
      dl.addLine(cx + r * 0.5F, cy, cx - r * 0.5F, cy + r, c(color), s(2.0F));
   }

   public static void chevronRotated(ImDrawList dl, float cx, float cy, float r, int color, float angle) {
      float dx = (float)Math.cos(angle);
      float dy = (float)Math.sin(angle);
      float tipX = cx + dx * r * 0.5F;
      float tipY = cy + dy * r * 0.5F;
      float backX = cx - dx * r * 0.5F;
      float backY = cy - dy * r * 0.5F;
      dl.addLine(backX - dy * r, backY + dx * r, tipX, tipY, c(color), s(1.6F));
      dl.addLine(tipX, tipY, backX + dy * r, backY - dx * r, c(color), s(1.6F));
   }

   public static void check(ImDrawList dl, float cx, float cy, float r, int color) {
      dl.addLine(cx - r, cy, cx - r * 0.3F, cy + r * 0.7F, c(color), s(1.8F));
      dl.addLine(cx - r * 0.3F, cy + r * 0.7F, cx + r, cy - r * 0.7F, c(color), s(1.8F));
   }

   public static void cross(ImDrawList dl, float cx, float cy, float r, int color) {
      dl.addLine(cx - r, cy - r, cx + r, cy + r, c(color), s(1.6F));
      dl.addLine(cx - r, cy + r, cx + r, cy - r, c(color), s(1.6F));
   }

   public static void magnifier(ImDrawList dl, float cx, float cy, float r, int color) {
      float cr = r * 0.62F;
      dl.addCircle(cx - r * 0.15F, cy - r * 0.15F, cr, c(color), 20, s(1.6F));
      dl.addLine(cx + r * 0.3F, cy + r * 0.3F, cx + r, cy + r, c(color), s(1.8F));
   }

   public static void crosshair(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      dl.addCircle(cx, cy, r * 0.62F, c(color), 20, t);
      dl.addLine(cx, cy - r, cx, cy - r * 0.35F, c(color), t);
      dl.addLine(cx, cy + r * 0.35F, cx, cy + r, c(color), t);
      dl.addLine(cx - r, cy, cx - r * 0.35F, cy, c(color), t);
      dl.addLine(cx + r * 0.35F, cy, cx + r, cy, c(color), t);
   }

   public static void grid(ImDrawList dl, float cx, float cy, float r, int color) {
      float g = r * 0.18F;
      float c = r * 0.8F;
      dl.addRectFilled(cx - c, cy - c, cx - g, cy - g, c(color), s(1.5F));
      dl.addRectFilled(cx + g, cy - c, cx + c, cy - g, c(color), s(1.5F));
      dl.addRectFilled(cx - c, cy + g, cx - g, cy + c, c(color), s(1.5F));
      dl.addRectFilled(cx + g, cy + g, cx + c, cy + c, c(color), s(1.5F));
   }

   public static void ring(ImDrawList dl, float cx, float cy, float r, int color) {
      dl.addCircle(cx, cy, r * 0.62F, c(color), 24, r * 0.42F);
   }

   public static void eye(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      dl.addBezierQuadratic(cx - r, cy, cx, cy - r * 1.15F, cx + r, cy, c(color), t, 16);
      dl.addBezierQuadratic(cx - r, cy, cx, cy + r * 1.15F, cx + r, cy, c(color), t, 16);
      dl.addCircleFilled(cx, cy, r * 0.3F, c(color), 16);
   }

   public static void monitor(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      dl.addRect(cx - r, cy - r * 0.8F, cx + r, cy + r * 0.4F, c(color), s(2.0F), 0, t);
      dl.addLine(cx, cy + r * 0.4F, cx, cy + r * 0.8F, c(color), t);
      dl.addLine(cx - r * 0.5F, cy + r * 0.85F, cx + r * 0.5F, cy + r * 0.85F, c(color), t);
   }

   public static void papyrus(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      float rollW = r * 0.36F;
      float rollH = r * 0.85F;
      float sheetH = r * 0.62F;
      dl.addRect(cx - r, cy - rollH, cx - r + rollW, cy + rollH, c(color), rollW * 0.5F, 0, t);
      dl.addRect(cx + r - rollW, cy - rollH, cx + r, cy + rollH, c(color), rollW * 0.5F, 0, t);
      dl.addLine(cx - r + rollW, cy - sheetH, cx + r - rollW, cy - sheetH, c(color), t);
      dl.addLine(cx - r + rollW, cy + sheetH, cx + r - rollW, cy + sheetH, c(color), t);
      dl.addLine(cx - r * 0.3F, cy - r * 0.15F, cx + r * 0.3F, cy - r * 0.15F, c(color), t);
      dl.addLine(cx - r * 0.3F, cy + r * 0.22F, cx + r * 0.12F, cy + r * 0.22F, c(color), t);
   }

   public static void house(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      dl.addLine(cx - r, cy - r * 0.05F, cx, cy - r * 0.95F, c(color), t);
      dl.addLine(cx, cy - r * 0.95F, cx + r, cy - r * 0.05F, c(color), t);
      dl.addRect(cx - r * 0.7F, cy - r * 0.3F, cx + r * 0.7F, cy + r * 0.85F, c(color), 0.0F, 0, t);
      dl.addRectFilled(cx - r * 0.2F, cy + r * 0.25F, cx + r * 0.2F, cy + r * 0.85F, c(color));
   }

   public static void ankh(ImDrawList dl, float cx, float cy, float r, int color) {
      float t = s(1.5F);
      float barY = cy - r * 0.15F;
      float top = cy - r;
      dl.addBezierCubic(cx, barY, cx - r * 0.62F, barY - r * 0.3F, cx - r * 0.5F, top, cx, top, c(color), t, 12);
      dl.addBezierCubic(cx, barY, cx + r * 0.62F, barY - r * 0.3F, cx + r * 0.5F, top, cx, top, c(color), t, 12);
      dl.addLine(cx - r * 0.72F, barY, cx + r * 0.72F, barY, c(color), t);
      dl.addLine(cx, barY, cx, cy + r, c(color), t);
   }

   private static float frac(float v, float min, float max) {
      float r = max - min;
      return r < 1.0E-6F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (v - min) / r));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Snap {
      float apply(float var1);
   }
}
