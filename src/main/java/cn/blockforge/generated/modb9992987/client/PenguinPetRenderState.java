package cn.blockforge.generated.modb9992987.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** 渲染状态：把服务端同步的表情/携带/嗅探数据带给模型。 */
public class PenguinPetRenderState extends LivingEntityRenderState {
    public int expressionId;
    public int expressionTicks;
    public int animatedExpressionId;
    public float expressionElapsed;
    public float expressionWeight;
    public float carryWeight;
    public float sniffWeight;
    public boolean carriedByCamera;
    public int carryMode;
    public int sniffTicks;
    public int intimacy;
    public boolean tamed;
    public boolean staying;
    public boolean bossMode;
    /** 独立于原版 limbAnimator 的连续步态相位，避免跟随插值时动作停住。 */
    public float gaitTime;
    public float gaitAmount;
    public float age;
    public float relativeHeadYaw;
    public float pitch;
    public float limbSwingAmplitude;
    public float limbSwingAnimationProgress;
}