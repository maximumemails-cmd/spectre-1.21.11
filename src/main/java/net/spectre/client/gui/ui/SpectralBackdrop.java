package net.spectre.client.gui.ui;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Full-screen atmosphere behind Spectre screens: a void dim, the shader fog/vignette layer and a
 * field of slow ectoplasm wisps. Wisp positions are pure functions of time and index, so there is
 * no per-frame state or allocation.
 */
@Environment(EnvType.CLIENT)
public final class SpectralBackdrop {
   private static final int WISPS = 40;

   private SpectralBackdrop() {
   }

   public static void draw(ImDrawList dl, float w, float h, float alpha, float dim, float scale) {
      if (alpha <= 0.002F) {
         return;
      }

      dl.addRectFilled(0.0F, 0.0F, w, h, SpectralTokens.alpha(SpectralTokens.VOID, dim * alpha));
      int atmos = BackdropBlur.atmosphereId();
      if (atmos > 0) {
         dl.addImage(atmos, 0.0F, 0.0F, w, h, 0.0F, 1.0F, 1.0F, 0.0F, SpectralTokens.alpha(0xFFFFFFFF, alpha));
      }

      if (SpectralTokens.atmosphere()) {
         wisps(dl, w, h, alpha, scale);
      }
   }

   private static void wisps(ImDrawList dl, float w, float h, float alpha, float scale) {
      double t = Anim.reducedMotion() ? 0.0 : Anim.time();
      int accent = SpectralTokens.accent();
      int phantom = SpectralTokens.PHANTOM;
      float glow = SpectralTokens.glow();

      for (int i = 0; i < WISPS; i++) {
         float speed = 0.012F + (i % 5) * 0.004F;
         double life = i * 0.381966 + t * speed;
         float phase = (float)(life - Math.floor(life));
         float sway = (float)Math.sin(t * 0.35 + i * 1.7) * 0.018F;
         float fx = (float)((i * 0.6180339 + sway) % 1.0);
         if (fx < 0.0F) {
            fx++;
         }

         float x = w * fx;
         float y = h * (1.05F - phase * 1.1F);
         float bell = (float)Math.sin(Math.PI * phase);
         float a = bell * bell * (0.18F + 0.14F * (i % 3)) * alpha * (0.5F + 0.5F * glow);
         if (a < 0.01F) {
            continue;
         }

         float size = (1.2F + (i % 4) * 0.7F) * scale;
         int col = i % 4 == 0 ? phantom : accent;
         GhostPaint.orb(dl, x, y, size * 6.0F, SpectralTokens.alpha(col, a * 0.35F));
         dl.addCircleFilled(x, y, size, SpectralTokens.alpha(SpectralTokens.mix(col, 0xFFFFFFFF, 0.5F), a), 10);
      }
   }
}
