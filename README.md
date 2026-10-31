# Wasteland Encounters — creature expansion test build

Fabric Minecraft 26.3 · version 0.2.3

## Current update — 0.2.3

Deathclaw spawn weight is doubled (default 2 → 4, with a one-time configuration migration). Raiders flee primed/ignited Creepers before resuming combat. Existing habitat, group sizes and creature behavior remain. See UPDATE-0.2.3.md.

## Build and install

This repository contains our code and build files. Creature models, textures,
animations and audio imported from other mods are excluded from public Git.
Their original metadata says All Rights Reserved; the root MIT license does
not cover them. ASSET-ORIGINS.txt records their sources.

For the existing personal build, import assets from your Wasteland 0.2.3 JAR:

```sh
python3 import_assets.py /path/to/wasteland-encounters-fabric-26.3-0.2.3.jar
./gradlew build
```

Windows: use `py import_assets.py ...` and `gradlew.bat build`.
Java 25 is required. The included wrapper uses Gradle 9.7.1. The importer checks
all asset hashes against asset-manifest.json before writing anything. Imported
assets stay ignored by Git. A build without these local assets fails with an
explanation rather than producing a mod with missing creatures.

Install the resulting JAR from build/libs, GeckoLib 5.5.7 for Fabric 26.3,
Fabric API 0.161.0+26.3, Fabric Loader 0.19.5+ and Java 25. For multiplayer,
install the mod and its dependencies on both the client and server.

## Earlier changes

## Test in game

Create your normal, non-Hardcore desert or badlands test world with cheats enabled. Search the creative spawn-egg tab for **Deathclaw Spawn Egg**, or use:

```
/give @s wasteland:deathclaw_spawn_egg
/summon wasteland:deathclaw ~ ~ ~
```

Creative and spectator players are ignored as prey. Use Survival when testing attacks on yourself. You can observe fights between the Deathclaw and other creatures from Creative.

Natural spawning is enabled in desert, badlands, eroded badlands, and wooded badlands, in groups of 1–2, with a low spawn weight of 4. Spawn attempts accept both daylight and darkness, subject to ordinary hostile mob caps and spawn distances.

## Raider speed update in 0.2.2

Raider pursuit and wandering are 25% slower. Pursuit's movement multiplier is now 0.825 (previously 1.1); wandering is 0.6 (previously 0.8). The change applies to existing raiders as their goals are recreated on loading. Ghoul speed is unchanged.

This update includes the 0.2.1 rendering fix below. Install only the 0.2.2 Wasteland Encounters JAR; remove older versions and restart Minecraft. GeckoLib is unchanged.

## Rendering fix in 0.2.1

Corrects custom renderer model and animation IDs to GeckoLib 5 resource IDs such as `wasteland:entity/ghoul`. Full on-disk paths caused repeated legacy-path warnings and missing animations for ghouls and Deathclaw appearances. Texture paths remain full texture paths as GeckoLib requires.

The rendering fix is included in 0.2.2; keep GeckoLib unchanged. This hotfix updates the compiled renderer's lookup strings and mod metadata. All other compiled classes and assets are byte-for-byte unchanged from 0.2.0. Source includes the corresponding fix.

The new `source/check_render_resources.py` checks the compiled JAR's IDs against the bundled GeckoLib loader normalization, required animation clips, and all appearance textures. Eight resource checks passed; a graphical client has not been run here.

## New creatures in 0.2.0

| Creature | Behavior and spawning |
| --- | --- |
| Raider | Roaming melee scavengers across Overworld land, excluding oceans and mushroom fields. Small groups of 2–4; spawn weight 6. Attacks living creatures except other raiders and Creative/Spectator players. Six skins; 24 HP. |
| Feral Ghoul | Zombie AI and the same natural spawn conditions and biome coverage as zombies, with an adult body moving at baby-zombie speed (0.345, 50% faster than an ordinary zombie). Groups of 1–4; spawn weight 20. Twelve skins; 24 HP. Burns in daylight like a zombie. |
| Protectron | Uses vanilla iron-golem village-defense, patrol, anger, melee, and iron-ingot repair behavior, with 100 HP. Appears in occupied villages near players, including existing/player-built villages. Checks every 60 seconds and adds at most one per check, capped at two Protectrons within 64 blocks of the resident. Persistent, including on Peaceful. |
| Mothman | Rare forest encounter using the `minecraft:is_forest` biome tag. Hostile to Survival players, retaliates when attacked, and wanders on the ground. 60 HP, melee damage 8, spawn weight 2, one per group. Uses ordinary dark hostile-mob spawn conditions. |
| Deathclaw variants | The original appearance plus the NukaCraft model in three skins. All retain our current Deathclaw combat, roar, grab/slam, leaping/climbing, habitat and friendly exclusions. Spawn weight and pack size are unchanged. |

Raider equipment: 80% wooden tools or a stick, 20% stone sword/axe; damageable weapons have randomized wear. Leather chest armor appears 30% of the time, with helmet/leggings/boots each independently at 15%. Equipment is unenchanted. Raiders ignore sunlight, retain ordinary hostile spawning/despawning, and are independent of vanilla raid events. Ghouls never become baby jockeys, summon ordinary zombie reinforcements, or drown-convert into regular drowned. Neither raiders nor ghouls convert villagers into ordinary zombie villagers.

Protectrons augment village defenses alongside existing iron golems. Their appearance requires a nearby living villager and an occupied village POI; an empty village is not populated. If no clear dry space is found, the check waits for the next minute. Large villages may have separate groups more than 64 blocks apart.

