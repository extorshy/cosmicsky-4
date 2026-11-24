package com.example.skystudio.mixin;

import com.example.skystudio.SkyConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides the time the CLIENT uses for sky/lighting only. The server and other players are
 * untouched, so it is purely cosmetic (no gameplay advantage).
 *
 * UNVERIFIED against 26.3: the target method name. defaultRequire=0 means that if it does not
 * exist the game still launches and the time override simply has no effect.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeMixin {
    @Inject(method = "getOverworldClockTime", at = @At("HEAD"), cancellable = true)
    private void skystudio$overrideTime(CallbackInfoReturnable<Long> cir) {
        Long t = SkyConfig.currentOverrideTicks();
        if (t != null) cir.setReturnValue(t);
    }
}
