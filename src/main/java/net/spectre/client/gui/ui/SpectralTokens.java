package net.spectre.client.gui.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.spectre.client.config.ThemeStore;

/**
 * Single source of truth for the "Spectral" look: palette, elevation, radii and glow.
 * <p>
 * Static palette values are ARGB ({@code A_*}) and ImGui ABGR ({@code *}) constants. Accent-derived
 * values follow the user's accent and are recomputed once per frame by {@link #refresh(ThemeStore)}.
 */
@Environment(EnvType.CLIENT)
public final class SpectralTokens {
   // ---- ARGB palette -------------------------------------------------------------------------
   public static final int A_VOID = 0xFF07060C;
   public static final int A_PANEL = 0xFF0E0B17;
   public static final int A_PANEL_2 = 0xFF141024;
   public static final int A_RAISED = 0xFF1B1630;
   public static final int A_RAISED_HI = 0xFF261F42;
   public static final int A_ECTO = 0xFF6EF2D0;
   public static final int A_PHANTOM = 0xFF8D6BFF;
   public static final int A_EMBER = 0xFFFF4F73;
   public static final int A_TEXT = 0xFFE9E6F5;
   public static final int A_MUTED = 0xFF8C86A6;
   public static final int A_FAINT = 0xFF4E4866;
   public static final int A_SWITCH_OFF = 0xFF2A2440;
   public static final int A_LINE = 0x2EB9A8FF;
   public static final int A_LINE_STRONG = 0x55B9A8FF;
   public static final int A_RIM = 0x40FFFFFF;
   public static final int A_SHADOW = 0xC0020106;

   // ---- ImGui ABGR equivalents --------------------------------------------------------------
   public static final int VOID = abgr(A_VOID);
   public static final int PANEL = abgr(A_PANEL);
   public static final int PANEL_2 = abgr(A_PANEL_2);
   public static final int RAISED = abgr(A_RAISED);
   public static final int RAISED_HI = abgr(A_RAISED_HI);
   public static final int PHANTOM = abgr(A_PHANTOM);
   public static final int EMBER = abgr(A_EMBER);
   public static final int TEXT = abgr(A_TEXT);
   public static final int MUTED = abgr(A_MUTED);
   public static final int FAINT = abgr(A_FAINT);
   public static final int SWITCH_OFF = abgr(A_SWITCH_OFF);
   public static final int LINE = abgr(A_LINE);
   public static final int LINE_STRONG = abgr(A_LINE_STRONG);
   public static final int RIM = abgr(A_RIM);
   public static final int SHADOW = abgr(A_SHADOW);

   // ---- Shape -------------------------------------------------------------------------------
   public static final float RADIUS_SM = 6.0F;
   public static final float RADIUS_MD = 10.0F;
   public static final float RADIUS_LG = 16.0F;

   // ---- Accent-derived, refreshed per frame (ABGR) ------------------------------------------
   private static int accent = abgr(A_ECTO);
   private static int accentSoft = abgr(0x406EF2D0);
   private static int accentDeep = abgr(0xFF1F4A47);
   private static int accentInk = abgr(0xFF0A1A18);
   private static float glow = 0.7F;
   private static float flicker = 1.0F;
   private static boolean glass = true;
   private static boolean atmosphere = true;
   private static float blurStrength = 1.0F;
   private static boolean hudGlass;

   private SpectralTokens() {
   }

   /** Called once per ImGui frame from {@code GuiPalette.refresh()}. */
   public static void refresh(ThemeStore theme) {
      int a = theme.accent.argbWithAlpha(255);
      accent = abgr(a);
      accentSoft = abgr(a & 0x00FFFFFF | 0x40000000);
      accentDeep = abgr(mixArgb(a, A_PANEL, 0.72F));
      accentInk = abgr(mixArgb(a, A_VOID, 0.88F));
      glow = theme.glow.get();
      glass = theme.glass.get();
      atmosphere = theme.atmosphere.get();
      blurStrength = theme.blurStrength.get();
      hudGlass = theme.hudGlass.get();
      // Candle flicker: two slow incommensurate sines, at most ~6% amplitude.
      double t = Anim.time();
      flicker = Anim.reducedMotion() ? 1.0F : (float)(0.97 + 0.02 * Math.sin(t * 1.31) + 0.01 * Math.sin(t * 3.7 + 1.3));
   }

   public static int accent() {
      return accent;
   }

   public static int accentSoft() {
      return accentSoft;
   }

   public static int accentDeep() {
      return accentDeep;
   }

   public static int accentInk() {
      return accentInk;
   }

   /** User glow intensity (0..1) modulated by the candle flicker. */
   public static float glow() {
      return glow * flicker;
   }

   public static boolean glass() {
      return glass;
   }

   public static boolean atmosphere() {
      return atmosphere;
   }

   public static float blurStrength() {
      return blurStrength;
   }

   public static boolean hudGlass() {
      return hudGlass;
   }

   // ---- Colour helpers (ImGui ABGR unless stated) -------------------------------------------
   public static int abgr(int argb) {
      return argb & 0xFF00FF00 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
   }

   public static int alpha(int abgr, float alpha) {
      int a = Math.round((abgr >>> 24) * Math.max(0.0F, Math.min(1.0F, alpha)));
      return a << 24 | abgr & 0x00FFFFFF;
   }

   /** Linear mix of two packed colours (works for ARGB and ABGR alike). */
   public static int mix(int from, int to, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int a = lerp(from >>> 24, to >>> 24, t);
      int b = lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
      int g = lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
      int r = lerp(from & 0xFF, to & 0xFF, t);
      return a << 24 | b << 16 | g << 8 | r;
   }

   public static int mixArgb(int from, int to, float t) {
      return mix(from, to, t);
   }

   private static int lerp(int a, int b, float t) {
      return Math.round(a + (b - a) * t);
   }
}
