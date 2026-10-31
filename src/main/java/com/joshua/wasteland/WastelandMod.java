package com.joshua.wasteland;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;

public final class WastelandMod implements ModInitializer {
    public static final String ID = "wasteland";
    public static final EncounterConfig CONFIG = EncounterConfig.load();
    public static final ResourceKey<EntityType<?>> DEATHCLAW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("deathclaw"));
    public static final EntityType<Deathclaw> DEATHCLAW = Registry.register(BuiltInRegistries.ENTITY_TYPE, DEATHCLAW_KEY,
        EntityType.Builder.of(Deathclaw::new, MobCategory.MONSTER).sized(1.8F, 3.5F).clientTrackingRange(12).updateInterval(1).build(DEATHCLAW_KEY));
    public static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, id("deathclaw_spawn_egg"));
    public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
        new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(DEATHCLAW)));
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    @Override public void onInitialize() {
        FabricDefaultAttributeRegistry.register(DEATHCLAW, Deathclaw.attributes());
        SpawnPlacements.register(DEATHCLAW, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && DeathclawHabitat.contains(level.getBiome(pos)) && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type));
        if (CONFIG.naturalSpawning) BiomeModifications.addSpawn(BiomeSelectors.tag(DeathclawHabitat.BIOMES), MobCategory.MONSTER, DEATHCLAW, CONFIG.spawnWeight, 1, 2);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(EGG));
        NewCreatures.install();
        CrowdControl.install();
    }
}
