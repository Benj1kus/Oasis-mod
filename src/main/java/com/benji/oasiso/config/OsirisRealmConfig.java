package com.benji.oasiso.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class OsirisRealmConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Azumaal - base stats
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_DAMAGE_SCALER_MAX_MULTIPLIER;

    // Azumaal - global attack timing/search
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_TARGET_SEARCH_RANGE;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_COOLDOWN_MIN;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_COOLDOWN_MAX;
    public static final ForgeConfigSpec.IntValue AZUMAAL_POST_SPAWN_ATTACK_COOLDOWN;

    // Azumaal - melee
    public static final ForgeConfigSpec.IntValue AZUMAAL_MELEE_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_1_PREPARE_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_1_DAMAGE_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_2_PREPARE_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_ATTACK_2_DAMAGE_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_DASH_MOVE_TICKS;
    public static final ForgeConfigSpec.IntValue AZUMAAL_PREPARE_MOVE_TICKS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_PREPARE_BACK_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_DASH_STOP_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MELEE_DAMAGE_RANGE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MELEE_DAMAGE_MULTIPLIER;

    // Azumaal - double attack
    public static final ForgeConfigSpec.IntValue AZUMAAL_DOUBLE_APPROACH_TICKS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_DOUBLE_APPROACH_DISTANCE;
    public static final ForgeConfigSpec.IntValue AZUMAAL_DOUBLE_ATTACK_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_DOUBLE_DAMAGE_1_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_DOUBLE_DAMAGE_2_TICK;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_DOUBLE_DAMAGE_RANGE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_DOUBLE_DAMAGE_MULTIPLIER;

    // Azumaal - throw attack
    public static final ForgeConfigSpec.IntValue AZUMAAL_THROW_APPROACH_TICKS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_APPROACH_DISTANCE;
    public static final ForgeConfigSpec.IntValue AZUMAAL_THROW_UP_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_THROW_UP_TICK;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_UP_RANGE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_UP_VELOCITY;
    public static final ForgeConfigSpec.IntValue AZUMAAL_AIR_THROW_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_AIR_CHASE_TICKS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_AIR_HEIGHT_OFFSET;
    public static final ForgeConfigSpec.IntValue AZUMAAL_THROW_DOWN_TICK;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_DOWN_RANGE;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_DOWN_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_DOWN_HORIZONTAL;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_THROW_DOWN_VERTICAL;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_AIR_HOLD_HORIZONTAL_DAMPING;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_AIR_FOLLOW_FACTOR;

    // Azumaal - summon attacks
    public static final ForgeConfigSpec.IntValue AZUMAAL_SUMMON_WEAK_ANIMATION_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_SUMMON_WEAK_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_SUMMON_WARNING_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_SUMMON_MEGA_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_SUMMON_MEGA_TICK;
    public static final ForgeConfigSpec.IntValue AZUMAAL_MEGA_TRACKING_WAVES;
    public static final ForgeConfigSpec.IntValue AZUMAAL_MEGA_RANDOM_CIRCLES;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MEGA_RANDOM_MIN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MEGA_RANDOM_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_MEGA_RANDOM_ANGLE_JITTER;
    public static final ForgeConfigSpec.IntValue AZUMAAL_RADIAL_WARNING_ARROWS;

    // Azumaal - eyes / stun
    public static final ForgeConfigSpec.IntValue AZUMAAL_EYES_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_EYES_MAGIC_TICK;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_EYES_RANGE;
    public static final ForgeConfigSpec.IntValue AZUMAAL_STUN_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_STUN_DEBUFF_DURATION;

    // Azumaal - clones
    public static final ForgeConfigSpec.IntValue AZUMAAL_CLONE_SUMMON_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_CLONE_COUNT;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_CLONE_FORMATION_RADIUS;
    public static final ForgeConfigSpec.IntValue AZUMAAL_CLONE_FORMATION_TICKS;

    // Azumaal - defense
    public static final ForgeConfigSpec.IntValue AZUMAAL_DEFENSE_CAST_DURATION;
    public static final ForgeConfigSpec.IntValue AZUMAAL_DEFENSE_EYELID_COUNT;

    // Azumaal - parkour
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_PARKOUR_HEIGHT;
    public static final ForgeConfigSpec.IntValue AZUMAAL_PARKOUR_RISE_TICKS;
    public static final ForgeConfigSpec.IntValue AZUMAAL_PARKOUR_HOLD_TICKS;
    public static final ForgeConfigSpec.IntValue AZUMAAL_PARKOUR_DESCEND_TICKS;
    public static final ForgeConfigSpec.DoubleValue AZUMAAL_PARKOUR_PARTICIPANT_RANGE;
    public static final ForgeConfigSpec.IntValue AZUMAAL_PARKOUR_ENTROPY_DURATION;

    // Paladin
    public static final ForgeConfigSpec.DoubleValue PALADIN_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue PALADIN_ATTACK_DAMAGE;

    // Chaos Spawner
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_NOVICE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_EASY_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_NORMAL_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_INSECTS_HARD_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_CACTUS_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_INSECTS_BRUTAL_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_CRUSADERS_HARD_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_CRUSADERS_BRUTAL_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHAOS_SPAWNER_GOLEMS_MOBS;

    static {
        BUILDER.push("Azumaal Boss");
        AZUMAAL_MAX_HEALTH = BUILDER.comment("Maximum health of Azumaal. Requires restart to affect newly created bosses.").defineInRange("maxHealth", 1500.0D, 1.0D, 1000000.0D);
        AZUMAAL_ATTACK_DAMAGE = BUILDER.comment("Base ATTACK_DAMAGE attribute of Azumaal. Attack multipliers below are applied to this value.").defineInRange("attackDamage", 22.0D, 0.0D, 100000.0D);
        BUILDER.pop();

        BUILDER.push("Azumaal Damage Scaler");
        AZUMAAL_DAMAGE_SCALER_MAX_MULTIPLIER = BUILDER.comment("Maximum total multiplier that Azumaal Damage Scaler may reach.", "2.0 = scaler may increase final damage up to 2x the base damage.", "The scaler formula itself is NOT changed by this config.").defineInRange("maxDamageMultiplier", 2.0D, 1.0D, 20.0D);
        BUILDER.pop();

        BUILDER.push("Azumaal Attack Timing");
        AZUMAAL_TARGET_SEARCH_RANGE = doubleValue("targetSearchRange", "Maximum range in blocks for selecting an attack target.", 96.0D, 1.0D, 512.0D);
        AZUMAAL_ATTACK_COOLDOWN_MIN = intValue("attackCooldownMin", "Minimum random cooldown between attacks, in ticks.", 35, 0, 12000);
        AZUMAAL_ATTACK_COOLDOWN_MAX = intValue("attackCooldownMax", "Maximum random cooldown between attacks, in ticks.", 70, 0, 12000);
        AZUMAAL_POST_SPAWN_ATTACK_COOLDOWN = intValue("postSpawnAttackCooldown", "Cooldown before Azumaal can attack after the intro/spawn phase, in ticks.", 40, 0, 12000);
        BUILDER.pop();

        BUILDER.push("Azumaal Melee Attacks");
        AZUMAAL_MELEE_DURATION = intValue("meleeDuration", "Total melee animation/attack duration, in ticks.", 40, 1, 1200);
        AZUMAAL_ATTACK_1_PREPARE_TICK = intValue("attack1PrepareTick", "Tick when attack_1 starts its backwards preparation movement.", 8, 0, 1200);
        AZUMAAL_ATTACK_1_DAMAGE_TICK = intValue("attack1DamageTick", "Tick when attack_1 deals damage.", 22, 0, 1200);
        AZUMAAL_ATTACK_2_PREPARE_TICK = intValue("attack2PrepareTick", "Tick when attack_2 starts its backwards preparation movement.", 6, 0, 1200);
        AZUMAAL_ATTACK_2_DAMAGE_TICK = intValue("attack2DamageTick", "Tick when attack_2 deals damage.", 24, 0, 1200);
        AZUMAAL_DASH_MOVE_TICKS = intValue("dashMoveTicks", "How many ticks the melee dash takes.", 3, 1, 200);
        AZUMAAL_PREPARE_MOVE_TICKS = intValue("prepareMoveTicks", "How many ticks the backwards preparation movement takes.", 6, 1, 200);
        AZUMAAL_PREPARE_BACK_DISTANCE = doubleValue("prepareBackDistance", "How far Azumaal moves backwards before a melee attack.", 0.95D, 0.0D, 32.0D);
        AZUMAAL_DASH_STOP_DISTANCE = doubleValue("dashStopDistance", "Distance from target at which melee dash stops.", 2.0D, 0.0D, 32.0D);
        AZUMAAL_MELEE_DAMAGE_RANGE = doubleValue("meleeDamageRange", "Maximum range for normal melee damage.", 8.0D, 0.1D, 128.0D);
        AZUMAAL_MELEE_DAMAGE_MULTIPLIER = doubleValue("meleeDamageMultiplier", "Multiplier applied to Azumaal ATTACK_DAMAGE for normal melee hits.", 1.0D, 0.0D, 20.0D);
        BUILDER.pop();

        BUILDER.push("Azumaal Double Attack");
        AZUMAAL_DOUBLE_APPROACH_TICKS = intValue("approachTicks", "Ticks spent approaching before the double attack.", 5, 1, 200);
        AZUMAAL_DOUBLE_APPROACH_DISTANCE = doubleValue("approachDistance", "Distance left between Azumaal and target after double-attack approach.", 2.0D, 0.0D, 32.0D);
        AZUMAAL_DOUBLE_ATTACK_DURATION = intValue("duration", "Total duration of the double attack, in ticks.", 55, 1, 1200);
        AZUMAAL_DOUBLE_DAMAGE_1_TICK = intValue("firstDamageTick", "Tick of the first damage pulse.", 23, 0, 1200);
        AZUMAAL_DOUBLE_DAMAGE_2_TICK = intValue("secondDamageTick", "Tick of the second damage pulse.", 38, 0, 1200);
        AZUMAAL_DOUBLE_DAMAGE_RANGE = doubleValue("damageRange", "Maximum hit range of both double-attack strikes.", 8.0D, 0.1D, 128.0D);
        AZUMAAL_DOUBLE_DAMAGE_MULTIPLIER = doubleValue("damageMultiplier", "ATTACK_DAMAGE multiplier for each double-attack hit.", 0.80D, 0.0D, 20.0D);
        BUILDER.pop();

        BUILDER.push("Azumaal Throw Attack");
        AZUMAAL_THROW_APPROACH_TICKS = intValue("approachTicks", "Ticks spent approaching before the throw.", 5, 1, 200);
        AZUMAAL_THROW_APPROACH_DISTANCE = doubleValue("approachDistance", "Distance left between Azumaal and target after throw approach.", 2.0D, 0.0D, 32.0D);
        AZUMAAL_THROW_UP_DURATION = intValue("throwUpDuration", "Duration of the upward throw phase.", 45, 1, 1200);
        AZUMAAL_THROW_UP_TICK = intValue("throwUpTick", "Tick when the target is launched upward.", 22, 0, 1200);
        AZUMAAL_THROW_UP_RANGE = doubleValue("throwUpRange", "Maximum range at which the upward launch can connect.", 10.0D, 0.1D, 128.0D);
        AZUMAAL_THROW_UP_VELOCITY = doubleValue("throwUpVelocity", "Vertical velocity applied by the upward launch.", 1.60D, 0.0D, 10.0D);
        AZUMAAL_AIR_THROW_DURATION = intValue("airThrowDuration", "Total duration of the air throw phase.", 50, 1, 1200);
        AZUMAAL_AIR_CHASE_TICKS = intValue("airChaseTicks", "Ticks used to move Azumaal into position above the airborne target.", 15, 1, 1200);
        AZUMAAL_AIR_HEIGHT_OFFSET = doubleValue("airHeightOffset", "Height offset Azumaal keeps relative to the airborne target.", 1.75D, -16.0D, 32.0D);
        AZUMAAL_THROW_DOWN_TICK = intValue("throwDownTick", "Tick when the downward slam happens.", 24, 0, 1200);
        AZUMAAL_THROW_DOWN_RANGE = doubleValue("throwDownRange", "Maximum range at which the downward slam can connect.", 5.0D, 0.1D, 128.0D);
        AZUMAAL_THROW_DOWN_DAMAGE_MULTIPLIER = doubleValue("throwDownDamageMultiplier", "ATTACK_DAMAGE multiplier for the downward slam.", 1.10D, 0.0D, 20.0D);
        AZUMAAL_THROW_DOWN_HORIZONTAL = doubleValue("throwDownHorizontalVelocity", "Horizontal velocity applied by the downward slam.", 1.15D, 0.0D, 10.0D);
        AZUMAAL_THROW_DOWN_VERTICAL = doubleValue("throwDownVerticalVelocity", "Vertical velocity applied by the downward slam. Negative values push downward.", -1.55D, -10.0D, 10.0D);
        AZUMAAL_AIR_HOLD_HORIZONTAL_DAMPING = doubleValue("airHoldHorizontalDamping", "Horizontal movement multiplier while the target is suspended.", 0.82D, 0.0D, 1.0D);
        AZUMAAL_AIR_FOLLOW_FACTOR = doubleValue("airFollowFactor", "How quickly Azumaal follows the target after the initial air chase.", 0.28D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("Azumaal Summon Attacks");
        AZUMAAL_SUMMON_WEAK_ANIMATION_DURATION = intValue("weakAnimationDuration", "Tick when Azumaal returns to idle animation during the weak summon attack.", 60, 1, 1200);
        AZUMAAL_SUMMON_WEAK_TICK = intValue("weakWarningTick", "Tick when weak summon warning entities are created.", 36, 0, 1200);
        AZUMAAL_SUMMON_WARNING_DURATION = intValue("weakWarningDuration", "How long the weak summon warning phase remains active, in ticks.", 160, 1, 12000);
        AZUMAAL_SUMMON_MEGA_DURATION = intValue("megaDuration", "Total duration of the mega summon cast, in ticks.", 60, 1, 1200);
        AZUMAAL_SUMMON_MEGA_TICK = intValue("megaWarningTick", "Tick when mega summon warning circles are created.", 36, 0, 1200);
        AZUMAAL_MEGA_TRACKING_WAVES = intValue("megaTrackingWaves", "Number of tracking waves used by the tracking mega pattern.", 3, 1, 64);
        AZUMAAL_MEGA_RANDOM_CIRCLES = intValue("megaRandomCircles", "Number of circles in the scattered mega pattern.", 5, 1, 64);
        AZUMAAL_MEGA_RANDOM_MIN_RADIUS = doubleValue("megaRandomMinRadius", "Minimum scattered-circle distance from Azumaal.", 6.0D, 0.0D, 128.0D);
        AZUMAAL_MEGA_RANDOM_MAX_RADIUS = doubleValue("megaRandomMaxRadius", "Maximum scattered-circle distance from Azumaal.", 20.0D, 0.0D, 128.0D);
        AZUMAAL_MEGA_RANDOM_ANGLE_JITTER = doubleValue("megaRandomAngleJitter", "Random angular jitter of scattered circles, in radians.", 0.35D, 0.0D, 3.14159D);
        AZUMAAL_RADIAL_WARNING_ARROWS = intValue("radialWarningArrows", "Number of arrows in the radial weak-summon pattern.", 8, 1, 64);
        BUILDER.pop();

        BUILDER.push("Azumaal Eyes Attack");
        AZUMAAL_EYES_DURATION = intValue("duration", "Total eyes attack duration, in ticks.", 45, 1, 1200);
        AZUMAAL_EYES_MAGIC_TICK = intValue("magicTick", "Tick when the eyes attack applies its stun.", 25, 0, 1200);
        AZUMAAL_EYES_RANGE = doubleValue("range", "Radius in which the eyes attack can stun players.", 20.0D, 0.1D, 256.0D);
        AZUMAAL_STUN_DURATION = intValue("stunDuration", "How long players are position-locked by the eyes attack, in ticks.", 80, 1, 12000);
        AZUMAAL_STUN_DEBUFF_DURATION = intValue("debuffDuration", "Duration of Darkness, Blindness and Entropy from the eyes attack, in ticks.", 100, 0, 12000);
        BUILDER.pop();

        BUILDER.push("Azumaal Clone Attack");
        AZUMAAL_CLONE_SUMMON_DURATION = intValue("summonDuration", "Delay before the clone formation begins, in ticks.", 60, 1, 1200);
        AZUMAAL_CLONE_COUNT = intValue("cloneCount", "Number of Azumaal clones created by the rainbow clone attack.", 5, 1, 15);
        AZUMAAL_CLONE_FORMATION_RADIUS = doubleValue("formationRadius", "Radius of the clone formation.", 6.0D, 0.5D, 64.0D);
        AZUMAAL_CLONE_FORMATION_TICKS = intValue("formationTicks", "Time used to spread boss and clones into formation.", 20, 1, 1200);
        BUILDER.pop();

        BUILDER.push("Azumaal Defense Attack");
        AZUMAAL_DEFENSE_CAST_DURATION = intValue("castDuration", "Ticks spent casting before the defensive eyelids appear.", 60, 1, 1200);
        AZUMAAL_DEFENSE_EYELID_COUNT = intValue("eyelidCount", "Number of Eyelid entities summoned by the defense attack.", 6, 1, 64);
        BUILDER.pop();

        BUILDER.push("Azumaal Parkour Attack");
        AZUMAAL_PARKOUR_HEIGHT = doubleValue("height", "How high Azumaal rises during the parkour attack.", 20.0D, 0.0D, 128.0D);
        AZUMAAL_PARKOUR_RISE_TICKS = intValue("riseTicks", "Rise duration in ticks.", 30, 1, 12000);
        AZUMAAL_PARKOUR_HOLD_TICKS = intValue("holdTicks", "Time players have to complete the parkour challenge. 500 ticks = 25 seconds.", 500, 1, 72000);
        AZUMAAL_PARKOUR_DESCEND_TICKS = intValue("descendTicks", "Descend duration in ticks.", 30, 1, 12000);
        AZUMAAL_PARKOUR_PARTICIPANT_RANGE = doubleValue("participantRange", "Range used to collect players participating in the parkour attack.", 96.0D, 1.0D, 512.0D);
        AZUMAAL_PARKOUR_ENTROPY_DURATION = intValue("failureEntropyDuration", "Entropy duration applied to players who fail the parkour challenge, in ticks.", 200, 0, 72000);
        BUILDER.pop();

        BUILDER.push("Paladin Mini Boss");
        PALADIN_MAX_HEALTH = BUILDER.comment("Maximum health of Paladin.").defineInRange("maxHealth", 800.0D, 1.0D, 1000000.0D);
        PALADIN_ATTACK_DAMAGE = BUILDER.comment("Base ATTACK_DAMAGE attribute of Paladin.").defineInRange("attackDamage", 24.0D, 0.0D, 100000.0D);
        BUILDER.pop();

        BUILDER.push("Chaos Spawner Waves");
        BUILDER.comment(
                "Each entry uses: entity_id|min|max",
                "Example: minecraft:zombie|2|5",
                "Optional fourth value is a random-choice group: entity_id|min|max|group_name",
                "Only ONE entry from each group is selected when a wave starts.",
                "This preserves the default INSECTS_BRUTAL behavior where Titana OR two Dashers are selected.",
                "Use 0|0 as min/max to effectively disable an entry. Invalid/non-Mob entity IDs are skipped safely."
        );

        CHAOS_SPAWNER_NOVICE_MOBS = spawnList("novice", List.of(
                "minecraft:zombie|2|3",
                "minecraft:skeleton|1|2",
                "minecraft:spider|1|2"
        ));

        CHAOS_SPAWNER_EASY_MOBS = spawnList("easy", List.of(
                "minecraft:zombie|3|5",
                "minecraft:skeleton|1|3",
                "minecraft:spider|1|3"
        ));

        CHAOS_SPAWNER_NORMAL_MOBS = spawnList("normal", List.of(
                "minecraft:zombie|5|8",
                "minecraft:skeleton|3|4",
                "minecraft:spider|1|4"
        ));

        CHAOS_SPAWNER_INSECTS_HARD_MOBS = spawnList("insectsHard", List.of(
                "oasiso:monki|2|5",
                "oasiso:dasher|1|1"
        ));

        CHAOS_SPAWNER_CACTUS_MOBS = spawnList("cactus", List.of(
                "oasiso:cacto|4|6"
        ));

        CHAOS_SPAWNER_INSECTS_BRUTAL_MOBS = spawnList("insectsBrutal", List.of(
                "oasiso:monki|3|6",
                "oasiso:titana|1|1|brutalBoss",
                "oasiso:dasher|2|2|brutalBoss",
                "oasiso:bombul|1|1"
        ));

        CHAOS_SPAWNER_CRUSADERS_HARD_MOBS = spawnList("crusadersHard", List.of(
                "oasiso:crusader_warrior|2|4",
                "oasiso:crusader_assasin|1|1",
                "oasiso:crusader_wizard|1|1"
        ));

        CHAOS_SPAWNER_CRUSADERS_BRUTAL_MOBS = spawnList("crusadersBrutal", List.of(
                "oasiso:crusader_warrior|3|5",
                "oasiso:crusader_assasin|1|2",
                "oasiso:crusader_wizard|1|2",
                "oasiso:crusader_tank|1|1"
        ));

        CHAOS_SPAWNER_GOLEMS_MOBS = spawnList("golems", List.of(
                "oasiso:sand_golem|1|2"
        ));
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private OsirisRealmConfig() {
    }

    private static ForgeConfigSpec.IntValue intValue(String name, String comment, int defaultValue, int min, int max) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
    }

    private static ForgeConfigSpec.DoubleValue doubleValue(String name, String comment, double defaultValue, double min, double max) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, min, max);
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> spawnList(String name, List<String> defaults) {
        return BUILDER
                .comment("Wave entries: entity_id|min|max or entity_id|min|max|randomChoiceGroup")
                .defineListAllowEmpty(
                        List.of(name),
                        () -> defaults,
                        OsirisRealmConfig::isValidSpawnerEntry
                );
    }

    private static boolean isValidSpawnerEntry(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }

        String[] parts = text.split("\\|", -1);
        if (parts.length < 3 || parts.length > 4) {
            return false;
        }

        if (!parts[0].matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            return false;
        }

        try {
            int min = Integer.parseInt(parts[1].trim());
            int max = Integer.parseInt(parts[2].trim());
            return min >= 0 && max >= min && max <= 128;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
