package io.github.beeebea.fastmove.network;

import io.github.beeebea.fastmove.FastMove;
import io.github.beeebea.fastmove.config.FastMoveConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigStatePayload(
        boolean enableFastMove,
        boolean diveRollEnabled,
        int diveRollStaminaCost,
        double diveRollSpeedBoostMultiplier,
        int diveRollCoolDown,
        boolean diveRollWhenSwimming,
        boolean diveRollWhenFlying,
        boolean wallRunEnabled,
        int wallRunStaminaCost,
        double wallRunSpeedBoostMultiplier,
        int wallRunDurationTicks,
        boolean slideEnabled,
        int slideStaminaCost,
        double slideSpeedBoostMultiplier,
        int slideCoolDown
) implements CustomPacketPayload {
    public static final Type<ConfigStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FastMove.MOD_ID, "config_state"));

    public static final StreamCodec<ByteBuf, ConfigStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ConfigStatePayload::enableFastMove,
            ByteBufCodecs.BOOL, ConfigStatePayload::diveRollEnabled,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::diveRollStaminaCost,
            ByteBufCodecs.DOUBLE, ConfigStatePayload::diveRollSpeedBoostMultiplier,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::diveRollCoolDown,
            ByteBufCodecs.BOOL, ConfigStatePayload::diveRollWhenSwimming,
            ByteBufCodecs.BOOL, ConfigStatePayload::diveRollWhenFlying,
            ByteBufCodecs.BOOL, ConfigStatePayload::wallRunEnabled,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::wallRunStaminaCost,
            ByteBufCodecs.DOUBLE, ConfigStatePayload::wallRunSpeedBoostMultiplier,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::wallRunDurationTicks,
            ByteBufCodecs.BOOL, ConfigStatePayload::slideEnabled,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::slideStaminaCost,
            ByteBufCodecs.DOUBLE, ConfigStatePayload::slideSpeedBoostMultiplier,
            ByteBufCodecs.VAR_INT, ConfigStatePayload::slideCoolDown,
            ConfigStatePayload::new
    );

    public static ConfigStatePayload fromConfig(FastMoveConfig config) {
        return new ConfigStatePayload(
                config.enableFastMove,
                config.diveRollEnabled,
                config.diveRollStaminaCost,
                config.diveRollSpeedBoostMultiplier,
                config.diveRollCoolDown,
                config.diveRollWhenSwimming,
                config.diveRollWhenFlying,
                config.wallRunEnabled,
                config.wallRunStaminaCost,
                config.wallRunSpeedBoostMultiplier,
                config.wallRunDurationTicks,
                config.slideEnabled,
                config.slideStaminaCost,
                config.slideSpeedBoostMultiplier,
                config.slideCoolDown
        );
    }

    public FastMoveConfig toConfig() {
        FastMoveConfig config = new FastMoveConfig();
        config.enableFastMove = enableFastMove;
        config.diveRollEnabled = diveRollEnabled;
        config.diveRollStaminaCost = diveRollStaminaCost;
        config.diveRollSpeedBoostMultiplier = diveRollSpeedBoostMultiplier;
        config.diveRollCoolDown = diveRollCoolDown;
        config.diveRollWhenSwimming = diveRollWhenSwimming;
        config.diveRollWhenFlying = diveRollWhenFlying;
        config.wallRunEnabled = wallRunEnabled;
        config.wallRunStaminaCost = wallRunStaminaCost;
        config.wallRunSpeedBoostMultiplier = wallRunSpeedBoostMultiplier;
        config.wallRunDurationTicks = wallRunDurationTicks;
        config.slideEnabled = slideEnabled;
        config.slideStaminaCost = slideStaminaCost;
        config.slideSpeedBoostMultiplier = slideSpeedBoostMultiplier;
        config.slideCoolDown = slideCoolDown;
        return config;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
