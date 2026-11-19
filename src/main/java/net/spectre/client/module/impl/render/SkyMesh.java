package net.spectre.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyMesh {
   static final RenderPipeline GLOW = pipeline("sky_glow", BlendFunction.LIGHTNING);
   static final RenderPipeline PAINT = pipeline("sky_paint", null);
   private static final int VERTEX_SIZE = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
   private final String label;
   private final RenderPipeline pipeline;
   private final int maxQuads;
   private ByteBufferBuilder bytes;
   private GpuBuffer buffer;
   private int indexCount;
   private boolean built;

   SkyMesh(String label, RenderPipeline pipeline, int maxQuads) {
      this.label = label;
      this.pipeline = pipeline;
      this.maxQuads = maxQuads;
   }

   private static RenderPipeline pipeline(String name, BlendFunction blend) {
      Builder builder = RenderPipeline.builder().withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER).withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
         .withLocation(Identifier.fromNamespaceAndPath("spectre", "pipeline/" + name))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         
         .withCull(false)
         .withDepthWrite(false).withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
      return (blend == null ? builder.withoutBlend() : builder.withBlend(blend)).build();
   }

   BufferBuilder begin() {
      if (this.bytes == null) {
         this.bytes = new ByteBufferBuilder(this.maxQuads * 4 * VERTEX_SIZE);
      }

      return new BufferBuilder(this.bytes, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
   }

   void upload(BufferBuilder builder) {
      this.built = true;

      try (MeshData mesh = builder.build()) {
         if (mesh == null) {
            this.indexCount = 0;
            return;
         }

         if (this.buffer == null) {
            this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, 40, this.maxQuads * 4L * VERTEX_SIZE);
         }

         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0L, mesh.vertexBuffer().remaining()), mesh.vertexBuffer());
         this.indexCount = mesh.drawState().indexCount();
      }
   }

   boolean isBuilt() {
      return this.built;
   }

   void draw(PoseStack pose, float alpha) {
      if (this.indexCount != 0 && !(alpha <= 0.002F)) {
         AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix();

         try {
            modelView.mul(pose.last().pose());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
               .writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
            // 1.21.11: the sky draws straight into the main target (no separate sky render target).
            RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

            try (RenderPass pass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> this.label, RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView(), java.util.OptionalInt.empty(), RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView(), OptionalDouble.empty())) {
               pass.setPipeline(this.pipeline);
               RenderSystem.bindDefaultUniforms(pass);
               pass.setUniform("DynamicTransforms", transforms);
               pass.setVertexBuffer(0, this.buffer);
               pass.setIndexBuffer(indices.getBuffer(this.indexCount), indices.type());
               pass.drawIndexed(0, 0, this.indexCount, 1);
            }
         } finally {
            modelView.popMatrix();
         }
      }
   }

   void close() {
      if (this.buffer != null) {
         this.buffer.close();
      }

      if (this.bytes != null) {
         this.bytes.close();
      }

      this.buffer = null;
      this.bytes = null;
      this.indexCount = 0;
      this.built = false;
   }

   static void vertex(BufferBuilder builder, float x, float y, float z, int argb) {
      builder.addVertex(x, y, z).setColor(argb);
   }
}
