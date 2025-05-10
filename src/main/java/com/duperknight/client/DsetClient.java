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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public class DsetClient implements ClientModInitializer {

    private static double xPortal, yPortal, zPortal, xDestine, yDestine, zDestine;
    private static boolean portalActive = false;
    private static long expireTime;
    private static final Map<UUID, Long> playerTeleportCooldowns = new ConcurrentHashMap<>();
    private static final long TELEPORT_COOLDOWN_MS = 2000;

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("dset_client_config.properties");
    private static final Properties configProps = new Properties();

    @Override
    public void onInitializeClient() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                configProps.load(Files.newBufferedReader(CONFIG_PATH));
                xDestine = Double.parseDouble(configProps.getProperty("xDestine", "0"));
                yDestine = Double.parseDouble(configProps.getProperty("yDestine", "0"));
                zDestine = Double.parseDouble(configProps.getProperty("zDestine", "0"));
            } else {
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                                                                                expireTime = (long) (DoubleArgumentType.getDouble(ctx, "expireTime") * 1000);
                                                                                portalActive = true;

                                                                                MinecraftClient client = MinecraftClient.getInstance();
                                                                                Vec3d portalCenter = new Vec3d(xPortal, yPortal, zPortal);
                                                                                Vec3d lookDir = client.player.getRotationVec(1.0F);
                                                                                Vec3d lookHoriz = new Vec3d(lookDir.x, 0, lookDir.z).normalize();
                                                                                Vec3d perp;
                                                                                if (Math.abs(lookHoriz.x) > Math.abs(lookHoriz.z)) {
                                                                                    perp = new Vec3d(0, 0, 1);
                                                                                } else {
                                                                                    perp = new Vec3d(1, 0, 0);
                                                                                }

                                                                                client.execute(() -> {
                                                                                    for (double u = -0.5; u <= 0.5; u += 0.6) {
                                                                                        for (double v = 0.0; v <= 2.0; v += 0.6) {
                                                                                            Vec3d pos = portalCenter.add(perp.multiply(u)).add(0, v, 0);
                                                                                            client.player.networkHandler.sendChatCommand(
                                                                                                String.format(Locale.US,
                                                                                                    "summon minecraft:area_effect_cloud %.2f %.2f %.2f " +
                                                                                                    "{Particle:{type:dust,scale:2.25,color:[0.976,0.502,0.114]},Radius:0.1," +
                                                                                                    "Duration:%d,Tags:[\"dset.portal\"],potion_contents:{custom_color:11141120}}",
                                                                                                    pos.x, pos.y, pos.z, (expireTime/1000)*20
                                                                                                )
                                                                                            );
                                                                                            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                                                                                        }
                                                                                    }
                                                                                });

                                                                                new Timer().schedule(new TimerTask() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        portalActive = false;
                                                                                    }
                                                                                }, expireTime);

                                                                                return 1;
                                                                            })))))))));

            dispatcher.register(literal("qportal")
                .executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    PlayerEntity player = client.player;
                    if (player == null) return 0;
                    Vec3d eyePos = player.getCameraPosVec(1.0F);
                    Vec3d lookDir = player.getRotationVec(1.0F).multiply(3.0);
                    Vec3d portalCenter = eyePos.add(lookDir);
                    xPortal = portalCenter.x;
                    yPortal = portalCenter.y;
                    zPortal = portalCenter.z;
                    expireTime = 3000; // ms
                    portalActive = true;

                    Vec3d lookHoriz = new Vec3d(lookDir.x, 0, lookDir.z).normalize();
                    Vec3d perp = new Vec3d(-lookHoriz.z, 0, lookHoriz.x);

                    client.execute(() -> {
                        for (double u = -0.5; u <= 0.5; u += 0.6) {
                            for (double v = 0.0; v <= 2.0; v += 0.6) {
                                Vec3d pos = portalCenter
                                    .add(perp.multiply(u))
                                    .add(0, v, 0);
                                client.player.networkHandler.sendChatCommand(
                                    String.format(Locale.US,
                                        "summon minecraft:area_effect_cloud %.2f %.2f %.2f " +
                                        "{Particle:{type:dust,scale:2.25,color:[0.976,0.502,0.114]},Radius:0.1," +
                                        "Duration:60,Tags:[\"dset.portal\"],potion_contents:{custom_color:11141120}}",
                                        pos.x, pos.y, pos.z
                                    )
                                );
                                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                            }
                        }
                    });

                    new Timer().schedule(new TimerTask() {
                        @Override public void run() { portalActive = false; }
                    }, expireTime);

                    return 1;
                })
            );

            dispatcher.register(literal("setqportal")
                .then(argument("x", DoubleArgumentType.doubleArg())
                .then(argument("y", DoubleArgumentType.doubleArg())
                .then(argument("z", DoubleArgumentType.doubleArg())
                .executes(ctx -> {
                    xDestine = DoubleArgumentType.getDouble(ctx, "x");
                    yDestine = DoubleArgumentType.getDouble(ctx, "y");
                    zDestine = DoubleArgumentType.getDouble(ctx, "z");
                    saveConfig();
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of(String.format("qportal destination set to %.2f, %.2f, %.2f",
                            xDestine, yDestine, zDestine)), false);
                    return 1;
                })))));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (portalActive && client.player != null && client.world != null) {
                Vec3d portalCenter = new Vec3d(xPortal, yPortal, zPortal);
                Box portalCheckBox = new Box(
                        portalCenter.x - 0.5, portalCenter.y, portalCenter.z - 0.5,
                        portalCenter.x + 0.5, portalCenter.y + 2.0, portalCenter.z + 0.5
                );
                long currentTime = System.currentTimeMillis();
                client.world.getEntitiesByClass(PlayerEntity.class, portalCheckBox, entity -> true)
                        .forEach(player -> {
                            if (currentTime - playerTeleportCooldowns.getOrDefault(player.getUuid(), 0L) > TELEPORT_COOLDOWN_MS) {
                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(
                                        String.format(Locale.US, "tp %s %.2f %.2f %.2f", player.getName().getString(), xDestine, yDestine, zDestine)
                                );
                                playerTeleportCooldowns.put(player.getUuid(), currentTime);
                            }
                        });
            }
        });
    }

    private static void saveConfig() {
        configProps.setProperty("xDestine", Double.toString(xDestine));
        configProps.setProperty("yDestine", Double.toString(yDestine));
        configProps.setProperty("zDestine", Double.toString(zDestine));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            configProps.store(
                Files.newBufferedWriter(CONFIG_PATH,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                "DSET Client Config"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
