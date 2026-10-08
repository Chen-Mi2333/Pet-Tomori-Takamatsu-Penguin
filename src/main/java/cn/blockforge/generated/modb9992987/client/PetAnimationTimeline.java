package cn.blockforge.generated.modb9992987.client;

/** 每只企鹅独立的动画时钟；只在提取渲染状态时推进，重复绘制不改变进度。 */
public final class PetAnimationTimeline {
    public record Sample(int expression, float elapsed, float expressionWeight,
                         float carryWeight, float sniffWeight) {}

    private float lastAge = Float.NaN;
    private float expressionStart;
    private int lastTicks;
    private int lastExpression = -1;
    private int activeExpression;
    private float expressionWeight;
    private float carryWeight;
    private float sniffWeight;

    public Sample sample(float age, int expression, int ticks, int carryMode, int sniffTicks) {
        if (!Float.isFinite(age)) age = 0;
        boolean first = !Float.isFinite(lastAge) || age < lastAge;
        float delta = first ? 0 : age - lastAge;
        if (first) {
            expressionWeight = 0;
            carryWeight = carryMode > 0 ? 1 : 0;
            sniffWeight = sniffTicks > 0 ? 1 : 0;
            lastTicks = 0;
            lastExpression = -1;
        }
        boolean on = ticks > 0;
        if (on && (lastTicks <= 0 || expression != lastExpression || ticks > lastTicks)) {
            activeExpression = expression;
            expressionStart = age;
            // 换动作重新淡入，不把上一表情的满权重带到新动作第零帧。
            expressionWeight = 0;
        }
        expressionWeight = approach(expressionWeight, on, delta, on ? 0.45F : 0.25F);
        carryWeight = approach(carryWeight, carryMode > 0, delta, 0.25F);
        sniffWeight = approach(sniffWeight, sniffTicks > 0, delta, 0.30F);
        lastAge = age;
        lastTicks = ticks;
        lastExpression = expression;
        return new Sample(activeExpression, Math.max(0, age - expressionStart),
            expressionWeight, carryWeight, sniffWeight);
    }

    private static float approach(float current, boolean on, float delta, float rate) {
        float target = on ? 1 : 0;
        return target + (current - target) * (float) Math.exp(-delta * rate);
    }
}
