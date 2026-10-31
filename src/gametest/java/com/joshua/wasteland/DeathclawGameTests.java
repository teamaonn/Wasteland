package com.joshua.wasteland;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class DeathclawGameTests {
    static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        System.out.println("DEATHCLAW PASS: " + message);
    }
    static void set(Deathclaw claw, String name, Object value) throws Exception {
        Field f = Deathclaw.class.getDeclaredField(name); f.setAccessible(true); f.set(claw, value);
    }
    static void call(Deathclaw claw, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method m = Deathclaw.class.getDeclaredMethod(name, types); m.setAccessible(true); m.invoke(claw, arguments);
    }
    static IronGolem golem(ServerLevel level, Vec3 pos) {
        IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
        golem.setPos(pos); golem.setNoAi(true); level.addFreshEntity(golem); return golem;
    }
    @GameTest(maxTicks = 150, skyAccess = true)
    public void combatAndCleanup(GameTestHelper helper) throws Exception {
        var origin=helper.absolutePos(new BlockPos(-70,2,-70));
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) helper.getLevel().setChunkForced((origin.getX()>>4)+x,(origin.getZ()>>4)+z,true);
        helper.runAfterDelay(5,()->{try { combat(helper); } catch(Exception e) { throw new RuntimeException(e); }});
    }
    private void combat(GameTestHelper helper) throws Exception {
        try (var model = new java.io.InputStreamReader(DeathclawGameTests.class.getResourceAsStream("/assets/wasteland/geckolib/models/entity/deathclaw.geo.json"));
             var animation = new java.io.InputStreamReader(DeathclawGameTests.class.getResourceAsStream("/assets/wasteland/geckolib/animations/entity/deathclaw.animation.json"))) {
            var geometry = com.geckolib.loading.definition.geometry.Geometry.GSON.fromJson(model, com.geckolib.loading.definition.geometry.Geometry.class);
            check(geometry != null, "GeckoLib 5 accepts the ported model schema");
            var clips = com.geckolib.loading.definition.animation.ActorAnimations.GSON.fromJson(animation, com.geckolib.loading.definition.animation.ActorAnimations.class);
            check(clips.bake(WastelandMod.id("entity/deathclaw"), com.geckolib.loading.math.MathParser.createWithDeduplication()) != null,
                "GeckoLib 5 bakes all eight animation clips and their expressions");
        }
        ServerLevel level = helper.getLevel();
        Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(-70, 2, -70)));
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(origin).offset(-7,-1,-7), BlockPos.containing(origin).offset(19,6,7)))
            level.setBlock(pos, pos.getY() < origin.y ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        Deathclaw claw = WastelandMod.DEATHCLAW.create(level, EntitySpawnReason.COMMAND);
        claw.setPos(origin); claw.setNoAi(true); level.addFreshEntity(claw);
        check(claw.getMaxHealth() == 500, "registered health is 500 HP");
        IronGolem front = golem(level, origin.add(0,0,2.8));
        IronGolem left = golem(level, origin.add(2.2,0,2.8));
        IronGolem right = golem(level, origin.add(-2.2,0,2.8));
        IronGolem behind = golem(level, origin.add(0,0,-3));
        Deathclaw ally = WastelandMod.DEATHCLAW.create(level, EntitySpawnReason.COMMAND);
        ally.setPos(origin.add(.5,0,2)); ally.setNoAi(true); level.addFreshEntity(ally);
        claw.setTarget(front);
        call(claw, "start", new Class<?>[]{int.class}, Deathclaw.SWIPE);
        for(int i=0;i<4;i++) call(claw, "tickAttack", new Class<?>[]{ServerLevel.class}, level);
        check(front.getHealth() == 100 && left.getHealth() == 100, "swipe does not damage during first four windup ticks");
        call(claw, "tickAttack", new Class<?>[]{ServerLevel.class}, level);
        check(front.getHealth() == 55 && left.getHealth() == 55 && right.getHealth() == 55, "fifth-tick swipe deals equal 45 HP to all three forward targets");
        check(behind.getHealth() == 100 && ally.getHealth() == 500, "swipe excludes creatures behind it and other Deathclaws");
        check(!CombatGeometry.inArc(0,4.1,0,1,4,120), "swipe excludes targets past four-block reach");
        IronGolem dodger = golem(level, origin.add(0,0,3));
        claw.setTarget(dodger); call(claw,"start",new Class<?>[]{int.class},Deathclaw.SWIPE);
        for(int i=0;i<4;i++)call(claw,"tickAttack",new Class<?>[]{ServerLevel.class},level);
        dodger.setPos(origin.add(0,0,-3));
        call(claw,"tickAttack",new Class<?>[]{ServerLevel.class},level);
        check(dodger.getHealth()==100,"dodging out of the arc during windup avoids damage");
        CrowdControl.roar(behind,40,10);
        check(behind.hasEffect(CrowdControl.DAZED) && CrowdControl.locked(behind), "roar applies disorientation and a temporary AI lock");
        set(claw,"held",behind);set(claw,"heldNoGravity",false);behind.setNoGravity(true);CrowdControl.hold(behind,25);
        claw.discard();
        check(!behind.isNoGravity() && !CrowdControl.locked(behind), "removing a grabbing Deathclaw restores victim gravity and releases its hold");
        Vec3 arena = origin.add(10,0,0);
        Deathclaw slammer = WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.COMMAND);
        slammer.setPos(arena);slammer.setNoAi(true);level.addFreshEntity(slammer);
        var victim = EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);
        victim.setPos(arena.add(0,0,2.1));victim.setNoAi(true);
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        victim.setHealth(100);level.addFreshEntity(victim);
        IronGolem splash = golem(level,arena.add(2.5,0,2.1));
        Deathclaw safeAlly = WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.COMMAND);
        safeAlly.setPos(arena.add(-2.5,0,2.1));safeAlly.setNoAi(true);level.addFreshEntity(safeAlly);
        slammer.setTarget(victim);set(slammer,"held",victim);set(slammer,"heldNoGravity",false);
        CrowdControl.hold(victim,25);call(slammer,"start",new Class<?>[]{int.class},Deathclaw.SLAM);
        for(int i=0;i<18;i++)call(slammer,"tickAttack",new Class<?>[]{ServerLevel.class},level);
        check(victim.getHealth()==65, "slam deals 35 HP once to the grabbed victim");
        if(splash.getHealth()!=85) {
            var box=new net.minecraft.world.phys.AABB(victim.position(),victim.position()).inflate(4);
            var hit=level.clip(new net.minecraft.world.level.ClipContext(slammer.getEyePosition(),splash.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,slammer));
            System.out.println("SLAM DIAG slammer="+slammer.position()+" victim="+victim.position()+" splash="+splash.position()+" dist="+victim.position().distanceToSqr(splash.position())+" LOS="+slammer.hasLineOfSight(splash)+" inQuery="+level.getEntitiesOfClass(LivingEntity.class,box).contains(splash)+" clip="+hit.getType()+" block="+hit.getBlockPos());
        }
        check(splash.getHealth()==85 && safeAlly.getHealth()==500, "impact deals 15 HP nearby and spares other Deathclaws (splash="+splash.getHealth()+", ally="+safeAlly.getHealth()+")");
        check(victim.onGround() && CrowdControl.locked(victim) && !victim.isNoGravity(), "slam stuns at ground impact and releases the gravity state");
        check(level.getBlockState(BlockPos.containing(arena).below()).is(Blocks.STONE), "slam leaves terrain intact");
        int cx = BlockPos.containing(origin).getX() >> 4, cz = BlockPos.containing(origin).getZ() >> 4;
        var biomes = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var desert = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT);
        var plains = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        for(int dx=-1;dx<=2;dx++) for(int dz=-1;dz<=1;dz++)
            level.getChunk(cx+dx,cz+dz).fillBiomesFromNoise((qx,qy,qz)->qx < (cx+1)*4 ? desert : plains);
        Vec3 desertCenter = new Vec3(cx*16+8,origin.y,cz*16+8);
        Deathclaw boundary = WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.COMMAND);
        boundary.setNoAi(true);boundary.setPos(desertCenter);level.addFreshEntity(boundary);
        check(level.getBiome(BlockPos.containing(desertCenter)).is(net.minecraft.world.level.biome.Biomes.DESERT), "boundary test starts inside a real desert biome");
        double edge=desertCenter.x;
        while(edge < desertCenter.x+24 && level.getBiome(BlockPos.containing(edge,origin.y,desertCenter.z)).is(net.minecraft.world.level.biome.Biomes.DESERT)) edge+=.25;
        check(edge < desertCenter.x+24, "boundary test locates the adjoining plains");
        boundary.setPos(edge-.25,origin.y,desertCenter.z);
        double before=boundary.getX();boundary.move(MoverType.SELF,new Vec3(.5,0,0));
        check(boundary.getX()==before,"movement refuses to cross the desert boundary");
        boundary.setPos(desertCenter);
        level.setBlock(BlockPos.containing(desertCenter.add(0,0,1)),Blocks.WATER.defaultBlockState(),2);
        boundary.move(MoverType.SELF,new Vec3(0,0,1));
        check(boundary.position().distanceToSqr(desertCenter)<.001,"movement refuses to enter water");
        level.setBlock(BlockPos.containing(desertCenter.add(0,0,1)),Blocks.AIR.defaultBlockState(),2);
        level.setBlock(BlockPos.containing(desertCenter).below(),Blocks.SAND.defaultBlockState(),2);
        level.setBlock(BlockPos.containing(desertCenter),Blocks.AIR.defaultBlockState(),2);
        check(SpawnPlacements.checkSpawnRules(WastelandMod.DEATHCLAW,level,EntitySpawnReason.NATURAL,BlockPos.containing(desertCenter),level.getRandom()),"natural spawn predicate accepts dry desert ground");
        for (var biomeKey : java.util.List.of(net.minecraft.world.level.biome.Biomes.DESERT,
                net.minecraft.world.level.biome.Biomes.BADLANDS, net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS,
                net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS)) {
            var habitat = biomes.getOrThrow(biomeKey);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) level.getChunk(cx+dx,cz+dz).fillBiomesFromNoise((qx,qy,qz)->habitat);
            boundary.setPos(desertCenter);
            check(SpawnPlacements.checkSpawnRules(WastelandMod.DEATHCLAW,level,EntitySpawnReason.NATURAL,BlockPos.containing(desertCenter),level.getRandom()),
                "natural spawning accepts " + biomeKey);
            boundary.move(MoverType.SELF,new Vec3(.5,0,0));
            check(boundary.getX()>desertCenter.x+.4,"movement accepts " + biomeKey);
        }
        // A continuous desert/badlands region is one habitat, rather than two fenced zones.
        for(int dx=-1;dx<=2;dx++) for(int dz=-1;dz<=1;dz++)
            level.getChunk(cx+dx,cz+dz).fillBiomesFromNoise((qx,qy,qz)->qx < (cx+1)*4 ? desert : biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.BADLANDS));
        boundary.setPos(edge-.25,origin.y,desertCenter.z);
        before=boundary.getX();boundary.move(MoverType.SELF,new Vec3(.5,0,0));
        check(boundary.getX()>before+.4,"movement crosses freely from desert into badlands");
        CrowdControl.stun(dodger,2);
        helper.runAfterDelay(5, () -> {
            check(!CrowdControl.locked(dodger) && !dodger.hasEffect(CrowdControl.STUNNED), "stun expires without leaving its victim locked");
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setChunkForced((BlockPos.containing(origin).getX()>>4)+x,(BlockPos.containing(origin).getZ()>>4)+z,false);
            helper.succeed();
        });
    }
}
