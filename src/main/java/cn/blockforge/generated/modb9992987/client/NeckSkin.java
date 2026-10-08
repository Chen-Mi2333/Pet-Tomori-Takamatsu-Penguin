package cn.blockforge.generated.modb9992987.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 头部保持刚性完整转动；只让真实颈口附近的胸部顶点柔性跟随，补缝不再拉住脸。 */
final class NeckSkin {
    private record Vertex(Vector3f rest, float u, float v, float headWeight) {}
    private record Triangle(Vertex a, Vertex b, Vertex c, boolean head) {}
    private final List<Triangle> triangles = new ArrayList<>();

    private final List<Vector3f> headVertices;
    private final List<Vector3f> neckBoundary;

    NeckSkin(List<Vector3f> headVertices, List<Vector3f> bodyVertices) {
        this.headVertices = headVertices;
        this.neckBoundary = new ArrayList<>();
        for (Vector3f p : bodyVertices) {
            if (distanceSquared(p, headVertices) < 1.0E-6F) neckBoundary.add(p);
        }
    }

    private static float distanceSquared(Vector3f p, List<Vector3f> points) {
        float best = Float.POSITIVE_INFINITY;
        for (Vector3f q : points) best = Math.min(best, p.distanceSquared(q));
        return best;
    }

    // 不用高度切整个头：头发、脸颊、下巴即使低于颈口也都完整跟头旋转。
    // 相同位置在头、胸口、肩根共享权重；只在真实颈口向胸部的两小格过渡。
    float headWeight(Vector3f p) {
        if (distanceSquared(p, headVertices) < 1.0E-6F) return 1.0F;
        float distance = (float) Math.sqrt(distanceSquared(p, neckBoundary));
        float w = Math.max(0, 1 - distance / 2.0F);
        return w * w * (3 - 2 * w);
    }

    private Vertex vertex(Vector3f p, float u, float v) {
        return new Vertex(new Vector3f(p).div(16), u, v, headWeight(p));
    }

    void add(boolean head, Vector3f a, float au, float av, Vector3f b, float bu, float bv,
             Vector3f c, float cu, float cv) {
        triangles.add(new Triangle(vertex(a, au, av), vertex(b, bu, bv), vertex(c, cu, cv), head));
    }

    void render(Map<String, Matrix4f> matrices, VertexConsumer out, int light, int overlay,
                int color, TextureAtlasSprite sprite, boolean eyesOnly) {
        Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f();
        Vector3f tmp = new Vector3f(), n = new Vector3f(), edge = new Vector3f();
        float u0 = sprite == null ? 0 : sprite.getU0();
        float v0 = sprite == null ? 0 : sprite.getV0();
        float us = sprite == null ? 1 : sprite.getU1() - u0;
        float vs = sprite == null ? 1 : sprite.getV1() - v0;
        for (Triangle t : triangles) {
            // 白眼蒙版只有头部有内容，附加两层不再重复提交整身和双翅。
            if (eyesOnly && !t.head) continue;
            transform(t.a, matrices, a, tmp);
            transform(t.b, matrices, b, tmp);
            transform(t.c, matrices, c, tmp);
            n.set(b).sub(a).cross(edge.set(c).sub(a));
            // 肩乘缩到 0.38 后面积平方只有约 2%；不能把真实的眼角/脸部小面误删。
            if (n.lengthSquared() < 1.0E-20F || !n.isFinite()) continue;
            n.normalize();
            emit(out, a, t.a, n, light, overlay, color, u0, v0, us, vs);
            emit(out, b, t.b, n, light, overlay, color, u0, v0, us, vs);
            emit(out, c, t.c, n, light, overlay, color, u0, v0, us, vs);
            emit(out, c, t.c, n, light, overlay, color, u0, v0, us, vs);
            if (eyesOnly) {
                // 原模型眼片绕序混合；身体原本双面显示，原版 eyes 却会剔除背面。
                // 只给白眼补反向面，剔除后仍只显示一面，不改变本体或发光强度。
                n.negate();
                emit(out, a, t.a, n, light, overlay, color, u0, v0, us, vs);
                emit(out, c, t.c, n, light, overlay, color, u0, v0, us, vs);
                emit(out, b, t.b, n, light, overlay, color, u0, v0, us, vs);
                emit(out, b, t.b, n, light, overlay, color, u0, v0, us, vs);
            }
        }
    }

    private static void transform(Vertex v, Map<String, Matrix4f> matrices, Vector3f out, Vector3f tmp) {
        matrices.get("body").transformPosition(v.rest, out);
        matrices.get("head").transformPosition(v.rest, tmp);
        out.lerp(tmp, v.headWeight);
    }

    private static void emit(VertexConsumer out, Vector3f p, Vertex v, Vector3f n,
                             int light, int overlay, int color, float u0, float v0, float us, float vs) {
        out.addVertex(p.x, p.y, p.z, color, u0 + v.u * us, v0 + v.v * vs, overlay, light, n.x, n.y, n.z);
    }
}
