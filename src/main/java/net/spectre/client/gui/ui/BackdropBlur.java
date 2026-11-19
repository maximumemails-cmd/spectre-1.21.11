package net.spectre.client.gui.ui;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frosted-glass backdrop for Spectre UI: a dual-Kawase blur of the current frame (three downsample
 * and two upsample passes, ending at half resolution) plus a quarter-resolution "atmosphere" layer
 * with drifting fog and a vignette.
 * <p>
 * It runs once per frame just before ImGui draws, and only while something asked for it. The
 * results are exposed to ImGui as raw GL texture ids. If a shader fails to compile, Spectre falls
 * back to opaque panels.
 */
@Environment(EnvType.CLIENT)
public final class BackdropBlur {
   private static final Logger LOGGER = LoggerFactory.getLogger("Spectre");
   private static final RenderPipeline DOWN = pipeline("spectral_down", "SpectralBlur", true);
   private static final RenderPipeline UP = pipeline("spectral_up", "SpectralBlur", true);
   private static final RenderPipeline ATMOS = pipeline("spectral_atmos", "SpectralAtmos", false);
   private static final Supplier<String> LABEL = () -> "Spectre backdrop";
   private static final int LEVELS = 3;
   private static final TextureTarget[] DOWN_T = new TextureTarget[LEVELS];
   private static final TextureTarget[] UP_T = new TextureTarget[LEVELS - 1];
   private static final GpuBuffer[] BUFFERS = new GpuBuffer[LEVELS * 2];
   private static TextureTarget atmos;
   private static GpuBuffer atmosBuffer;
   private static ByteBuffer data;
   private static int width;
   private static int height;
   private static boolean requested;
   private static boolean ready;
   private static boolean failed;
   private static boolean warned;

   private BackdropBlur() {
   }

   private static RenderPipeline pipeline(String shader, String uniform, boolean sampler) {
      RenderPipeline.Builder b = RenderPipeline.builder()
         .withLocation(Identifier.fromNamespaceAndPath("spectre", "pipeline/" + shader))
         .withVertexShader("core/screenquad")
         .withFragmentShader(Identifier.fromNamespaceAndPath("spectre", "core/" + shader));
      if (sampler) {
         b.withSampler("InSampler");
      }

      return b.withUniform(uniform, UniformType.UNIFORM_BUFFER)
         .withoutBlend()
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
         .build();
   }

   /** Ask for the backdrop this frame (Spectre screens, glass HUD). */
   public static void request() {
      requested = true;
   }

   /** True when the glass texture of the current frame is valid. */
   public static boolean ready() {
      return ready;
   }

   /** GL id of the blurred frame (half resolution), or 0. */
   public static int textureId() {
      return ready ? glId(UP_T[0]) : 0;
   }

   /** GL id of the atmosphere layer (fog + vignette, premultiplied-style alpha), or 0. */
   public static int atmosphereId() {
      return ready && atmos != null ? glId(atmos) : 0;
   }

   /** Runs the passes if requested; called once per frame before ImGui renders. */
   public static void frame() {
      boolean want = requested;
      requested = false;
      ready = false;
      if (want && !failed) {
         try {
            ready = run();
         } catch (RuntimeException e) {
            failed = true;
            LOGGER.warn("[Spectre] Backdrop blur disabled after an error; panels fall back to opaque", e);
         }
      }
   }

   private static boolean run() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.getMainRenderTarget();
      if (main.getColorTexture() == null || main.width < 16 || main.height < 16) {
         return false; // minimised or not ready yet: skip this frame
      }

      GpuDevice device = RenderSystem.getDevice();
      if (!device.precompilePipeline(DOWN).isValid() || !device.precompilePipeline(UP).isValid() || !device.precompilePipeline(ATMOS).isValid()) {
         if (!warned) {
            warned = true;
            LOGGER.warn("[Spectre] Backdrop shaders did not compile; glass panels are disabled");
         }

         failed = true;
         throw new IllegalStateException("backdrop shaders invalid");
      }

      ensureTargets(main.width, main.height);
      CommandEncoder encoder = device.createCommandEncoder();
      float strength = Math.max(0.25F, SpectralTokens.blurStrength());
      GpuTextureView source = main.getColorTextureView();
      int sw = main.width;
      int sh = main.height;

      for (int i = 0; i < LEVELS; i++) {
         blurPass(encoder, DOWN, BUFFERS[i], source, sw, sh, DOWN_T[i], 1.0F + 0.6F * strength, 0.0F);
         source = DOWN_T[i].getColorTextureView();
         sw = DOWN_T[i].width;
         sh = DOWN_T[i].height;
      }

