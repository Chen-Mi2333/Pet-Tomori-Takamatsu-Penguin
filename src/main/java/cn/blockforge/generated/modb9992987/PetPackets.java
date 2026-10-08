package cn.blockforge.generated.modb9992987;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 自定义网络包：客户端轮盘选择表情 -> 服务端执行。
 * 其余同步全部走实体 SynchedEntityData，不需要额外包。
 */
public final class PetPackets {

    /** payload 本体：entityId + expressionIndex。 */
    public record SelectExpressionPayload(int entityId, int expressionIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SelectExpressionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModContent.MOD_ID, "select_expression"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectExpressionPayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SelectExpressionPayload::entityId,
                ByteBufCodecs.VAR_INT, SelectExpressionPayload::expressionIndex,
                SelectExpressionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 指令包：0 = 嗅探找钻石，1 = 肩乘切换（肩上 -> 下来）。 */
    public record PetCommandPayload(int entityId, int command) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PetCommandPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModContent.MOD_ID, "pet_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetCommandPayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PetCommandPayload::entityId,
                ByteBufCodecs.VAR_INT, PetCommandPayload::command,
                PetCommandPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 通用侧注册：payload 类型 + 服务端接收器（接收器只在服务端有作用）。 */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SelectExpressionPayload.TYPE, SelectExpressionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectExpressionPayload.TYPE, (payload, context) -> {
            int entityId = payload.entityId();
            int expressionIndex = payload.expressionIndex();
            context.server().execute(() -> {
                ServerLevel level = context.player().serverLevel();
                Entity target = level.getEntity(entityId);
                if (target instanceof PenguinPetEntity pet && canCommand(pet, context.player())) {
                    pet.requestExpression(PetExpression.byWheelIndex(expressionIndex));
                }
            });
        });

        PayloadTypeRegistry.serverboundPlay().register(PetCommandPayload.TYPE, PetCommandPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PetCommandPayload.TYPE, (payload, context) -> {
            int entityId = payload.entityId();
            int command = payload.command();
            context.server().execute(() -> {
                ServerLevel level = context.player().serverLevel();
                Entity target = level.getEntity(entityId);
                if (target instanceof PenguinPetEntity pet && canCommand(pet, context.player())) {
                    if (command == 0) {
                        pet.requestSniff();
                    } else if (command == 1) {
                        pet.requestCarryToggle(context.player());
                    } else if (command == 2) {
                        pet.requestStayToggle(context.player());
                    }
                }
            });
        });
    }

    /** 只有“对的企鹅”才生效：本模组的宠物实体；主人或 8 格内看着它的玩家可指挥。 */
    private static boolean canCommand(PenguinPetEntity pet, Player player) {
        return pet.isOwner(player) || pet.distanceTo(player) <= 8.0F;
    }

    private PetPackets() {
    }
}
