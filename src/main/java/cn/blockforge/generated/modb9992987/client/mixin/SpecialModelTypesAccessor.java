package cn.blockforge.generated.modb9992987.client.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露官方特殊模型渲染器类型表。 */
@Mixin(SpecialModelRenderers.class)
public interface SpecialModelTypesAccessor {
    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> modB9992987$getIdMapper() {
        throw new AssertionError("mixin accessor 未应用");
    }
}
