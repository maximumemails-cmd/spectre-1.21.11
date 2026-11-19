package net.spectre.client.gui.ui;

import imgui.ImDrawList;
import imgui.ImFont;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Letter-spaced labels and glowing headings without per-glyph allocation. */
@Environment(EnvType.CLIENT)
public final class SpectralText {
   private static final String[] ASCII = new String[128];

   static {
      for (char c = 0; c < ASCII.length; c++) {
         ASCII[c] = String.valueOf(c);
      }
   }

   private SpectralText() {
   }

   private static String glyph(char c) {
      return c < ASCII.length ? ASCII[c] : String.valueOf(c);
   }

   private static float advance(ImFont font, float size, char c) {
      return font.getCharAdvance(c) * size / font.getFontSize();
   }

   public static float spacedWidth(ImFont font, String text, float spacing) {
      if (font == null || text.isEmpty()) {
         return 0.0F;
      } else {
         float size = font.getFontSize();
         float w = 0.0F;

         for (int i = 0; i < text.length(); i++) {
            w += advance(font, size, text.charAt(i));
         }

         return w + spacing * (text.length() - 1);
      }
   }

   /** Left-aligned, letter-spaced text (ImGui ABGR colour). */
   public static void spaced(ImDrawList dl, ImFont font, float x, float y, String text, float spacing, int col) {
      if (font == null || text.isEmpty() || (col >>> 24) == 0) {
         return;
      }

      float size = font.getFontSize();

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c != ' ') {
            dl.addText(font, Math.round(size), x, y, col, glyph(c));
         }

         x += advance(font, size, c) + spacing;
      }
   }

   /** Heading with a faint ectoplasm halo behind it. */
   public static void glowing(ImDrawList dl, ImFont font, float x, float y, String text, int col, float glow) {
      if (font == null || text.isEmpty()) {
         return;
      }

      float size = font.getFontSize();
      float w = font.calcTextSizeAX(size, Float.MAX_VALUE, 0.0F, text);
      if (glow > 0.01F) {
         GhostPaint.glow(dl, x, y + size * 0.3F, w, size * 0.45F, size * 0.22F, size * 0.9F, SpectralTokens.alpha(SpectralTokens.accent(), 0.22F * glow));
      }

      dl.addText(font, Math.round(size), x, y, col, text);
   }
}
