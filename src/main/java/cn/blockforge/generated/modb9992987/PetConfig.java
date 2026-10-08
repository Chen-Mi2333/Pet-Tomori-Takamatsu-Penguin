package cn.blockforge.generated.modb9992987;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 模组配置：粒子档位（full 全量 / low 低配 / off 关闭）。
 * 存到 config/mod_b9992987.properties，用 /petconfig particles <档位> 修改。
 */
public final class PetConfig {
    public enum Mode {
        FULL, LOW, OFF;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Mode parse(String s) {
            if (s == null) {
                return FULL;
            }
            return switch (s.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "low", "低配", "simple" -> LOW;
                case "off", "关闭", "none" -> OFF;
                default -> FULL;
            };
        }
    }

    private static final Path FILE =
        FabricLoader.getInstance().getConfigDir().resolve(ModContent.MOD_ID + ".properties");
    private static Mode particleMode = Mode.FULL;

    public static Mode particleMode() {
        return particleMode;
    }

    public static void setParticleMode(Mode mode) {
        particleMode = mode;
        save();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(FILE)) {
                    props.load(in);
                }
                particleMode = Mode.parse(props.getProperty("particles", "full"));
            }
        } catch (IOException ignored) {
            particleMode = Mode.FULL;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties props = new Properties();
            props.setProperty("particles", particleMode.key());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "pet companion mod config");
            }
        } catch (IOException ignored) {
            // 配置写不进去不影响游戏
        }
    }

    private PetConfig() {
    }
}