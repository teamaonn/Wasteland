package com.joshua.wasteland;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

/** Short, collision-checked vaults and wall climbs. Never changes terrain or gravity flags. */
final class DeathclawTraversal {
    record Route(int kind, List<Vec3> points) {}
    static boolean clear(Deathclaw claw, Vec3 point, double floor) {
        var level = claw.level();
        if (WastelandMod.CONFIG.desertBoundary && !DeathclawHabitat.contains(level.getBiome(BlockPos.containing(point)))) return false;
        if (!level.noCollision(claw, claw.getBoundingBox().move(point.subtract(claw.position())))) return false;
        // Check water below an airborne route too, stopping at a solid bridge/floor.
        for (int y = (int)Math.floor(point.y); y >= Math.floor(floor)-1; y--) {
            BlockPos p = BlockPos.containing(point.x, y, point.z);
            if (level.getFluidState(p).is(FluidTags.WATER)) return false;
            if (level.getBlockState(p).isFaceSturdy(level,p,Direction.UP)) break;
        }
        return true;
    }
    static boolean supported(Deathclaw claw, Vec3 point) {
        BlockPos p = BlockPos.containing(point.add(0,-.05,0));
        return claw.level().getBlockState(p).isFaceSturdy(claw.level(),p,Direction.UP);
    }
    static Route plan(Deathclaw claw, Vec3 target) {
        Vec3 start = claw.position();
        Vec3 direction = new Vec3(target.x-start.x,0,target.z-start.z).normalize();
        if (direction.lengthSqr()<.5 || claw.level().noCollision(claw,claw.getBoundingBox().move(direction.scale(.65)))) return null;
        for (int kind=1;kind<=2;kind++) {
            for (int height=kind==1 ? 2 : 6;height >= (kind==1 ? -2 : 1);height--) {
                for (double distance=kind==1 ? 2 : 1;distance<=5;distance+=.5) {
                    Vec3 end = start.add(direction.scale(distance)).add(0,height,0);
                    if (!supported(claw,end)) continue;
                    double apex = Math.max(start.y,end.y)+(kind==1 ? 2.05 : .15);
                    if (kind==2 && !wall(claw,direction,start.y,apex)) continue;
                    List<Vec3> points = new ArrayList<>();
                    Vec3 up = new Vec3(start.x,apex,start.z), across = new Vec3(end.x,apex,end.z);
                    segment(points,start,up,kind==1 ? .25 : .16);
                    segment(points,up,across,kind==1 ? .32 : .20);
                    segment(points,across,end,.25);
                    double floor = Math.min(start.y,end.y);
                    if (points.stream().allMatch(p -> clear(claw,p,floor))) return new Route(kind,List.copyOf(points));
                }
            }
        }
        return null;
    }
    private static boolean wall(Deathclaw claw, Vec3 direction, double bottom, double top) {
        // A climb needs an actual wall beside its feet throughout the ascent.
        for(double y=bottom;y<top-.3;y+=.4) {
            var probe = claw.getBoundingBox().move(direction.scale(.65)).move(0,y-bottom,0);
            probe = new net.minecraft.world.phys.AABB(probe.minX, y, probe.minZ, probe.maxX,y+.3,probe.maxZ);
            if (claw.level().noCollision(claw,probe)) return false;
        }
        return true;
    }
    private static void segment(List<Vec3> points, Vec3 from, Vec3 to, double speed) {
        int steps = Math.max(1,(int)Math.ceil(to.distanceTo(from)/speed));
        for(int i=1;i<=steps;i++) points.add(from.lerp(to,(double)i/steps));
    }
}
