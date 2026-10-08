package cn.blockforge.generated.modb9992987.client.mixin;

import cn.blockforge.generated.modb9992987.client.SlingshotSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelRenderers.class)
public abstract class SpecialModelTypesMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void modB9992987$registerSlingshot(CallbackInfo ci) {
        SpecialModelTypesAccessor.modB9992987$getIdMapper().put(
            SlingshotSpecialRenderer.TYPE_ID, SlingshotSpecialRenderer.Unbaked.CODEC);
    }
}
