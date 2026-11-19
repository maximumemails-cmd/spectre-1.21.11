package net.spectre.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

/**
 * 1.21.11 compatibility: Minecraft 26.x exposes the frame's view-rotation and projection matrices on
 * {@code CameraRenderState}. In 1.21.11 they are only arguments of {@code LevelRenderer.renderLevel},
 * so {@code MixinLevelRendererCompat} copies them here at the start of each level render.
 */
@Environment(EnvType.CLIENT)
public final class RenderMatrices {
   private static final Matrix4f VIEW_ROTATION = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();

   private RenderMatrices() {
   }

   public static void capture(Matrix4f viewRotation, Matrix4f projection) {
      VIEW_ROTATION.set(viewRotation);
      PROJECTION.set(projection);
   }

   public static Matrix4f viewRotation() {
      return VIEW_ROTATION;
   }

   public static Matrix4f projection() {
      return PROJECTION;
   }
}
