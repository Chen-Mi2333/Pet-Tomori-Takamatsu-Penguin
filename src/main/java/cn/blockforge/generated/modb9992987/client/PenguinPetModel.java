package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.PetExpression;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原始 bbmodel 企鹅模型：几何、骨骼和动作全部来自用户提供的 penguin_pet.bbmodel
 * （tools/bake_bbmodel.py 烘焙），渲染走原版 ModelPart 管线，
 * 与 Iris / Sodium / 实体剔除类模组天然兼容。
 *
 * <p>动作优先级：idle 打底，走路、嗅探、背负、表情动画按权重混合；
 * 表情轮盘的 9 个表情映射到原模型的 happy/excited/angry/carry/sleep 动画，
 * 捡石头使用同格式手烘的 pickup 动画。</p>
 */
public class PenguinPetModel extends EntityModel<PenguinPetRenderState> {
    public static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("mod_b9992987", "textures/entity/penguin_pet_atlas.png");

    /** 骨骼顺序与父先子后顺序（与 geo 一致）。 */
    public static final List<String> BONES = List.of(
        "body", "head", "tuft_l", "tuft_r", "wing_l", "wing_r", "foot_l", "foot_r", "tail");

    private static final float R2R = 0.017453292F;

    private final Map<String, ModelPart> parts = new HashMap<>();
    private final Map<String, float[]> baseOrigin = new HashMap<>();
    private final BbBakedAnimations anims;
    private final Map<String, float[]> pose;
    private final Map<String, float[]> scratch;

    public PenguinPetModel(ResourceManager resources) {
        super(buildRoot(resources), id -> RenderTypes.entityCutout(id, false));
        this.anims = loadAnimations(resources);
        ModelPart root = root();
        for (String bone : BONES) {
            ModelPart part = find(root, bone);
            if (part == null) {
                throw new IllegalStateException("geo 缺少骨骼: " + bone);
            }
            parts.put(bone, part);
            baseOrigin.put(bone, new float[] {part.x, part.y, part.z});
        }
        pose = BbBakedAnimations.newPose(BONES);
        scratch = BbBakedAnimations.newPose(BONES);
    }

    private static ModelPart buildRoot(ResourceManager resources) {
        try {
            var bones = BbBakedModel.loadBones(resources);
            return BbBakedModel.toLayerDefinition(bones, 128, 128).bakeRoot();
        } catch (Exception e) {
            throw new IllegalStateException("原始 bbmodel 烘焙几何加载失败", e);
        }
    }

    private static BbBakedAnimations loadAnimations(ResourceManager resources) {
        try {
            return BbBakedAnimations.load(resources);
        } catch (Exception e) {
            throw new IllegalStateException("烘焙动画加载失败", e);
        }
    }

    /** 骨骼父子关系（与 geo 的 outliner 一致），按层级查找。 */
    private static final Map<String, String> PARENTS = Map.of(
        "body", "root", "head", "body", "tuft_l", "head", "tuft_r", "head",
        "wing_l", "body", "wing_r", "body", "foot_l", "body", "foot_r", "body", "tail", "body");

    private static ModelPart find(ModelPart root, String name) {
        ModelPart cur = root;
        String parent = PARENTS.get(name);
        while (parent != null && !"root".equals(parent)) {
            cur = cur.getChild(parent);
            parent = PARENTS.get(parent);
        }
        return cur.getChild(name);
    }

    @Override
    public void setupAnim(PenguinPetRenderState state) {
        float t = state.age * 0.05F; // tick → 秒
        PetExpression expression = PetExpression.byIndex(state.animatedExpressionId);
        float envExpr = state.expressionWeight;
        float envCarry = state.carryWeight;
        float envSniff = state.sniffWeight;
        float exprElapsed = state.expressionElapsed;
        boolean exprOn = envExpr > 0.001F;

        // ---------------- 动画混合 ----------------
        anims.apply("idle", t, BONES, pose);

        float walk = Math.min(1.0F, state.limbSwingAmplitude);
        if (walk > 0.01F && anims.has("walk")) {
            // limbSwingAnimationProgress 每步累积约 2π，换算成 walk 动画的秒
            float walkT = state.limbSwingAnimationProgress * 0.15915494F; // /(2π) × 1.0s
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
        // 疑问：在原动画上叠加歪头节拍（Blockbench 空间，z 为翻滚）
        if (expression == PetExpression.CONFUSED && exprOn) {
            float beat = exprElapsed % 32.0F;
            float dir = ((int) (exprElapsed / 32.0F) & 1) == 0 ? 1.0F : -1.0F;
            float tilt = beat < 18.0F ? (float) Math.sin(beat * 0.1745F) : 0.0F;
            float[] head = pose.get("head");
            head[2] += dir * tilt * 18.0F * envExpr;
            head[0] += -tilt * 6.0F * envExpr;
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

    /** 表情轮盘 → 原模型动画映射。 */
    private static String expressionAnim(PetExpression expression) {
        return switch (expression) {
            case SMILE, SMUG -> "happy";
            case HUNGRY, GREET -> "excited";
            case CRY -> "sleep";
            case CONFUSED -> "idle";
            case ANGRY -> "angry";
            case CLINGY -> "carry";
            case PICKUP -> "pickup";
        };
    }

    public ModelPart part(String name) {
        return parts.getOrDefault(name, root());
    }

    public static List<String> boneNames() {
        return BONES;
    }

    public ModelPart rootPart() {
        return root();
    }
}
