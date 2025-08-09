package com.example.ignorepickup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/ignorepickup.json");

    private static final Set<String> KNOWN_ITEMS = new TreeSet<>();
    private static final Set<String> IGNORED_ACTIVE = new TreeSet<>();
    private static boolean ENABLED = true; // simple global enable switch
    private static boolean BATCH_SAVING = false;
    private static boolean DIRTY = false;

    private static final class ConfigData {
        List<String> known = new ArrayList<>();
        List<String> ignoredActive = new ArrayList<>();
    }

    public static void load() {
        KNOWN_ITEMS.clear();
        IGNORED_ACTIVE.clear();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    if (data.known != null) KNOWN_ITEMS.addAll(data.known);
                    if (data.ignoredActive != null) IGNORED_ACTIVE.addAll(data.ignoredActive);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        if (BATCH_SAVING) { DIRTY = true; return; }
        try {
            ConfigData data = new ConfigData();
            data.known = new ArrayList<>(KNOWN_ITEMS);
            data.ignoredActive = new ArrayList<>(IGNORED_ACTIVE);
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
            DIRTY = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean addKnown(String item) {
        boolean added = KNOWN_ITEMS.add(item);
        if (added) save();
        return added;
    }

    public static boolean removeKnown(String item) {
        boolean removed = KNOWN_ITEMS.remove(item);
        if (removed) {
            IGNORED_ACTIVE.remove(item);
            save();
        }
        return removed;
    }

    public static Set<String> getKnown() {
        return Collections.unmodifiableSet(KNOWN_ITEMS);
    }

    public static Set<String> getIgnoredActive() {
        return Collections.unmodifiableSet(IGNORED_ACTIVE);
    }

    public static void setIgnoredActive(Collection<String> items) {
        IGNORED_ACTIVE.clear();
        IGNORED_ACTIVE.addAll(items);
        save();
    }

    public static boolean isEnabled(String item) {
        return IGNORED_ACTIVE.contains(item);
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void toggleIgnored(String item) {
        KNOWN_ITEMS.add(item);
        if (IGNORED_ACTIVE.contains(item)) {
            IGNORED_ACTIVE.remove(item);
        } else {
            IGNORED_ACTIVE.add(item);
        }
        save();
    }

    // Convenience helpers used by commands
    public static void block(String item) {
        KNOWN_ITEMS.add(item);
        IGNORED_ACTIVE.add(item);
        save();
    }

    public static void allow(String item) {
        KNOWN_ITEMS.add(item);
        IGNORED_ACTIVE.remove(item);
        save();
    }

    // Add known without immediate persistence (for high-frequency paths)
    public static void addKnownEphemeral(String item) {
        if (KNOWN_ITEMS.add(item) && BATCH_SAVING) DIRTY = true;
    }

    // Batch save control
    public static void startBatch() { BATCH_SAVING = true; }
    public static void endBatch() { BATCH_SAVING = false; if (DIRTY) save(); }
}
