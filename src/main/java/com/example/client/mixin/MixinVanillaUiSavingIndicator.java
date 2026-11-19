package com.example.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
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

/** 1.21.11: the saving indicator is drawn by {@link GameRenderer#render} (26.x drew it from the HUD). */
@Mixin(GameRenderer.class)
abstract class MixinVanillaUiSavingIndicator {
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void spectre$closing(Gui hud,GuiGraphics g,DeltaTracker delta,Operation<Void> original){
        VanillaUi.extractClosing(g,delta.getGameTimeDeltaTicks());
        net.spectre.client.media.MediaPlayer.extractPinned(g);
        original.call(hud,g,delta);
    }
}
