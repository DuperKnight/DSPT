package com.duperknight.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import java.util.Locale;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import java.util.Map; // Added
import java.util.UUID; // Added
import java.util.concurrent.ConcurrentHashMap; // Added

public class DsetClient implements ClientModInitializer {

    private static double xPortal, yPortal, zPortal, xDestine, yDestine, zDestine;
    private static boolean portalActive = false;
    private static long expireTime;
    private static final Map<UUID, Long> playerTeleportCooldowns = new ConcurrentHashMap<>(); // Added
    private static final long TELEPORT_COOLDOWN_MS = 2000; // 2 seconds cooldown - Added

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("portal")
                    .then(argument("xPortal", DoubleArgumentType.doubleArg())
                            .then(argument("yPortal", DoubleArgumentType.doubleArg())
                                    .then(argument("zPortal", DoubleArgumentType.doubleArg())
                                            .then(argument("xDestine", DoubleArgumentType.doubleArg())
                                                    .then(argument("yDestine", DoubleArgumentType.doubleArg())
                                                            .then(argument("zDestine", DoubleArgumentType.doubleArg())
                                                                    .then(argument("expireTime", DoubleArgumentType.doubleArg())
                                                                            .executes(ctx -> {
                                                                                xPortal = DoubleArgumentType.getDouble(ctx, "xPortal");
                                                                                yPortal = DoubleArgumentType.getDouble(ctx, "yPortal");
                                                                                zPortal = DoubleArgumentType.getDouble(ctx, "zPortal");
                                                                                xDestine = DoubleArgumentType.getDouble(ctx, "xDestine");
                                                                                yDestine = DoubleArgumentType.getDouble(ctx, "yDestine");
                                                                                zDestine = DoubleArgumentType.getDouble(ctx, "zDestine");
                                                                                expireTime = (long) (DoubleArgumentType.getDouble(ctx, "expireTime") * 1000); // Convert seconds to milliseconds
                                                                                portalActive = true;

                                                                                // Draw the portal
                                                                                double centerX = Math.floor(xPortal) + 0.5;
                                                                                double centerZ = Math.floor(zPortal) + 0.5;
                                                                                double baseY = yPortal;

                                                                                // Define boundaries for the wall
                                                                                double xMin = centerX - 0.5;
                                                                                double xMax = centerX + 0.5;
                                                                                double yBottom = baseY;
                                                                                double yTop = baseY + 2;

                                                                                MinecraftClient.getInstance().execute(() -> {
                                                                                    // Loop through the area and summon particles
                                                                                    for (double spawnX = xMin; spawnX <= xMax; spawnX += 0.6) {
                                                                                        for (double spawnY = yBottom; spawnY <= yTop; spawnY += 0.6) {
                                                                                            final double fx = spawnX;
                                                                                            final double fy = spawnY;
                                                                                            final double fz = centerZ;
                                                                                            MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                                                                                                    String.format(Locale.US, "summon minecraft:area_effect_cloud %.2f %.2f %.2f {Particle:{type:dust,scale:2.25,color:[0.976,0.502,0.114]},Radius:0.1,Duration:%d,Tags:[\"dset.portal\"],potion_contents:{custom_color:11141120}}", fx, fy, fz, expireTime/1000*20)
                                                                                            );
                                                                                            try {
                                                                                                Thread.sleep(10); // Add a small delay to prevent getting kicked
                                                                                            } catch (InterruptedException e) {
                                                                                                e.printStackTrace();
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                });

                                                                                // Schedule portal expiration
                                                                                new Timer().schedule(new TimerTask() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        portalActive = false;
                                                                                    }
                                                                                }, expireTime);

                                                                                return 1;
                                                                            })))))))));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (portalActive && client.player != null && client.world != null) {
                //Find all players within the portal radius and teleport them
                Vec3d portalCenter = new Vec3d(xPortal, yPortal, zPortal);
                // Define a 1x2x1 box around the portal center for entity detection
                Box portalCheckBox = new Box(
                        portalCenter.x - 0.5, portalCenter.y, portalCenter.z - 0.5,
                        portalCenter.x + 0.5, portalCenter.y + 2.0, portalCenter.z + 0.5
                );
                long currentTime = System.currentTimeMillis(); // Added
                client.world.getEntitiesByClass(PlayerEntity.class, portalCheckBox, entity -> true)
                        .forEach(player -> {
                            if (currentTime - playerTeleportCooldowns.getOrDefault(player.getUuid(), 0L) > TELEPORT_COOLDOWN_MS) { // Added cooldown check
                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                                        String.format(Locale.US, "tp %s %.2f %.2f %.2f", player.getName().getString(), xDestine, yDestine, zDestine)
                                );
                                playerTeleportCooldowns.put(player.getUuid(), currentTime); // Added cooldown update
                            }
                        });
            }
        });
    }
}
