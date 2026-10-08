package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.GeneratedMod;
import cn.blockforge.generated.modb9992987.PetExpression;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企鹅高松灯 .blend 的 1:1 平滑网格模型。
 *
 * <p>几何来自 models/entity/tomori_penguin.json（tools/bake_blend.py 从原始 .blend
 * 烘焙：真实三角面 + 原始 UV，非方块拼合）。每个三角面以退化四边形（第 4 顶点与
 * 第 1 顶点重合）注入原版 {@link ModelPart} 骨骼树，渲染走与鸡、原版企鹅完全相同
 * 的实体管线（entityCutoutNoCull + 原版实体材质），因此 Iris / Sodium 等光影、
 * 优化模组对它是标准支持路径——这也是 MC-MMD 的 Iris 兼容思路：自定义几何、
 * 标准管线。</p>
 *
 * <p>动作仍由原始 bbmodel 烘焙出的 8 个动画 + pickup 驱动（BbBakedAnimations），
 * 骨骼层级：body → head / wing_* / foot_* / tail / tuft_*，翅与脚各有细分链。</p>
 */
public class TomoriPenguinMeshModel extends EntityModel<PenguinPetRenderState> {
    public static final Identifier MESH =
        Identifier.fromNamespaceAndPath("mod_b9992987", "models/entity/tomori_penguin.json");
    public static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("mod_b9992987", "textures/entity/tomori_penguin.png");

    /** JSON 网格以脚底 y=0、y 向上烘焙；原版模型空间 y 向下、脚在 y=24。 */
    private static final float MODEL_HEIGHT = 24.0F;

    private static final float R2R = 0.017453292F;

    /** 骨骼父子（父先子后，构树顺序即此顺序）。 */
    private static final Map<String, String> PARENTS = orderedParents();

    /** 参与动画混合/写回的全部骨骼。 */
    public static final List<String> BONES = List.copyOf(PARENTS.keySet());

