package com.joshua.wasteland;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Shared by spawning, prey selection, wandering and movement boundaries. */
public final class DeathclawHabitat {
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, WastelandMod.id("deathclaw_habitat"));
    private DeathclawHabitat() {}
    public static boolean contains(Holder<Biome> biome) { return biome.is(BIOMES); }
}
