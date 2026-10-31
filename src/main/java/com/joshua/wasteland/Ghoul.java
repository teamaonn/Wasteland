package com.joshua.wasteland;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;

public final class Ghoul extends Zombie implements GeoEntity,Appearance {
    private static final EntityDataAccessor<Integer> SKIN=SynchedEntityData.defineId(Ghoul.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public Ghoul(EntityType<? extends Zombie> t,Level l) { super(t,l); }
    public static AttributeSupplier.Builder attributes() { return Zombie.createAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.345).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b);b.define(SKIN,0); }
    public int appearance() { return entityData.get(SKIN); }
    @Override protected boolean convertsInWater() { return false; }
    @Override public boolean convertVillagerToZombieVillager(net.minecraft.server.level.ServerLevel l,net.minecraft.world.entity.npc.villager.Villager v) { return false; }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource r,net.minecraft.world.DifficultyInstance d) { }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,EntitySpawnReason r,SpawnGroupData g) {
        setBaby(false);setCanPickUpLoot(false);entityData.set(SKIN,random.nextInt(12));return g;
    }
    @Override protected void addAdditionalSaveData(ValueOutput o) { super.addAdditionalSaveData(o);o.putInt("WastelandSkin",appearance()); }
    @Override protected void readAdditionalSaveData(ValueInput i) { super.readAdditionalSaveData(i);entityData.set(SKIN,Math.clamp(i.getIntOr("WastelandSkin",0),0,11));setBaby(false); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { c.add(new AnimationController<Ghoul>("body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop(s.animatable().isSwinging()?"attack":s.isMoving()?(s.animatable().getTarget()!=null?"run":"walk"):"idle")))); }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return NewCreatures.sound("ghoul.ambient"); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return NewCreatures.sound("ghoul.hurt"); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return NewCreatures.sound("ghoul.death"); }
}