    private static Map<String, String> orderedParents() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("body", "root");
        m.put("head", "body");
        m.put("tuft_l", "head");
        m.put("tuft_r", "head");
        m.put("tail", "body");
        m.put("wing_l", "body");
        m.put("wing_l_upper", "wing_l");
        m.put("wing_l_mid", "wing_l_upper");
        m.put("wing_l_fore", "wing_l_mid");
        m.put("wing_l_hand", "wing_l_fore");
        m.put("wing_r", "body");
        m.put("wing_r_upper", "wing_r");
        m.put("wing_r_mid", "wing_r_upper");
        m.put("wing_r_fore", "wing_r_mid");
        m.put("wing_r_hand", "wing_r_fore");
        m.put("foot_l", "body");
        m.put("foot_l_mid", "foot_l");
        m.put("foot_l_toe", "foot_l_mid");
        m.put("foot_r", "body");
        m.put("foot_r_mid", "foot_r");
        m.put("foot_r_toe", "foot_r_mid");
        return m;
    }

    private final Map<String, ModelPart> parts = new HashMap<>();
    private final Map<String, float[]> baseOrigin = new HashMap<>();
    private final BbBakedAnimations anims;
    private final Map<String, float[]> pose;
    private final Map<String, float[]> scratch;

    private record MeshTriangle(
        float ax, float ay, float az, float au, float av,
        float bx, float by, float bz, float bu, float bv,
        float cx, float cy, float cz, float cu, float cv,
        float nx, float ny, float nz) {
    }

    private static Map<String, List<MeshTriangle>> lastBuildTriangles = Map.of();
    private static Map<String, float[]> lastBuildPivots = Map.of();
    private static Map<String, List<Vector3f>> lastBuildWingAnchors = Map.of();
    private final Map<String, float[]> bindPivots;
    private final WingSkin wingSkin;
    private final NeckSkin neckSkin;
    private final Map<String, Matrix4f> skinMatrices = new HashMap<>();

    private final int triangleCount;
    private final Map<String, List<MeshTriangle>> meshTriangles;
    private final PoseStack meshMatrices = new PoseStack();
    private static int lastBuildTriangleCount;

    /** 携带时整体缩小，保留已确认的肩上大小。 */
    private float renderScale = 1.0F;


    public TomoriPenguinMeshModel(ResourceManager resources) {
        super(buildTree(resources), id -> RenderTypes.entityCutout(id, false));
        this.triangleCount = lastBuildTriangleCount;
        this.meshTriangles = lastBuildTriangles;
        this.bindPivots = lastBuildPivots;
        this.neckSkin = new NeckSkin(restVertices("head"), restVertices("body"));
        this.wingSkin = new WingSkin(lastBuildWingAnchors, neckSkin);
        for (var group : meshTriangles.entrySet()) {
            String bone = group.getKey();
            boolean wing = bone.startsWith("wing_");
            if (!wing && !bone.equals("body") && !bone.equals("head")) continue;
            float[] p = bindPivots.get(bone);
            for (MeshTriangle tri : group.getValue()) {
                Vector3f a = new Vector3f(tri.ax + p[0], tri.ay + p[1], tri.az + p[2]);
                Vector3f b = new Vector3f(tri.bx + p[0], tri.by + p[1], tri.bz + p[2]);
                Vector3f c = new Vector3f(tri.cx + p[0], tri.cy + p[1], tri.cz + p[2]);
                if (wing) {
                    wingSkin.add(bone.substring(5, 6), a, tri.au, tri.av, b, tri.bu, tri.bv, c, tri.cu, tri.cv);
                } else {
                    neckSkin.add(bone.equals("head"), a, tri.au, tri.av, b, tri.bu, tri.bv, c, tri.cu, tri.cv);
                }
            }
        }
        this.anims = loadAnimations(resources);
        this.pose = BbBakedAnimations.newPose(BONES);
        this.scratch = BbBakedAnimations.newPose(BONES);
        ModelPart root = root();
        for (String bone : BONES) {
            ModelPart part = find(root, bone);
            if (part == null) {
                throw new IllegalStateException("网格骨骼树缺少: " + bone);
            }
            parts.put(bone, part);
            baseOrigin.put(bone, new float[] {part.x, part.y, part.z});
        }
        GeneratedMod.LOGGER.info("[pet] .blend 平滑网格加载完成：{} 个三角面，{} 根骨骼",
            triangleCount, BONES.size());
    }

    /** 只在加载时识别原网格连接点，不修改用户模型，也不每帧扫描顶点。 */
    private List<Vector3f> restVertices(String bone) {
        float[] p = bindPivots.get(bone);
        java.util.Set<Vector3f> vertices = new java.util.HashSet<>();
        for (MeshTriangle tri : meshTriangles.getOrDefault(bone, List.of())) {
            vertices.add(new Vector3f(tri.ax + p[0], tri.ay + p[1], tri.az + p[2]));
            vertices.add(new Vector3f(tri.bx + p[0], tri.by + p[1], tri.bz + p[2]));
            vertices.add(new Vector3f(tri.cx + p[0], tri.cy + p[1], tri.cz + p[2]));
        }
        return List.copyOf(vertices);
    }

    private static BbBakedAnimations loadAnimations(ResourceManager resources) {
        try {
            return BbBakedAnimations.load(resources);
        } catch (Exception e) {
            throw new IllegalStateException("烘焙动画加载失败", e);
        }
    }

    private static ModelPart find(ModelPart root, String name) {
        List<String> chain = new ArrayList<>();
        String parent = PARENTS.get(name);
        while (parent != null && !"root".equals(parent)) {
            chain.add(0, parent);
            parent = PARENTS.get(parent);
        }
        ModelPart cur = root;
        for (String hop : chain) {
            cur = cur.getChild(hop);
        }
        return cur.getChild(name);
    }

    // ------------------------------------------------------------------
    // 网格 → ModelPart 树
    // ------------------------------------------------------------------

    private static ModelPart buildTree(ResourceManager resources) {
        JsonObject root;
        try {
            root = readJson(resources, MESH);
        } catch (Exception e) {
            throw new IllegalStateException("加载 .blend 平滑网格失败: " + MESH, e);
        }

        Map<String, List<Vector3f>> anchors = new HashMap<>();
        if (root.has("wing_anchors")) {
            for (var entry : root.getAsJsonObject("wing_anchors").entrySet()) {
                List<Vector3f> points = new ArrayList<>();
                for (JsonElement value : entry.getValue().getAsJsonArray()) {
                    JsonArray p = value.getAsJsonArray();
                    points.add(new Vector3f(p.get(0).getAsFloat(), MODEL_HEIGHT - p.get(1).getAsFloat(), p.get(2).getAsFloat()));
                }
                anchors.put(entry.getKey(), List.copyOf(points));
            }
        }
        lastBuildWingAnchors = Map.copyOf(anchors);

        // 骨骼轴心（JSON 空间 y 向上）→ 模型空间 y 向下
        Map<String, float[]> pivots = new HashMap<>();
        JsonElement pivotsEl = root.get("pivots");
        if (pivotsEl != null && pivotsEl.isJsonObject()) {
            for (var e : pivotsEl.getAsJsonObject().entrySet()) {
                JsonArray p = e.getValue().getAsJsonArray();
                pivots.put(e.getKey(), new float[] {
                    p.get(0).getAsFloat(),
                    MODEL_HEIGHT - p.get(1).getAsFloat(),
                    p.get(2).getAsFloat()
                });
            }
        }
        float[] bodyPivot = pivots.getOrDefault("body", new float[] {0, 19.1F, 0.6F});
        float[] headPivot = pivots.getOrDefault("head", new float[] {0, 10.5F, 0.3F});
        for (String bone : PARENTS.keySet()) {
            if (!pivots.containsKey(bone)) {
                // 空骨骼（尾、发束）：贴到最近的真实关节上，动画写回不 NPE
                pivots.put(bone, switch (bone) {
                    case "tuft_l", "tuft_r" -> headPivot;
                    default -> bodyPivot;
                });
            }
        }

        // 每个骨骼的三角面（不再伪装成 Cuboid，直接按四边形提交给 VertexConsumer）
        Map<String, List<MeshTriangle>> boneTris = new HashMap<>();
        int[] triangleCounter = new int[1];
        // 静止姿态轮廓包围盒（模型空间 y ∈ [0,24]）。越界说明顶点↔骨骼轴心
        // 换算又错了——上一版"只剩一圈碎块"就是这个原因，这里留一条响亮的日志。
        float[] bounds = {1e9F, 1e9F, 1e9F, -1e9F, -1e9F, -1e9F};
        JsonObject partsObj = root.getAsJsonObject("parts");
        for (var partEntry : partsObj.entrySet()) {
            String bone = partEntry.getKey();
            if (!PARENTS.containsKey(bone)) {
                GeneratedMod.LOGGER.warn("[pet] 网格里有未知骨骼部件 {}，已跳过", bone);
                continue;
            }
            float[] pivot = pivots.get(bone);
            boneTris.put(bone, buildTriangles(partEntry.getValue().getAsJsonObject(), pivot, triangleCounter, bounds));
        }
        if (bounds[1] < -2.5F || bounds[4] > 26.5F) {
            GeneratedMod.LOGGER.error("[pet] 网格静止轮廓越界，模型会散架或沉地：y=[{}, {}]", bounds[1], bounds[4]);
        }

        // 子表（父先子后顺序保证递归安全）
        Map<String, List<String>> kids = new HashMap<>();
        for (var e : PARENTS.entrySet()) {
            if (!"root".equals(e.getValue())) {
                kids.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
        }

        ModelPart rootPart;
        Map<String, ModelPart> built = new HashMap<>();
        // 反序遍历：子骨骼先建好，父骨骼构造时直接传入 children 映射
        List<String> reverseOrder = new ArrayList<>(PARENTS.keySet());
        java.util.Collections.reverse(reverseOrder);
        for (String bone : reverseOrder) {
            Map<String, ModelPart> childMap = new HashMap<>();
            for (String child : kids.getOrDefault(bone, List.of())) {
                childMap.put(child, built.get(child));
            }
            ModelPart part = new ModelPart(List.of(), childMap);
            String parent = PARENTS.get(bone);
            float[] pp = "root".equals(parent) ? new float[] {0, 0, 0} : pivots.get(parent);
            float[] p = pivots.get(bone);
            part.setPos(p[0] - pp[0], p[1] - pp[1], p[2] - pp[2]);
            built.put(bone, part);
        }
        rootPart = new ModelPart(List.of(), Map.of("body", built.get("body")));
        lastBuildPivots = Map.copyOf(pivots);
        lastBuildTriangles = Map.copyOf(boneTris);
        lastBuildTriangleCount = triangleCounter[0];
        return rootPart;
    }

    /** 把一个部件的三角面转换成骨骼局部坐标下的三角形（含面法线与图集 UV）。 */
    private static List<MeshTriangle> buildTriangles(JsonObject part, float[] pivot, int[] counter, float[] bounds) {
        JsonArray vertices = part.getAsJsonArray("vertices");
        JsonArray triangles = part.getAsJsonArray("triangles");

        float[] xs = new float[vertices.size()];
        float[] ys = new float[vertices.size()];
        float[] zs = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            JsonArray v = vertices.get(i).getAsJsonArray();
            xs[i] = v.get(0).getAsFloat() - pivot[0];
            // pivot[1] 已是模型空间（24-py），顶点也必须先翻到模型空间再相减；
            // 早先写成 pivot[1]-vy 等于把 y 翻转做了两次，身体/脚会整体沉到地下，
            // 游戏里只剩头、翅、发束飘成一圈碎块（"灰方块"症状的根源）。
            ys[i] = (MODEL_HEIGHT - v.get(1).getAsFloat()) - pivot[1];
            zs[i] = v.get(2).getAsFloat() - pivot[2];
            bounds[0] = Math.min(bounds[0], pivot[0] + xs[i]);
            bounds[1] = Math.min(bounds[1], pivot[1] + ys[i]);
            bounds[2] = Math.min(bounds[2], pivot[2] + zs[i]);
            bounds[3] = Math.max(bounds[3], pivot[0] + xs[i]);
            bounds[4] = Math.max(bounds[4], pivot[1] + ys[i]);
            bounds[5] = Math.max(bounds[5], pivot[2] + zs[i]);
        }

        List<MeshTriangle> tris = new ArrayList<>(triangles.size());
        for (JsonElement tEl : triangles) {
            JsonArray t = tEl.getAsJsonArray();
            int i0 = t.get(0).getAsInt();
            int i1 = t.get(1).getAsInt();
            int i2 = t.get(2).getAsInt();
            // y 翻转后必须反转绕序：发射顺序 (a, c, b)，法线 = cross(c-a, b-a)
            float ax = xs[i0], ay = ys[i0], az = zs[i0];
            float bx = xs[i1], by = ys[i1], bz = zs[i1];
            float cx = xs[i2], cy = ys[i2], cz = zs[i2];
            float ux = cx - ax, uy = cy - ay, uz = cz - az;
            float vx = bx - ax, vy = by - ay, vz = bz - az;
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6F) {
                nx = 0; ny = 1; nz = 0; len = 1;
            }
            tris.add(new MeshTriangle(
                ax, ay, az, t.get(3).getAsFloat(), t.get(4).getAsFloat(),
                cx, cy, cz, t.get(7).getAsFloat(), t.get(8).getAsFloat(),
                bx, by, bz, t.get(5).getAsFloat(), t.get(6).getAsFloat(),
                nx / len, ny / len, nz / len));
            counter[0]++;
        }
        return tris;
    }

    private static JsonObject readJson(ResourceManager resources, Identifier id) throws Exception {
        var resource = resources.getResource(id)
            .orElseThrow(() -> new IllegalStateException("missing mesh asset " + id));
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // 动画（与方块版共用同一套烘焙动画与混合逻辑）
    // ------------------------------------------------------------------

    @Override
    public void setupAnim(PenguinPetRenderState state) {
        float t = state.age * 0.05F; // tick → 秒
        PetExpression expression = PetExpression.byIndex(state.animatedExpressionId);
        float envExpr = state.expressionWeight;
        float envCarry = state.carryWeight;
        float envSniff = state.sniffWeight;
        float exprElapsed = state.expressionElapsed;
        boolean exprOn = envExpr > 0.001F;
        boolean carryOn = state.carryMode > 0;
        // 只保留原来的左肩大小；缩放在 renderMesh 里围绕脚底进行。
        renderScale = carryOn ? 0.38F : 1.0F;
        if (state.bossMode) {
            renderScale = 1.18F;
        }

        anims.apply("idle", t, BONES, pose);

        // 步态相位直接取 limbSwing（按“走过的距离”累积，1 周期 = 2π）：
        // 走多快步频多快，企鹅放慢脚步后动作自然跟着变慢，不再“位置在动/动作定住”。
        float walk = state.gaitAmount > 0.01F
            ? state.gaitAmount
            : Math.min(1.0F, state.limbSwingAmplitude);
        if (!carryOn && walk > 0.01F && anims.has("walk")) {
            float walkT = state.limbSwingAnimationProgress * 0.159155F; // 1/(2π)
            anims.apply("walk", walkT, BONES, scratch);
            BbBakedAnimations.blend(BONES, pose, scratch, walk);
        }
        if (envSniff > 0.02F && anims.has("sniff")) {
            anims.apply("sniff", t, BONES, scratch);
            BbBakedAnimations.blend(BONES, pose, scratch, envSniff);
        }
        if (envCarry > 0.02F && anims.has("carry")) {
            anims.apply("carry", t, BONES, scratch);
            BbBakedAnimations.blend(BONES, pose, scratch, envCarry);
        }
        String exprAnim = expressionAnim(expression);
        if (envExpr > 0.01F && exprAnim != null && anims.has(exprAnim)) {
            anims.apply(exprAnim, exprElapsed * 0.05F, BONES, scratch);
            BbBakedAnimations.blend(BONES, pose, scratch, envExpr);
        }

        // 翅膀的统一校正移到所有额外动作角度之后，避免动作把翅膀再次折回身体。

        if (!carryOn && walk > 0.01F) {
            float step = (float) Math.sin(state.limbSwingAnimationProgress);
            for (String side : List.of("l", "r")) {
                float sign = "l".equals(side) ? -1.0F : 1.0F;
                pose.get("foot_" + side)[0] += sign * step * 5.0F * walk;
                pose.get("foot_" + side + "_mid")[0] += sign * step * 8.0F * walk;
                pose.get("foot_" + side + "_toe")[0] += sign * step * 10.0F * walk;
            }
        }
        float tuft = (float) Math.sin(t * 4.1887903F) * 1.4F;
        pose.get("tuft_l")[2] += tuft;
        pose.get("tuft_r")[2] -= tuft;

        if (exprOn) {
            // 参考视频的动作重点不是单独转头，而是头、身体、两侧鳍和重心一起动。
            float beat = exprElapsed * 0.24F;
            float wave = (float) Math.sin(beat);
            float pulse = (float) Math.abs(Math.sin(beat * 0.82F));
            switch (expression) {
                case SMILE -> {
                    pose.get("body")[4] += pulse * 0.65F * envExpr;
                    pose.get("head")[2] += wave * 5.0F * envExpr;
                    pose.get("wing_l")[2] -= (12.0F + pulse * 12.0F) * envExpr;
                    pose.get("wing_r")[2] += (12.0F + pulse * 12.0F) * envExpr;
                }
                case SMUG -> {
                    pose.get("body")[2] += 6.0F * envExpr;
                    pose.get("head")[1] += 15.0F * envExpr;
                    pose.get("head")[2] += 9.0F * envExpr;
                    pose.get("wing_l")[0] -= 24.0F * envExpr;
                    pose.get("wing_r")[2] += (18.0F + pulse * 10.0F) * envExpr;
                }
                case HUNGRY -> {
                    // 视频“饿啊”：连续小跳、快速扑鳍并抬头催促。
                    pose.get("body")[4] += pulse * 2.8F * envExpr;
                    pose.get("body")[0] += (pulse * 8.0F - 3.0F) * envExpr;
                    pose.get("wing_l")[2] -= (22.0F + pulse * 30.0F) * envExpr;
                    pose.get("wing_r")[2] += (22.0F + pulse * 30.0F) * envExpr;
                    pose.get("head")[0] += wave * 9.0F * envExpr;
                }
                case GREET -> {
                    // 视频“咕咕嘎嘎”：抬头后仰、张开双鳍，身体随叫声起伏。
                    pose.get("head")[0] -= (18.0F + pulse * 10.0F) * envExpr;
                    pose.get("body")[0] -= (8.0F + pulse * 5.0F) * envExpr;
                    pose.get("body")[4] += pulse * 0.9F * envExpr;
                    pose.get("wing_l")[2] -= (40.0F + pulse * 12.0F) * envExpr;
                    pose.get("wing_r")[2] += (40.0F + pulse * 12.0F) * envExpr;
                }
                case CRY -> {
                    pose.get("head")[0] += (24.0F + pulse * 7.0F) * envExpr;
                    pose.get("head")[2] -= 7.0F * envExpr;
                    pose.get("body")[0] += 10.0F * envExpr;
                    pose.get("wing_l")[0] += 22.0F * envExpr;
                    pose.get("wing_r")[0] += 22.0F * envExpr;
                }
                case CONFUSED -> {
                    float dir = ((int) (exprElapsed / 32.0F) & 1) == 0 ? 1.0F : -1.0F;
                    pose.get("head")[2] += dir * pulse * 22.0F * envExpr;
                    pose.get("head")[0] -= pulse * 6.0F * envExpr;
                    pose.get("wing_l")[2] -= pulse * 13.0F * envExpr;
                    pose.get("wing_r")[2] += (1.0F - pulse) * 9.0F * envExpr;
                }
                case ANGRY -> {
                    pose.get("body")[0] += 13.0F * envExpr;
                    pose.get("head")[0] += 12.0F * envExpr;
                    pose.get("head")[1] += wave * 8.0F * envExpr;
                    pose.get("wing_l")[2] -= (28.0F + pulse * 18.0F) * envExpr;
                    pose.get("wing_r")[2] += (28.0F + pulse * 18.0F) * envExpr;
                }
                case CLINGY -> {
                    pose.get("body")[2] += wave * 5.0F * envExpr;
                    pose.get("head")[2] -= 12.0F * envExpr;
                    pose.get("head")[1] += wave * 8.0F * envExpr;
                    pose.get("wing_l")[0] -= 25.0F * envExpr;
                    pose.get("wing_r")[0] -= 25.0F * envExpr;
                }
                case PICKUP -> { }
            }
        }
        float bossHover = 0.0F;
        if (state.bossMode) {
            bossHover = (float) Math.sin(t * 5.0F);
            pose.get("body")[0] -= 7.0F;
            pose.get("body")[4] += bossHover * 0.35F;
            pose.get("head")[0] -= 8.0F;
        }

        if (state.bossMode || carryOn) {
            // 不动已经确认的 Boss 和肩背安静姿态。
            applyContinuousWingPose(pose, expression, exprElapsed, exprOn ? envExpr : 0,
                state.bossMode, carryOn, t);
        } else {
            // 原样恢复旧版表情的肩根和分段动作，不再被简化动作清空。
            PetWingAnimation.apply(pose, expression, exprElapsed, envExpr);
        }
        if (state.carryMode > 0) {
            // 肩上保持站姿，脚掌是定位基准，不再叠加三段大角度收腿。
            for (String bone : List.of("body", "foot_l", "foot_l_mid", "foot_l_toe",
                "foot_r", "foot_r_mid", "foot_r_toe")) {
                float[] p = pose.get(bone);
                java.util.Arrays.fill(p, 0);
                p[6] = p[7] = p[8] = 1;
            }
        }

        // ---------------- 写入 ModelPart（BB → 原版换算） ----------------
        for (String bone : BONES) {
            float[] p = pose.get(bone);
            ModelPart part = parts.get(bone);
            float[] o = baseOrigin.get(bone);
            part.xRot = p[0] * R2R;
            part.yRot = p[1] * R2R;
            part.zRot = -p[2] * R2R;
            part.setPos(o[0] + p[3], o[1] - p[4], o[2] + p[5]);
            part.xScale = p[6];
            part.yScale = p[7];
            part.zScale = p[8];
        }

        // 视线跟随：表情动画期间保留一部分，避免完全无视玩家。
        float gaze = (1.0F - envExpr * 0.75F) * (1.0F - envSniff);
        ModelPart head = parts.get("head");
        head.yRot += state.relativeHeadYaw * R2R * gaze;
        head.xRot += -state.pitch * R2R * gaze;
    }

    /** 小角度分配到真实蒙皮关节，避免旧版多层外展累加成反折。 */
    private static void applyContinuousWingPose(Map<String, float[]> current, PetExpression expression,
                                                float elapsed, float weight, boolean boss,
                                                boolean carry, float time) {
        String[] suffix = {"", "_upper", "_mid", "_fore", "_hand"};
        float pulse = (float) Math.sin(elapsed * 0.19F);
        for (String side : List.of("l", "r")) {
            boolean left = side.equals("l");
            float sign = left ? -1 : 1;
            float lift = 0, sweep = 0, curl = 0;
            switch (expression) {
                case GREET -> { lift = 47 + 10 * pulse; sweep = 10; curl = 8 * pulse; }
                case HUNGRY -> { lift = 27 + 14 * pulse; sweep = 26; curl = 10; }
                case SMILE -> { lift = 20 + 7 * pulse; sweep = 10; curl = 7; }
                case SMUG -> { lift = left ? 30 : 4; sweep = left ? 32 : 0; curl = left ? 20 : 0; }
                case CRY -> { lift = 10; sweep = 34; curl = 18; }
                case CONFUSED -> { lift = left ? 20 + pulse * 5 : 5; sweep = left ? 10 : 0; curl = 8; }
                case ANGRY -> { lift = 38 + 5 * pulse; sweep = -5; curl = 5; }
                case CLINGY -> { lift = 16; sweep = 32; curl = 14; }
                case PICKUP -> { lift = 6; sweep = 40; curl = 14; }
            }
            lift *= weight; sweep *= weight; curl *= weight;
            if (boss) { lift = -8 + (float) Math.sin(time * 5) * 3; sweep = -8; curl = 5; }
            if (carry) { lift *= 0.15F; sweep *= 0.15F; curl *= 0.2F; }
            float[] liftShare = {0.65F, 0.20F, 0.09F, 0.04F, 0.02F};
            float[] sweepShare = {0.4F, 0.28F, 0.18F, 0.09F, 0.05F};
            for (int i = 0; i < suffix.length; i++) {
                float[] p = current.get("wing_" + side + suffix[i]);
                java.util.Arrays.fill(p, 0);
                p[6] = p[7] = p[8] = 1;
                p[0] = -sweep * sweepShare[i];
                p[1] = sign * curl * (i < 2 ? 0 : 0.2F);
                p[2] = sign * (lift * liftShare[i] - (i == 0 ? 12 : 0));
                if (i > 1) p[0] += (float) Math.sin(time * 3.4F - i * 0.6F) * (boss ? 0.5F : 1.3F);
            }
        }
    }

    /** 表情轮盘 → 原模型动画映射（与方块版一致）。 */
    private static String expressionAnim(PetExpression expression) {
        return switch (expression) {
            case SMILE, SMUG -> "happy";
            case HUNGRY -> "walk";
            case GREET -> "excited";
            case CRY -> "sleep";
            case CONFUSED -> "idle";
            case ANGRY -> "angry";
            case CLINGY -> "carry";
            case PICKUP -> "pickup";
        };
    }

    public record PoseSnapshot(List<net.minecraft.client.model.geom.PartPose> bones, float scale) {}

    public PoseSnapshot capturePose(PenguinPetRenderState state) {
        setupAnim(state);
        // ModelPart.getTransform() 不保存缩放，必须把呼吸/挤压的三轴缩放一起快照。
        return new PoseSnapshot(BONES.stream().map(n -> {
            ModelPart p = parts.get(n);
            return new net.minecraft.client.model.geom.PartPose(p.x, p.y, p.z,
                p.xRot, p.yRot, p.zRot, p.xScale, p.yScale, p.zScale);
        }).toList(), renderScale);
    }

    public void restorePose(PoseSnapshot snapshot) {
        for (int i = 0; i < BONES.size(); i++) parts.get(BONES.get(i)).loadPose(snapshot.bones().get(i));
        renderScale = snapshot.scale();
    }

    public ModelPart part(String name) {
        return parts.getOrDefault(name, root());
    }

    /**
     * 自定义网格提交：逐骨骼摆好矩阵后，把三角面作为"退化四边形"(a,b,c,c)
     * 直接写入 VertexConsumer。不再塞进 ModelPart.Cuboid——原版 1.21.11 的
     * 实体渲染走批处理命令队列，Sodium 会对 Cuboid 走立方体快速路径，
     * 任意三角形伪装成立方体面时会被重排成散块（装 Sodium 后"只剩一圈碎块"
     * 的根源）。submitCustom + 直接顶点提交是自定义几何的标准兼容路径，
     * 与 Iris 光影管线天然兼容。
     *
     * <p>1.21.2 起实体贴图被拼进统一图集：走 submitModel 时原版会自动把 UV
     * 换算到图集坐标，submitCustom 直提顶点则必须自己换算——上一版漏了这步，
     * 0..1 的原始 UV 采到图集别的位置，游戏里就是"模型在、颜色全黑"。
     * sprite 的 minU..maxU / minV..maxV 即本贴图在图集里的矩形，线性映射回去
     * 与原版 Cuboid 管线完全一致。</p>
     */
    public void renderMesh(PoseStack.Pose entry, VertexConsumer consumer,
                           int light, int overlay, int color, TextureAtlasSprite sprite) {
        renderMesh(entry, consumer, light, overlay, color, sprite, 1.0F, false);
    }

    /** 白眼与本体共用完全相同的姿态和坐标；原版眼睛层允许同深度叠加。 */
    public void renderEyes(PoseStack.Pose entry, VertexConsumer consumer) {
        renderMesh(entry, consumer, 0x00F000F0,
            OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, 1.0F, true);
    }

    /** 保留旧离线检查入口；正式白眼绘制不再绕脚底放大整个头部。 */
    public void renderMesh(PoseStack.Pose entry, VertexConsumer consumer,
                           int light, int overlay, int color, TextureAtlasSprite sprite, float extraScale) {
        renderMesh(entry, consumer, light, overlay, color, sprite, extraScale, extraScale != 1.0F);
    }

    private void renderMesh(PoseStack.Pose entry, VertexConsumer consumer,
                            int light, int overlay, int color, TextureAtlasSprite sprite, float extraScale,
                            boolean eyesOnly) {
        meshMatrices.setIdentity();
        meshMatrices.last().set(entry);
        // 必须缩放“模型局部矩阵”，不能把已经加上镜头/世界位移的最终坐标再乘比例。
        // 旧写法会让肩背缩放围绕屏幕原点发生，因此游戏里看起来几乎没缩小还会错位。
        float scale = renderScale * extraScale;
        // 模型脚底在 y=24。绕脚底缩放，抵消原版 -1.501 的模型平移；
        // 原先只 scale 会凭空抬升 (1-scale)*1.5 格，正是落到玩家头上的原因。
        meshMatrices.translate(0, MODEL_HEIGHT / 16.0F, 0);
        meshMatrices.scale(scale, scale, scale);
        meshMatrices.translate(0, -MODEL_HEIGHT / 16.0F, 0);
        drawBone("body", consumer, light, overlay, color, sprite, eyesOnly);
        neckSkin.render(skinMatrices, consumer, light, overlay, color, sprite, eyesOnly);
        if (!eyesOnly) wingSkin.render(skinMatrices, consumer, light, overlay, color);
    }

    private void drawBone(String bone, VertexConsumer consumer,
                          int light, int overlay, int color, TextureAtlasSprite sprite, boolean eyesOnly) {
        ModelPart part = parts.get(bone);
        meshMatrices.pushPose();
        part.translateAndRotate(meshMatrices);
        PoseStack.Pose e = meshMatrices.last();
        Matrix4f m = e.pose();
        float[] bind = bindPivots.get(bone);
        skinMatrices.computeIfAbsent(bone, k -> new Matrix4f()).set(m)
            .translate(-bind[0] / 16, -bind[1] / 16, -bind[2] / 16);
        Vector3f tmp = new Vector3f();
        Vector3f nrm = new Vector3f();
        // 原始 0..1 UV → 图集矩形内的 UV（与原版模型管线同一换算）
        float u0 = sprite == null ? 0.0F : sprite.getU0();
        float v0 = sprite == null ? 0.0F : sprite.getV0();
        float us = sprite == null ? 1.0F : sprite.getU1() - u0;
        float vs = sprite == null ? 1.0F : sprite.getV1() - v0;
        boolean skinned = bone.startsWith("wing_") || bone.equals("body") || bone.equals("head");
        for (MeshTriangle t : skinned || eyesOnly ? List.<MeshTriangle>of() : meshTriangles.getOrDefault(bone, List.of())) {
            e.transformNormal(t.nx(), t.ny(), t.nz(), nrm);
            float nx = nrm.x();
            float ny = nrm.y();
            float nz = nrm.z();
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1.0E-6F) {
                nx /= len; ny /= len; nz /= len;
            }
            // ModelPart.Vertex.worldX/worldY/worldZ 会把模型小格换成方块单位；
            // 直发顶点时必须保留同样的 /16 换算。整体缩放已在局部矩阵上完成。
            m.transformPosition(t.ax() / 16.0F, t.ay() / 16.0F, t.az() / 16.0F, tmp);
            consumer.addVertex(tmp.x(), tmp.y(), tmp.z(), color, u0 + t.au() * us, v0 + t.av() * vs, overlay, light, nx, ny, nz);
            m.transformPosition(t.bx() / 16.0F, t.by() / 16.0F, t.bz() / 16.0F, tmp);
            consumer.addVertex(tmp.x(), tmp.y(), tmp.z(), color, u0 + t.bu() * us, v0 + t.bv() * vs, overlay, light, nx, ny, nz);
            m.transformPosition(t.cx() / 16.0F, t.cy() / 16.0F, t.cz() / 16.0F, tmp);
            consumer.addVertex(tmp.x(), tmp.y(), tmp.z(), color, u0 + t.cu() * us, v0 + t.cv() * vs, overlay, light, nx, ny, nz);
            consumer.addVertex(tmp.x(), tmp.y(), tmp.z(), color, u0 + t.cu() * us, v0 + t.cv() * vs, overlay, light, nx, ny, nz);
        }
        for (String child : KIDS.getOrDefault(bone, List.of())) {
            drawBone(child, consumer, light, overlay, color, sprite, eyesOnly);
        }
        meshMatrices.popPose();
    }

    /** 父先子后的子表（懒建，只建一次）。 */
    private static final Map<String, List<String>> KIDS = buildKids();

    private static Map<String, List<String>> buildKids() {
        Map<String, List<String>> kids = new HashMap<>();
        for (var e : PARENTS.entrySet()) {
            if (!"root".equals(e.getValue())) {
                kids.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
        }
        return Map.copyOf(kids);
    }

    public static List<String> boneNames() {
        return BONES;
    }

    public int meshTriangles() {
        return triangleCount;
    }
}
