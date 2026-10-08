package cn.blockforge.generated.modb9992987.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 连续权重蒙皮：同一位置总用同一组权重，关节能弯而接缝不分家。 */
final class WingSkin {
    private record Vertex(Vector3f rest, float u, float v, String a, String b,
                          float weight, float seamWeight, float headWeight) {}
    private record Triangle(Vertex a, Vertex b, Vertex c) {}
    private final List<Triangle> triangles = new ArrayList<>();
    private final Map<String, List<Vector3f>> anchors;
    private final NeckSkin neckSkin;

    WingSkin(Map<String, List<Vector3f>> anchors, NeckSkin neckSkin) {
        this.anchors = anchors;
        this.neckSkin = neckSkin;
    }

    void add(String side, Vector3f a, float au, float av, Vector3f b, float bu, float bv,
             Vector3f c, float cu, float cv) {
        triangles.add(new Triangle(vertex(side, a, au, av), vertex(side, b, bu, bv), vertex(side, c, cu, cv)));
    }

    private Vertex vertex(String side, Vector3f p, float u, float v) {
        String root = "wing_" + side;
        float distance = Float.POSITIVE_INFINITY;
        for (Vector3f anchor : anchors.getOrDefault(side, List.of())) {
            distance = Math.min(distance, p.distance(anchor));
        }
        // 原肩缝顶点完整跟随身体；外侧两小格再平滑过渡至整条蒙皮链。
        float seamWeight = Math.min(1, distance / 2.0F);
        seamWeight = seamWeight * seamWeight * (3 - 2 * seamWeight);
        String[] bones = {"body", root, root + "_upper", root + "_mid", root + "_fore", root + "_hand"};
        // 肩内侧留在胸部，向外连续过渡到上臂、肘和鳍尖。
        float[] centers = {2.0F, 3.5F, 4.8F, 6.3F, 7.7F, 9.3F};
        float x = Math.abs(p.x);
        int i = 0;
        while (i < centers.length - 2 && x > centers[i + 1]) i++;
        float w = Math.max(0, Math.min(1, (x - centers[i]) / (centers[i + 1] - centers[i])));
        w = w * w * (3 - 2 * w);
        return new Vertex(new Vector3f(p).div(16), u, v, bones[i], bones[i + 1], w, seamWeight,
            neckSkin.headWeight(p));
    }

    void render(Map<String, Matrix4f> skinMatrices, VertexConsumer out, int light, int overlay, int color) {
        Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f();
        Vector3f tmp = new Vector3f(), n = new Vector3f(), edge = new Vector3f();
        for (Triangle t : triangles) {
            transform(t.a, skinMatrices, a, tmp);
            transform(t.b, skinMatrices, b, tmp);
            transform(t.c, skinMatrices, c, tmp);
            n.set(b).sub(a).cross(edge.set(c).sub(a));
            if (n.lengthSquared() < 1.0E-20F || !n.isFinite()) continue;
            n.normalize();
            emit(out, a, t.a, n, light, overlay, color);
            emit(out, b, t.b, n, light, overlay, color);
            emit(out, c, t.c, n, light, overlay, color);
            emit(out, c, t.c, n, light, overlay, color);
        }
    }

    private static void transform(Vertex v, Map<String, Matrix4f> matrices, Vector3f result, Vector3f tmp) {
        matrices.get(v.a).transformPosition(v.rest, result);
        matrices.get(v.b).transformPosition(v.rest, tmp);
        result.lerp(tmp, v.weight);
        float movingWeight = v.seamWeight;
        result.mul(movingWeight);
        // 与胸口/颈口共用同一目标，不再把接在头部的肩缝错误钉死在身体上。
        matrices.get("body").transformPosition(v.rest, tmp);
        result.fma((1 - movingWeight) * (1 - v.headWeight), tmp);
        matrices.get("head").transformPosition(v.rest, tmp);
        result.fma((1 - movingWeight) * v.headWeight, tmp);
    }
    private static void emit(VertexConsumer out, Vector3f p, Vertex v, Vector3f n, int light, int overlay, int color) {
        out.addVertex(p.x, p.y, p.z, color, v.u, v.v, overlay, light, n.x, n.y, n.z);
    }
}
