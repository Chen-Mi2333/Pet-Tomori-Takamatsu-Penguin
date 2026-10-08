package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.BlockSlingshotItem;
import cn.blockforge.generated.modb9992987.GeneratedMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 直接读取用户提供的 {@code 弹弓.bbmodel} 网格与内嵌贴图。
 * 模型保持原格式；这里只把 Blockbench 的多边形提交到 Minecraft 原生物品渲染管线。
 */
public final class SlingshotSpecialRenderer
    implements SpecialModelRenderer<SlingshotSpecialRenderer.RenderData> {

    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath("mod_b9992987", "block_slingshot");
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("mod_b9992987", "textures/item/block_slingshot_mesh_r50.png");
    private static final String MODEL_RESOURCE =
        "/assets/mod_b9992987/models/item/block_slingshot.bbmodel";
    private static final net.minecraft.client.renderer.rendertype.RenderType LAYER = RenderTypes.entityCutout(TEXTURE, false);

    private static final float TEXTURE_SIZE = 256.0F;
    private static final float BAND_ORIGIN_Z = 1.23F;

    private static float shownPull;
    private static float previousPull;
    private static float recoil;

    private final List<MeshTriangle> triangles;
    private static SlingshotSpecialRenderer firstPersonRenderer;
    private static float previousRecoil;

    /**
     * 参考用户 TACZ 的 LeftHandRender/RightHandRender：先定位武器和手部锚点，
     * 再直接提交玩家皮肤手臂。这里适配 1.21.11 队列，不链接 1.20.1 的 TACZ 类。
     */
    public static void renderFirstPerson(AbstractClientPlayer player, InteractionHand hand,
                                          float tickDelta, float equip, float swing,
                                          PoseStack matrices, SubmitNodeCollector queue, int light) {
        if (firstPersonRenderer == null) firstPersonRenderer = new SlingshotSpecialRenderer();
        HumanoidArm drawArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = drawArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float pull = smooth(Mth.lerp(tickDelta, previousPull, shownPull));
        float kick = Mth.lerp(tickDelta, previousRecoil, recoil);
        matrices.pushPose();
        // 全模型保持在相机前至少 0.6 格，不再在 z≈0 的近裁剪面上放大。
        matrices.translate(side * (0.10F - pull * 0.07F), -0.45F + pull * 0.13F - equip * 0.16F,
            -0.95F - pull * 0.04F);
        matrices.mulPose(Axis.YP.rotationDegrees(side * (28.0F + pull * 12.0F)));
        matrices.mulPose(Axis.ZP.rotationDegrees(side * (-8.0F + pull * 5.0F)));
        matrices.mulPose(Axis.XP.rotationDegrees(-3.0F - kick * 7.0F
            + Mth.sin(swing * (float) Math.PI) * 3.0F));
        // 弓架高 23 小格，保持 r51 的 0.27 缩放，拉弓不放大。
        float size = 0.27F;
        if (!player.isInvisible()) {
            // 握把和弹兜锚点取自原 bbmodel，皮筋与拉兜手使用同一 pull。
            renderAnchoredArm(player, drawArm.getOpposite(), new Vector3f(0, 4.6F, 0).mul(size / 16.0F),
                new Vector3f(-side * 0.48F, -0.38F, 0.28F), matrices, queue, light);
            renderAnchoredArm(player, drawArm, new Vector3f(0, 20.6F, 3.18F + 9.35F * pull).mul(size / 16.0F)
                    .add(side * 0.055F, -0.025F, 0.01F),
                new Vector3f(side * 0.49F, -0.32F, 0.38F), matrices, queue, light);
        }
        matrices.scale(size, size, size);
        matrices.translate(-0.5F, 0, -0.5F);
        queue.submitCustomGeometry(matrices, LAYER, (entry, consumer) ->
            firstPersonRenderer.draw(entry, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, pull));
        matrices.popPose();
    }

    private static void renderAnchoredArm(AbstractClientPlayer player, HumanoidArm arm, Vector3f hand,
                                           Vector3f elbow, PoseStack matrices,
                                           SubmitNodeCollector queue, int light) {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        var part = arm == HumanoidArm.RIGHT ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        var origin = part.getInitialPose();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean slim = player.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        // 原版手臂轴向为 +Y；以手掌末端 y=10 贴接触点，避免手背盖住小弓架。
        float palmX = -side * (slim ? 0.5F : 1.0F);
        float roll = side * 0.1F;
        float px = (float) (palmX * Math.cos(roll) - 10.0F * Math.sin(roll));
        float py = (float) (palmX * Math.sin(roll) + 10.0F * Math.cos(roll));
        Vector3f towardHand = new Vector3f(hand).sub(elbow).normalize();
        matrices.pushPose();
        matrices.translate(hand.x, hand.y, hand.z);
        matrices.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), towardHand));
        matrices.scale(0.42F, 1.20F, 0.42F);
        matrices.translate(-(origin.x() + px) / 16.0F, -(origin.y() + py) / 16.0F, -origin.z() / 16.0F);
        var skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightArm(matrices, queue, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            renderer.renderLeftArm(matrices, queue, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
        matrices.popPose();
    }

    public SlingshotSpecialRenderer() {
        this.triangles = loadTriangles();
        GeneratedMod.LOGGER.info("[slingshot] 用户 bbmodel 网格加载完成：{} 个三角面", triangles.size());
    }

    /** 每客户端 tick 更新一次，避免同一帧多次渲染导致动画速度受帧数影响。 */
    public static void clientTick(Minecraft client) {
        float target = 0.0F;
        if (client.player != null && client.player.isUsingItem()
            && client.player.getActiveItem().getItem() instanceof BlockSlingshotItem) {
            target = Mth.clamp(client.player.getTicksUsingItem() / 20.0F, 0.0F, 1.0F);
        }
        previousRecoil = recoil;
        previousPull = shownPull;
        if (target > 0.0F) {
            shownPull += (target - shownPull) * 0.42F;
            recoil *= 0.45F;
        } else {
            if (shownPull > 0.08F && previousPull > 0.08F) {
                recoil = Math.max(recoil, shownPull);
            }
            shownPull *= 0.18F;
            recoil *= 0.58F;
        }
        if (shownPull < 0.001F) {
            shownPull = 0.0F;
        }
        if (recoil < 0.001F) {
            recoil = 0.0F;
        }
    }

    @Override
    public RenderData extractArgument(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        boolean active = client.player != null && client.player.isUsingItem()
            && client.player.getActiveItem().getItem() instanceof BlockSlingshotItem;
        return new RenderData(active ? shownPull : 0.0F, recoil);
    }

    @Override
    public void submit(RenderData data, PoseStack matrices, SubmitNodeCollector queue,
                       int light, int overlay, boolean glint, int color) {
        float pull = smooth(data.pull());
        float kick = data.recoil();
        boolean firstPerson = false;
        float side = 1.0F;

        matrices.pushPose();
        if (firstPerson) {
            // TACZ 式动作：小尺寸武器保持在双手之间，拉动时平滑抬向准星，不再猛贴镜头。
            matrices.translate(-side * pull * 0.10F, pull * 0.07F, pull * 0.07F);
            matrices.mulPose(Axis.YP.rotationDegrees(side * pull * 18.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(-side * pull * 11.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(pull * 6.0F - kick * 5.0F));
            // 第一人称主路径由 renderFirstPerson 接管；其他调用也不做拉弓放大。
        } else {
            matrices.mulPose(Axis.XP.rotationDegrees(-pull * 12.0F));
            matrices.translate(0.0F, pull * 0.05F, pull * 0.07F);
        }

        final float drawPull = pull;
        // 1.21.11 对“无染色”的特殊模型会传入 0；直接相乘会把贴图整件染成纯黑。
        // 与原版三叉戟/盾牌一样，无染色情况必须显式使用不透明白色。
        final int drawColor = (color & 0x00FFFFFF) == 0 ? 0xFFFFFFFF : color;
        queue.submitCustomGeometry(matrices, LAYER, (entry, consumer) ->
            draw(entry, consumer, light, overlay, drawColor, drawPull));
        matrices.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        for (MeshTriangle t : triangles) {
            consumer.accept(toModelSpace(t.ax(), t.ay(), t.az(), t.part(), 0.0F));
            consumer.accept(toModelSpace(t.bx(), t.by(), t.bz(), t.part(), 0.0F));
            consumer.accept(toModelSpace(t.cx(), t.cy(), t.cz(), t.part(), 0.0F));
        }
    }

    private void draw(PoseStack.Pose entry, VertexConsumer consumer, int light, int overlay,
                      int color, float pull) {
        Matrix4f matrix = entry.pose();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f normal = new Vector3f();
        for (MeshTriangle t : triangles) {
            Vector3f pa = toModelSpace(t.ax(), t.ay(), t.az(), t.part(), pull);
            Vector3f pb = toModelSpace(t.bx(), t.by(), t.bz(), t.part(), pull);
            Vector3f pc = toModelSpace(t.cx(), t.cy(), t.cz(), t.part(), pull);
            matrix.transformPosition(pa, a);
            matrix.transformPosition(pb, b);
            matrix.transformPosition(pc, c);
            normal.set(pb).sub(pa).cross(new Vector3f(pc).sub(pa)).normalize();
            entry.transformNormal(normal.x, normal.y, normal.z, normal);
            normal.normalize();
            vertex(consumer, a, color, t.au(), t.av(), overlay, light, normal);
            vertex(consumer, b, color, t.bu(), t.bv(), overlay, light, normal);
            vertex(consumer, c, color, t.cu(), t.cv(), overlay, light, normal);
            vertex(consumer, c, color, t.cu(), t.cv(), overlay, light, normal);
        }
    }

    private static void vertex(VertexConsumer consumer, Vector3f p, int color, float u, float v,
                               int overlay, int light, Vector3f n) {
        consumer.addVertex(p.x, p.y, p.z, color, u, v, overlay, light, n.x, n.y, n.z);
    }

    /** Blockbench 小格坐标 → 物品模型坐标；皮筋和弹兜使用原文件的拉弓保持曲线。 */
    private static Vector3f toModelSpace(float x, float y, float z, Part part, float pull) {
        float eased = pull;
        if (part == Part.BAND) {
            float scaleZ = 1.0F + 4.869792F * eased;
            z = BAND_ORIGIN_Z + (z - BAND_ORIGIN_Z) * scaleZ;
        } else if (part == Part.POUCH) {
            z += 9.35F * eased;
        }
        return new Vector3f(x / 16.0F + 0.5F, y / 16.0F, z / 16.0F + 0.5F);
    }

    private static float smooth(float v) {
        v = Mth.clamp(v, 0.0F, 1.0F);
        return v * v * (3.0F - 2.0F * v);
    }

    private static List<MeshTriangle> loadTriangles() {
        try (var stream = SlingshotSpecialRenderer.class.getResourceAsStream(MODEL_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("缺少资源 " + MODEL_RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, JsonObject> elements = new HashMap<>();
            for (JsonElement e : root.getAsJsonArray("elements")) {
                JsonObject obj = e.getAsJsonObject();
                elements.put(obj.get("uuid").getAsString(), obj);
            }
            Map<String, Part> elementParts = new HashMap<>();
            Map<String, String> names = new HashMap<>();
            for (JsonElement g : root.getAsJsonArray("groups")) {
                JsonObject group = g.getAsJsonObject();
                names.put(group.get("uuid").getAsString(), group.get("name").getAsString());
            }
            for (JsonElement node : root.getAsJsonArray("outliner")) {
                classify(node, Part.FRAME, names, elementParts);
            }

            List<MeshTriangle> result = new ArrayList<>();
            for (var entry : elements.entrySet()) {
                JsonObject element = entry.getValue();
                Part part = elementParts.getOrDefault(entry.getKey(), Part.FRAME);
                JsonObject vertices = element.getAsJsonObject("vertices");
                for (var faceEntry : element.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = faceEntry.getValue().getAsJsonObject();
                    JsonArray order = face.getAsJsonArray("vertices");
                    JsonObject uv = face.getAsJsonObject("uv");
                    if (order.size() < 3) {
                        continue;
                    }
                    for (int i = 1; i < order.size() - 1; i++) {
                        result.add(makeTriangle(vertices, uv,
                            order.get(0).getAsString(), order.get(i).getAsString(),
                            order.get(i + 1).getAsString(), part));
                    }
                }
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("读取用户弹弓 bbmodel 失败", e);
        }
    }

    private static void classify(JsonElement node, Part inherited, Map<String, String> names,
                                 Map<String, Part> elementParts) {
        if (node.isJsonPrimitive()) {
            elementParts.put(node.getAsString(), inherited);
            return;
        }
        JsonObject obj = node.getAsJsonObject();
        String uuid = obj.get("uuid").getAsString();
        String name = names.getOrDefault(uuid, "");
        Part own = switch (name) {
            case "左侧皮筋", "右侧皮筋" -> Part.BAND;
            case "红色弹兜" -> Part.POUCH;
            default -> inherited;
        };
        JsonArray children = obj.getAsJsonArray("children");
        if (children != null) {
            for (JsonElement child : children) {
                classify(child, own, names, elementParts);
            }
        }
    }

    private static MeshTriangle makeTriangle(JsonObject vertices, JsonObject uv,
                                             String ia, String ib, String ic, Part part) {
        float[] a = vec(vertices.getAsJsonArray(ia));
        float[] b = vec(vertices.getAsJsonArray(ib));
        float[] c = vec(vertices.getAsJsonArray(ic));
        float[] auv = tex(uv.getAsJsonArray(ia));
        float[] buv = tex(uv.getAsJsonArray(ib));
        float[] cuv = tex(uv.getAsJsonArray(ic));
        return new MeshTriangle(a[0], a[1], a[2], auv[0], auv[1],
            b[0], b[1], b[2], buv[0], buv[1],
            c[0], c[1], c[2], cuv[0], cuv[1], part);
    }

    private static float[] vec(JsonArray a) {
        return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static float[] tex(JsonArray a) {
        return new float[] {a.get(0).getAsFloat() / TEXTURE_SIZE, a.get(1).getAsFloat() / TEXTURE_SIZE};
    }

    private enum Part { FRAME, BAND, POUCH }

    private record MeshTriangle(
        float ax, float ay, float az, float au, float av,
        float bx, float by, float bz, float bu, float bv,
        float cx, float cy, float cz, float cu, float cv,
        Part part) {
    }

    public record RenderData(float pull, float recoil) {
    }

    /** 无参数特殊模型定义，供 items/block_slingshot.json 以 type 引用。 */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderData> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<RenderData> bake(SpecialModelRenderer.BakingContext context) {
            return new SlingshotSpecialRenderer();
        }
    }
}
