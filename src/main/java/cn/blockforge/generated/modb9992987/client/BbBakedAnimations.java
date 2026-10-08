package cn.blockforge.generated.modb9992987.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烘焙动画采样器：读取 animations/penguin_pet.json（原始 bbmodel 的 8 个动画
 * 加上手烘的 pickup），按秒采样出每根骨骼的旋转/位移/缩放。
 *
 * <p>数值保持 Blockbench 约定（角度为度、位移为模型单位、y 向上）；
 * 到原版 ModelPart 的换算（pitch=+rx, yaw=ry, roll=-rz、位移 y 取反）
 * 由 PenguinPetModel 统一处理。</p>
 */
public final class BbBakedAnimations {
    public static final Identifier ANIMATIONS =
        Identifier.fromNamespaceAndPath("mod_b9992987", "animations/penguin_pet.json");

    /** 一条通道：时间轴 + 对应数值，线性插值。 */
    private record Channel(float[] times, float[][] values) {
        void sample(float t, float[] out, int off, float def) {
            if (times.length == 0) {
                out[off] = out[off + 1] = out[off + 2] = def;
                return;
            }
            if (t <= times[0]) {
                copy(values[0], out, off);
                return;
            }
            if (t >= times[times.length - 1]) {
                copy(values[values.length - 1], out, off);
                return;
            }
            int i = 0;
            while (i < times.length - 1 && times[i + 1] < t) {
                i++;
            }
            float span = times[i + 1] - times[i];
            float k = span <= 0 ? 0 : (t - times[i]) / span;
            // 平滑步：保留原动画关键帧，但让起停速度连续，避免旧版线性采样的机械感。
            k = k * k * (3.0F - 2.0F * k);
            float[] a = values[i];
            float[] b = values[i + 1];
            out[off] = a[0] + (b[0] - a[0]) * k;
            out[off + 1] = a[1] + (b[1] - a[1]) * k;
            out[off + 2] = a[2] + (b[2] - a[2]) * k;
        }

        private static void copy(float[] src, float[] dst, int off) {
            dst[off] = src[0];
            dst[off + 1] = src[1];
            dst[off + 2] = src[2];
        }
    }

    private static final Channel EMPTY = new Channel(new float[0], new float[0][]);

    private record Anim(float length, Map<String, Channel[]> bones) {
    }

    private final Map<String, Anim> anims = new HashMap<>();

    public static BbBakedAnimations load(net.minecraft.server.packs.resources.ResourceManager resources) throws Exception {
        BbBakedAnimations result = new BbBakedAnimations();
        JsonObject root = BbBakedModel.readJson(resources, ANIMATIONS);
        for (var e : root.getAsJsonObject("animations").entrySet()) {
            JsonObject a = e.getValue().getAsJsonObject();
            float length = a.get("length").getAsFloat();
            Map<String, Channel[]> bones = new HashMap<>();
            for (var b : a.getAsJsonObject("bones").entrySet()) {
                JsonObject chans = b.getValue().getAsJsonObject();
                bones.put(b.getKey(), new Channel[] {
                    channel(chans, "rotation"), channel(chans, "position"), channel(chans, "scale")
                });
            }
            result.anims.put(e.getKey(), new Anim(length, bones));
        }
        return result;
    }

    private static Channel channel(JsonObject owner, String name) {
        if (!owner.has(name)) {
            return EMPTY;
        }
        JsonArray keys = owner.getAsJsonArray(name);
        float[] times = new float[keys.size()];
        float[][] values = new float[keys.size()][3];
        for (int i = 0; i < keys.size(); i++) {
            JsonArray kf = keys.get(i).getAsJsonArray();
            times[i] = kf.get(0).getAsFloat();
            JsonArray v = kf.get(1).getAsJsonArray();
            values[i][0] = v.get(0).getAsFloat();
            values[i][1] = v.get(1).getAsFloat();
            values[i][2] = v.get(2).getAsFloat();
        }
        return new Channel(times, values);
    }

    public boolean has(String name) {
        return anims.containsKey(name);
    }

    /**
     * 在时刻 t（秒，自动取模循环）采样动画，写入 pose。
     * pose 每根骨骼是 float[9]：rot x/y/z（度）、pos x/y/z（单位）、scale x/y/z。
     */
    public void apply(String name, float t, List<String> boneOrder, Map<String, float[]> pose) {
        Anim anim = anims.get(name);
        for (String bone : boneOrder) {
            float[] p = pose.get(bone);
            Channel[] chans = anim == null ? null : anim.bones().get(bone);
            float localT = anim == null || anim.length() <= 0
                ? 0 : (t % anim.length() + anim.length()) % anim.length();
            (chans == null ? EMPTY : chans[0]).sample(localT, p, 0, 0F);
            (chans == null ? EMPTY : chans[1]).sample(localT, p, 3, 0F);
            (chans == null ? EMPTY : chans[2]).sample(localT, p, 6, 1F);
        }
    }

    /** dst = mix(dst, src, w)，逐骨骼。 */
    public static void blend(List<String> boneOrder, Map<String, float[]> dst,
                             Map<String, float[]> src, float w) {
        for (String bone : boneOrder) {
            float[] d = dst.get(bone);
            float[] s = src.get(bone);
            for (int i = 0; i < 3; i++) {
                d[i] += (s[i] - d[i]) * w;
                d[i + 3] += (s[i + 3] - d[i + 3]) * w;
                d[i + 6] += (s[i + 6] - d[i + 6]) * w;
            }
        }
    }

    public static Map<String, float[]> newPose(List<String> boneOrder) {
        Map<String, float[]> pose = new HashMap<>();
        for (String bone : boneOrder) {
            pose.put(bone, new float[] {0, 0, 0, 0, 0, 0, 1, 1, 1});
        }
        return pose;
    }

    private BbBakedAnimations() {
    }
}
