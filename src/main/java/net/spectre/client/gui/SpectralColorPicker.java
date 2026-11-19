package net.spectre.client.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.spectre.client.gui.ui.Anim;
import net.spectre.client.gui.ui.GhostPaint;
import net.spectre.client.gui.ui.SpectralTokens;

/**
 * Spectral colour picker: saturation/value field, hue and alpha rails with spring-tracked handles,
 * a hex field, a before/after preview and recently used swatches. Values are RGBA floats (0..1).
 */
@Environment(EnvType.CLIENT)
final class SpectralColorPicker {
   private static final int RECENT = 8;
   private static final int[] RECENT_ARGB = new int[RECENT];
   private static int recentCount;
   private static final Int2FloatOpenHashMap HUES = new Int2FloatOpenHashMap();
   private static final ImString HEX = new ImString(16);
   private static final float[] HSV = new float[3];
   private static final float[] RGB = new float[3];
   private static int hexFor = 0;
   private static int startArgb;
   private static int startFor;

   private SpectralColorPicker() {
   }

   /** Remember a colour in the recent swatch row (call when a picker closes). */
   static void remember(float[] rgba) {
      int argb = toArgb(rgba);

      for (int i = 0; i < recentCount; i++) {
         if (RECENT_ARGB[i] == argb) {
            System.arraycopy(RECENT_ARGB, 0, RECENT_ARGB, 1, i);
            RECENT_ARGB[0] = argb;
            return;
         }
      }

      System.arraycopy(RECENT_ARGB, 0, RECENT_ARGB, 1, RECENT - 1);
      RECENT_ARGB[0] = argb;
      recentCount = Math.min(RECENT, recentCount + 1);
   }

