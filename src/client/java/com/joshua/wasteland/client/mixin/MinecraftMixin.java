package com.joshua.wasteland.client.mixin;
import com.joshua.wasteland.CrowdControl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    private boolean wasteland$stunned() { var p = Minecraft.getInstance().player; return p != null && p.hasEffect(CrowdControl.STUNNED); }
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void wasteland$blockAttack(CallbackInfoReturnable<Boolean> cir) { if(wasteland$stunned()) cir.setReturnValue(false); }
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void wasteland$blockMining(boolean attack, CallbackInfo ci) { if(wasteland$stunned()) ci.cancel(); }
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void wasteland$blockUse(CallbackInfo ci) { if(wasteland$stunned()) ci.cancel(); }
}
