package net.spectre.client.gui.ui;

import imgui.ImDrawList;
import imgui.ImGui;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Spectral drawing primitives on top of ImGui draw lists: anti-aliased rounded gradients, soft
 * multi-band shadows, glows, rim lights, frosted glass and click ripples.
 * <p>
 * Shapes are emitted directly as vertex meshes ({@code primReserve}/{@code primWriteVtx}) into
 * reusable scratch arrays, so drawing allocates nothing. All colours are ImGui ABGR.
 */
@Environment(EnvType.CLIENT)
public final class GhostPaint {
   private static final float TAU = (float)(Math.PI * 2.0);
   private static final int MAX_SEG = 10;
   private static final float[] COS = new float[4 * (MAX_SEG + 1)];
   private static final float[] SIN = new float[4 * (MAX_SEG + 1)];
   private static final float[] CX = new float[4 * (MAX_SEG + 1)];
   private static final float[] CY = new float[4 * (MAX_SEG + 1)];
   private static final float[] CORNER_X = new float[4];
   private static final float[] CORNER_Y = new float[4];
   private static final float[] BAND_R = new float[4];
   private static final float[] BAND_A = {1.0F, 0.42F, 0.12F, 0.0F};
   private static final Long2ObjectOpenHashMap<Ripple> RIPPLES = new Long2ObjectOpenHashMap<>();
   private static float uvX;
   private static float uvY;
   private static int count;

   private GhostPaint() {
   }

   /** Called once per ImGui frame after {@code ImGui.newFrame()}. */
   public static void beginFrame() {
      uvX = ImGui.getFontTexUvWhitePixelX();
      uvY = ImGui.getFontTexUvWhitePixelY();
      if (!RIPPLES.isEmpty()) {
         long now = Anim.nanos();
         RIPPLES.values().removeIf(r -> now - r.start > 900_000_000L);
      }
   }

   // ---- shape outline ------------------------------------------------------------------------
   /** Fills COS/SIN/CX/CY with the perimeter of a rounded rect (clockwise, screen space). */
   private static void outline(float x, float y, float w, float h, float r, int seg) {
      r = Math.max(0.0F, Math.min(r, Math.min(w, h) * 0.5F));
      int n = 0;
      float[] cxs = CORNER_X;
      float[] cys = CORNER_Y;
      cxs[0] = x + r; cxs[1] = x + w - r; cxs[2] = x + w - r; cxs[3] = x + r;
      cys[0] = y + r; cys[1] = y + r; cys[2] = y + h - r; cys[3] = y + h - r;

      for (int corner = 0; corner < 4; corner++) {
         float a0 = (float)Math.PI * (1.0F + corner * 0.5F);

         for (int i = 0; i <= seg; i++) {
            float a = a0 + (float)Math.PI * 0.5F * i / seg;
            COS[n] = (float)Math.cos(a);
            SIN[n] = (float)Math.sin(a);
            CX[n] = cxs[corner];
            CY[n] = cys[corner];
            n++;
         }
      }

      count = n;
   }

   private static int segments(float r) {
      return Math.max(2, Math.min(MAX_SEG, Math.round(r * 0.45F) + 2));
   }

   // ---- fills --------------------------------------------------------------------------------
   /** Rounded rect with a vertical gradient and a 1px anti-aliased fringe. */
   public static void gradient(ImDrawList dl, float x, float y, float w, float h, float r, int top, int bottom) {
      if (w <= 0.5F || h <= 0.5F || (top >>> 24) == 0 && (bottom >>> 24) == 0) {
         return;
      }

      r = Math.max(0.0F, Math.min(r, Math.min(w, h) * 0.5F));
      outline(x, y, w, h, r, segments(r));
      int n = count;
      float rin = Math.max(0.0F, r - 0.5F);
      float rout = r + 0.5F;
      dl.primReserve(n * 3 + n * 6, 1 + n * 2);
      int base = dl.getVtxCurrentIdx();
      dl.primWriteVtx(x + w * 0.5F, y + h * 0.5F, uvX, uvY, SpectralTokens.mix(top, bottom, 0.5F));

      for (int i = 0; i < n; i++) {
         float ix = CX[i] + COS[i] * rin;
         float iy = CY[i] + SIN[i] * rin;
         int col = SpectralTokens.mix(top, bottom, (iy - y) / h);
         dl.primWriteVtx(ix, iy, uvX, uvY, col);
         dl.primWriteVtx(CX[i] + COS[i] * rout, CY[i] + SIN[i] * rout, uvX, uvY, col & 0x00FFFFFF);
      }

      for (int i = 0; i < n; i++) {
         int j = (i + 1) % n;
         int ii = base + 1 + i * 2;
         int jj = base + 1 + j * 2;
         dl.primWriteIdx(base);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(jj);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(ii + 1);
         dl.primWriteIdx(jj + 1);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(jj + 1);
         dl.primWriteIdx(jj);
      }
   }

