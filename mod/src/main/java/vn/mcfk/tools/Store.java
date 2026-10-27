package vn.mcfk.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cài đặt Veinminer riêng cho từng người chơi, lưu trong thư mục world. */
public final class Store {
    public static final class S {
        public int range = 1;
        public int maxBlocks = 64;
        public String mode = "SNEAK"; // OFF | SNEAK | ALWAYS
    }

    private static final Map<UUID, S> MAP = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    public static S get(UUID id) {
        return MAP.computeIfAbsent(id, k -> new S());
    }

    public static void clamp(S s) {
        s.range = Math.max(1, Math.min(Config.I.maxRangeCap, s.range));
        s.maxBlocks = Math.max(1, Math.min(Config.I.maxBlocksCap, s.maxBlocks));
        if (!"OFF".equals(s.mode) && !"SNEAK".equals(s.mode) && !"ALWAYS".equals(s.mode)) s.mode = "SNEAK";
    }

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("mcfktools-players.json");
        MAP.clear();
        try {
            if (Files.exists(file)) {
                Map<String, S> m = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, S>>() {}.getType());
                if (m != null) {
                    m.forEach((k, v) -> {
                        try { MAP.put(UUID.fromString(k), v); } catch (Exception ignored) {}
                    });
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        if (file == null) return;
        try {
            Map<String, S> m = new HashMap<>();
            MAP.forEach((k, v) -> m.put(k.toString(), v));
            Files.writeString(file, GSON.toJson(m));
        } catch (Exception ignored) {}
    }

    private Store() {}
}
