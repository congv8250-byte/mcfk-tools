package vn.mcfk.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** config/mcfktools.json, tự tạo với giá trị mặc định. */
public final class Config {
    public boolean sneakDisables3x3 = true;
    public int maxTreeBlocks = 256;
    public int treeRadius = 24;
    public boolean requireLeaves = true;
    public int maxBlocksPerTick = 64;
    public int maxRangeCap = 5;
    public int maxBlocksCap = 512;
    public boolean toolsForEveryone = true;
    public boolean breakBlockEntities = false;

    public static Config I = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcfktools.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                Config c = GSON.fromJson(Files.readString(f), Config.class);
                if (c != null) I = c;
            }
        } catch (Exception e) {
            I = new Config();
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(I));
        } catch (Exception ignored) {}
    }

    private Config() {}
}
