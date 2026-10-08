package cn.blockforge.generated.modb9992987;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/**
 * 自定义粒子：问号（表情图标）与钻石闪光（寻钻方向指示）。
 * 爱心 / 生气 / 音符直接用原版粒子。
 */
public final class ModParticles {
    public static final SimpleParticleType QUESTION =
        Registry.register(Registries.PARTICLE_TYPE, ModContent.id("question"),
            FabricParticleTypes.simple(true));

    public static final SimpleParticleType DIAMOND_SPARKLE =
        Registry.register(Registries.PARTICLE_TYPE, ModContent.id("diamond_sparkle"),
            FabricParticleTypes.simple(true));

    private ModParticles() {
    }
}
