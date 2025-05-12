package com.duperknight.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class UpdateChecker {
    private static final String UPDATE_URL = "https://api.modrinth.com/v2/project/duper-spt/version";
    private static final String CURRENT_VERSION;
    private static final String MOD_ID = "dspt";

    static {
        String ver = "0.0.0";
        try {
            ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null);
            if (modContainer != null) {
                ver = modContainer.getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            System.err.println("[DSPT] Failed to load version from FabricLoader: " + e.getMessage());
        }
        CURRENT_VERSION = ver;
    }

    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                JsonArray versions = JsonParser.parseReader(
                        new InputStreamReader(conn.getInputStream())
                ).getAsJsonArray();
                if (!versions.isEmpty()) {
                    JsonObject latest = versions.get(0).getAsJsonObject();
                    String latestVer = latest.get("version_number").getAsString();
                    if (isNewer(latestVer, CURRENT_VERSION)) {
                        MinecraftClient.getInstance().execute(() -> {
                            if (MinecraftClient.getInstance().player != null) {
                                MinecraftClient.getInstance().player.sendMessage(
                                        Text.literal(String.format("§e[DSPT] New version available: %s (current: %s)", latestVer, CURRENT_VERSION)),
                                        false
                                );
                            }
                        });
                    }
                }
            } catch (Exception ignored) { }
        }).start();
    }

    private static boolean isNewer(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
            int lv = i < l.length ? Integer.parseInt(l[i]) : 0;
            if (rv > lv) return true;
            if (rv < lv) return false;
        }
        return false;
    }
}