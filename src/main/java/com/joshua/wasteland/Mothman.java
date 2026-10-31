package com.joshua.wasteland;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class Mothman extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public Mothman(EntityType<? extends Monster> t,Level l) { super(t,l);xpReward=15; }
    public static AttributeSupplier.Builder attributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.MOVEMENT_SPEED,.30).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.FOLLOW_RANGE,24); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.7));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,12));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<Mothman>("body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop(s.animatable().isSwinging()?"attack":s.isMoving()?"walk":"idle")))); }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return NewCreatures.sound("mothman.ambient"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return NewCreatures.sound("mothman.hurt"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return NewCreatures.sound("mothman.death"); }
}