   /** Flat rounded fill (delegates to ImGui's native AA rect: one call). */
   public static void fill(ImDrawList dl, float x, float y, float w, float h, float r, int col) {
      if ((col >>> 24) != 0 && w > 0.0F && h > 0.0F) {
         dl.addRectFilled(x, y, x + w, y + h, col, r);
      }
   }

   // ---- soft shadow / glow -------------------------------------------------------------------
   /**
    * Soft shadow outside a rounded rect: three feathered bands approximating a gaussian falloff.
    * Only the outside ring is drawn, so translucent panels above stay clean.
    */
   public static void shadow(ImDrawList dl, float x, float y, float w, float h, float r, float spread, float dy, int col) {
      band(dl, x, y + dy, w, h, r, spread, col);
   }

   /** Outward glow halo (no offset). */
   public static void glow(ImDrawList dl, float x, float y, float w, float h, float r, float spread, int col) {
      band(dl, x, y, w, h, r, spread, col);
   }

   private static void band(ImDrawList dl, float x, float y, float w, float h, float r, float spread, int col) {
      if ((col >>> 24) == 0 || spread <= 0.5F || w <= 0.0F || h <= 0.0F) {
         return;
      }

      r = Math.max(0.0F, Math.min(r, Math.min(w, h) * 0.5F));
      outline(x, y, w, h, r, 6);
      int n = count;
      float[] radii = BAND_R;
      float[] alphas = BAND_A;
      radii[0] = r; radii[1] = r + spread * 0.3F; radii[2] = r + spread * 0.65F; radii[3] = r + spread;
      int rings = radii.length;
      dl.primReserve(n * 6 * (rings - 1), n * rings);
      int base = dl.getVtxCurrentIdx();

      for (int k = 0; k < rings; k++) {
         int c = SpectralTokens.alpha(col, alphas[k]);

         for (int i = 0; i < n; i++) {
            dl.primWriteVtx(CX[i] + COS[i] * radii[k], CY[i] + SIN[i] * radii[k], uvX, uvY, c);
         }
      }

      for (int k = 0; k < rings - 1; k++) {
         int a = base + k * n;
         int b = base + (k + 1) * n;

         for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            dl.primWriteIdx(a + i);
            dl.primWriteIdx(b + i);
            dl.primWriteIdx(b + j);
            dl.primWriteIdx(a + i);
            dl.primWriteIdx(b + j);
            dl.primWriteIdx(a + j);
         }
      }
   }

   /** Radial glow disc (soft dot), e.g. wisps and switch knobs. */
   public static void orb(ImDrawList dl, float cx, float cy, float radius, int col) {
      if ((col >>> 24) == 0 || radius <= 0.5F) {
         return;
      }

      int n = Math.max(12, Math.min(32, Math.round(radius)));
      dl.primReserve(n * 3 + n * 6, 1 + n * 2);
      int base = dl.getVtxCurrentIdx();
      dl.primWriteVtx(cx, cy, uvX, uvY, col);
      int mid = SpectralTokens.alpha(col, 0.35F);

      for (int i = 0; i < n; i++) {
         float a = TAU * i / n;
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         dl.primWriteVtx(cx + c * radius * 0.45F, cy + s * radius * 0.45F, uvX, uvY, mid);
         dl.primWriteVtx(cx + c * radius, cy + s * radius, uvX, uvY, col & 0x00FFFFFF);
      }

      for (int i = 0; i < n; i++) {
         int j = (i + 1) % n;
         int ii = base + 1 + i * 2;
         int jj = base + 1 + j * 2;
         dl.primWriteIdx(base);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(jj);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(ii + 1);
         dl.primWriteIdx(jj + 1);
         dl.primWriteIdx(ii);
         dl.primWriteIdx(jj + 1);
         dl.primWriteIdx(jj);
      }
   }

   // ---- edges ---------------------------------------------------------------------------------
   /** 1px highlight along the top edge, as if lit from above. */
   public static void rim(ImDrawList dl, float x, float y, float w, float h, float r, int col) {
      if ((col >>> 24) == 0) {
         return;
      }

      r = Math.max(0.0F, Math.min(r, Math.min(w, h) * 0.5F));
      float inset = 0.5F;
      dl.pathClear();
      if (r > 0.5F) {
         dl.pathArcTo(x + r, y + r, r - inset, (float)Math.PI * 1.15F, (float)Math.PI * 1.5F, 5);
         dl.pathArcTo(x + w - r, y + r, r - inset, (float)Math.PI * 1.5F, (float)Math.PI * 1.85F, 5);
      } else {
         dl.pathLineTo(x, y + inset);
         dl.pathLineTo(x + w, y + inset);
      }

      dl.pathStroke(col, 0, 1.0F);
   }

   /** Thin border all round. */
   public static void border(ImDrawList dl, float x, float y, float w, float h, float r, int col, float thickness) {
      if ((col >>> 24) != 0) {
         dl.addRect(x + 0.5F, y + 0.5F, x + w - 0.5F, y + h - 0.5F, col, r, 0, thickness);
      }
   }

   /** 1px line that fades out towards both ends. */
   public static void hairline(ImDrawList dl, float x0, float x1, float y, int col) {
      if ((col >>> 24) == 0 || x1 <= x0) {
         return;
      }

      float mid = (x0 + x1) * 0.5F;
      int clear = col & 0x00FFFFFF;
      dl.addRectFilledMultiColor(x0, y, mid, y + 1.0F, clear, col, col, clear);
      dl.addRectFilledMultiColor(mid, y, x1, y + 1.0F, col, clear, clear, col);
   }

   // ---- composite surfaces ----------------------------------------------------------------
   /**
    * A Spectral surface: soft shadow, frosted glass (when available), tinted gradient, rim light
    * and border. {@code elevation} 0..1 scales the shadow (hover lift raises it).
    */
   public static void surface(ImDrawList dl, float x, float y, float w, float h, float r, int tint, float tintAlpha, float elevation, float alpha) {
      if (alpha <= 0.002F) {
         return;
      }

      float e = Math.max(0.0F, elevation);
      shadow(dl, x, y, w, h, r, 10.0F + 14.0F * e, 3.0F + 5.0F * e, SpectralTokens.alpha(SpectralTokens.SHADOW, (0.55F + 0.3F * e) * alpha));
      boolean glassDrawn = SpectralTokens.glass() && BackdropBlur.ready() && glass(dl, x, y, w, h, r, alpha);
      float ta = glassDrawn ? tintAlpha : Math.max(tintAlpha, 0.94F);
      int top = SpectralTokens.alpha(SpectralTokens.mix(tint, SpectralTokens.RAISED_HI, 0.18F), ta * alpha);
      int bottom = SpectralTokens.alpha(tint, ta * alpha);
      gradient(dl, x, y, w, h, r, top, bottom);
      rim(dl, x, y, w, h, r, SpectralTokens.alpha(SpectralTokens.RIM, alpha));
      border(dl, x, y, w, h, r, SpectralTokens.alpha(SpectralTokens.LINE, alpha), 1.0F);
   }

   /** Draws the blurred backdrop clipped to a rounded rect. Returns false if unavailable. */
   public static boolean glass(ImDrawList dl, float x, float y, float w, float h, float r, float alpha) {
      int tex = BackdropBlur.textureId();
      float sw = ImGui.getIO().getDisplaySizeX();
      float sh = ImGui.getIO().getDisplaySizeY();
      if (tex <= 0 || sw <= 0.0F || sh <= 0.0F) {
         return false;
      } else {
         int white = SpectralTokens.alpha(0xFFFFFFFF, alpha);
         dl.addImageRounded(tex, x, y, x + w, y + h, x / sw, 1.0F - y / sh, (x + w) / sw, 1.0F - (y + h) / sh, white, r);
         return true;
      }
   }

   /** Ectoplasm glow behind an active element; intensity follows the theme glow setting. */
   public static void ecto(ImDrawList dl, float x, float y, float w, float h, float r, float amount) {
      float a = amount * SpectralTokens.glow();
      if (a > 0.01F) {
         glow(dl, x, y, w, h, r, 6.0F + 10.0F * a, SpectralTokens.alpha(SpectralTokens.accent(), 0.55F * a));
      }
   }

   // ---- ripples ------------------------------------------------------------------------------
   /** Starts a ripple for {@code key} at the given screen point (call on click). */
   public static void ripple(long key, float px, float py) {
      if (!Anim.reducedMotion()) {
         RIPPLES.put(key, new Ripple(px, py, Anim.nanos()));
      }
   }

   /** Draws the ripple for {@code key}, if active, clipped to the element rect. */
   public static void drawRipple(ImDrawList dl, long key, float x, float y, float w, float h, int col) {
      Ripple rp = RIPPLES.get(key);
      if (rp != null) {
         float t = (Anim.nanos() - rp.start) / 6.2E8F;
         if (t >= 1.0F) {
            RIPPLES.remove(key);
         } else {
            float dx = Math.max(rp.x - x, x + w - rp.x);
            float dy = Math.max(rp.y - y, y + h - rp.y);
            float max = (float)Math.sqrt(dx * dx + dy * dy);
            float radius = max * Anim.easeOutCubic(t);
            dl.pushClipRect(x, y, x + w, y + h, true);
            dl.addCircleFilled(rp.x, rp.y, radius, SpectralTokens.alpha(col, 0.22F * (1.0F - t)), 32);
            dl.popClipRect();
         }
      }
   }

   private record Ripple(float x, float y, long start) {
   }
}
