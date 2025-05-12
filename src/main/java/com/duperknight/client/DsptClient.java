package com.duperknight.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DsptClient implements ClientModInitializer {
    private static final Map<UUID, Long> playerTeleportCooldowns = new ConcurrentHashMap<>();
    private static final long TELEPORT_COOLDOWN_MS = 2000;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        PortalCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (PortalCommands.isPortalActive() && client.player != null && client.world != null) {
                Vec3d center = new Vec3d(
                        PortalCommands.getXPortal(),
                        PortalCommands.getYPortal(),
                        PortalCommands.getZPortal()
                );
                Box box = new Box(
                        center.x - 0.5, center.y, center.z - 0.5,
                        center.x + 0.5, center.y + 2.0, center.z + 0.5
                );
                long now = System.currentTimeMillis();
                client.world.getEntitiesByClass(PlayerEntity.class, box, e -> true)
                        .forEach(player -> {
                            if (now - playerTeleportCooldowns.getOrDefault(player.getUuid(), 0L) > TELEPORT_COOLDOWN_MS) {
                                String target;
                                if (PortalCommands.isQuickPortal()) {
                                    target = String.format("tp %s %.2f %.2f %.2f",
                                            player.getName().getString(),
                                            ConfigManager.getXDestine(),
                                            ConfigManager.getYDestine(),
                                            ConfigManager.getZDestine()
                                    );
                                } else {
                                    target = String.format("tp %s %.2f %.2f %.2f",
                                            player.getName().getString(),
                                            PortalCommands.getXDestPortal(),
                                            PortalCommands.getYDestPortal(),
                                            PortalCommands.getZDestPortal()
                                    );
                                }
                                assert MinecraftClient.getInstance().player != null;
                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(target);
                                playerTeleportCooldowns.put(player.getUuid(), now);
                            }
                        });
            }
        });
    }
}