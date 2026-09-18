package com.LeglessLizard.goomod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec SPEC;

    // Spread / decay
    public static final ModConfigSpec.IntValue SPREAD_MIN_DELAY;
    public static final ModConfigSpec.IntValue SPREAD_MAX_DELAY;
    public static final ModConfigSpec.IntValue DECAY_GRACE_PERIOD;
    public static final ModConfigSpec.IntValue DECAY_RANDOM_MIN;
    public static final ModConfigSpec.IntValue DECAY_RANDOM_MAX;

    // Volatile goo explosion
    public static final ModConfigSpec.DoubleValue VOLATILE_BASE_POWER;
    public static final ModConfigSpec.DoubleValue VOLATILE_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue VOLATILE_MAX_POWER;

    // Philosopher's Phage chances
    public static final ModConfigSpec.DoubleValue PPHAGE_DIAMOND_EMERALD_CHANCE;
    public static final ModConfigSpec.DoubleValue PPHAGE_IRON_GOLD_CHANCE;
    public static final ModConfigSpec.DoubleValue PPHAGE_REDSTONE_COAL_COPPER_CHANCE;
    public static final ModConfigSpec.DoubleValue PPHAGE_NETHER_MODDED_CHANCE;
    public static final ModConfigSpec.DoubleValue PPHAGE_SIZE_FACTOR;
    public static final ModConfigSpec.DoubleValue PPHAGE_DUPLICATION_DECAY;

    // Disruptor limit
    public static final ModConfigSpec.BooleanValue ONLY_ONE_DISRUPTOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        SPREAD_MIN_DELAY = builder
                .comment("Minimum ticks between spread attempts.")
                .defineInRange("spreadMinDelay", 10, 1, 600);
        SPREAD_MAX_DELAY = builder
                .comment("Maximum ticks between spread attempts.")
                .defineInRange("spreadMaxDelay", 100, 1, 1200);
        DECAY_GRACE_PERIOD = builder
                .comment("Ticks after losing sculk neighbour before decay can start.")
                .defineInRange("decayGracePeriod", 400, 0, 24000);
        DECAY_RANDOM_MIN = builder
                .comment("Minimum additional ticks after grace period before decay.")
                .defineInRange("decayRandomMin", 200, 0, 12000);
        DECAY_RANDOM_MAX = builder
                .comment("Maximum additional ticks after grace period before decay.")
                .defineInRange("decayRandomMax", 300, 0, 12000);

        VOLATILE_BASE_POWER = builder
                .comment("Base explosion power for volatile goo.")
                .defineInRange("volatileBasePower", 2.0, 0.0, 20.0);
        VOLATILE_PER_BLOCK = builder
                .comment("Explosion power added per decayed block in the colony.")
                .defineInRange("volatilePerBlock", 0.1, 0.0, 5.0);
        VOLATILE_MAX_POWER = builder
                .comment("Maximum explosion power cap.")
                .defineInRange("volatileMaxPower", 10.0, 0.0, 100.0);

        PPHAGE_DIAMOND_EMERALD_CHANCE = builder
                .comment("Base duplication chance for diamond and emerald ores.")
                .defineInRange("pphageDiamondEmeraldChance", 0.6, 0.0, 1.0);
        PPHAGE_IRON_GOLD_CHANCE = builder
                .comment("Base duplication chance for iron and gold ores.")
                .defineInRange("pphageIronGoldChance", 0.8, 0.0, 1.0);
        PPHAGE_REDSTONE_COAL_COPPER_CHANCE = builder
                .comment("Base duplication chance for redstone, coal and copper ores.")
                .defineInRange("pphageRedstoneCoalCopperChance", 0.9, 0.0, 1.0);
        PPHAGE_NETHER_MODDED_CHANCE = builder
                .comment("Base duplication chance for nether ores and any ore not listed above.")
                .defineInRange("pphageNetherModdedChance", 0.6, 0.0, 1.0);
        PPHAGE_SIZE_FACTOR = builder
                .comment("Vein size penalty multiplier (higher = larger veins harder).")
                .defineInRange("pphageSizeFactor", 0.05, 0.0, 10.0);
        PPHAGE_DUPLICATION_DECAY = builder
                .comment("Duplication count penalty multiplier (higher = faster drop).")
                .defineInRange("pphageDuplicationDecay", 0.2, 0.0, 10.0);

        ONLY_ONE_DISRUPTOR = builder
                .comment("If true, only one Phage Disruptor can exist in the world at a time.")
                .define("onlyOneDisruptor", true);

        SPEC = builder.build();
    }
}
