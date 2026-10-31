# Wasteland Encounters 0.2.3

- Double Deathclaw natural spawn weight, default 2 to 4. Existing configuration files are migrated once using spawnWeightRevision; customized weights are doubled once, respecting the existing maximum of 100.
- Preserve habitat restrictions, group sizes, equipment, sounds, animation assets and combat tuning.
- Raiders prioritize fleeing primed or manually ignited Creepers within 16 blocks. They stop melee pursuit and attempt escape paths; if trapped, the escape goal still blocks melee rather than attacking an active bomb. They resume normal behavior when the priming threat ends.

Source remains buildable with Java 25 and Gradle 9.7.1 using the included Gradle wrapper. Existing GeckoLib 5.5.7 is still required.
