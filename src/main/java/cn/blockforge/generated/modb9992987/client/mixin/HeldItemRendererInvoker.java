package cn.blockforge.generated.modb9992987.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 开放官方第一人称物品提交方法，供客户端扩展保留调用入口。 */
@Mixin(ItemInHandRenderer.class)
public interface HeldItemRendererInvoker {
    @Invoker("renderPlayerArm")
    void modB9992987$renderPlayerArm(PoseStack poses, SubmitNodeCollector collector, int light,
                                     float equipProgress, float swingProgress, HumanoidArm arm);

    @Invoker("renderItem")
    void modB9992987$renderItem(LivingEntity player, ItemStack stack, ItemDisplayContext context,
                                PoseStack poses, SubmitNodeCollector collector, int light);
}
