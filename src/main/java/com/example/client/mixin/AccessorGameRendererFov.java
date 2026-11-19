package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.11: the camera does not carry its field of view, so read it from the game renderer. */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public interface AccessorGameRendererFov {
   @Invoker("getFov")
   float spectre$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
