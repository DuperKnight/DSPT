package com.duperknight.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PortalCommands {

    private static double xPortal, yPortal, zPortal;
    private static boolean portalActive = false;
    private static long expireTime;

    public static boolean isPortalActive() {
        return portalActive;
    }

    public static double getXPortal() {
        return xPortal;
    }

    public static double getYPortal() {
        return yPortal;
    }

    public static double getZPortal() {
        return zPortal;
    }

    public static void register() {
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
                                                                                ConfigManager.setXDestine(DoubleArgumentType.getDouble(ctx, "xDestine"));
                                                                                ConfigManager.setYDestine(DoubleArgumentType.getDouble(ctx, "yDestine"));
                                                                                ConfigManager.setZDestine(DoubleArgumentType.getDouble(ctx, "zDestine"));
                                                                                ConfigManager.saveConfig();
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
                                                                                                                    "Duration:%d,potion_contents:{custom_color:11141120}}",
                                                                                                            pos.x, pos.y, pos.z, (expireTime / 1000) * 20));
                                                                                            try {
                                                                                                Thread.sleep(10);
                                                                                            } catch (InterruptedException ignored) {
                                                                                            }
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
                        expireTime = 3000;
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
                                                            "Duration:60,potion_contents:{custom_color:11141120}}",
                                                    pos.x, pos.y, pos.z));
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException ignored) {
                                    }
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
                    }));

            dispatcher.register(literal("setqportal")
                    .then(argument("x", DoubleArgumentType.doubleArg())
                            .then(argument("y", DoubleArgumentType.doubleArg())
                                    .then(argument("z", DoubleArgumentType.doubleArg())
                                            .executes(ctx -> {
                                                ConfigManager.setXDestine(DoubleArgumentType.getDouble(ctx, "x"));
                                                ConfigManager.setYDestine(DoubleArgumentType.getDouble(ctx, "y"));
                                                ConfigManager.setZDestine(DoubleArgumentType.getDouble(ctx, "z"));
                                                ConfigManager.saveConfig();
                                                MinecraftClient.getInstance().player.sendMessage(
                                                        Text.of(String.format("quick portal destination set to %.2f, %.2f, %.2f",
                                                                ConfigManager.getXDestine(), ConfigManager.getYDestine(), ConfigManager.getZDestine())), false);
                                                return 1;
                                            })))));
        });
    }
}