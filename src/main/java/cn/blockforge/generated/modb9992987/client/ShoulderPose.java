package cn.blockforge.generated.modb9992987.client;

import net.minecraft.util.Mth;

/** 肩乘和玩家模型使用同一对身体角度，不能混入本地玩家的视线角度。 */
final class ShoulderPose {
    static float bodyYaw(float previousBodyYaw, float currentBodyYaw, float tickDelta) {
        return Mth.rotLerp(tickDelta, previousBodyYaw, currentBodyYaw);
    }

    private ShoulderPose() {}
}
