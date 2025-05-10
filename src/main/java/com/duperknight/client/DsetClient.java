package com.duperknight.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.math.Box;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DsetClient implements ClientModInitializer {

    private static final Map<UUID, Long> playerTeleportCooldowns = new ConcurrentHashMap<>();
    private static final long TELEPORT_COOLDOWN_MS = 2000;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        PortalCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (PortalCommands.isPortalActive() && client.player != null && client.world != null) {
                Vec3d portalCenter = new Vec3d(PortalCommands.getXPortal(), PortalCommands.getYPortal(), PortalCommands.getZPortal());
                Box portalCheckBox = new Box(
                        portalCenter.x - 0.5, portalCenter.y, portalCenter.z - 0.5,
                        portalCenter.x + 0.5, portalCenter.y + 2.0, portalCenter.z + 0.5
                );
                long currentTime = System.currentTimeMillis();
                client.world.getEntitiesByClass(PlayerEntity.class, portalCheckBox, entity -> true)
                        .forEach(player -> {
                            if (currentTime - playerTeleportCooldowns.getOrDefault(player.getUuid(), 0L) > TELEPORT_COOLDOWN_MS) {
                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                                        String.format(java.util.Locale.US, "tp %s %.2f %.2f %.2f", player.getName().getString(), ConfigManager.getXDestine(), ConfigManager.getYDestine(), ConfigManager.getZDestine())
                                );
                                playerTeleportCooldowns.put(player.getUuid(), currentTime);
                            }
                        });
            }
        });
    }
}
