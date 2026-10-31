package com.joshua.wasteland.mixin;
import com.joshua.wasteland.CrowdControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void wasteland$stunStopsAI(CallbackInfo ci) {
        if (CrowdControl.locked((Mob)(Object)this)) ci.cancel();
    }
}
