package com.joshua.wasteland;

import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.RandomSource;

/** Roaming melee scavenger: independent of pillager raids and Fallout gun systems. */
public final class Raider extends Zombie implements Appearance {
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(Raider.class,EntityDataSerializers.INT);
    public Raider(EntityType<? extends Zombie> type,Level level) { super(type,level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b);b.define(SKIN,0); }
    public int appearance() { return entityData.get(SKIN); }
    public static AttributeSupplier.Builder attributes() { return Zombie.createAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.FOLLOW_RANGE,24).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0); }
    public boolean validPrey(LivingEntity e) { return e!=this && !(e instanceof Raider) && e.isAlive() && !e.isSpectator() && (!(e instanceof Player p)||!p.isCreative()); }
    @Override public boolean canAttack(LivingEntity e) { return validPrey(e) && super.canAttack(e); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new FleePrimedCreeperGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,.825,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.6));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this,Raider.class));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,(entity,level)->validPrey(entity)));
    }
    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override public boolean convertVillagerToZombieVillager(net.minecraft.server.level.ServerLevel l,net.minecraft.world.entity.npc.villager.Villager v) { return false; }
    @Override protected void populateDefaultEquipmentSlots(RandomSource r,DifficultyInstance difficulty) {
        for(EquipmentSlot slot:EquipmentSlot.values()) if(slot.isArmor() || slot==EquipmentSlot.MAINHAND || slot==EquipmentSlot.OFFHAND) setItemSlot(slot,ItemStack.EMPTY);
        int roll=r.nextInt(100);
        Item weapon=roll<30?Items.WOODEN_SWORD:roll<55?Items.WOODEN_AXE:roll<70?Items.STICK:roll<80?Items.WOODEN_PICKAXE:roll<90?Items.STONE_SWORD:Items.STONE_AXE;
        ItemStack stack=new ItemStack(weapon);
        if(stack.isDamageableItem()) stack.setDamageValue(r.nextInt(Math.max(1,stack.getMaxDamage()*3/4)));
        setItemSlot(EquipmentSlot.MAINHAND,stack);
        if(r.nextFloat()<.30F) setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
        if(r.nextFloat()<.15F) setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
        if(r.nextFloat()<.15F) setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.LEATHER_LEGGINGS));
        if(r.nextFloat()<.15F) setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group) {
        setBaby(false);setCanBreakDoors(false);setCanPickUpLoot(false);
        // Skip zombie-specific baby jockeys, enchantments, and reinforcement bonuses.
        populateDefaultEquipmentSlots(random,d);entityData.set(SKIN,random.nextInt(6));return group;
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return net.minecraft.sounds.SoundEvents.PILLAGER_AMBIENT; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) { return net.minecraft.sounds.SoundEvents.PILLAGER_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return net.minecraft.sounds.SoundEvents.PILLAGER_DEATH; }
    @Override protected void addAdditionalSaveData(ValueOutput o) { super.addAdditionalSaveData(o);o.putInt("WastelandSkin",appearance()); }
    @Override protected void readAdditionalSaveData(ValueInput i) { super.readAdditionalSaveData(i);entityData.set(SKIN,Math.clamp(i.getIntOr("WastelandSkin",0),0,5));setBaby(false); }
}