      for (int i = UP_T.length - 1; i >= 0; i--) {
         boolean last = i == 0;
         blurPass(encoder, UP, BUFFERS[LEVELS + i], source, sw, sh, UP_T[i], 1.0F + 0.6F * strength, last ? 1.0F : 0.0F);
         source = UP_T[i].getColorTextureView();
         sw = UP_T[i].width;
         sh = UP_T[i].height;
      }

      atmosPass(encoder);
      return true;
   }

   private static void blurPass(CommandEncoder encoder, RenderPipeline pipeline, GpuBuffer buffer, GpuTextureView from, int fromW, int fromH, TextureTarget into, float offset, float shade) {
      data.clear();
      data.putFloat(0, 0.5F / fromW);
      data.putFloat(4, 0.5F / fromH);
      data.putFloat(8, offset);
      data.putFloat(12, shade);
      data.position(0).limit(16);
      GpuBufferSlice slice = buffer.slice();
      encoder.writeToBuffer(slice, data);

      try (RenderPass pass = encoder.createRenderPass(LABEL, into.getColorTextureView(), OptionalInt.empty())) {
         pass.setPipeline(pipeline);
         pass.setUniform("SpectralBlur", slice);
         pass.bindTexture("InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(0, 3);
      }
   }

   private static void atmosPass(CommandEncoder encoder) {
      int accent = SpectralTokens.accent();
      data.clear();
      data.putFloat(0, (accent & 0xFF) / 255.0F);
      data.putFloat(4, (accent >> 8 & 0xFF) / 255.0F);
      data.putFloat(8, (accent >> 16 & 0xFF) / 255.0F);
      data.putFloat(12, 1.0F);
      data.putFloat(16, Anim.reducedMotion() ? 0.0F : (float)(Anim.time() % 3600.0));
      data.putFloat(20, SpectralTokens.atmosphere() ? 1.0F : 0.0F);
      data.putFloat(24, (float)width / Math.max(1, height));
      data.putFloat(28, 0.55F);
      data.position(0).limit(32);
      GpuBufferSlice slice = atmosBuffer.slice();
      encoder.writeToBuffer(slice, data);

      try (RenderPass pass = encoder.createRenderPass(LABEL, atmos.getColorTextureView(), OptionalInt.empty())) {
         pass.setPipeline(ATMOS);
         pass.setUniform("SpectralAtmos", slice);
         pass.draw(0, 3);
      }
   }

   private static void ensureTargets(int w, int h) {
      if (w == width && h == height && DOWN_T[0] != null) {
         return;
      }

      release();
      width = w;
      height = h;
      GpuDevice device = RenderSystem.getDevice();

      for (int i = 0; i < LEVELS; i++) {
         int div = 2 << i;
         DOWN_T[i] = target("down " + i, w / div, h / div);
      }

      for (int i = 0; i < UP_T.length; i++) {
         int div = 2 << i;
         UP_T[i] = target("up " + i, w / div, h / div);
      }

      atmos = target("atmosphere", w / 4, h / 4);

      for (int i = 0; i < BUFFERS.length; i++) {
         BUFFERS[i] = device.createBuffer(() -> "Spectre backdrop blur config", 136, 16L);
      }

      atmosBuffer = device.createBuffer(() -> "Spectre backdrop atmosphere config", 136, 32L);
      data = BufferUtils.createByteBuffer(32);
   }

   private static TextureTarget target(String name, int w, int h) {
      TextureTarget t = new TextureTarget("Spectre backdrop " + name, Math.max(1, w), Math.max(1, h), false);
      // ImGui samples these directly (no sampler object): give them linear filtering.
      int id = glId(t);
      if (id > 0) {
         GlStateManager._bindTexture(id);
         GlStateManager._texParameter(3553, 10241, 9729);
         GlStateManager._texParameter(3553, 10240, 9729);
         GlStateManager._texParameter(3553, 10242, 33071);
         GlStateManager._texParameter(3553, 10243, 33071);
         GlStateManager._bindTexture(0);
      }

      return t;
   }

   private static int glId(TextureTarget t) {
      return t != null && t.getColorTexture() instanceof GlTexture gl ? gl.glId() : 0;
   }

   public static void release() {
      for (int i = 0; i < DOWN_T.length; i++) {
         if (DOWN_T[i] != null) {
            DOWN_T[i].destroyBuffers();
            DOWN_T[i] = null;
         }
      }

      for (int i = 0; i < UP_T.length; i++) {
         if (UP_T[i] != null) {
            UP_T[i].destroyBuffers();
            UP_T[i] = null;
         }
      }

      if (atmos != null) {
         atmos.destroyBuffers();
         atmos = null;
      }

      for (int i = 0; i < BUFFERS.length; i++) {
         if (BUFFERS[i] != null) {
            BUFFERS[i].close();
            BUFFERS[i] = null;
         }
      }

      if (atmosBuffer != null) {
         atmosBuffer.close();
         atmosBuffer = null;
      }

      width = 0;
      height = 0;
      ready = false;
   }
}
