package cn.blockforge.generated.modb9992987;

/**
 * 情绪成长值：亲密度 0~100，分四档。待机与互动时按档位挑选表情。
 */
public enum PetMood {
    /** 生气：亲密度很低，待机偏烦躁。 */
    ANGRY("angry", 0, 19),
    /** 普通：默认档。 */
    NORMAL("normal", 20, 59),
    /** 开心：经常互动后进入。 */
    HAPPY("happy", 60, 84),
    /** 兴奋：满级附近，待机也会手舞足蹈。 */
    EXCITED("excited", 85, 100);

    private final String id;
    private final int min;
    private final int max;

    PetMood(String id, int min, int max) {
        this.id = id;
        this.min = min;
        this.max = max;
    }

    public String id() {
        return id;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public String translationKey() {
        return "mood.mod_b9992987." + id;
    }

    public static PetMood of(int intimacy) {
        for (PetMood m : values()) {
            if (intimacy >= m.min && intimacy <= m.max) {
                return m;
            }
        }
        return intimacy < 0 ? ANGRY : EXCITED;
    }

    /** 该档待机时随机播放的表情池。 */
    public PetExpression[] idlePool() {
        return switch (this) {
            case ANGRY -> new PetExpression[]{PetExpression.ANGRY, PetExpression.CONFUSED};
            case NORMAL -> new PetExpression[]{PetExpression.SMILE, PetExpression.CONFUSED, PetExpression.HUNGRY};
            case HAPPY -> new PetExpression[]{PetExpression.SMILE, PetExpression.CLINGY, PetExpression.GREET};
            case EXCITED -> new PetExpression[]{PetExpression.GREET, PetExpression.SMUG, PetExpression.HUNGRY};
        };
    }
}