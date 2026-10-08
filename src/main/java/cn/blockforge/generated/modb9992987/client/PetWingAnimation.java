package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.PetExpression;
import java.util.List;
import java.util.Map;

/** 恢复肩部修补前的动作参数；接缝由蒙皮处理，不再为了补缝改动作。 */
final class PetWingAnimation {
    private static final float WING_BASE_OUT = 4.0F;
    private static final float WING_MIN_OUT = 2.5F;
    private static final float WING_MAX_INWARD = 6.0F;
    private static final float[] WING_GAINS = {0.30F, 0.22F, 0.16F, 0.12F};
    private static final float[] WING_FOLLOW = {0.12F, 0.10F, 0.08F, 0.06F};

    static void apply(Map<String, float[]> pose, PetExpression expression, float elapsed, float weight) {
        applyWingPose(pose);
        if (weight > 0.001F) applyVideoWingPose(pose, expression, elapsed, weight);
    }

    private static void applyWingPose(Map<String, float[]> currentPose) {
        for (String side : List.of("l", "r")) {
            float sign = "l".equals(side) ? 1.0F : -1.0F;
            String[] chain = {
                "wing_" + side,
                "wing_" + side + "_upper",
                "wing_" + side + "_mid",
                "wing_" + side + "_fore",
                "wing_" + side + "_hand"
            };
            float[] shoulder = currentPose.get(chain[0]);
            float shX = shoulder[0];
            float shY = shoulder[1];
            float spread = shoulder[2] * sign;
            spread = Math.max(spread, -WING_MAX_INWARD);
            spread = Math.max(WING_MIN_OUT, spread + WING_BASE_OUT);
            float outRz = -spread * sign;
            shoulder[2] = outRz;
            for (int i = 1; i < chain.length; i++) {
                float[] bone = currentPose.get(chain[i]);
                bone[0] += shX * WING_GAINS[i - 1];
                bone[1] += shY * WING_GAINS[i - 1];
                bone[2] += outRz * WING_FOLLOW[i - 1];
            }
        }
    }

    private static void applyVideoWingPose(Map<String, float[]> currentPose,
                                           PetExpression expression, float elapsed, float weight) {
        float pulse = (float) Math.abs(Math.sin(elapsed * 0.24F * 0.82F));
        float wave = (float) Math.sin(elapsed * 0.24F);
        switch (expression) {
            case GREET -> poseBothWings(currentPose,
                new float[] {10, 18, 12, 6, 4},
                new float[] {45 + pulse * 8, 50, 25, 12, 8}, weight);
            case HUNGRY -> poseBothWings(currentPose,
                new float[] {26, 38, 26, 15, 8},
                new float[] {14, 18, 6, 4, 3}, weight);
            case CLINGY -> poseBothWings(currentPose,
                new float[] {30, 42, 28, 16, 9},
                new float[] {18, 24, 8, 5, 4}, weight);
            case CRY -> poseBothWings(currentPose,
                new float[] {24, 38, 26, 15, 8},
                new float[] {7, 14, -12, -15, -11}, weight);
            case ANGRY -> poseBothWings(currentPose,
                new float[] {0, -8, -4, 0, 0},
                new float[] {48 + pulse * 6, 16, 8, 6, 5}, weight);
            case SMILE -> poseBothWings(currentPose,
                new float[] {6, 10, 7, 4, 2},
                new float[] {22 + pulse * 8, 26, 12, 7, 4}, weight);
            case CONFUSED -> {
                poseOneWing(currentPose, "l", new float[] {8, 12, 8, 4, 2},
                    new float[] {22, 35, 18, 8, 5}, weight);
                poseOneWing(currentPose, "r", new float[] {0, 0, 0, 0, 0},
                    new float[] {6, 6, 5, 4, 3}, weight);
            }
            case SMUG -> {
                poseOneWing(currentPose, "l", new float[] {28, 48, 32, 17, 10},
                    new float[] {20, 40, 14, -9, -13}, weight);
                poseOneWing(currentPose, "r", new float[] {0, 0, 0, 0, 0},
                    new float[] {8, 8, 6, 4, 3}, weight);
            }
            case PICKUP -> { }
        }
        for (String side : List.of("l", "r")) {
            currentPose.get("wing_" + side + "_fore")[0] += wave * 2.0F * weight;
            currentPose.get("wing_" + side + "_hand")[2] += wave * 2.5F * weight;
        }
    }

    private static void poseBothWings(Map<String, float[]> currentPose,
                                      float[] sweep, float[] lift, float weight) {
        poseOneWing(currentPose, "l", sweep, lift, weight);
        poseOneWing(currentPose, "r", sweep, lift, weight);
    }

    private static void poseOneWing(Map<String, float[]> currentPose, String side,
                                    float[] sweep, float[] lift, float weight) {
        String[] suffixes = {"", "_upper", "_mid", "_fore", "_hand"};
        float sign = "l".equals(side) ? -1.0F : 1.0F;
        for (int i = 0; i < suffixes.length; i++) {
            float[] bone = currentPose.get("wing_" + side + suffixes[i]);
            bone[0] += -sweep[i] * weight;
            bone[2] += sign * lift[i] * weight;
        }
    }

    private PetWingAnimation() {}
}
