package com.example.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.spectre.client.gui.VanillaUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** 1.21.11: screen changes go through {@link Minecraft#setScreen}; the saving indicator is drawn from {@link GameRenderer#render}. */
@Mixin(Minecraft.class)
abstract class MixinVanillaUiGui {
    @Inject(method="setScreen",at=@At("HEAD"))
    private void spectre$transition(Screen next,CallbackInfo ci){VanillaUi.changingScreen(next);}
}
