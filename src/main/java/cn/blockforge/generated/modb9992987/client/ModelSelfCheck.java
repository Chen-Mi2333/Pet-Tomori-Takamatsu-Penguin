package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.GeneratedMod;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** 启动自查：确认 .blend 平滑网格、动画、图集与音效都在资源包里。 */
public final class ModelSelfCheck {
    public static void run(Minecraft client) {
        GeneratedMod.LOGGER.info("[pet] ===== .blend 平滑网格资源启动自查开始 =====");
        int deviations = 0;
        try {
            TomoriPenguinMeshModel model = new TomoriPenguinMeshModel(client.getResourceManager());
            if (model.meshTriangles() <= 0) {
                deviations++;
                GeneratedMod.LOGGER.warn("[pet] 平滑网格没有三角面");
            }
            GeneratedMod.LOGGER.info("[pet] .blend 平滑网格加载成功：{} 个三角面，{} 根骨骼",
                model.meshTriangles(), TomoriPenguinMeshModel.boneNames().size());
        } catch (Throwable t) {
            deviations++;
            GeneratedMod.LOGGER.error("[pet] .blend 平滑网格自查失败（渲染器会回退）", t);
        }

        String[] assets = {
            "models/entity/tomori_penguin.json",
            "textures/entity/tomori_penguin.png",
            "animations/penguin_pet.json",
            "sounds/mob/pet/idle.ogg",
            "sounds/mob/pet/happy.ogg",
            "sounds/mob/pet/angry.ogg",
            "sounds/mob/pet/excited.ogg",
            "sounds/mob/pet/sniff.ogg",
            "sounds/mob/pet/play.ogg",
            "sounds/mob/pet/carry.ogg",
            "sounds/mob/pet/hurt.ogg",
        };
        for (String path : assets) {
            boolean ok = client.getResourceManager().getResource(
                Identifier.fromNamespaceAndPath("mod_b9992987", path)).isPresent();
            if (!ok) {
                deviations++;
                GeneratedMod.LOGGER.warn("[pet] 资源缺失: {}", path);
            }
        }
        // 注意：自查发生在 CLIENT_STARTED，此时语言文件尚未装载进 I18n，
        // 不能用 I18n.hasTranslation 判断（必然误报）。直接读资源包里的 lang JSON。
        if (!hasLangKey(client.getResourceManager(), "entity.mod_b9992987.penguin_pet")) {
            deviations++;
            GeneratedMod.LOGGER.warn("[pet] 语言键 entity.mod_b9992987.penguin_pet 未翻译");
        }
        GeneratedMod.LOGGER.info("[pet] ===== 自查完成，偏差/缺失 {} 项 =====", deviations);
    }

    /** 在 zh_cn / en_us 语言文件里查找键（自查时机早于语言装载，只能直接读文件）。 */
    private static boolean hasLangKey(ResourceManager resources, String key) {
        for (String lang : new String[]{"zh_cn", "en_us"}) {
            var found = resources.getResource(
                Identifier.fromNamespaceAndPath("mod_b9992987", "lang/" + lang + ".json"));
            if (found.isEmpty()) {
                continue;
            }
            try (var in = found.get().open()) {
                var root = JsonParser.parseReader(new java.io.InputStreamReader(in,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.has(key)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // 单个语言文件解析失败时继续尝试下一个
            }
        }
        return false;
    }

    private ModelSelfCheck() {
    }
}
