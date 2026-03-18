package io.github.beeebea.fastmove.network;

import io.github.beeebea.fastmove.FastMove;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MoveStatePayload(UUID playerId, int moveState) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MoveStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FastMove.MOD_ID, "move_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoveStatePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtilCodec.STREAM_CODEC,
            MoveStatePayload::playerId,
            ByteBufCodecs.VAR_INT,
            MoveStatePayload::moveState,
            MoveStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
