package com.joshua.wasteland;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class TraversalGameTests {
    @GameTest(maxTicks=150,skyAccess=true)
    public void obstacles(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(70,3,70)));
        start = start.add(-.1,0,0);
        BlockPos base = BlockPos.containing(start);
        var desert = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) level.getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z).fillBiomesFromNoise((a,b,c)->desert);
        for(BlockPos p:BlockPos.betweenClosed(base.offset(-5,-1,-5),base.offset(9,11,5)))
            level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        Deathclaw claw = WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.COMMAND);
        claw.setNoAi(true);claw.setPos(start);level.addFreshEntity(claw);claw.setOnGround(true);
        for(int z=-5;z<=5;z++) level.setBlock(base.offset(1,0,z),Blocks.OAK_FENCE.defaultBlockState(),3);
        Vec3 target = start.add(4,0,0);
        var leap = DeathclawTraversal.plan(claw,target);
        DeathclawGameTests.check(leap!=null && leap.kind()==1,"fence obstruction produces a collision-checked leap");
        var prey = DeathclawGameTests.golem(level,target);claw.setTarget(prey);claw.tickCount=10;
        claw.customServerAiStep(level);
        var routeField=Deathclaw.class.getDeclaredField("route");routeField.setAccessible(true);
        DeathclawGameTests.check(routeField.get(claw)!=null,"pursuit AI starts traversal before attacking through a fence");
        for(int i=0;i<100 && routeField.get(claw)!=null;i++) claw.travel(Vec3.ZERO);
        DeathclawGameTests.check(claw.getX()>start.x+2 && claw.onGround() && !claw.isNoGravity(),"leap crosses the fence and lands without changing gravity");
        DeathclawGameTests.check(level.getBlockState(base.offset(1,0,0)).is(Blocks.OAK_FENCE),"traversal leaves fencing intact");
        claw.setPos(start);claw.setOnGround(true);
        for(int z=-5;z<=5;z++) for(int y=0;y<4;y++) level.setBlock(base.offset(1,y,z),Blocks.STONE.defaultBlockState(),3);
        claw.setPos(start.add(-.4,0,0));
        var climb=DeathclawTraversal.plan(claw,start.add(3,4,0));
        DeathclawGameTests.check(climb!=null && climb.kind()==2,"four-block wall uses climbing when a leap cannot clear it");
        DeathclawGameTests.set(claw,"route",climb);DeathclawGameTests.set(claw,"routeIndex",0);DeathclawGameTests.set(claw,"routeFloor",start.y);
        for(int i=0;i<150 && routeField.get(claw)!=null;i++) claw.travel(Vec3.ZERO);
        DeathclawGameTests.check(claw.getY()>=start.y+4 && claw.onGround(),"climb reaches and stands on the wall top");
        claw.setPos(start);
        for(int z=-5;z<=5;z++) for(int y=0;y<4;y++) level.setBlock(base.offset(1,y,z),y==0?Blocks.OAK_FENCE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int x=-3;x<=7;x++) for(int z=-5;z<=5;z++) level.setBlock(base.offset(x,4,z),Blocks.STONE.defaultBlockState(),3);
        DeathclawGameTests.check(DeathclawTraversal.plan(claw,target)==null,"low ceiling prevents an unsafe leap or climb");
        for(int x=-3;x<=7;x++) for(int z=-5;z<=5;z++) level.setBlock(base.offset(x,4,z),Blocks.AIR.defaultBlockState(),3);
        for(int x=1;x<=5;x++) for(int z=-5;z<=5;z++) level.setBlock(base.offset(x,-1,z),Blocks.WATER.defaultBlockState(),3);
        DeathclawGameTests.check(DeathclawTraversal.plan(claw,target)==null,"airborne traversal refuses a water crossing");
        for(int x=1;x<=5;x++) for(int z=-5;z<=5;z++) level.setBlock(base.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        leap=DeathclawTraversal.plan(claw,target);
        DeathclawGameTests.set(claw,"route",leap);DeathclawGameTests.set(claw,"routeIndex",0);DeathclawGameTests.set(claw,"routeFloor",start.y);
        level.setBlock(base.above(3),Blocks.STONE.defaultBlockState(),3);
        claw.travel(Vec3.ZERO);
        DeathclawGameTests.check(routeField.get(claw)==null && claw.position().distanceToSqr(start)<.001 && !claw.isNoGravity(),"new obstruction safely cancels traversal before moving");
        level.setBlock(base.above(3),Blocks.AIR.defaultBlockState(),3);
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) level.getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z).fillBiomesFromNoise((a,b,c)->plains);
        DeathclawGameTests.check(!DeathclawTraversal.clear(claw,start.add(3,2,0),start.y),"airborne route validation rejects non-habitat biomes");
        claw.discard();prey.discard();helper.succeed();
    }
}
