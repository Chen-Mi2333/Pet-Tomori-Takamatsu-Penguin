package cn.blockforge.generated.modb9992987;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * 注册表集中地：实体、生成蛋、音效事件。全部用 Identifier + Registry.register。
 */
public final class ModContent {
    public static final String MOD_ID = GeneratedMod.MOD_ID;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // ---------------- 实体 ----------------
    public static final ResourceKey<EntityType<?>> PENGUIN_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, id("penguin_pet"));

    public static final EntityType<PenguinPetEntity> PENGUIN = Registry.register(
        Registries.ENTITY_TYPE,
        PENGUIN_KEY,
        EntityType.Builder.of(PenguinPetEntity::new, MobCategory.CREATURE)
            // 1:1 网格高 22px = 1.375 格，碰撞箱取 1.4 格让脚正好落地
            .sized(0.7F, 1.4F)
            .clientTrackingRange(80)
            .updateInterval(3)
            .build(PENGUIN_KEY)
    );

    // ---------------- 物品 ----------------
    public static final ResourceKey<Item> PENGUIN_SPAWN_EGG_KEY =
        ResourceKey.create(Registries.ITEM, id("penguin_spawn_egg"));

    public static final Item PENGUIN_SPAWN_EGG = Registry.register(
        Registries.ITEM,
        PENGUIN_SPAWN_EGG_KEY,
        new SpawnEggItem(new Item.Properties()
            .setId(PENGUIN_SPAWN_EGG_KEY)
            .spawnEgg(PENGUIN))
    );

    public static final ResourceKey<Item> BLOCK_SLINGSHOT_KEY =
        ResourceKey.create(Registries.ITEM, id("block_slingshot"));

    public static final Item BLOCK_SLINGSHOT = Registry.register(
        Registries.ITEM,
        BLOCK_SLINGSHOT_KEY,
        new BlockSlingshotItem(new Item.Properties()
            .setId(BLOCK_SLINGSHOT_KEY)
            .stacksTo(1))
    );

    public static final ResourceKey<EntityType<?>> THROWN_BLOCK_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, id("thrown_block"));

    public static final EntityType<ThrownBlockEntity> THROWN_BLOCK = Registry.register(
        Registries.ENTITY_TYPE,
        THROWN_BLOCK_KEY,
        EntityType.Builder.of(ThrownBlockEntity::new, MobCategory.MISC)
            .sized(0.35F, 0.35F)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build(THROWN_BLOCK_KEY)
    );

    // ---------------- 创造模式标签页 ----------------
    /** 模组自己的创造页签：保证刷怪蛋一定能被找到。 */
    public static final ResourceKey<CreativeModeTab> PET_GROUP_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

    public static final CreativeModeTab PET_GROUP = Registry.register(
        Registries.CREATIVE_MODE_TAB,
        PET_GROUP_KEY,
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.mod_b9992987.main"))
            .icon(() -> new ItemStack(PENGUIN_SPAWN_EGG))
            .displayItems((parameters, output) -> {
                output.accept(PENGUIN_SPAWN_EGG);
                output.accept(BLOCK_SLINGSHOT);
            })
            .build());

    /** 同时把刷怪蛋塞进原版“刷怪蛋”页签，两边都能找到。 */
    private static final ResourceKey<CreativeModeTab> VANILLA_SPAWN_EGGS_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));

    // ---------------- 音效事件（对应 assets/.../sounds.json） ----------------
    public static final SoundEvent SOUND_IDLE = sound("entity.penguin_pet.idle");
    public static final SoundEvent SOUND_HAPPY = sound("entity.penguin_pet.happy");
    public static final SoundEvent SOUND_ANGRY = sound("entity.penguin_pet.angry");
    public static final SoundEvent SOUND_EXCITED = sound("entity.penguin_pet.excited");
    public static final SoundEvent SOUND_SNIFF = sound("entity.penguin_pet.sniff");
    public static final SoundEvent SOUND_PLAY = sound("entity.penguin_pet.play");
    public static final SoundEvent SOUND_CARRY = sound("entity.penguin_pet.carry");
    public static final SoundEvent SOUND_HURT = sound("entity.penguin_pet.hurt");
    public static final SoundEvent SOUND_QUESTION = sound("entity.penguin_pet.question");
    public static final SoundEvent SOUND_BOSS_THEME = sound("entity.penguin_pet.boss_theme");

    private static SoundEvent sound(String path) {
        Identifier id = id(path);
        return Registry.register(
            Registries.SOUND_EVENT,
            ResourceKey.create(Registries.SOUND_EVENT, id),
            SoundEvent.createVariableRangeEvent(id)
        );
    }

    /** 表情音效种类 -> SoundEvent 的映射（情境表情与声音事件一一对应，不变调）。 */
    public static SoundEvent resolve(PetExpression.PetSfx sfx) {
        return switch (sfx) {
            case IDLE -> SOUND_IDLE;
            case HAPPY -> SOUND_HAPPY;
            case ANGRY -> SOUND_ANGRY;
            case EXCITED -> SOUND_EXCITED;
            case SNIFF -> SOUND_SNIFF;
            case PLAY -> SOUND_PLAY;
            case CARRY -> SOUND_CARRY;
            case HURT -> SOUND_HURT;
            case QUESTION -> SOUND_QUESTION;
            case NONE -> null;
        };
    }

    public static void bootstrap() {
        FabricDefaultAttributeRegistry.register(PENGUIN, PenguinPetEntity.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(VANILLA_SPAWN_EGGS_KEY)
            .register(entries -> entries.accept(PENGUIN_SPAWN_EGG));
    }

    private ModContent() {
    }
}
