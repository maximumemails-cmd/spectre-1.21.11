package com.example.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.spectre.client.module.impl.render.TimeChangerModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Override the displayed clock without altering server synchronization or tick rate.
 * <p>
 * 1.21.11: there is no world-clock registry (26.x {@code ClientClockManager}); the client day time
 * lives on {@link ClientLevel.ClientLevelData}. Only the overworld clock is overridden, as before.
 */
@Mixin(ClientLevel.ClientLevelData.class)
public abstract class MixinTimeChangerClientLevel {
    @Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
    private void spectre$displayTime(CallbackInfoReturnable<Long> cir) {
        var module = TimeChangerModule.active;
        if (module == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getLevelData() == (Object) this && level.dimension() == Level.OVERWORLD) {
            cir.setReturnValue(module.forcedDayTime());
        }
    }
}
