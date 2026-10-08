package cn.blockforge.generated.modb9992987;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 弹弓：副手或快捷栏里有方块时，右键把那块方块射出去。
 * 打中 Boss 企鹅丢出的大石头会把它弹回去。
 */
public class BlockSlingshotItem extends Item {
    public BlockSlingshotItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack ammo = findAmmo(player, hand);
        if (ammo.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.slingshot_no_ammo"));
            return InteractionResult.FAIL;
        }
        // 开始拉弓时在快捷栏上方明确显示将要发射的方块名称与现有数量。
        // 弹药优先取另一只手，其次按物品栏顺序寻找第一组方块。
        player.sendOverlayMessage(Component.translatable("message.mod_b9992987.slingshot_loaded",
            ammo.getHoverName(), ammo.getCount()));
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack sling, Level world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof Player player)) {
            return false;
        }
        ItemStack ammo = findAmmo(player, player.getUsedItemHand());
        int usedTicks = getUseDuration(sling, user) - remainingUseTicks;
        float pull = BowItem.getPowerForTime(usedTicks);
        // 至少拉 3 tick 才发射，防止点一下就误射；拉得越满，方块飞得越快。
        if (ammo.isEmpty() || usedTicks < 3) {
            return false;
        }
        if (!world.isClientSide()) {
            ThrownBlockEntity shot = new ThrownBlockEntity(world, player, ammo.copyWithCount(1));
            shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                0.85F + pull * 1.15F, 0.8F);
            ((net.minecraft.server.level.ServerLevel) world).addFreshEntity(shot);
            world.playSound(null, player, SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, 0.75F + pull * 0.2F);
            if (!player.isCreative()) {
                ammo.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private static ItemStack findAmmo(Player player, InteractionHand usedHand) {
        InteractionHand other = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack off = player.getItemInHand(other);
        if (isBlockAmmo(off)) {
            return off;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isBlockAmmo(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isBlockAmmo(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.AIR)) {
            return false;
        }
        return stack.getItem() instanceof BlockItem;
    }
}
