package com.joshua.wasteland;

import java.io.InputStreamReader;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.*;

public final class CreatureGameTests {
    static void check(boolean ok,String message) { DeathclawGameTests.check(ok,message); }
    @GameTest(maxTicks=200,skyAccess=true)
    public void factionsAssetsAndVillages(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();
        Vec3 origin=Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(240,3,240)));
        BlockPos center=BlockPos.containing(origin);
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,true);
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-17,-1,-17),center.offset(17,10,17))) level.setBlock(p,p.getY()<origin.y?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(String name:java.util.List.of("ghoul","protectron","mothman","deathclaw_nuka")) {
            try(var geometry=new InputStreamReader(getClass().getResourceAsStream("/assets/wasteland/geckolib/models/entity/"+name+".geo.json"));var animation=new InputStreamReader(getClass().getResourceAsStream("/assets/wasteland/geckolib/animations/entity/"+name+".animation.json"))) {
                check(com.geckolib.loading.definition.geometry.Geometry.GSON.fromJson(geometry,com.geckolib.loading.definition.geometry.Geometry.class)!=null,"ported "+name+" model parses in GeckoLib 5");
                var clips=com.geckolib.loading.definition.animation.ActorAnimations.GSON.fromJson(animation,com.geckolib.loading.definition.animation.ActorAnimations.class);
                check(clips.bake(WastelandMod.id("entity/"+name),com.geckolib.loading.math.MathParser.createWithDeduplication())!=null,"all "+name+" animation clips bake in GeckoLib 5");
            }
        }
        Raider raider=NewCreatures.RAIDER.create(level,EntitySpawnReason.COMMAND),ally=NewCreatures.RAIDER.create(level,EntitySpawnReason.COMMAND);
        var villager=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);villager.setPos(origin);villager.setNoAi(true);level.addFreshEntity(villager);
        var sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);
        check(!raider.canAttack(ally)&&raider.canAttack(villager)&&raider.canAttack(sheep),"raiders attack villagers and animals while excluding fellow raiders");
        int cheap=0,armored=0;java.util.Set<Integer> skins=new java.util.HashSet<>();
        for(int i=0;i<500;i++) {
            raider.finalizeSpawn(level,level.getCurrentDifficultyAt(center),EntitySpawnReason.NATURAL,null);
            ItemStack hand=raider.getMainHandItem();
            checkEquipment(hand);
            if(hand.is(Items.STICK)||hand.is(Items.WOODEN_SWORD)||hand.is(Items.WOODEN_AXE)||hand.is(Items.WOODEN_PICKAXE)) cheap++;
            if(!raider.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) armored++;
            for(var slot:java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                var item=raider.getItemBySlot(slot);
                if(!item.isEmpty()&&!(item.is(Items.LEATHER_HELMET)||item.is(Items.LEATHER_CHESTPLATE)||item.is(Items.LEATHER_LEGGINGS)||item.is(Items.LEATHER_BOOTS))) throw new IllegalStateException("unexpected raider armor");
            }
            if(raider.isBaby()||raider.isPassenger()) throw new IllegalStateException("raider finalized as baby or jockey");
            skins.add(raider.appearance());
        }
        check(cheap>350&&armored>80&&armored<220&&skins.size()==6,"raiders mostly carry wooden gear or sticks, sometimes wear leather, and use all six skins");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());raider.addAdditionalSaveData(out);
        var restored=NewCreatures.RAIDER.create(level,EntitySpawnReason.LOAD);restored.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));
        check(restored.appearance()==raider.appearance()&&ItemStack.isSameItemSameComponents(restored.getMainHandItem(),raider.getMainHandItem()),"raider skin and worn equipment survive save/load");
        Ghoul ghoul=NewCreatures.GHOUL.create(level,EntitySpawnReason.COMMAND);ghoul.finalizeSpawn(level,level.getCurrentDifficultyAt(center),EntitySpawnReason.NATURAL,null);
        check(Math.abs(ghoul.getAttributeValue(Attributes.MOVEMENT_SPEED)-.345)<.001&&!ghoul.isBaby()&&!ghoul.isPassenger(),"adult ghouls use baby-zombie movement speed without baby jockeys");
        check(ghoul.getMainHandItem().isEmpty()&&ghoul.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)==0,"ghouls carry no weapons and cannot summon ordinary zombie reinforcements");
        check(!ghoul.convertVillagerToZombieVillager(level,villager),"ghouls do not turn villagers into ordinary zombie villagers");
        var biomes=level.registryAccess().lookupOrThrow(Registries.BIOME);
        for(var key:java.util.List.of(Biomes.FOREST,Biomes.DESERT,Biomes.MUSHROOM_FIELDS)) {
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) level.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z).fillBiomesFromNoise((a,b,c)->biomes.getOrThrow(key));
            var spawns=level.getChunkSource().getGenerator().getMobsAt(level,level.structureManager(),MobCategory.MONSTER,center).unwrap();
            boolean hasGhoul=spawns.stream().anyMatch(w->w.value().type()==NewCreatures.GHOUL),hasZombie=spawns.stream().anyMatch(w->w.value().type()==EntityTypes.ZOMBIE),hasMoth=spawns.stream().anyMatch(w->w.value().type()==NewCreatures.MOTHMAN);
            check(hasGhoul==hasZombie,"ghoul biome spawn coverage matches zombies in "+key);
            check(hasMoth==key.equals(Biomes.FOREST),"Mothman spawn pool respects forest habitat in "+key);
        }
        java.util.Set<Integer> variants=new java.util.HashSet<>();
        for(int i=0;i<120;i++) {var claw=WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.COMMAND);claw.finalizeSpawn(level,level.getCurrentDifficultyAt(center),EntitySpawnReason.NATURAL,null);variants.add(claw.appearance());
            var data=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());claw.addAdditionalSaveData(data);var copy=WastelandMod.DEATHCLAW.create(level,EntitySpawnReason.LOAD);copy.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data.buildResult()));
            if(copy.appearance()!=claw.appearance()||copy.getMaxHealth()!=500) throw new IllegalStateException("variant persistence/combat attributes changed");
        }
        check(variants.size()==4,"original and three NukaCraft Deathclaw appearances share the existing entity and retain their appearance on reload");
        check(!NewCreatures.tryVillageProtectron(level,center),"Protectrons refuse to appear outside a village");
        var robot=NewCreatures.PROTECTRON.create(level,EntitySpawnReason.COMMAND);
        check(robot instanceof net.minecraft.world.entity.animal.golem.IronGolem&&robot.getMaxHealth()==100&&robot.canAttack(ghoul)&&!robot.canAttack(EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND)),"Protectrons inherit iron-golem health and target eligibility, including creeper avoidance");
        check(robot.doHurtTarget(level,ghoul)&&ghoul.getHealth()<ghoul.getMaxHealth(),"Protectron melee damages a hostile ghoul");
        // An occupied home POI marks a real village section, including player-built villages.
        level.setBlock(center,Blocks.BED.red().defaultBlockState(),3);
        level.getPoiManager().add(center,level.registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE).getOrThrow(PoiTypes.HOME));
        level.getPoiManager().take(h->h.is(PoiTypes.HOME),(h,p)->p.equals(center),center,2);
        helper.runAfterDelay(10,()->{
            check(level.isVillage(center),"occupied home POI is recognized as a village");
            check(NewCreatures.tryVillageProtectron(level,center),"village with a resident receives its first Protectron");
            check(NewCreatures.tryVillageProtectron(level,center),"village receives a second Protectron");
            check(!NewCreatures.tryVillageProtectron(level,center),"village Protectron population is capped at two nearby defenders");
            var robots=level.getEntitiesOfClass(Protectron.class,new AABB(center).inflate(64));
            check(robots.size()==2&&robots.stream().allMatch(Mob::isPersistenceRequired),"village Protectrons persist instead of despawning with distance");
            robots.forEach(Entity::discard);villager.discard();
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) level.setChunkForced((center.getX()>>4)+x,(center.getZ()>>4)+z,false);
            helper.succeed();
        });
    }
    private static void checkEquipment(ItemStack stack) {
        if(!(stack.is(Items.WOODEN_SWORD)||stack.is(Items.WOODEN_AXE)||stack.is(Items.WOODEN_PICKAXE)||stack.is(Items.STICK)||stack.is(Items.STONE_SWORD)||stack.is(Items.STONE_AXE))||stack.isEnchanted()) throw new IllegalStateException("unexpected raider weapon/enchantment");
    }
}
