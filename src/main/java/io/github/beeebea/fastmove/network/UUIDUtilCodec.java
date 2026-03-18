package io.github.beeebea.fastmove.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public final class UUIDUtilCodec {
    private UUIDUtilCodec() {}

    public static final StreamCodec<RegistryFriendlyByteBuf, UUID> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readUUID();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, UUID value) {
            buffer.writeUUID(value);
        }
    };
}
