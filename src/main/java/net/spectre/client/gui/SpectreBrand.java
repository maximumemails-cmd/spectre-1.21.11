package net.spectre.client.gui;

import imgui.ImDrawList;

/** Shared Spectre artwork and colors for startup, menus and the gameplay HUD. */
public final class SpectreBrand {
    public static final String LOGO_RESOURCE = "/assets/spectre/textures/gui/logo.png";
    public static final String BACKDROP_RESOURCE = "/assets/spectre/textures/gui/spectre_backdrop.png";
    public static final int ACCENT = 0xFF6EF2D0;
    public static final int TEXT = 0xFFE9E6F5;
    public static final int MUTED = 0xFF8C86A6;
    private SpectreBrand() {}

    public static ThemeImages.Texture logo() { return SpectreDraw.texture(LOGO_RESOURCE); }

    public static void backdrop(ImDrawList dl, float w, float h, float time, float alpha) {
        float t = GuiPalette.animationSpeed() <= 0 ? 0 : time;
        dl.addRectFilledMultiColor(0, 0, w, h,
            SpectreDraw.col(0xFF07060C, alpha), SpectreDraw.col(0xFF130E26, alpha),
            SpectreDraw.col(0xFF06050B, alpha), SpectreDraw.col(0xFF0E0A1D, alpha));
        var backdrop=SpectreDraw.texture(BACKDROP_RESOURCE);
        if(backdrop!=null&&w>0&&h>0){
            float aspect=(float)backdrop.width()/backdrop.height(),screen=w/h;
            float u=screen<aspect?(1-screen/aspect)*.5f:0;
            float v=screen>aspect?(1-aspect/screen)*.5f:0;
            dl.addImage(backdrop.id(),0,0,w,h,u,v,1-u,1-v,SpectreDraw.col(0xFFD6CCFF,alpha));
        }
        // Spectral atmosphere: shader fog + vignette and drifting ectoplasm wisps.
        net.spectre.client.gui.ui.SpectralBackdrop.draw(dl, w, h, alpha, 0f, Math.max(0.75f, h / 1080f));
    }

    public static void mark(ImDrawList dl, float cx, float cy, float size, float time, float alpha) {
        SpectreDraw.glow(dl, cx, cy, size * .66f, ACCENT, .12f * alpha);
        WorkspaceMascot.drawSplash(dl, cx, cy, size, alpha, 1);
    }
}
