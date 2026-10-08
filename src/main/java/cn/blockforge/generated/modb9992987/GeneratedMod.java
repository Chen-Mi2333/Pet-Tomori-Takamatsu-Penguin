package cn.blockforge.generated.modb9992987;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 咕咕嘎嘎凑企鹅高松灯主入口。 */
public final class GeneratedMod implements ModInitializer {
    public static final String MOD_ID = "mod_b9992987";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContent.bootstrap();
        PetConfig.load();
        PetPackets.register();
        registerCommands();
        LOGGER.info("{} initialized: 9 expressions, 4 moods, carry, diamond sniff and stone pickup enabled", MOD_ID);
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("petconfig")
                .then(Commands.literal("particles")
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("full");
                            builder.suggest("low");
                            builder.suggest("off");
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PetConfig.Mode mode = PetConfig.Mode.parse(
                                StringArgumentType.getString(context, "mode"));
                            PetConfig.setParticleMode(mode);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("message.mod_b9992987.config_particles", mode.key()), true);
                            return 1;
                        })))
                .then(Commands.literal("status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.translatable("message.mod_b9992987.config_status",
                                PetConfig.particleMode().key()), false);
                        return 1;
                    }))));
    }
}
