package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.GeneratedMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取烘焙好的原版几何（geo/penguin_pet.json），构建原版 ModelPart 骨骼树。
 *
 * <p>几何来自用户提供的原始 penguin_pet.bbmodel（tools/bake_bbmodel.py 转换），
 * 坐标已经是原版模型空间（脚在 y=24、-Z 为正面），纹理尺寸声明 128×128
 * 对应 256² 图集，UV 换算与原版 Cuboid 完全一致。</p>
 */
public final class BbBakedModel {
    public static final Identifier GEO = Identifier.fromNamespaceAndPath("mod_b9992987", "geo/penguin_pet.json");

    /** 一个立方体：位置是相对骨骼 pivot 的角点，u/v 是图集紧凑十字原点。 */
    public record Cube(float x, float y, float z, float w, float h, float d, int u, int v) {
    }

    /** 一根骨骼：pivot 是相对父骨骼的偏移。 */
    public record Bone(String name, String parent, float[] pivot, List<Cube> cubes) {
    }

    private BbBakedModel() {
    }

    public static List<Bone> loadBones(net.minecraft.server.packs.resources.ResourceManager resources) throws Exception {
        JsonObject root = readJson(resources, GEO);
        JsonArray bones = root.getAsJsonArray("bones");
        List<Bone> list = new ArrayList<>();
        for (var boneEl : bones) {
            JsonObject b = boneEl.getAsJsonObject();
            JsonArray pivot = b.getAsJsonArray("pivot");
            float[] p = {pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat()};
            String parent = b.has("parent") && !b.get("parent").isJsonNull()
                ? b.get("parent").getAsString() : null;
            List<Cube> cubes = new ArrayList<>();
            for (var cubeEl : b.getAsJsonArray("cubes")) {
                JsonObject c = cubeEl.getAsJsonObject();
                cubes.add(new Cube(
                    c.get("x").getAsFloat(), c.get("y").getAsFloat(), c.get("z").getAsFloat(),
                    c.get("w").getAsFloat(), c.get("h").getAsFloat(), c.get("d").getAsFloat(),
                    c.get("u").getAsInt(), c.get("v").getAsInt()));
            }
            list.add(new Bone(b.get("name").getAsString(), parent, p, cubes));
        }
        return list;
    }

    public static LayerDefinition toLayerDefinition(List<Bone> bones, int texWidth, int texHeight) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        Map<String, PartDefinition> added = new HashMap<>();
        // 父骨骼先建；bbmodel 的 outliner 顺序保证父在前，这里仍做一轮兜底重排。
        int guard = bones.size() * bones.size() + 1;
        while (!bones.stream().allMatch(b -> added.containsKey(b.name())) && guard-- > 0) {
            for (Bone b : bones) {
                if (added.containsKey(b.name())) {
                    continue;
                }
                PartDefinition parent = b.parent() == null ? root : added.get(b.parent());
                if (parent == null) {
                    continue;
                }
                CubeListBuilder builder = CubeListBuilder.create();
                for (Cube c : b.cubes()) {
                    builder.texOffs(c.u(), c.v())
                        .addBox(c.x(), c.y(), c.z(), c.w(), c.h(), c.d());
                }
                added.put(b.name(), parent.addOrReplaceChild(b.name(), builder,
                    PartPose.offset(b.pivot()[0], b.pivot()[1], b.pivot()[2])));
            }
        }
        if (guard <= 0) {
            GeneratedMod.LOGGER.warn("[pet] geo 骨骼父子顺序异常，部分骨骼未构建");
        }
        return LayerDefinition.create(data, texWidth, texHeight);
    }

    /**
     * 用调用方传入的 ResourceManager 读取 JSON。
     * 首次资源加载时 MinecraftClient.getInstance().getResourceManager() 仍是空快照，
     * 必须用 EntityRendererFactory.Context 里的那份，否则渲染器构造期读不到资源。
     */
    static JsonObject readJson(net.minecraft.server.packs.resources.ResourceManager resources, Identifier id) throws Exception {
        var resource = resources.getResource(id)
            .orElseThrow(() -> new IllegalStateException("missing baked asset " + id));
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
