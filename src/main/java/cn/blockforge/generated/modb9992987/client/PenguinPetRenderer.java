package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.GeneratedMod;
import cn.blockforge.generated.modb9992987.PenguinPetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/** 宠物企鹅渲染器，使用官方 26.1.2 提交节点管线。 */
public class PenguinPetRenderer extends MobRenderer<PenguinPetEntity, PenguinPetRenderState, EntityModel<PenguinPetRenderState>> {
    public static final Identifier TEXTURE = TomoriPenguinMeshModel.TEXTURE;
    public static final Identifier BOSS_TEXTURE = Identifier.fromNamespaceAndPath("mod_b9992987", "textures/entity/tomori_penguin_boss.png");
    public static final Identifier EYES_TEXTURE = Identifier.fromNamespaceAndPath("mod_b9992987", "textures/entity/penguin_pet_eyes.png");
    public static final Identifier EYES_GLOW_TEXTURE = Identifier.fromNamespaceAndPath("mod_b9992987", "textures/entity/penguin_pet_eyes_glow.png");

    private final Map<PenguinPetEntity, PetAnimationTimeline> animationTimelines = new java.util.WeakHashMap<>();

    public PenguinPetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, buildModel(ctx), 0.5F);
    }

    private static EntityModel<PenguinPetRenderState> buildModel(EntityRendererProvider.Context ctx) {
        var resources = ctx.getResourceManager();
        try {
            return new TomoriPenguinMeshModel(resources);
        } catch (Throwable t) {
            GeneratedMod.LOGGER.error("[pet] .blend 平滑网格加载失败，回退到旧 bbmodel 方块模型", t);
            try {
                return new PenguinPetModel(resources);
            } catch (Throwable t2) {
                GeneratedMod.LOGGER.error("[pet] bbmodel 方块模型也失败，启用安全降级模型", t2);
                try {
                    return new PenguinPetFallbackModel(PenguinPetFallbackModel.getLayerDefinition().bakeRoot());
                } catch (Throwable t3) {
                    GeneratedMod.LOGGER.error("[pet] 降级模型也失败，使用空白根模型", t3);
                    return new EmptyRootModel();
                }
            }
        }
    }

    @Override
    public PenguinPetRenderState createRenderState() {
        return new PenguinPetRenderState();
    }

    @Override
    public void submit(PenguinPetRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        Minecraft client = Minecraft.getInstance();
        if (state.carriedByCamera && client.options.getCameraType().isFirstPerson()) {
            return;
        }
        super.submit(state, poses, new MeshSubmitCollector(collector, this.model), camera);
    }

    @Override
    public void extractRenderState(PenguinPetEntity entity, PenguinPetRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.age = entity.tickCount + tickDelta;
        state.expressionId = entity.currentExpressionId();
        state.expressionTicks = entity.expressionTicks();
        state.carryMode = entity.carryMode();
        state.sniffTicks = entity.sniffTicks();
        state.intimacy = entity.intimacy();
        state.tamed = entity.isTame();
        state.staying = entity.isStaying();
        state.bossMode = entity.isBossMode();
        state.relativeHeadYaw = state.yRot;
        state.pitch = state.xRot;
        state.limbSwingAmplitude = state.walkAnimationSpeed;
        state.limbSwingAnimationProgress = state.walkAnimationPos;
        PetAnimationTimeline.Sample animation = animationTimelines.computeIfAbsent(entity, ignored -> new PetAnimationTimeline())
            .sample(state.age, state.expressionId, state.expressionTicks, state.carryMode, state.sniffTicks);
        state.animatedExpressionId = animation.expression();
        state.expressionElapsed = animation.elapsed();
        state.expressionWeight = animation.expressionWeight();
        state.carryWeight = animation.carryWeight();
        state.sniffWeight = animation.sniffWeight();
        state.carriedByCamera = false;

        if (state.carryMode > 0) {
            var owner = entity.getOwner();
            if (owner != null && owner.isAlive() && owner.level() == entity.level()) {
                Vec3 base = new Vec3(
                    net.minecraft.util.Mth.lerp(tickDelta, owner.xo, owner.getX()),
                    net.minecraft.util.Mth.lerp(tickDelta, owner.yo, owner.getY()),
                    net.minecraft.util.Mth.lerp(tickDelta, owner.zo, owner.getZ()));
                float bodyYaw = ShoulderPose.bodyYaw(owner.yBodyRotO, owner.yBodyRot, tickDelta);
                Vec3 target = PenguinPetEntity.carryPosition(owner, base, bodyYaw);
                state.x = target.x;
                state.y = target.y;
                state.z = target.z;
                state.bodyRot = bodyYaw;
                state.relativeHeadYaw = 0.0F;
                state.pitch = 0.0F;
                state.limbSwingAmplitude = 0.0F;
                state.carriedByCamera = owner == Minecraft.getInstance().getCameraEntity();
            }
        }
        double speed = entity.getDeltaMovement().horizontalDistance();
        boolean moving = speed > 0.012 || state.limbSwingAmplitude > 0.015F;
        state.gaitAmount = moving ? Math.min(1.0F, Math.max(0.35F, (float) (speed * 6.5))) : 0.0F;
        state.gaitTime = entity.tickCount * 0.05F;
    }

    @Override
    public Identifier getTextureLocation(PenguinPetRenderState state) {
        return state.bossMode ? BOSS_TEXTURE : TEXTURE;
    }

    @Override
    protected RenderType getRenderType(PenguinPetRenderState state, boolean showBody, boolean translucent, boolean outline) {
        if (outline || translucent) {
            return super.getRenderType(state, showBody, translucent, outline);
        }
        Identifier texture = state.bossMode ? BOSS_TEXTURE : TEXTURE;
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture, false);
    }

    @Override
    protected boolean affectedByCulling(PenguinPetEntity entity) {
        return false;
    }

    private static final class MeshSubmitCollector implements SubmitNodeCollector {
        private final SubmitNodeCollector delegate;
        private final Model meshModel;

        MeshSubmitCollector(SubmitNodeCollector delegate, Model meshModel) {
            this.delegate = delegate;
            this.meshModel = meshModel;
        }

        @Override
        public OrderedSubmitNodeCollector order(int index) {
            return delegate.order(index);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poses, RenderType type,
                                     int light, int overlay, int color, TextureAtlasSprite sprite,
                                     int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
            if (model == meshModel && model instanceof TomoriPenguinMeshModel mesh && state instanceof PenguinPetRenderState petState) {
                var pose = mesh.capturePose(petState);
                delegate.submitCustomGeometry(poses, type, (entry, consumer) -> {
                    mesh.restorePose(pose);
                    mesh.renderMesh(entry, consumer, light, overlay, color, sprite);
                });
                if (petState.bossMode && !petState.isInvisibleToPlayer) {
                    delegate.submitCustomGeometry(poses, net.minecraft.client.renderer.rendertype.RenderTypes.eyes(EYES_TEXTURE), (entry, consumer) -> {
                        mesh.restorePose(pose);
                        mesh.renderEyes(entry, consumer);
                    });
                    delegate.submitCustomGeometry(poses, net.minecraft.client.renderer.rendertype.RenderTypes.eyes(EYES_GLOW_TEXTURE), (entry, consumer) -> {
                        mesh.restorePose(pose);
                        mesh.renderEyes(entry, consumer);
                    });
                }
                return;
            }
            delegate.submitModel(model, state, poses, type, light, overlay, color, sprite, outlineColor, crumbling);
        }

        @Override public void submitShadow(PoseStack p, float s, List<EntityRenderState.ShadowPiece> pieces) { delegate.submitShadow(p, s, pieces); }
        @Override public void submitNameTag(PoseStack p, Vec3 pos, int y, Component label, boolean above, int color, double dist, CameraRenderState camera) { delegate.submitNameTag(p, pos, y, label, above, color, dist, camera); }
        @Override public void submitText(PoseStack p, float x, float y, FormattedCharSequence text, boolean shadow, net.minecraft.client.gui.Font.DisplayMode mode, int color, int background, int light, int opacity) { delegate.submitText(p, x, y, text, shadow, mode, color, background, light, opacity); }
        @Override public void submitFlame(PoseStack p, EntityRenderState state, Quaternionf q) { delegate.submitFlame(p, state, q); }
        @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState leash) { delegate.submitLeash(p, leash); }
        @Override public void submitModelPart(ModelPart part, PoseStack p, RenderType type, int light, int overlay, TextureAtlasSprite sprite, boolean crumble, boolean solid, int outline, ModelFeatureRenderer.CrumblingOverlay command, int color) { delegate.submitModelPart(part, p, type, light, overlay, sprite, crumble, solid, outline, command, color); }
        @Override public void submitMovingBlock(PoseStack p, net.minecraft.client.renderer.block.MovingBlockRenderState state) { delegate.submitMovingBlock(p, state); }
        @Override public void submitBlockModel(PoseStack p, RenderType type, List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts, int[] t, int light, int overlay, int color) { delegate.submitBlockModel(p, type, parts, t, light, overlay, color); }
        @Override public void submitBreakingBlockModel(PoseStack p, net.minecraft.client.renderer.block.dispatch.BlockStateModel model, long seed, int light) { delegate.submitBreakingBlockModel(p, model, seed, light); }
        @Override public void submitItem(PoseStack p, ItemDisplayContext display, int light, int overlay, int color, int[] transform, List<net.minecraft.client.resources.model.geometry.BakedQuad> quads, net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foil) { delegate.submitItem(p, display, light, overlay, color, transform, quads, foil); }
        @Override public void submitCustomGeometry(PoseStack p, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) { delegate.submitCustomGeometry(p, type, renderer); }
        @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) { delegate.submitParticleGroup(renderer); }
    }

    private static final class EmptyRootModel extends EntityModel<PenguinPetRenderState> {
        EmptyRootModel() { super(new ModelPart(List.of(), Map.of())); }
        @Override public void setupAnim(PenguinPetRenderState state) { }
    }
}
