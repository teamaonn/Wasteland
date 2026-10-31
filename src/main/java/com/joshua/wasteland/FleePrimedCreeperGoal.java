package com.joshua.wasteland;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/** A primed Creeper owns movement priority even if no escape path is available. */
final class FleePrimedCreeperGoal extends Goal {
    private final Raider raider;
    private Creeper threat;
    private int repath;
    FleePrimedCreeperGoal(Raider raider) {
        this.raider = raider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private Creeper findThreat() {
        Creeper nearest = null;
        double distance = 16 * 16;
        for (Creeper creeper : raider.level().getEntitiesOfClass(Creeper.class, raider.getBoundingBox().inflate(16),
                c -> c.isAlive() && (c.getSwellDir() > 0 || c.isIgnited()))) {
            double d = raider.distanceToSqr(creeper);
            if (d < distance) { distance = d; nearest = creeper; }
        }
        return nearest;
    }
    @Override public boolean canUse() { threat = findThreat(); return threat != null; }
    @Override public boolean canContinueToUse() { threat = findThreat(); return threat != null; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { raider.setTarget(null); raider.getNavigation().stop(); repath = 0; }
    @Override public void stop() { raider.getNavigation().stop(); threat = null; }
    @Override public void tick() {
        raider.setTarget(null);
        if (threat == null || repath-- > 0) return;
        repath = 5;
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 away = DefaultRandomPos.getPosAway(raider, 16, 7, threat.position());
            if (away == null || away.distanceToSqr(threat.position()) <= raider.distanceToSqr(threat)) continue;
            var path = raider.getNavigation().createPath(away.x, away.y, away.z, 0);
            if (path == null || !path.canReach()) continue;
            raider.getNavigation().moveTo(path, 1.15);
            raider.getLookControl().setLookAt(away.x, away.y + raider.getEyeHeight(), away.z);
            return;
        }
    }
}
