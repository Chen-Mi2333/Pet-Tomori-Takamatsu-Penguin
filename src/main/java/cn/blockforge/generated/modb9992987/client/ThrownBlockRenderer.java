package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.ThrownBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** 按投掷物类型放大显示方块：Boss 巨石比弹弓方块更大。 */
public final class ThrownBlockRenderer extends ThrownItemRenderer<ThrownBlockEntity> {
    public ThrownBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownBlockEntity entity, ThrownItemRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        ((State) state).scale = entity.renderScale();
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack poses,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.scale(((State) state).scale, ((State) state).scale, ((State) state).scale);
        super.submit(state, poses, collector, camera);
        poses.popPose();
    }

    private static final class State extends ThrownItemRenderState {
        private float scale = 1.0F;
    }
}
