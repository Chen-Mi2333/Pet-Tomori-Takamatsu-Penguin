package cn.blockforge.generated.modb9992987.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 安全降级模型：烘焙几何异常时使用。
 * 只保留原始 bbmodel 的 body + head 两个立方体（同一张烘焙图集、同一套坐标），
 * 保证任何情况下都能看到正确的企鹅轮廓，不会变透明方块。
 */
public class PenguinPetFallbackModel extends EntityModel<PenguinPetRenderState> {
    private final ModelPart body;
    private final ModelPart head;

    public PenguinPetFallbackModel(ModelPart root) {
        super(root, id -> RenderTypes.entityCutout(id, false));
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
    }

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, 0.0F, -4.0F, 10.0F, 10.0F, 8.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0)
            .addBox(-4.5F, -8.0F, -4.0F, 9.0F, 8.0F, 8.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(data, 128, 128);
    }

    @Override
    public void setupAnim(PenguinPetRenderState state) {
        head.yRot = state.relativeHeadYaw * 0.017453292F;
        head.xRot = -state.pitch * 0.017453292F;
    }

    public ModelPart part(String name) {
        return "head".equals(name) ? head : body;
    }
}
