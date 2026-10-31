package com.joshua.wasteland;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;

/** Server-owned tuning. Changes take effect after restarting the world/server. */
public final class EncounterConfig {
    public double health = 500;
    public double speed = 0.36;
    public double aggressionRange = 20;
    public double observationRange = 48;
    public float swipeDamage = 45;
    public double swipeReach = 4;
    public double swipeArcDegrees = 120;
    public int swipeWindupTicks = 5;
    public int swipeRecoveryTicks = 20;
    public int roarDurationTicks = 40;
    public int roarStaggerTicks = 10;
    public int roarCooldownTicks = 500;
    public double roarRadius = 20;
    public float slamDamage = 35;
    public float shockwaveDamage = 15;
    public double shockwaveRadius = 4;
    public int slamStunTicks = 30;
    public int slamCooldownTicks = 200;
    public int spawnWeight = 4;
    public int spawnWeightRevision = 1;
    public boolean naturalSpawning = true;
    public int raiderSpawnWeight = 6;
    public int ghoulSpawnWeight = 20;
    public int mothmanSpawnWeight = 2;
    public boolean protectronVillageSpawning = true;
    public boolean desertBoundary = true;

    public static EncounterConfig load() {
        var path = FabricLoader.getInstance().getConfigDir().resolve("wasteland-encounters.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            EncounterConfig config = Files.exists(path) ? gson.fromJson(Files.readString(path), EncounterConfig.class) : new EncounterConfig();
            if (config == null) config = new EncounterConfig();
            // Upgrade existing configurations once, including customized spawn weights.
            if (Files.exists(path) && !com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject().has("spawnWeightRevision")) {
                config.spawnWeight = (int)Math.min(100L, (long)config.spawnWeight * 2);
                config.spawnWeightRevision = 1;
            }
            config.validate();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config));
            return config;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load " + path + ": " + ex.getMessage(), ex);
        }
    }
    private void validate() {
        health = finite(health, 500, 1, 10000); speed = finite(speed, .36, .01, 1);
        aggressionRange = finite(aggressionRange, 20, 1, 64);
        observationRange = finite(observationRange, 48, aggressionRange, 128);
        swipeDamage = (float)finite(swipeDamage, 45, 0, 1000);
        swipeReach = finite(swipeReach, 4, 1, 12);
        swipeArcDegrees = finite(swipeArcDegrees, 120, 1, 360);
        swipeWindupTicks = Math.clamp(swipeWindupTicks, 1, 100);
        swipeRecoveryTicks = Math.clamp(swipeRecoveryTicks, 1, 200);
        roarDurationTicks = Math.clamp(roarDurationTicks, 1, 200);
        roarStaggerTicks = Math.clamp(roarStaggerTicks, 0, 100);
        roarCooldownTicks = Math.clamp(roarCooldownTicks, roarDurationTicks + 20, 12000);
        roarRadius = finite(roarRadius, 20, 1, 64);
        slamDamage = (float)finite(slamDamage, 35, 0, 1000);
        shockwaveDamage = (float)finite(shockwaveDamage, 15, 0, 1000);
        shockwaveRadius = finite(shockwaveRadius, 4, 1, 16);
        slamStunTicks = Math.clamp(slamStunTicks, 1, 200);
        slamCooldownTicks = Math.clamp(slamCooldownTicks, 40, 12000);
        raiderSpawnWeight = Math.clamp(raiderSpawnWeight,1,100);
        ghoulSpawnWeight = Math.clamp(ghoulSpawnWeight,1,100);
        mothmanSpawnWeight = Math.clamp(mothmanSpawnWeight,1,100);
        spawnWeight = Math.clamp(spawnWeight, 1, 100);
    }
    private static double finite(double n, double fallback, double min, double max) {
        return Double.isFinite(n) ? Math.clamp(n, min, max) : fallback;
    }
}
