package com.duperknight.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("dset_client_config.properties");
    private static final Properties configProps = new Properties();
    private static double xDestine, yDestine, zDestine;

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                configProps.load(Files.newBufferedReader(CONFIG_PATH));
                xDestine = Double.parseDouble(configProps.getProperty("xDestine", "0"));
                yDestine = Double.parseDouble(configProps.getProperty("yDestine", "0"));
                zDestine = Double.parseDouble(configProps.getProperty("zDestine", "0"));
            } else {
                xDestine = 0;
                yDestine = 0;
                zDestine = 0;
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
            xDestine = 0;
            yDestine = 0;
            zDestine = 0;
        }
    }

    public static void saveConfig() {
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

    public static double getXDestine() {
        return xDestine;
    }

    public static void setXDestine(double xDestine) {
        ConfigManager.xDestine = xDestine;
    }

    public static double getYDestine() {
        return yDestine;
    }

    public static void setYDestine(double yDestine) {
        ConfigManager.yDestine = yDestine;
    }

    public static double getZDestine() {
        return zDestine;
    }

    public static void setZDestine(double zDestine) {
        ConfigManager.zDestine = zDestine;
    }
}

