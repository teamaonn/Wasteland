package com.joshua.wasteland;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** World-tick deadlines, never wall-clock timers. No permanent AI/gravity changes. */
public final class CrowdControl {
    public static final Holder<MobEffect> DAZED = effect("dazed", 0x916739);
    public static final Holder<MobEffect> STUNNED = effect("stunned", 0xB38E60);
    private record Lock(LivingEntity entity, net.minecraft.world.level.Level level, long expires, Vec3 anchor, boolean hold) {}
    private static final Map<UUID, Lock> LOCKS = new HashMap<>();
    private static Holder<MobEffect> effect(String name, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, WastelandMod.id(name), new MobEffect(MobEffectCategory.HARMFUL, color) {});
    }
    public static boolean locked(LivingEntity e) {
        var lock = LOCKS.get(e.getUUID());
        return lock != null && lock.level == e.level() && lock.expires > e.level().getGameTime();
    }
    public static void roar(LivingEntity e, int ticks, int stagger) {
        e.addEffect(new MobEffectInstance(DAZED, ticks, 0, false, false, true));
        e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0, false, false, false));
        if (stagger > 0) stun(e, stagger);
    }
    public static void stun(LivingEntity e, int ticks) { lock(e, ticks, false); }
    public static void hold(LivingEntity e, int ticks) { lock(e, ticks, true); }
    private static void lock(LivingEntity e, int ticks, boolean hold) {
        long until = e.level().getGameTime() + ticks;
        Lock old = LOCKS.get(e.getUUID());
        if (old != null) until = Math.max(until, old.expires);
        LOCKS.put(e.getUUID(), new Lock(e, e.level(), until, e.position(), hold));
        e.addEffect(new MobEffectInstance(STUNNED, ticks, 0, false, false, true));
        e.stopUsingItem();
        if (e instanceof Mob mob) mob.getNavigation().stop();
    }
    public static void releaseHold(LivingEntity e) {
        Lock lock = LOCKS.get(e.getUUID());
        if (lock != null && lock.hold) { LOCKS.remove(e.getUUID()); e.removeEffect(STUNNED); }
    }
    public static void position(LivingEntity e, Vec3 pos) {
        if (e instanceof ServerPlayer p) p.connection.teleport(pos.x, pos.y, pos.z, p.getYRot(), p.getXRot());
        else e.teleportTo(pos.x, pos.y, pos.z);
    }
    public static void install() {
        AttackEntityCallback.EVENT.register((p, level, hand, target, hit) -> locked(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((p, level, hand, target, hit) -> locked(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((p, level, hand, hit) -> locked(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((p, level, hand) -> locked(p) ? InteractionResult.FAIL : InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((level, p, pos, state, blockEntity) -> !locked(p));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var iterator = LOCKS.entrySet().iterator();
            while (iterator.hasNext()) {
                Lock lock = iterator.next().getValue(); LivingEntity e = lock.entity;
                if (e.level() != lock.level || !e.isAlive() || e.isRemoved() || e.level().getGameTime() >= lock.expires || (e instanceof ServerPlayer p && p.hasDisconnected())) {
                    e.removeEffect(STUNNED); iterator.remove(); continue;
                }
                if (!lock.hold) {
                    // Hold horizontal position while retaining gravity and falling safety.
                    position(e, new Vec3(lock.anchor.x, e.getY(), lock.anchor.z));
                    e.setDeltaMovement(0, e.getDeltaMovement().y, 0);
                }
                if (e instanceof Mob mob) { mob.getNavigation().stop(); mob.setTarget(null); }
                e.stopUsingItem();
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> LOCKS.clear());
    }
}
