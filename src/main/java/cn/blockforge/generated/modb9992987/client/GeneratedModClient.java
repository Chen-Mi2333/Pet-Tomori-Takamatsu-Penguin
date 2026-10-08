package cn.blockforge.generated.modb9992987.client;

import cn.blockforge.generated.modb9992987.GeneratedMod;
import cn.blockforge.generated.modb9992987.ModContent;
import cn.blockforge.generated.modb9992987.ModParticles;
import cn.blockforge.generated.modb9992987.PenguinPetEntity;
import cn.blockforge.generated.modb9992987.PetPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.NoteParticle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** 客户端入口：渲染、粒子、键位（K 表情轮盘 / J 找钻石 / G 背负切换）、启动自查。 */
public final class GeneratedModClient implements ClientModInitializer {
    public static KeyMapping wheelKey;
    public static KeyMapping sniffKey;
    public static KeyMapping carryKey;
    public static KeyMapping stayKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModContent.PENGUIN, PenguinPetRenderer::new);
        EntityRendererRegistry.register(ModContent.THROWN_BLOCK, ThrownBlockRenderer::new);

        ParticleProviderRegistry.getInstance().register(ModParticles.QUESTION,
            sprites -> new NoteParticle.Provider(sprites));
        ParticleProviderRegistry.getInstance().register(ModParticles.DIAMOND_SPARKLE,
            sprites -> new EndRodParticle.Provider(sprites));

        var category = KeyMapping.Category.register(ModContent.id("category"));
        wheelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mod_b9992987.wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        sniffKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mod_b9992987.sniff", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        carryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mod_b9992987.carry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
        stayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mod_b9992987.stay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 驱动用户上传的弹弓 bbmodel 连续拉弦/回弹动画；此前漏接这一行，
            // 即使特殊模型成功加载，拉弦进度也始终停在 0。
            SlingshotSpecialRenderer.clientTick(client);
            while (wheelKey.consumeClick()) {
                openWheel(client);
            }
            while (sniffKey.consumeClick()) {
                commandNearestPet(client, 0);
            }
            while (carryKey.consumeClick()) {
                commandNearestPet(client, 1);
            }
            while (stayKey.consumeClick()) {
                commandNearestPet(client, 2);
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelSelfCheck.run(client));

        GeneratedMod.LOGGER.info("client init done: renderer, wheel key = K, sniff key = J, carry key = G");
    }

    /** J/G 键：对最近的宠物企鹅下指令（0=嗅钻，1=背负循环），走网络包让服务端执行。 */
    private static void commandNearestPet(Minecraft client, int command) {
        if (client.level == null || client.player == null) {
            return;
        }
        PenguinPetEntity pet = findPet(client);
        if (pet == null) {
            client.player.sendMessage(Component.translatable("message.mod_b9992987.no_pet_nearby"), true);
            return;
        }
        ClientPlayNetworking.send(new PetPackets.PetCommandPayload(pet.getId(), command));
    }

    /** 优先玩家准星指向的宠物，其次 6 格内自己的宠物。 */
    private static void openWheel(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        PenguinPetEntity pet = findPet(client);
        if (pet == null) {
            client.player.sendMessage(Component.translatable("message.mod_b9992987.no_pet_nearby"), true);
            return;
        }
        client.setScreen(new ExpressionWheelScreen(pet.getId()));
    }

    private static PenguinPetEntity findPet(Minecraft client) {
        // 1) 准星 8 格内
        Vec3 eye = client.player.getEyePosition();
        Vec3 look = client.player.getViewVector(1.0F);
        AABB cursorAABB = new AABB(eye, eye.add(look.multiply(8.0, 8.0, 8.0)));
        List<PenguinPetEntity> targets = client.level.getEntitiesOfClass(PenguinPetEntity.class,
            cursorAABB, e -> !e.isRemoved());
        PenguinPetEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PenguinPetEntity e : targets) {
            double dist = e.distanceToSqr(eye);
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        if (best != null) {
            return best;
        }
        // 2) 6 格内最近的（自己的宠物优先）
        AABB near = client.player.getBoundingBox().inflate(6.0, 6.0, 6.0);
        List<PenguinPetEntity> pets = client.level.getEntitiesOfClass(PenguinPetEntity.class, near,
            e -> !e.isRemoved());
        for (PenguinPetEntity e : pets) {
            double dist = e.distanceToSqr(client.player.getEyePosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }
}