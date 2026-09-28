package net.earthcomputer.clientcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.earthcomputer.clientcommands.features.ChorusManipulation;
import net.earthcomputer.clientcommands.features.EnchantmentCracker;
import net.earthcomputer.clientcommands.features.FishingCracker;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Minimal RNG-only configuration. Replaces the betterconfig-based Configs
 * from the Fabric version. Persisted as JSON in the config directory.
 */
public class Configs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configFile;

    // read-only state (not persisted)
    public static EnchantmentCracker.CrackState enchCrackState = EnchantmentCracker.CrackState.UNCRACKED;
    public static PlayerRandCracker.CrackState playerCrackState = PlayerRandCracker.CrackState.UNCRACKED;

    private static boolean enchantingPrediction = false;
    public static boolean getEnchantingPrediction() {
        return enchantingPrediction;
    }
    public static void setEnchantingPrediction(boolean enchantingPrediction) {
        Configs.enchantingPrediction = enchantingPrediction;
        if (enchantingPrediction) {
            ServerBrandManager.rngWarning();
        } else {
            EnchantmentCracker.resetCracker();
        }
        save();
    }

    public enum FishingManipulation implements StringRepresentable {
        OFF,
        MANUAL,
        AFK;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean isEnabled() {
            return this != OFF;
        }
    }

    private static FishingManipulation fishingManipulation = FishingManipulation.OFF;
    public static FishingManipulation getFishingManipulation() {
        return fishingManipulation;
    }
    public static void setFishingManipulation(FishingManipulation fishingManipulation) {
        Configs.fishingManipulation = fishingManipulation;
        if (fishingManipulation.isEnabled()) {
            ServerBrandManager.rngWarning();
        } else {
            FishingCracker.reset();
        }
        save();
    }

    public static boolean playerRNGMaintenance = true;

    public static boolean toolBreakWarning = false;

    private static int maxEnchantItemThrows = 64 * 32;
    public static int getMaxEnchantItemThrows() {
        return maxEnchantItemThrows;
    }
    public static void setMaxEnchantItemThrows(int maxEnchantItemThrows) {
        Configs.maxEnchantItemThrows = Mth.clamp(maxEnchantItemThrows, 0, 1000000);
        save();
    }

    private static boolean chorusManipulation = false;
    public static boolean getChorusManipulation() {
        return chorusManipulation;
    }
    public static void setChorusManipulation(boolean chorusManipulation) {
        Configs.chorusManipulation = chorusManipulation;
        if (chorusManipulation) {
            ServerBrandManager.rngWarning();
            ChorusManipulation.onChorusManipEnabled();
        }
        save();
    }

    private static int maxChorusItemThrows = 64 * 32;
    public static int getMaxChorusItemThrows() {
        return maxChorusItemThrows;
    }
    public static void setMaxChorusItemThrows(int maxChorusItemThrows) {
        Configs.maxChorusItemThrows = Mth.clamp(maxChorusItemThrows, 0, 1000000);
        save();
    }

    public static boolean infiniteTools = false;

    public static void init(Path configDir) {
        configFile = configDir.resolve("rngcommands.json");
        load();
    }

    public static void load() {
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) {
                return;
            }
            if (obj.has("playerRNGMaintenance")) {
                playerRNGMaintenance = obj.get("playerRNGMaintenance").getAsBoolean();
            }
            if (obj.has("toolBreakWarning")) {
                toolBreakWarning = obj.get("toolBreakWarning").getAsBoolean();
            }
            if (obj.has("maxEnchantItemThrows")) {
                maxEnchantItemThrows = Mth.clamp(obj.get("maxEnchantItemThrows").getAsInt(), 0, 1000000);
            }
            if (obj.has("maxChorusItemThrows")) {
                maxChorusItemThrows = Mth.clamp(obj.get("maxChorusItemThrows").getAsInt(), 0, 1000000);
            }
            if (obj.has("infiniteTools")) {
                infiniteTools = obj.get("infiniteTools").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            // corrupt config: keep defaults
        }
    }

    public static void save() {
        if (configFile == null) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("playerRNGMaintenance", playerRNGMaintenance);
            obj.addProperty("toolBreakWarning", toolBreakWarning);
            obj.addProperty("maxEnchantItemThrows", maxEnchantItemThrows);
            obj.addProperty("maxChorusItemThrows", maxChorusItemThrows);
            obj.addProperty("infiniteTools", infiniteTools);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            // best effort
        }
    }
}