   static boolean draw(String id, float[] rgba, float width) {
      int key = ImGui.getID(id);
      if (startFor != key) {
         startFor = key;
         startArgb = toArgb(rgba);
      }

      boolean changed = false;
      rgbToHsv(rgba[0], rgba[1], rgba[2], HSV);
      float hue = HUES.containsKey(key) ? HUES.get(key) : HSV[0];
      if (HSV[1] > 0.001F && HSV[2] > 0.001F) {
         hue = HSV[0];
      }

      float s = HSV[1];
      float v = HSV[2];
      ImDrawList dl = ImGui.getWindowDrawList();
      float r = SpectreWidgets.s(4.0F);
      // ---- saturation / value field ----
      float fx = ImGui.getCursorScreenPosX();
      float fy = ImGui.getCursorScreenPosY();
      float fw = width;
      float fh = width * 0.66F;
      ImGui.invisibleButton(id + "_sv", fw, fh);
      boolean svActive = ImGui.isItemActive();
      if (svActive) {
         s = clamp((ImGui.getIO().getMousePosX() - fx) / fw);
         v = 1.0F - clamp((ImGui.getIO().getMousePosY() - fy) / fh);
         changed = true;
      }

      hsvToRgb(hue, 1.0F, 1.0F, RGB);
      int hueCol = 0xFF000000 | Math.round(RGB[2] * 255) << 16 | Math.round(RGB[1] * 255) << 8 | Math.round(RGB[0] * 255);
      GhostPaint.shadow(dl, fx, fy, fw, fh, r, SpectreWidgets.s(8.0F), SpectreWidgets.s(2.0F), SpectreWidgets.c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.6F)));
      dl.addRectFilledMultiColor(fx, fy, fx + fw, fy + fh, SpectreWidgets.c(0xFFFFFFFF), SpectreWidgets.c(hueCol), SpectreWidgets.c(hueCol), SpectreWidgets.c(0xFFFFFFFF));
      dl.addRectFilledMultiColor(fx, fy, fx + fw, fy + fh, 0, 0, SpectreWidgets.c(0xFF000000), SpectreWidgets.c(0xFF000000));
      dl.addRect(fx + 0.5F, fy + 0.5F, fx + fw - 0.5F, fy + fh - 0.5F, SpectreWidgets.c(SpectralTokens.LINE_STRONG), r, 0, 1.0F);
      float hx = Anim.spring(Anim.key(0x4350, key), fx + s * fw, svActive ? Anim.SNAPPY : Anim.TACTILE);
      float hy = Anim.spring(Anim.key(0x4351, key), fy + (1.0F - v) * fh, svActive ? Anim.SNAPPY : Anim.TACTILE);
      float grab = Math.max(0.0F, SpectreWidgets.tactile(key ^ 0x51, svActive));
      float hr = SpectreWidgets.s(7.0F) + SpectreWidgets.s(2.0F) * grab;
      GhostPaint.shadow(dl, hx - hr, hy - hr, hr * 2, hr * 2, hr, SpectreWidgets.s(4.0F), SpectreWidgets.s(1.0F), SpectreWidgets.c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.8F)));
      dl.addCircle(hx, hy, hr, SpectreWidgets.c(0xFFFFFFFF), 24, SpectreWidgets.s(2.5F));
      ImGui.dummy(0.0F, SpectreWidgets.s(6.0F));
      // ---- hue rail ----
      float railH = SpectreWidgets.s(14.0F);
      float rx = ImGui.getCursorScreenPosX();
      float ry = ImGui.getCursorScreenPosY();
      ImGui.invisibleButton(id + "_hue", fw, railH);
      boolean hueActive = ImGui.isItemActive();
      if (hueActive) {
         hue = clamp((ImGui.getIO().getMousePosX() - rx) / fw);
         changed = true;
      }

      for (int i = 0; i < 6; i++) {
         float x0 = rx + fw * i / 6.0F;
         float x1 = rx + fw * (i + 1) / 6.0F;
         int c0 = hueSwatch(i / 6.0F);
         int c1 = hueSwatch((i + 1) / 6.0F);
         dl.addRectFilledMultiColor(x0, ry, x1, ry + railH, SpectreWidgets.c(c0), SpectreWidgets.c(c1), SpectreWidgets.c(c1), SpectreWidgets.c(c0));
      }

      rail(dl, rx, ry, fw, railH, Anim.spring(Anim.key(0x4352, key), hue, hueActive ? Anim.SNAPPY : Anim.TACTILE), hueActive, key ^ 0x52);
      ImGui.dummy(0.0F, SpectreWidgets.s(4.0F));
      // ---- alpha rail ----
      float ax = ImGui.getCursorScreenPosX();
      float ay = ImGui.getCursorScreenPosY();
      ImGui.invisibleButton(id + "_alpha", fw, railH);
      boolean alphaActive = ImGui.isItemActive();
      float alpha = rgba[3];
      if (alphaActive) {
         alpha = clamp((ImGui.getIO().getMousePosX() - ax) / fw);
         changed = true;
      }

      float cell = railH * 0.5F;

      for (int i = 0; i * cell < fw; i++) {
         for (int j = 0; j < 2; j++) {
            if ((i + j & 1) == 0) {
               dl.addRectFilled(ax + i * cell, ay + j * cell, Math.min(ax + fw, ax + (i + 1) * cell), ay + (j + 1) * cell, SpectreWidgets.c(0xFF3A3450));
            } else {
               dl.addRectFilled(ax + i * cell, ay + j * cell, Math.min(ax + fw, ax + (i + 1) * cell), ay + (j + 1) * cell, SpectreWidgets.c(0xFF221C34));
            }
         }
      }

      hsvToRgb(hue, s, v, RGB);
      int solid = 0xFF000000 | Math.round(RGB[2] * 255) << 16 | Math.round(RGB[1] * 255) << 8 | Math.round(RGB[0] * 255);
      dl.addRectFilledMultiColor(ax, ay, ax + fw, ay + railH, SpectreWidgets.c(solid & 0x00FFFFFF), SpectreWidgets.c(solid), SpectreWidgets.c(solid), SpectreWidgets.c(solid & 0x00FFFFFF));
      rail(dl, ax, ay, fw, railH, Anim.spring(Anim.key(0x4353, key), alpha, alphaActive ? Anim.SNAPPY : Anim.TACTILE), alphaActive, key ^ 0x53);
      ImGui.dummy(0.0F, SpectreWidgets.s(8.0F));
      if (changed) {
         HUES.put(key, hue);
         hsvToRgb(hue, s, v, RGB);
         rgba[0] = RGB[0];
         rgba[1] = RGB[1];
         rgba[2] = RGB[2];
         rgba[3] = alpha;
      }

      // ---- preview + hex ----
      float px = ImGui.getCursorScreenPosX();
      float py = ImGui.getCursorScreenPosY();
      float sw = SpectreWidgets.s(46.0F);
      float sh = SpectreWidgets.s(30.0F);
      ImGui.dummy(sw, sh);
      int before = SpectralTokens.abgr(startArgb);
      int after = SpectralTokens.abgr(toArgb(rgba));
      dl.addRectFilled(px, py, px + sw * 0.5F, py + sh, SpectreWidgets.c(before), SpectreWidgets.s(8.0F), 5);
      dl.addRectFilled(px + sw * 0.5F, py, px + sw, py + sh, SpectreWidgets.c(after), SpectreWidgets.s(8.0F), 10);
      dl.addRect(px + 0.5F, py + 0.5F, px + sw - 0.5F, py + sh - 0.5F, SpectreWidgets.c(SpectralTokens.LINE_STRONG), SpectreWidgets.s(8.0F), 0, 1.0F);
      ImGui.sameLine(0.0F, SpectreWidgets.s(10.0F));
      if (hexFor != key || !ImGui.isAnyItemActive()) {
         if (hexFor != key || !HEX.get().equalsIgnoreCase(hex(rgba))) {
            HEX.set(hex(rgba));
         }

         hexFor = key;
      }

      ImGui.setNextItemWidth(fw - sw - SpectreWidgets.s(10.0F));
      ImGui.pushStyleVar(11, SpectreWidgets.s(10.0F), SpectreWidgets.s(7.0F));
      if (ImGui.inputTextWithHint(id + "_hex", "#RRGGBBAA", HEX, 0) && parseHex(HEX.get(), rgba)) {
         HUES.remove(key);
         changed = true;
      }

      ImGui.popStyleVar();
      // ---- recent swatches ----
      if (recentCount > 0) {
         ImGui.dummy(0.0F, SpectreWidgets.s(4.0F));
         float size = SpectreWidgets.s(22.0F);
         float gap = SpectreWidgets.s(6.0F);

         for (int i = 0; i < recentCount; i++) {
            if (i > 0) {
               ImGui.sameLine(0.0F, gap);
            }

            float sx = ImGui.getCursorScreenPosX();
            float sy = ImGui.getCursorScreenPosY();
            ImGui.pushID(i);
            if (ImGui.invisibleButton("##recent", size, size)) {
               fromArgb(RECENT_ARGB[i], rgba);
               HUES.remove(key);
               changed = true;
            }

            float hover = SpectreWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
            ImGui.popID();
            float lift = SpectreWidgets.s(1.5F) * hover;
            GhostPaint.shadow(dl, sx, sy - lift, size, size, size * 0.3F, SpectreWidgets.s(3.0F) + SpectreWidgets.s(3.0F) * hover, SpectreWidgets.s(1.0F), SpectreWidgets.c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.6F)));
            dl.addRectFilled(sx, sy - lift, sx + size, sy + size - lift, SpectreWidgets.c(SpectralTokens.abgr(RECENT_ARGB[i])), size * 0.3F);
            dl.addRect(sx + 0.5F, sy - lift + 0.5F, sx + size - 0.5F, sy + size - lift - 0.5F, SpectreWidgets.c(SpectralTokens.mix(SpectralTokens.LINE_STRONG, 0xFFFFFFFF, 0.5F * hover)), size * 0.3F, 0, 1.0F);
         }
      }

      return changed;
   }

   private static void rail(ImDrawList dl, float x, float y, float w, float h, float t, boolean active, int key) {
      float r = h * 0.5F;
      dl.addRect(x + 0.5F, y + 0.5F, x + w - 0.5F, y + h - 0.5F, SpectreWidgets.c(SpectralTokens.LINE_STRONG), r, 0, 1.0F);
      float grab = Math.max(0.0F, SpectreWidgets.tactile(key, active));
      float cx = x + clamp(t) * w;
      float kr = r + SpectreWidgets.s(2.0F) + SpectreWidgets.s(1.5F) * grab;
      GhostPaint.shadow(dl, cx - kr, y + r - kr, kr * 2, kr * 2, kr, SpectreWidgets.s(4.0F), SpectreWidgets.s(1.0F), SpectreWidgets.c(SpectralTokens.alpha(SpectralTokens.SHADOW, 0.8F)));
      dl.addCircle(cx, y + r, kr, SpectreWidgets.c(0xFFFFFFFF), 24, SpectreWidgets.s(2.5F));
   }

   private static int hueSwatch(float h) {
      hsvToRgb(h, 1.0F, 1.0F, RGB);
      return 0xFF000000 | Math.round(RGB[2] * 255) << 16 | Math.round(RGB[1] * 255) << 8 | Math.round(RGB[0] * 255);
   }

   private static float clamp(float v) {
      return v < 0.0F ? 0.0F : Math.min(1.0F, v);
   }

   static int toArgb(float[] rgba) {
      return Math.round(clamp(rgba[3]) * 255) << 24 | Math.round(clamp(rgba[0]) * 255) << 16 | Math.round(clamp(rgba[1]) * 255) << 8 | Math.round(clamp(rgba[2]) * 255);
   }

   private static void fromArgb(int argb, float[] rgba) {
      rgba[0] = (argb >> 16 & 0xFF) / 255.0F;
      rgba[1] = (argb >> 8 & 0xFF) / 255.0F;
      rgba[2] = (argb & 0xFF) / 255.0F;
      rgba[3] = (argb >>> 24) / 255.0F;
   }

   private static String hex(float[] rgba) {
      int argb = toArgb(rgba);
      return String.format(Locale.ROOT, "#%06X%02X", argb & 0xFFFFFF, argb >>> 24);
   }

   private static boolean parseHex(String text, float[] rgba) {
      String t = text.trim();
      if (t.startsWith("#")) {
         t = t.substring(1);
      }

      if (t.length() != 6 && t.length() != 8) {
         return false;
      } else {
         try {
            long v = Long.parseLong(t, 16);
            int rgb = (int)(t.length() == 8 ? v >>> 8 : v);
            int a = t.length() == 8 ? (int)(v & 0xFF) : Math.round(rgba[3] * 255);
            fromArgb(a << 24 | rgb & 0xFFFFFF, rgba);
            return true;
         } catch (NumberFormatException e) {
            return false;
         }
      }
   }

   private static void rgbToHsv(float r, float g, float b, float[] out) {
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      float h = 0.0F;
      if (d > 1.0E-6F) {
         if (max == r) {
            h = (g - b) / d % 6.0F;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            h++;
         }
      }

      out[0] = h;
      out[1] = max <= 1.0E-6F ? 0.0F : d / max;
      out[2] = max;
   }

   private static void hsvToRgb(float h, float s, float v, float[] out) {
      float hh = (h % 1.0F + 1.0F) % 1.0F * 6.0F;
      int i = (int)Math.floor(hh);
      float f = hh - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - s * f);
      float t = v * (1.0F - s * (1.0F - f));
      switch (i % 6) {
         case 0 -> { out[0] = v; out[1] = t; out[2] = p; }
         case 1 -> { out[0] = q; out[1] = v; out[2] = p; }
         case 2 -> { out[0] = p; out[1] = v; out[2] = t; }
         case 3 -> { out[0] = p; out[1] = q; out[2] = v; }
         case 4 -> { out[0] = t; out[1] = p; out[2] = v; }
         default -> { out[0] = v; out[1] = p; out[2] = q; }
      }
   }

   static {
      HUES.defaultReturnValue(0.0F);
   }
}
