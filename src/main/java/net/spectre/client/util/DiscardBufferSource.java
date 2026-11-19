package net.spectre.client.util;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * A buffer source that drops all geometry. Used where 1.21.11 has no way to execute only one
 * feature phase (26.x {@code PreparedFrame.executeOutline()}): regular geometry goes here and is
 * discarded while outline geometry goes to a separate {@code OutlineBufferSource}.
 */
@Environment(EnvType.CLIENT)
public final class DiscardBufferSource extends MultiBufferSource.BufferSource {
   private static final VertexConsumer SINK = new VertexConsumer() {
      @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
      @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
      @Override public VertexConsumer setColor(int argb) { return this; }
      @Override public VertexConsumer setUv(float u, float v) { return this; }
      @Override public VertexConsumer setUv1(int u, int v) { return this; }
      @Override public VertexConsumer setUv2(int u, int v) { return this; }
      @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
      @Override public VertexConsumer setLineWidth(float width) { return this; }
   };

   public DiscardBufferSource() {
      super(new ByteBufferBuilder(64), new LinkedHashMap<>());
   }

   @Override
   public VertexConsumer getBuffer(RenderType renderType) {
      return SINK;
   }

   @Override
   public void endLastBatch() {
   }

   @Override
   public void endBatch() {
   }

   @Override
   public void endBatch(RenderType renderType) {
   }
}
