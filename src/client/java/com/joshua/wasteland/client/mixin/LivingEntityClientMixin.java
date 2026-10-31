package com.joshua.wasteland.client.mixin;

import com.joshua.wasteland.CrowdControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skip vanilla nausea's fade-in only during the Deathclaw's short roar effect. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {
    @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
    private void wasteland$instantDisorientation(Holder<MobEffect> effect, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (effect.equals(MobEffects.NAUSEA) && ((LivingEntity)(Object)this).hasEffect(CrowdControl.DAZED)) cir.setReturnValue(1F);
    }
}
