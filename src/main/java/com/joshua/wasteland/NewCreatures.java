package com.joshua.wasteland;

import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;

public final class NewCreatures {
    public static final EntityType<Raider> RAIDER=register("raider",Raider::new,MobCategory.MONSTER,.6F,1.95F);
    public static final EntityType<Ghoul> GHOUL=register("ghoul",Ghoul::new,MobCategory.MONSTER,.6F,1.95F);
    public static final EntityType<Protectron> PROTECTRON=register("protectron",Protectron::new,MobCategory.MISC,1.0F,2.0F);
    public static final EntityType<Mothman> MOTHMAN=register("mothman",Mothman::new,MobCategory.MONSTER,1.4F,2.3F);
    private static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    private static final List<Item> EGGS=new ArrayList<>();
    private static <T extends Mob> EntityType<T> register(String name,EntityType.EntityFactory<T> factory,MobCategory category,float width,float height) {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,WastelandMod.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.of(factory,category).sized(width,height).clientTrackingRange(10).updateInterval(2).build(key));
    }
    private static void egg(String name,EntityType<?> type) {
        var key=ResourceKey.create(Registries.ITEM,WastelandMod.id(name+"_spawn_egg"));
        EGGS.add(Registry.register(BuiltInRegistries.ITEM,key,new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type))));
    }
    public static SoundEvent sound(String name) { return SOUNDS.get(name); }
    public static void install() {
        for(String mob:List.of("ghoul","mothman","protectron")) for(String kind:List.of("ambient","hurt","death")) {
            if(mob.equals("protectron") && kind.equals("death")) continue;
            var id=WastelandMod.id("entity."+mob+"."+kind);SOUNDS.put(mob+"."+kind,Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));
        }
        FabricDefaultAttributeRegistry.register(RAIDER,Raider.attributes());FabricDefaultAttributeRegistry.register(GHOUL,Ghoul.attributes());
        FabricDefaultAttributeRegistry.register(PROTECTRON,Protectron.createAttributes());FabricDefaultAttributeRegistry.register(MOTHMAN,Mothman.attributes());
        SpawnPlacements.register(RAIDER,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(GHOUL,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules);
        SpawnPlacements.register(MOTHMAN,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,level,reason,pos,random)->level.getBiome(pos).is(BiomeTags.IS_FOREST)&&Monster.checkMonsterSpawnRules(type,level,reason,pos,random));
        var config=WastelandMod.CONFIG;
        if(config.naturalSpawning) {
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_OCEAN).negate()).and(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS).negate()),MobCategory.MONSTER,RAIDER,config.raiderSpawnWeight,2,4);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.ZOMBIE),MobCategory.MONSTER,GHOUL,config.ghoulSpawnWeight,1,4);
            BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST),MobCategory.MONSTER,MOTHMAN,config.mothmanSpawnWeight,1,1);
        }
        egg("raider",RAIDER);egg("ghoul",GHOUL);egg("protectron",PROTECTRON);egg("mothman",MOTHMAN);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out->EGGS.forEach(out::accept));
        ServerTickEvents.END_LEVEL_TICK.register(level->{if(config.naturalSpawning && config.protectronVillageSpawning && level.getGameTime()%1200==0) villagePatrols(level);});
    }
    static void villagePatrols(ServerLevel level) {
        for(var player:level.players()) {
            if(player.isSpectator()) continue;
            for(Villager villager:level.getEntitiesOfClass(Villager.class,player.getBoundingBox().inflate(48),v->v.isAlive()&&level.isVillage(v.blockPosition()))) {
                if(tryVillageProtectron(level,villager.blockPosition())) break;
            }
        }
    }
    static boolean tryVillageProtectron(ServerLevel level,BlockPos center) {
        if(!level.isVillage(center) || level.getEntitiesOfClass(Villager.class,new AABB(center).inflate(32),Entity::isAlive).isEmpty()) return false;
        if(level.getEntitiesOfClass(Protectron.class,new AABB(center).inflate(64),Entity::isAlive).size()>=2) return false;
        for(int attempt=0;attempt<12;attempt++) {
            BlockPos ground=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center.offset(level.getRandom().nextInt(25)-12,0,level.getRandom().nextInt(25)-12));
            if(!level.isVillage(ground) || !level.getFluidState(ground).isEmpty() || !level.getBlockState(ground.below()).isFaceSturdy(level,ground.below(),Direction.UP)) continue;
            Protectron robot=PROTECTRON.create(level,EntitySpawnReason.NATURAL);if(robot==null) return false;
            robot.setPos(Vec3.atBottomCenterOf(ground));
            if(!level.noCollision(robot) || level.containsAnyLiquid(robot.getBoundingBox())) continue;
            robot.finalizeSpawn(level,level.getCurrentDifficultyAt(ground),EntitySpawnReason.NATURAL,null);robot.setPersistenceRequired();
            level.addFreshEntity(robot);return true;
        }
        return false;
    }
}