Deathclaw appearance is selected on spawning: 50% original, with the remaining 50% divided equally between the three NukaCraft skins. Existing saved Deathclaws keep the original appearance. Appearances survive save/load. To test a specific appearance:

```
/summon wasteland:deathclaw ~ ~ ~ {WastelandSkin:1}
/summon wasteland:deathclaw ~ ~ ~ {WastelandSkin:2}
/summon wasteland:deathclaw ~ ~ ~ {WastelandSkin:3}
```

Spawn eggs for all four new mobs are in the creative spawn-egg tab. Commands:

```
/summon wasteland:raider ~ ~ ~
/summon wasteland:ghoul ~ ~ ~
/summon wasteland:protectron ~ ~ ~
/summon wasteland:mothman ~ ~ ~
```

Spawn weights are relative weights, not percentages or guaranteed encounter rates. Hostile mobs share vanilla mob caps and ordinary player-distance rules. Ghouls add another spawn choice alongside zombies.

## Deathclaw behavior

| Mechanic | Default |
| --- | --- |
| Health | 500 HP |
| Initial aggression | 20 blocks, with line of sight |
| Observation / pursuit limit | 48 blocks |
| Swipe | 120-degree forward arc, 4 blocks, 45 HP to each caught creature |
| Swipe warning | 5 ticks / 0.25 seconds |
| Roar | Immediate full nausea blend and aim wobble for 40 ticks / 2 seconds |
| Roar stagger | 10 ticks / 0.5 seconds |
| Roar cooldown | 500 ticks / 25 seconds |
| Grab | Up to 3 blocks; creatures up to 1.5 blocks wide and 3 blocks tall |
| Grab and lift | Raises victim, holds briefly, then drives it downward |
| Direct slam | 35 HP and a 30-tick / 1.5-second stun |
| Shockwave | 15 HP within 4 blocks of the victim's ground impact, with knockback |
| Slam cooldown | 200 ticks / 10 seconds |
| Movement | Roams across desert and all badlands variants; refuses other biomes and water |
| Leap / vault | Clears fences and low obstacles while pursuing; up to 5 blocks forward |
| Climbing | Climbs a solid wall or ledge up to 6 blocks high while pursuing |
| Traversal cooldown | 40 ticks / 2 seconds after finishing or stopping |
| Targets | Other living creatures, except Deathclaws and Creative/Spectator players |
| Drops | 2–5 leather, 30 XP |

Damage figures are raw damage before armor and other normal damage rules. Terrain is not broken by attacks. Grabs have no armor check. Swipes determine victims at the strike, so moving out of the arc during the warning avoids the hit. The slam victim receives the direct hit once, rather than also receiving splash damage.

Stun blocks attacks, use actions, mining, horizontal movement, and mob AI temporarily. It ends automatically. Grab cleanup handles death, entity removal, dimension changes, and disconnected players without leaving a permanent gravity change.

## Assets and sound

The model, baked adult skin, and idle/walk/run clips come from the supplied DesertMonsterDSAddon archive. Only those visual assets are included. The three attack clips and two traversal clips were authored for this independent mob. Dragon Survival-specific motion expressions were adapted to supported GeckoLib expressions or neutral values.

Our Deathclaw keeps Minecraft's ravager roar at a lower pitch, matching the sound referenced by the original add-on. Ghouls, Mothman and Protectrons use selected ambient/hurt/death sounds from the supplied NukaCraft archive. Player camera effects follow Minecraft's screen-effect settings.

## Movement update in 0.1.2

When pursuit is blocked, the Deathclaw first tries a short vault, then a climb. It checks clearance for its entire body, including overhead space, and needs solid support at the destination. Climbing requires a wall alongside its ascent. Neither ability breaks blocks. It does not attack during traversal.

Routes remain inside its habitat and refuse water crossings, including water beneath an airborne route. New obstacles cancel movement safely. The new leap and climb animations still need an in-game visual check, especially at tight village corners.

Replace the previous Wasteland Encounters JAR with the 0.1.2 JAR; keep only one version installed. The GeckoLib dependency is unchanged.

## Habitat update in 0.1.1

Spawning, target selection, wandering and movement now share the biome tag `wasteland:deathclaw_habitat`. It includes all four vanilla desert/badlands biomes, plus modded biomes registered under the optional `c:is_desert` and `c:is_badlands` convention tags. A datapack can add other biomes to this tag. Deathclaws can chase across desert–badlands borders, while other biomes remain outside their habitat.

The existing `desertBoundary` configuration setting now applies to the entire habitat. Spawn weight and group size are unchanged.

## Configuration

The existing `naturalSpawning` toggle covers the entire creature roster. `raiderSpawnWeight`, `ghoulSpawnWeight`, `mothmanSpawnWeight`, and `protectronVillageSpawning` provide additional tuning.

The mod creates `config/wasteland-encounters.json` on first launch. Restart the world/server after changing server tuning. Default timings match the bundled attack animations; changing attack timing values does not retime those animation keyframes automatically.

## Validation and current limits

See `TEST-RESULTS.txt` for automated validation. A graphical gameplay session has not been run here: model appearance, camera feel, animation alignment during real multiplayer latency, natural encounter frequency, and combat balance still need your in-game test.

This build implements the five creature groups above. Other NukaCraft mobs and systems are not included. New mob appearance, sounds, equipment poses, alternate Deathclaw attack alignment, and village encounters need in-game visual/gameplay review.

## Source and build

The complete source is in `source/` in the test bundle. From that directory, with Java 25:

```
./gradlew build
./gradlew -PgameTests=true runGameTest
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Built mod JARs appear in `build/libs`.

Dependency reference: https://modrinth.com/mod/geckolib/version/kSxHvs99
