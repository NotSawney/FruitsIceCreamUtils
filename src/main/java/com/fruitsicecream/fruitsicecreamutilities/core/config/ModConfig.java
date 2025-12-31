package com.fruitsicecream.fruitsicecreamutilities.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ModConfig {

    // ========================================
    // EXPERIENCE CORES CONFIGURATION
    // ========================================

    public static class ExperienceCores {
        // MK-I
        public final ForgeConfigSpec.IntValue mk1XpPerHour;
        public final ForgeConfigSpec.IntValue mk1MaxCapacity;
        public final ForgeConfigSpec.IntValue mk1PushInterval;
        public final ForgeConfigSpec.IntValue mk1XpPerPush;

        // MK-II
        public final ForgeConfigSpec.IntValue mk2XpPerHour;
        public final ForgeConfigSpec.IntValue mk2MaxCapacity;
        public final ForgeConfigSpec.IntValue mk2PushInterval;
        public final ForgeConfigSpec.IntValue mk2XpPerPush;

        // MK-III
        public final ForgeConfigSpec.IntValue mk3XpPerHour;
        public final ForgeConfigSpec.IntValue mk3MaxCapacity;
        public final ForgeConfigSpec.IntValue mk3PushInterval;
        public final ForgeConfigSpec.IntValue mk3XpPerPush;

        // MK-IV
        public final ForgeConfigSpec.IntValue mk4XpPerHour;
        public final ForgeConfigSpec.IntValue mk4MaxCapacity;
        public final ForgeConfigSpec.IntValue mk4PushInterval;
        public final ForgeConfigSpec.IntValue mk4XpPerPush;

        // MK-V
        public final ForgeConfigSpec.IntValue mk5XpPerHour;
        public final ForgeConfigSpec.IntValue mk5MaxCapacity;
        public final ForgeConfigSpec.IntValue mk5PushInterval;
        public final ForgeConfigSpec.IntValue mk5XpPerPush;

        ExperienceCores(ForgeConfigSpec.Builder builder) {
            builder.comment("Experience Core Configuration")
                    .comment("Configure production rates, capacities, and push mechanics for each tier")
                    .push("experience_cores");

            // MK-I
            builder.comment("Experience Core MK-I (Tier 1)")
                    .push("mk1");

            mk1XpPerHour = builder
                    .comment("XP generated per hour")
                    .defineInRange("xpPerHour", 360, 1, 100000);

            mk1MaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 180, 1, 1000000);

            mk1PushInterval = builder
                    .comment("Ticks between XP pushes to collector (20 ticks = 1 second)")
                    .defineInRange("pushInterval", 40, 1, 200);

            mk1XpPerPush = builder
                    .comment("XP transferred per push")
                    .defineInRange("xpPerPush", 2, 1, 1000);

            builder.pop();

            // MK-II
            builder.comment("Experience Core MK-II (Tier 2)")
                    .push("mk2");

            mk2XpPerHour = builder
                    .comment("XP generated per hour")
                    .defineInRange("xpPerHour", 720, 1, 100000);

            mk2MaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 720, 1, 1000000);

            mk2PushInterval = builder
                    .comment("Ticks between XP pushes to collector (20 ticks = 1 second)")
                    .defineInRange("pushInterval", 35, 1, 200);

            mk2XpPerPush = builder
                    .comment("XP transferred per push")
                    .defineInRange("xpPerPush", 4, 1, 1000);

            builder.pop();

            // MK-III
            builder.comment("Experience Core MK-III (Tier 3)")
                    .push("mk3");

            mk3XpPerHour = builder
                    .comment("XP generated per hour")
                    .defineInRange("xpPerHour", 1440, 1, 100000);

            mk3MaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 2160, 1, 1000000);

            mk3PushInterval = builder
                    .comment("Ticks between XP pushes to collector (20 ticks = 1 second)")
                    .defineInRange("pushInterval", 30, 1, 200);

            mk3XpPerPush = builder
                    .comment("XP transferred per push")
                    .defineInRange("xpPerPush", 6, 1, 1000);

            builder.pop();

            // MK-IV
            builder.comment("Experience Core MK-IV (Tier 4)")
                    .push("mk4");

            mk4XpPerHour = builder
                    .comment("XP generated per hour")
                    .defineInRange("xpPerHour", 2880, 1, 100000);

            mk4MaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 5760, 1, 1000000);

            mk4PushInterval = builder
                    .comment("Ticks between XP pushes to collector (20 ticks = 1 second)")
                    .defineInRange("pushInterval", 25, 1, 200);

            mk4XpPerPush = builder
                    .comment("XP transferred per push")
                    .defineInRange("xpPerPush", 10, 1, 1000);

            builder.pop();

            // MK-V
            builder.comment("Experience Core MK-V (Tier 5)")
                    .push("mk5");

            mk5XpPerHour = builder
                    .comment("XP generated per hour")
                    .defineInRange("xpPerHour", 5760, 1, 100000);

            mk5MaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 34560, 1, 1000000);

            mk5PushInterval = builder
                    .comment("Ticks between XP pushes to collector (20 ticks = 1 second)")
                    .defineInRange("pushInterval", 20, 1, 200);

            mk5XpPerPush = builder
                    .comment("XP transferred per push")
                    .defineInRange("xpPerPush", 16, 1, 1000);

            builder.pop();

            builder.pop();
        }

        // Helper methods para obtener valores por tier
        public int getXpPerHour(int tier) {
            return switch (tier) {
                case 1 -> mk1XpPerHour.get();
                case 2 -> mk2XpPerHour.get();
                case 3 -> mk3XpPerHour.get();
                case 4 -> mk4XpPerHour.get();
                case 5 -> mk5XpPerHour.get();
                default -> 360;
            };
        }

        public int getMaxCapacity(int tier) {
            return switch (tier) {
                case 1 -> mk1MaxCapacity.get();
                case 2 -> mk2MaxCapacity.get();
                case 3 -> mk3MaxCapacity.get();
                case 4 -> mk4MaxCapacity.get();
                case 5 -> mk5MaxCapacity.get();
                default -> 180;
            };
        }

        public int getPushInterval(int tier) {
            return switch (tier) {
                case 1 -> mk1PushInterval.get();
                case 2 -> mk2PushInterval.get();
                case 3 -> mk3PushInterval.get();
                case 4 -> mk4PushInterval.get();
                case 5 -> mk5PushInterval.get();
                default -> 40;
            };
        }

        public int getXpPerPush(int tier) {
            return switch (tier) {
                case 1 -> mk1XpPerPush.get();
                case 2 -> mk2XpPerPush.get();
                case 3 -> mk3XpPerPush.get();
                case 4 -> mk4XpPerPush.get();
                case 5 -> mk5XpPerPush.get();
                default -> 2;
            };
        }
    }

    // ========================================
    // EXPERIENCE COLLECTORS CONFIGURATION
    // ========================================

    public static class ExperienceCollectors {
        // Basic Collector
        public final ForgeConfigSpec.IntValue basicMaxCapacity;
        public final ForgeConfigSpec.ConfigValue<String> basicCompatibleCores;

        // Advanced Collector
        public final ForgeConfigSpec.IntValue advancedMaxCapacity;
        public final ForgeConfigSpec.ConfigValue<String> advancedCompatibleCores;

        ExperienceCollectors(ForgeConfigSpec.Builder builder) {
            builder.comment("Experience Collector Configuration")
                    .comment("Configure storage capacities and compatible core tiers")
                    .push("experience_collectors");

            // Basic Collector
            builder.comment("Basic Experience Collector (Tier 1)")
                    .push("basic");

            basicMaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 50000, 1000, 10000000);

            basicCompatibleCores = builder
                    .comment("Compatible core tiers (comma separated, e.g., '1,2,3')")
                    .define("compatibleCores", "1,2,3");

            builder.pop();

            // Advanced Collector
            builder.comment("Advanced Experience Collector (Tier 2)")
                    .push("advanced");

            advancedMaxCapacity = builder
                    .comment("Maximum XP storage capacity")
                    .defineInRange("maxCapacity", 200000, 1000, 10000000);

            advancedCompatibleCores = builder
                    .comment("Compatible core tiers (comma separated, e.g., '1,2,3,4,5')")
                    .define("compatibleCores", "1,2,3,4,5");

            builder.pop();

            builder.pop();
        }

        // Helper methods
        public int getMaxCapacity(int tier) {
            return switch (tier) {
                case 1 -> basicMaxCapacity.get();
                case 2 -> advancedMaxCapacity.get();
                default -> 50000;
            };
        }

        public boolean isCoreTierCompatible(int collectorTier, int coreTier) {
            String compatibleStr = collectorTier == 1
                    ? basicCompatibleCores.get()
                    : advancedCompatibleCores.get();

            try {
                String[] tiers = compatibleStr.split(",");
                for (String tier : tiers) {
                    if (Integer.parseInt(tier.trim()) == coreTier) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Si hay error en el parsing, usar valores por defecto
                if (collectorTier == 1) {
                    return coreTier >= 1 && coreTier <= 3;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    // ========================================
    // GENERAL CONFIGURATION
    // ========================================

    public static class General {
        public final ForgeConfigSpec.IntValue ticksPerGeneration;
        public final ForgeConfigSpec.BooleanValue enableLightEmission;
        public final ForgeConfigSpec.BooleanValue enableDebugLogging;

        General(ForgeConfigSpec.Builder builder) {
            builder.comment("General Settings")
                    .push("general");

            ticksPerGeneration = builder
                    .comment("Ticks between XP generation cycles (20 ticks = 1 second)")
                    .comment("Default: 200 ticks = 10 seconds")
                    .defineInRange("ticksPerGeneration", 200, 20, 6000);

            enableLightEmission = builder
                    .comment("Enable dynamic light emission based on XP fill level")
                    .define("enableLightEmission", true);

            enableDebugLogging = builder
                    .comment("Enable debug logging for troubleshooting")
                    .define("enableDebugLogging", false);

            builder.pop();
        }
    }

    // ========================================
    // SPEC & INSTANCES
    // ========================================

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ExperienceCores CORES;
    public static final ExperienceCollectors COLLECTORS;
    public static final General GENERAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        CORES = new ExperienceCores(builder);
        COLLECTORS = new ExperienceCollectors(builder);
        GENERAL = new General(builder);

        COMMON_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(Type.COMMON, COMMON_SPEC);
    }
}