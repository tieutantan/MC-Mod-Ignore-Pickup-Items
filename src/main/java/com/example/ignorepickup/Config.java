package com.example.ignorepickup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Config {

    private static final Logger LOGGER = LogManager.getLogger("IgnorePickup-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Dùng thư mục config chuẩn của NeoForge
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ignorepickup.json");
    private static final File CONFIG_FILE = CONFIG_PATH.toFile();

    // Sử dụng concurrent set để đọc nhanh, an toàn giữa các thread
    private static final Set<String> KNOWN_ITEMS = new ConcurrentSkipListSet<>();
    private static final Set<String> IGNORED_ACTIVE = new ConcurrentSkipListSet<>();

    // Global enable switch
    private static final AtomicBoolean ENABLED = new AtomicBoolean(true);

    // Batch flags
    private static final AtomicBoolean BATCH_SAVING = new AtomicBoolean(false);
    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);

    private Config() {
        // no-op
    }

    private static final class ConfigData {
        List<String> known = new ArrayList<>();
        List<String> ignoredActive = new ArrayList<>();
    }

    /** Load từ đĩa (thread-safe) */
    public static void load() {
        // Đảm bảo state nhất quán khi load
        synchronized (Config.class) {
            KNOWN_ITEMS.clear();
            IGNORED_ACTIVE.clear();
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                    final ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        if (data.known != null) KNOWN_ITEMS.addAll(data.known);
                        if (data.ignoredActive != null) IGNORED_ACTIVE.addAll(data.ignoredActive);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load config: {}", CONFIG_FILE.getAbsolutePath(), e);
                }
            }
            // Sau load, coi như sạch
            DIRTY.set(false);
        }
    }

    /** Ghi ra đĩa (thread-safe, atomic move) */
    public static void save() {
        // Tránh I/O nếu đang batch
        if (BATCH_SAVING.get()) {
            DIRTY.set(true);
            return;
        }

        // Snapshot dữ liệu để giảm lock duration
        final List<String> knownSnapshot = new ArrayList<>(KNOWN_ITEMS);
        final List<String> ignoredSnapshot = new ArrayList<>(IGNORED_ACTIVE);

        synchronized (Config.class) {
            final File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("Could not create config directory: {}", parent.getAbsolutePath());
            }

            final ConfigData data = new ConfigData();
            data.known = knownSnapshot;
            data.ignoredActive = ignoredSnapshot;

            final Path tempPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");

            try (Writer writer = new BufferedWriter(Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8))) {
                GSON.toJson(data, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to write temp config file: {}", tempPath, e);
                return;
            }

            try {
                // Cố gắng move atomic, nếu không hỗ trợ thì fallback replace existing
                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                try {
                    Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    LOGGER.error("Failed to replace config file (non-atomic): {}", CONFIG_PATH, e);
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to move temp config to final: {} -> {}", tempPath, CONFIG_PATH, e);
                return;
            }

            DIRTY.set(false);
        }
    }

    /** Thêm known + save ngay (không dùng trong hot path) */
    public static boolean addKnown(final String item) {
        final boolean added = KNOWN_ITEMS.add(item);
        if (added) save();
        return added;
    }

    /** Xoá known + đồng bộ ignored + save */
    public static boolean removeKnown(final String item) {
        final boolean removed = KNOWN_ITEMS.remove(item);
        if (removed) {
            IGNORED_ACTIVE.remove(item);
            save();
        }
        return removed;
    }

    /** View bất biến cho client */
    public static Set<String> getKnown() {
        return Collections.unmodifiableSet(KNOWN_ITEMS);
    }

    /** View bất biến cho client */
    public static Set<String> getIgnoredActive() {
        return Collections.unmodifiableSet(IGNORED_ACTIVE);
    }

    /** Gán lại toàn bộ danh sách ignored (không dùng cho hot path) */
    public static void setIgnoredActive(final Collection<String> items) {
        synchronized (Config.class) {
            IGNORED_ACTIVE.clear();
            IGNORED_ACTIVE.addAll(items);
            save();
        }
    }

    /** Kiểm tra item có đang bị ignore hay không (API giữ nguyên tên) */
    public static boolean isEnabled(final String item) {
        return IGNORED_ACTIVE.contains(item);
    }

    /** Enable/disable global */
    public static boolean isEnabled() {
        return ENABLED.get();
    }

    /** Toggle một item trong ignored + save */
    public static void toggleIgnored(final String item) {
        synchronized (Config.class) {
            KNOWN_ITEMS.add(item);
            if (!IGNORED_ACTIVE.add(item)) {
                IGNORED_ACTIVE.remove(item);
            }
            save();
        }
    }

    /** Block item + save */
    public static void block(final String item) {
        synchronized (Config.class) {
            KNOWN_ITEMS.add(item);
            IGNORED_ACTIVE.add(item);
            save();
        }
    }

    /** Allow item + save */
    public static void allow(final String item) {
        synchronized (Config.class) {
            KNOWN_ITEMS.add(item);
            IGNORED_ACTIVE.remove(item);
            save();
        }
    }

    /**
     * Add known theo kiểu ephemeral (hot path).
     * - Không ghi file ngay.
     * - Khi đang batch: chỉ đánh dấu DIRTY, save sẽ diễn ra ở endBatch nếu cần.
     */
    public static void addKnownEphemeral(final String item) {
        if (KNOWN_ITEMS.add(item) && BATCH_SAVING.get()) {
            DIRTY.set(true);
        }
    }

    /** Bắt đầu batch (không save ngay) */
    public static void startBatch() {
        BATCH_SAVING.set(true);
    }

    /** Kết thúc batch: nếu có thay đổi, save một lần */
    public static void endBatch() {
        BATCH_SAVING.set(false);
        if (DIRTY.get()) {
            save();
        }
    }
}
