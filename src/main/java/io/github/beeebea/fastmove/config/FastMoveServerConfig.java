package io.github.beeebea.fastmove.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FastMoveServerConfig {
    public static final ModConfigSpec SPEC;
    private static final Values VALUES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VALUES = new Values(builder);
        SPEC = builder.build();
    }

    private FastMoveServerConfig() {
    }

    public static FastMoveConfig snapshot() {
        FastMoveConfig config = new FastMoveConfig();
        config.enableFastMove = VALUES.enableFastMove.get();

        config.diveRollEnabled = VALUES.diveRollEnabled.get();
        config.diveRollStaminaCost = VALUES.diveRollStaminaCost.get();
        config.diveRollSpeedBoostMultiplier = VALUES.diveRollSpeedBoostMultiplier.get();
        config.diveRollCoolDown = VALUES.diveRollCoolDown.get();
        config.diveRollWhenSwimming = VALUES.diveRollWhenSwimming.get();
        config.diveRollWhenFlying = VALUES.diveRollWhenFlying.get();

        config.wallRunEnabled = VALUES.wallRunEnabled.get();
        config.wallRunStaminaCost = VALUES.wallRunStaminaCost.get();
        config.wallRunSpeedBoostMultiplier = VALUES.wallRunSpeedBoostMultiplier.get();
        config.wallRunDurationTicks = VALUES.wallRunDurationTicks.get();

        config.slideEnabled = VALUES.slideEnabled.get();
        config.slideStaminaCost = VALUES.slideStaminaCost.get();
        config.slideSpeedBoostMultiplier = VALUES.slideSpeedBoostMultiplier.get();
        config.slideCoolDown = VALUES.slideCoolDown.get();
        return config;
    }

    private static final class Values {
        final ModConfigSpec.BooleanValue enableFastMove;

        final ModConfigSpec.BooleanValue diveRollEnabled;
        final ModConfigSpec.IntValue diveRollStaminaCost;
        final ModConfigSpec.DoubleValue diveRollSpeedBoostMultiplier;
        final ModConfigSpec.IntValue diveRollCoolDown;
        final ModConfigSpec.BooleanValue diveRollWhenSwimming;
        final ModConfigSpec.BooleanValue diveRollWhenFlying;

        final ModConfigSpec.BooleanValue wallRunEnabled;
        final ModConfigSpec.IntValue wallRunStaminaCost;
        final ModConfigSpec.DoubleValue wallRunSpeedBoostMultiplier;
        final ModConfigSpec.IntValue wallRunDurationTicks;

        final ModConfigSpec.BooleanValue slideEnabled;
        final ModConfigSpec.IntValue slideStaminaCost;
        final ModConfigSpec.DoubleValue slideSpeedBoostMultiplier;
        final ModConfigSpec.IntValue slideCoolDown;

        Values(ModConfigSpec.Builder builder) {
            enableFastMove = builder.comment("Master enable switch").define("enableFastMove", true);

            builder.push("diveRoll");
            diveRollEnabled = builder.define("enabled", true);
            diveRollStaminaCost = builder.defineInRange("staminaCost", 50, 0, 256);
            diveRollSpeedBoostMultiplier = builder.defineInRange("speedBoostMultiplier", 1.0, 0.0, 3.0);
            diveRollCoolDown = builder.defineInRange("coolDown", 0, 0, 256);
            diveRollWhenSwimming = builder.define("whenSwimming", false);
            diveRollWhenFlying = builder.define("whenFlying", false);
            builder.pop();

            builder.push("wallRun");
            wallRunEnabled = builder.define("enabled", true);
            wallRunStaminaCost = builder.defineInRange("staminaCost", 0, 0, 256);
            wallRunSpeedBoostMultiplier = builder.defineInRange("speedBoostMultiplier", 1.0, 0.0, 3.0);
            wallRunDurationTicks = builder.defineInRange("durationTicks", 60, 1, 256);
            builder.pop();

            builder.push("slide");
            slideEnabled = builder.define("enabled", true);
            slideStaminaCost = builder.defineInRange("staminaCost", 10, 0, 256);
            slideSpeedBoostMultiplier = builder.defineInRange("speedBoostMultiplier", 1.0, 0.0, 3.0);
            slideCoolDown = builder.defineInRange("coolDown", 0, 0, 256);
            builder.pop();
        }
    }
}
