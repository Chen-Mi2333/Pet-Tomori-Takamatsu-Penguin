package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.PenguinPetEntity;
import cn.blockforge.generated.modb9992987.PetExpression;
import cn.blockforge.generated.modb9992987.PetPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** 表情轮盘，使用 26.1.2 的 GUI extraction API。 */
public class ExpressionWheelScreen extends Screen {
    private static final int COUNT = PetExpression.WHEEL.length;
    private static final int ATLAS_COLS = 3;
    private static final int ATLAS_ROWS = 3;
    private static final Identifier RING = id("textures/gui/wheel_ring.png");
    private static final Identifier HOVER = id("textures/gui/wheel_hover.png");
    private static final float BAND_INNER = 112.0F / 256.0F;
    private static final float BAND_OUTER = 244.0F / 256.0F;
    private static final float LABEL_RADIUS = 0.69F;

    private final int petId;
    private int hovered = -1;

    public ExpressionWheelScreen(int petId) {
        super(Component.translatable("screen.mod_b9992987.wheel"));
        this.petId = petId;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mod_b9992987", path);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        extractBackground(context, mouseX, mouseY, tickDelta);
        int cx = width / 2;
        int cy = height / 2;
        int half = wheelSize() / 2;
        int x1 = cx - half;
        int y1 = cy - half;
        int size = half * 2;
        hovered = slotAt(mouseX, mouseY);

        context.blit(RING, x1, y1, size, size, 0.0F, 1.0F, 0.0F, 1.0F);
        if (hovered >= 0) {
            context.blit(HOVER, x1, y1, size, size, cellU0(hovered, ATLAS_COLS),
                cellU1(hovered, ATLAS_COLS), cellV0(hovered, ATLAS_COLS, ATLAS_ROWS),
                cellV1(hovered, ATLAS_COLS, ATLAS_ROWS));
        }

        for (int i = 0; i < COUNT; i++) {
            double angle = Math.toRadians(i * 360.0 / COUNT - 90.0 + 180.0 / COUNT);
            PetExpression expression = PetExpression.WHEEL[i];
            int lx = (int) (cx + Math.cos(angle) * half * LABEL_RADIUS);
            int ly = (int) (cy + Math.sin(angle) * half * LABEL_RADIUS);
            context.centeredText(font, Component.translatable(expression.translationKey()), lx, ly - 6,
                i == hovered ? 0xFFFFE3A0 : 0xFFF0F0F0);
            Component sound = Component.literal("[").append(Component.translatable(expression.soundTranslationKey())).append("]");
            context.centeredText(font, sound, lx, ly + 7, 0xFFFFE36B);
        }

        PenguinPetEntity pet = findPet();
        int centerTop = cy - 13;
        Component lockText = pet != null && pet.isStaying()
            ? Component.translatable("screen.mod_b9992987.wheel.locked_on")
            : Component.translatable("screen.mod_b9992987.wheel.locked");
        context.centeredText(font, lockText, cx, centerTop, 0xFFE7E9EF);
        context.centeredText(font, pet != null
            ? Component.translatable("tooltip.mod_b9992987.wheel.mood", Component.translatable(pet.mood().translationKey()), pet.intimacy())
            : Component.translatable("tooltip.mod_b9992987.wheel.hint"), cx, centerTop + 14,
            pet != null ? 0xFF9EC8FF : 0xFFBFC4CC);
        context.centeredText(font, title, cx, 16, 0xFFFFFFFF);
        context.centeredText(font, Component.translatable("tooltip.mod_b9992987.wheel.close"), cx, y1 + size + 10, 0xFFAAAAAA);
        super.extractRenderState(context, mouseX, mouseY, tickDelta);
    }

    private int wheelSize() {
        int base = (int) (Math.min(width, height) * 0.82F);
        return Math.max(200, Math.min(430, base)) & ~1;
    }

    private static float cellU0(int cell, int cols) { return (cell % cols) / (float) cols; }
    private static float cellU1(int cell, int cols) { return ((cell % cols) + 1) / (float) cols; }
    private static float cellV0(int cell, int cols, int rows) { return (cell / cols) / (float) rows; }
    private static float cellV1(int cell, int cols, int rows) { return (cell / cols + 1) / (float) rows; }

    private PenguinPetEntity findPet() {
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getEntity(petId) instanceof PenguinPetEntity pet && !pet.isRemoved() ? pet : null;
    }

    private int slotAt(double mx, double my) {
        int cx = width / 2;
        int cy = height / 2;
        int half = wheelSize() / 2;
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < half * BAND_INNER || dist > half * BAND_OUTER) return -1;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0) angle += 360.0;
        return (int) Math.floor(angle * COUNT / 360.0) % COUNT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            int index = slotAt(click.x(), click.y());
            if (index >= 0) {
                ClientPlayNetworking.send(new PetPackets.SelectExpressionPayload(petId, index));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256 || input.key() == 257) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }
}
