package cn.blockforge.generated.modb9992987.client.mixin;

import cn.blockforge.generated.modb9992987.BlockSlingshotItem;
import cn.blockforge.generated.modb9992987.client.SlingshotSpecialRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 双手弹弓渲染接管官方 ItemInHandRenderer 的私有手持物路径。 */
@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void modB9992987$renderSlingshotRig(AbstractClientPlayer player,
                                                  float pitch, float handHeight,
                                                  InteractionHand hand, float swing,
                                                  ItemStack stack, float equip,
                                                  PoseStack poses, SubmitNodeCollector collector,
                                                  int light, CallbackInfo ci) {
        boolean main = player.getMainHandItem().getItem() instanceof BlockSlingshotItem;
        boolean off = player.getOffhandItem().getItem() instanceof BlockSlingshotItem;
        if ((main && hand == InteractionHand.OFF_HAND) || (!main && off && hand == InteractionHand.MAIN_HAND)) {
            ci.cancel();
            return;
        }
        if (stack.getItem() instanceof BlockSlingshotItem) {
            SlingshotSpecialRenderer.renderFirstPerson(player, hand, pitch, equip, swing, poses, collector, light);
            ci.cancel();
        }
    }
}
