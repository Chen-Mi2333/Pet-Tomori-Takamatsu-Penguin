package cn.blockforge.generated.modb9992987;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 表情定义：每个音效只保留一个动作，共 9 个轮盘动作。
 *
 * <p>动作姿态由客户端模型按视频编排；这里保存平滑过渡所需的基础目标、
 * 粒子和音效。顺序同时决定轮盘的 9 个扇区顺序。</p>
 */
public enum PetExpression {
    // 名称, 头俯仰, 头偏航, 头翻滚, 翼抬, 翼前伸, 尾俯仰, 发束摇摆, 身跳, 眯眼, 粒子, 音效
    SMILE("smile", 0, 0, 0, 16, 3, -6, 0, 0.0F, 0.15F, PetFx.HEART, PetSfx.HAPPY),
    SMUG("smug", -4, 0, 6, 20, 20, -8, 6, 0.0F, 0.30F, PetFx.NOTE, PetSfx.PLAY),
    HUNGRY("hungry", -8, 0, 0, 18, 20, -8, 0, 0.1F, 0.0F, PetFx.NOTE, PetSfx.SNIFF),
    GREET("greet", -10, 0, 0, 34, 14, -10, -4, 0.3F, 0.0F, PetFx.SPARKLE, PetSfx.EXCITED),
    CRY("cry", 20, -4, -8, -6, 26, 8, -10, 0.2F, 0.9F, PetFx.NONE, PetSfx.HURT),
    CONFUSED("confused", 4, -6, 0, 10, 2, 4, -6, 0.0F, 0.15F, PetFx.QUESTION, PetSfx.QUESTION),
    ANGRY("angry", 8, 0, 0, 22, 4, 12, -16, 0.0F, 0.0F, PetFx.ANGRY, PetSfx.ANGRY),
    CLINGY("clingy", 6, 12, 10, 20, 22, -4, 8, 0.1F, 0.4F, PetFx.HEART, PetSfx.CARRY),
    PICKUP("pickup", 30, 0, 0, 6, 55, -6, 2, 0.0F, 0.2F, PetFx.SPARKLE, PetSfx.IDLE);

    /** 粒子种类。 */
    public enum PetFx {
        NONE, HEART, ANGRY, QUESTION, SPARKLE, NOTE
    }

    /** 音效种类，映射到 sounds.json 里的事件。 */
    public enum PetSfx {
        NONE, IDLE, HAPPY, ANGRY, EXCITED, SNIFF, PLAY, CARRY, HURT, QUESTION
    }

    private final String id;
    public final float headPitch;
    public final float headYaw;
    public final float headRoll;
    public final float wingRoll;
    public final float wingPitch;
    public final float tailPitch;
    public final float tuftRoll;
    public final float bodyBob;
    public final float eyeSquint;
    public final PetFx fx;
    public final PetSfx sfx;

    PetExpression(String id, float headPitch, float headYaw, float headRoll, float wingRoll, float wingPitch,
                  float tailPitch, float tuftRoll, float bodyBob, float eyeSquint, PetFx fx, PetSfx sfx) {
        this.id = id;
        this.headPitch = headPitch;
        this.headYaw = headYaw;
        this.headRoll = headRoll;
        this.wingRoll = wingRoll;
        this.wingPitch = wingPitch;
        this.tailPitch = tailPitch;
        this.tuftRoll = tuftRoll;
        this.bodyBob = bodyBob;
        this.eyeSquint = eyeSquint;
        this.fx = fx;
        this.sfx = sfx;
    }

    public String id() {
        return id;
    }

    /** 轮盘与提示里显示的翻译键，如 expression.mod_b9992987.smile。 */
    public String translationKey() {
        return "expression.mod_b9992987." + id;
    }

    /** 轮盘第二行显示的音效标签。 */
    public String soundTranslationKey() {
        return "expression.mod_b9992987." + id + ".sound";
    }

    /** 存档和实体状态使用完整枚举，保证旧世界里的状态仍能正常读取。 */
    public static final PetExpression[] ALL = values();

    /** 表情轮盘只展示情绪动作，不再把捡石头放进轮盘。 */
    public static final PetExpression[] WHEEL = {
        SMILE, SMUG, HUNGRY, GREET, CRY, CONFUSED, ANGRY, CLINGY
    };

    public static PetExpression byIndex(int index) {
        if (index < 0 || index >= ALL.length) {
            return SMILE;
        }
        return ALL[index];
    }

    public static PetExpression byWheelIndex(int index) {
        if (index < 0 || index >= WHEEL.length) {
            return SMILE;
        }
        return WHEEL[index];
    }

    public static int indexOf(PetExpression e) {
        return e == null ? 0 : e.ordinal();
    }

    /** 把表情对应的粒子效果转成原版粒子类型；自定义粒子在 ModParticles 里注册。 */
    public ParticleOptions particleEffect() {
        return switch (fx) {
            case HEART -> ParticleTypes.HEART;
            case ANGRY -> ParticleTypes.ANGRY_VILLAGER;
            case SPARKLE -> ModParticles.DIAMOND_SPARKLE;
            case QUESTION -> ModParticles.QUESTION;
            case NOTE -> ParticleTypes.NOTE;
            case NONE -> null;
        };
    }

    /** 低配模式只保留疑问和闪光粒子。 */
    public boolean keepInLowSpec() {
        return fx == PetFx.QUESTION || fx == PetFx.SPARKLE;
    }
}
