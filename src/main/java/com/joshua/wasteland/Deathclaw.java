package com.joshua.wasteland;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Independent predator. No Dragon Survival gameplay or registries are loaded. */
public final class Deathclaw extends Monster implements GeoEntity, Appearance {
    public static final int IDLE = 0, SWIPE = 1, ROAR = 2, SLAM = 3;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Deathclaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAVERSAL = SynchedEntityData.defineId(Deathclaw.class, EntityDataSerializers.INT);
    private DeathclawTraversal.Route route;
    private int routeIndex, traversalCooldown;
    private double routeFloor;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(Deathclaw.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SWIPE_ANIM = RawAnimation.begin().thenPlay("swipe");
    private static final RawAnimation ROAR_ANIM = RawAnimation.begin().thenPlay("roar");
    private static final RawAnimation SLAM_ANIM = RawAnimation.begin().thenPlay("slam");
    private static final RawAnimation LEAP_ANIM = RawAnimation.begin().thenLoop("leap");
    private static final RawAnimation CLIMB_ANIM = RawAnimation.begin().thenLoop("climb");
    private int actionTick, attackCooldown, roarCooldown, slamCooldown;
    private float attackYaw;
    private LivingEntity held;
    private boolean heldNoGravity;

    public Deathclaw(EntityType<? extends Monster> type, Level level) { super(type, level); xpReward = 30; }
    private static EncounterConfig config() { return WastelandMod.CONFIG; }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, config().health)
            .add(Attributes.MOVEMENT_SPEED, config().speed).add(Attributes.ATTACK_DAMAGE, config().swipeDamage)
            .add(Attributes.FOLLOW_RANGE, config().observationRange).add(Attributes.KNOCKBACK_RESISTANCE, 1)
            .add(Attributes.ARMOR, 10).add(Attributes.STEP_HEIGHT, 1.2);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(ACTION, IDLE); builder.define(TRAVERSAL, 0); builder.define(SKIN,0); }
    public int appearance() { return entityData.get(SKIN); }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,EntitySpawnReason reason,SpawnGroupData group) {
        var result=super.finalizeSpawn(l,d,reason,group);int roll=random.nextInt(6);entityData.set(SKIN,Math.max(0,roll-2));return result;
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) { super.addAdditionalSaveData(out);out.putInt("WastelandSkin",appearance()); }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) { super.readAdditionalSaveData(in);entityData.set(SKIN,Math.clamp(in.getIntOr("WastelandSkin",0),0,3)); }
    public int action() { return entityData.get(ACTION); }
    @Override protected void registerGoals() { /* One server state machine owns navigation and combat. */ }
    private boolean prey(LivingEntity other) {
        return other != this && !(other instanceof Deathclaw) && other.isAlive() && !other.isSpectator()
            && (!(other instanceof Player p) || !p.isCreative());
    }
    private boolean inHabitat(Vec3 pos) { return DeathclawHabitat.contains(level().getBiome(BlockPos.containing(pos))); }
    private boolean allowed(Vec3 pos) {
        var p = BlockPos.containing(pos);
        return (!config().desertBoundary || inHabitat(pos))
            && !level().getFluidState(p).is(FluidTags.WATER)
            && !level().getFluidState(p.below()).is(FluidTags.WATER);
    }
    @Override public void move(MoverType type, Vec3 movement) {
        if (config().desertBoundary || !movement.equals(Vec3.ZERO)) {
            // Check intermediate points as well, so fast knockback cannot jump a river or boundary.
            int steps = Math.max(1, (int)Math.ceil(movement.horizontalDistance() * 4));
            steps = Math.min(steps, 256);
            for (int i = 1; i <= steps; i++) {
                Vec3 next = position().add(movement.x * i / steps, 0, movement.z * i / steps);
                if ((inHabitat(position()) || !config().desertBoundary) && !allowed(next)) {
                    movement = new Vec3(0, movement.y, 0); getNavigation().stop(); break;
                }
            }
        }
        super.move(type, movement);
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (traversalCooldown > 0) traversalCooldown--;
        if (route != null) { getNavigation().stop(); return; }
        if (attackCooldown > 0) attackCooldown--;
        if (roarCooldown > 0) roarCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (action() != IDLE) { tickAttack(level); return; }
        LivingEntity target = getTarget();
        setAggressive(target != null);
        if (target != null && (!prey(target) || !inHabitat(target.position()) || distanceToSqr(target) > config().observationRange * config().observationRange)) { setTarget(null); target = null; }
        if (target == null && tickCount % 10 == 0) {
            target = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config().aggressionRange),
                e -> prey(e) && inHabitat(e.position()) && distanceToSqr(e) <= config().aggressionRange * config().aggressionRange && hasLineOfSight(e))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
        }
        if (target == null) {
            // Observe without pursuing creatures in the outer 48-block zone.
            if (tickCount % 40 == 0) {
                var watched = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config().observationRange), this::prey)
                    .stream().filter(this::hasLineOfSight).min(java.util.Comparator.comparingDouble(this::distanceToSqr));
                watched.ifPresent(e -> getLookControl().setLookAt(e, 20, 20));
            }
            if (tickCount % 80 == 0) {
                Vec3 next = position().add(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
                BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(next));
                if (allowed(Vec3.atBottomCenterOf(surface))) getNavigation().moveTo(surface.getX(), surface.getY(), surface.getZ(), .65);
            }
            return;
        }
        getLookControl().setLookAt(target, 30, 30);
        double distance = distanceToSqr(target);
        if (onGround() && traversalCooldown == 0 && tickCount % 5 == 0 && distance > 2.25) {
            route = DeathclawTraversal.plan(this,target.position());
            if (route != null) {
                routeIndex = 0; routeFloor = Math.min(getY(),route.points().getLast().y);
                entityData.set(TRAVERSAL,route.kind()); getNavigation().stop();
                setYRot((float)(Math.toDegrees(Math.atan2(target.getZ()-getZ(),target.getX()-getX()))-90));
                setYBodyRot(getYRot()); return;
            }
            traversalCooldown = 10;
        }
        if (roarCooldown == 0 && distance <= config().roarRadius * config().roarRadius && hasLineOfSight(target)) {
            start(ROAR); roarCooldown = config().roarCooldownTicks;
            playSound(SoundEvents.RAVAGER_ROAR, 3, .65F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config().roarRadius), this::prey))
                if (distanceToSqr(e) <= config().roarRadius * config().roarRadius && hasLineOfSight(e)) CrowdControl.roar(e, config().roarDurationTicks, config().roarStaggerTicks);
        } else if (attackCooldown == 0 && distance <= 9 && slamCooldown == 0 && target.getBbWidth() <= 1.5 && target.getBbHeight() <= 3 && !target.isPassenger() && hasLineOfSight(target)) {
            held = target; heldNoGravity = held.isNoGravity();
            CrowdControl.hold(held, 25); start(SLAM); slamCooldown = config().slamCooldownTicks;
        } else if (attackCooldown == 0 && distance <= config().swipeReach * config().swipeReach && hasLineOfSight(target)) {
            start(SWIPE);
        } else if (tickCount % 5 == 0 && allowed(target.position())) getNavigation().moveTo(target, 1.25);
    }
    @Override public void travel(Vec3 input) {
        if (!level().isClientSide() && route != null) {
            Vec3 next = route.points().get(routeIndex);
            setDeltaMovement(Vec3.ZERO);
            if (!isAlive() || !DeathclawTraversal.clear(this,next,routeFloor)) { endTraversal(); return; }
            move(MoverType.SELF,next.subtract(position()));
            fallDistance = 0;
            if (position().distanceToSqr(next) > .0025 || ++routeIndex >= route.points().size()) endTraversal();
            return;
        }
        super.travel(input);
    }
    private void endTraversal() {
        route = null; entityData.set(TRAVERSAL,0); traversalCooldown = 40;
        setDeltaMovement(Vec3.ZERO); setOnGround(DeathclawTraversal.supported(this,position()));
    }
    private void start(int attack) {
        entityData.set(ACTION, attack); actionTick = 0; getNavigation().stop();
        LivingEntity target = getTarget();
        if (target != null) {
            attackYaw = (float)(Math.toDegrees(Math.atan2(target.getZ() - getZ(), target.getX() - getX())) - 90);
            setYRot(attackYaw); setYBodyRot(attackYaw);
        }
    }
    private void tickAttack(ServerLevel level) {
        actionTick++;
        getNavigation().stop(); setYRot(attackYaw); setYBodyRot(attackYaw);
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (action() == SWIPE) {
            if (actionTick == config().swipeWindupTicks) swipe(level);
            if (actionTick >= config().swipeWindupTicks + config().swipeRecoveryTicks) finish();
        } else if (action() == ROAR) {
            if (actionTick >= 20) finish();
        } else if (action() == SLAM) {
            if (actionTick > 18) { if (actionTick >= 24) finish(); return; }
            if (held == null || !prey(held) || held.level() != level || distanceToSqr(held) > 100) { release(); finish(); return; }
            if (actionTick <= 18) {
                Vec3 direction = Vec3.directionFromRotation(0, attackYaw);
                double lift = actionTick <= 10 ? 2.4 * actionTick / 10 : (actionTick < 15 ? 2.4 : 2.4 * (18 - actionTick) / 3);
                Vec3 place = position().add(direction.scale(2.1)).add(0, lift, 0);
                // Do not place a grabbed victim through a wall or ceiling.
                Vec3 delta = place.subtract(held.position());
                if (level.noCollision(held, held.getBoundingBox().move(delta))) CrowdControl.position(held, place);
                held.setDeltaMovement(Vec3.ZERO); held.fallDistance = 0;
                if (actionTick == 18) {
                    // Collision resolution finds the ground instead of teleporting through a slope.
                    held.move(MoverType.SELF, new Vec3(0, -6, 0));
                    if (held.onGround()) impact(level);
                    else { release(); finish(); } // A cliff releases the victim safely; no airborne shockwave.
                }
            }
            if (actionTick >= 24) { release(); finish(); }
        }
    }
    private void swipe(ServerLevel level) {
        Vec3 forward = Vec3.directionFromRotation(0, attackYaw);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config().swipeReach, 1, config().swipeReach), this::prey)) {
            Vec3 offset = e.position().subtract(position());
            if (CombatGeometry.inArc(offset.x, offset.z, forward.x, forward.z, config().swipeReach, config().swipeArcDegrees) && hasLineOfSight(e)) {
                if (e.hurtServer(level, damageSources().mobAttack(this), config().swipeDamage)) e.knockback(1.5, -offset.x, -offset.z, damageSources().mobAttack(this), config().swipeDamage);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2, .6F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + forward.x * 2, getY() + 1.5, getZ() + forward.z * 2, 3, .4, .3, .4, 0);
    }
    private void impact(ServerLevel level) {
        LivingEntity victim = held;
        Vec3 center = victim.position();
        release();
        victim.hurtServer(level, damageSources().mobAttack(this), config().slamDamage);
        CrowdControl.stun(victim, config().slamStunTicks);
        var box = new net.minecraft.world.phys.AABB(center, center).inflate(config().shockwaveRadius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::prey)) {
            if (e == victim || e.position().distanceToSqr(center) > config().shockwaveRadius * config().shockwaveRadius || !hasLineOfSight(e)) continue;
            e.hurtServer(level, damageSources().mobAttack(this), config().shockwaveDamage);
            Vec3 offset = e.position().subtract(center); e.knockback(1.25, -offset.x, -offset.z, damageSources().mobAttack(this), config().shockwaveDamage);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2, .65F);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + .15, center.z, 35, 1.7, .1, 1.7, .15);
    }
    private void release() {
        if (held != null) { held.setNoGravity(heldNoGravity); CrowdControl.releaseHold(held); held = null; }
    }
    private void finish() { entityData.set(ACTION, IDLE); attackCooldown = 10; }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) { release(); super.die(source); }
    @Override public void remove(Entity.RemovalReason reason) { release(); super.remove(reason); }
    @Override public boolean canBeLeashed() { return false; }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animations; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Deathclaw>("body", 0, test -> {
            Deathclaw claw = test.animatable();
            return test.setAndContinue(switch (claw.action()) {
                case SWIPE -> SWIPE_ANIM; case ROAR -> ROAR_ANIM; case SLAM -> SLAM_ANIM;
                default -> claw.entityData.get(TRAVERSAL) == 1 ? LEAP_ANIM : claw.entityData.get(TRAVERSAL) == 2 ? CLIMB_ANIM : test.isMoving() ? (claw.isAggressive() || claw.getDeltaMovement().horizontalDistanceSqr() > .02 ? RUN_ANIM : WALK_ANIM) : IDLE_ANIM;
            });
        }));
    }
}
