package com.joshua.wasteland;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/** Village defender using vanilla iron-golem goals, anger, and melee damage. */
public final class Protectron extends IronGolem implements GeoEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public Protectron(EntityType<? extends IronGolem> t,Level l) { super(t,l); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<Protectron>("body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop(s.animatable().getAttackAnimationTick()>0?"melee":s.isMoving()?"walk":"idle")))); }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return NewCreatures.sound("protectron.ambient"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return NewCreatures.sound("protectron.hurt"); }
}
