package net.spectre.client.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/**
 * 1.21.11 compatibility for Minecraft 26.x {@code FeatureRenderDispatcher.renderAllFeatures(SubmitNodeStorage)}.
 * <p>
 * In 26.x one dispatcher renders any storage handed to it and executes every phase immediately.
 * In 1.21.11 a dispatcher is bound to a single storage and only fills the shared buffer sources,
 * so this keeps one dispatcher per storage and flushes the buffers afterwards.
 */
@Environment(EnvType.CLIENT)
public final class FeatureCompat {
   private static final Map<SubmitNodeStorage, FeatureRenderDispatcher> DISPATCHERS = Collections.synchronizedMap(new WeakHashMap<>());

   private FeatureCompat() {
   }

   public static FeatureRenderDispatcher dispatcherFor(SubmitNodeStorage storage) {
      Minecraft mc = Minecraft.getInstance();
      RenderBuffers buffers = mc.renderBuffers();
      return DISPATCHERS.computeIfAbsent(storage, s -> new FeatureRenderDispatcher(
         s, mc.getBlockRenderer(), buffers.bufferSource(), mc.getAtlasManager(), buffers.outlineBufferSource(), buffers.crumblingBufferSource(), mc.font));
   }

   /** Renders and clears {@code storage}, then flushes the buffer sources it drew into. */
   public static void renderAll(SubmitNodeStorage storage) {
      dispatcherFor(storage).renderAllFeatures();
      flush();
   }

   public static void flush() {
      RenderBuffers buffers = Minecraft.getInstance().renderBuffers();
      buffers.bufferSource().endBatch();
      buffers.crumblingBufferSource().endBatch();
      buffers.outlineBufferSource().endOutlineBatch();
   }
}
