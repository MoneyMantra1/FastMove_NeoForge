package io.github.beeebea.fastmove;

import io.github.beeebea.fastmove.config.FastMoveConfig;
import io.github.beeebea.fastmove.config.FastMoveServerConfig;
import io.github.beeebea.fastmove.config.IFastMoveConfig;
import io.github.beeebea.fastmove.network.ConfigStatePayload;
import io.github.beeebea.fastmove.network.MoveStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

@Mod(FastMove.MOD_ID)
public class FastMove {
    public static final String MOD_ID = "fastmove";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    protected static FastMoveConfig serverConfig = null;

    public static FastMoveConfig getConfig() {
        if (serverConfig != null) return serverConfig;
        return CONFIG.getConfig();
    }

    private static final Object QUEUE_LOCK = new Object();
    private static final Queue<Runnable> ACTION_QUEUE = new LinkedList<>();

    public static IMoveStateUpdater moveStateUpdater;
    public static IFastMoveInput INPUT;
    public static IFastMoveConfig CONFIG;

    public FastMove(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing FastMove for NeoForge");

        moveStateUpdater = new IMoveStateUpdater();
        INPUT = new IFastMoveInput() {};
        CONFIG = FastMoveServerConfig::snapshot;

        modContainer.registerConfig(ModConfig.Type.SERVER, FastMoveServerConfig.SPEC);
        modEventBus.addListener(FastMove::registerPayloads);

        NeoForge.EVENT_BUS.addListener(FastMove::onServerTickEnd);
        NeoForge.EVENT_BUS.addListener(FastMove::onPlayerLoggedIn);

        if (FMLEnvironmentHelper.isClient()) {
            io.github.beeebea.fastmove.client.FastMoveClient.init(modEventBus);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(MoveStatePayload.TYPE, MoveStatePayload.STREAM_CODEC, FastMove::handleMoveStateServer);
    }

    private static void handleMoveStateServer(MoveStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            UUID uuid = payload.playerId();
            MoveState moveState = MoveState.STATE(payload.moveState());
            Player target = serverPlayer.serverLevel().getServer().getPlayerList().getPlayer(uuid);
            IFastPlayer fastPlayer = target instanceof IFastPlayer fp ? fp : null;
            if (fastPlayer != null) {
                fastPlayer.fastmove_setMoveState(moveState);
                sendToClients(target, new MoveStatePayload(uuid, payload.moveState()));
            }
        });
    }

    private static void onServerTickEnd(ServerTickEvent.Post ignored) {
        synchronized (QUEUE_LOCK) {
            while (!ACTION_QUEUE.isEmpty()) {
                ACTION_QUEUE.poll().run();
            }
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, ConfigStatePayload.fromConfig(getConfig()));
        }
    }

    public static void sendToClients(Player source, MoveStatePayload payload) {
        synchronized (QUEUE_LOCK) {
            ACTION_QUEUE.add(() -> {
                var server = source.getServer();
                if (server == null) return;

                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target != source && target.distanceToSqr(source) < 6400) {
                        PacketDistributor.sendToPlayer(target, payload);
                    }
                }
            });
        }
    }

    private static final class FMLEnvironmentHelper {
        private static boolean isClient() {
            return net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT;
        }
    }
}
