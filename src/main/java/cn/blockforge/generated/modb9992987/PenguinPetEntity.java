package cn.blockforge.generated.modb9992987;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import java.util.UUID;

/**
 * 咕咕嘎嘎凑企鹅高松灯：表情 / 情绪成长 / 肩背携带 / 钻石嗅探 / 捡石头。
 *
 * <p>同步方式：表情、亲密度、携带模式、嗅探计时全部走 SynchedEntityData，
 * 音效与粒子由服务端直接广播，客户端无需自定义包（轮盘选择除外）。
 */
public class PenguinPetEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> EXPRESSION =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EXPRESSION_TICKS =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INTIMACY =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);
    /** 0=地上跟随，1=左肩。 */
    private static final EntityDataAccessor<Integer> CARRY_MODE =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SNIFF_TICKS =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> SNIFF_TARGET =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> STAYING =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOSS_MODE =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(PenguinPetEntity.class, EntityDataSerializers.INT);

    public static final float BOSS_HEALTH = 114.5F;

    public static final int SNIFF_RANGE = 32;
    public static final int ALERT_RANGE = 16;
    /** 捡石头：轮盘指令时最多找 16 格内的掉落石头。 */
    public static final int STONE_SEEK_RANGE = 16;
    /** 捡石头：走到多近就算够着（格）。 */
    public static final float STONE_PICK_TOUCH = 1.3F;

    private int petCooldown;
    private int feedCount;
    private int alertCooldown;
    private int followTeleportCooldown;
    /** 表情演出期间是否冻结了移动/跳跃目标（服务端）。 */
    private boolean movementGoalsFrozen;
    /** 服务端：正在走过去捡的掉落石头。 */
    private ItemEntity stoneTarget;
    /** 服务端：捡石头寻路的剩余 ticks，超时自动放弃。 */
    private int stoneSeekTicks;
    /** 服务端：捡起一块石头后的冷却，避免贴着石头反复捡。 */
    private int stonePickCooldown;
    private static final int BOSS_REVIVE_TICKS = 100;
    private int bossThrowCooldown;
    private int bossAttackCooldown;
    private int bossHoverTicks;
    private int bossReviveTicks;
    private double bossAnchorY;
    private final ServerBossEvent bossBar = new ServerBossEvent(
        UUID.randomUUID(),
        Component.translatable("boss.mod_b9992987.penguin"),
        BossEvent.BossBarColor.WHITE,
        BossEvent.BossBarOverlay.PROGRESS);

    public PenguinPetEntity(EntityType<? extends TamableAnimal> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.155)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.add(EXPRESSION, 0);
        builder.add(EXPRESSION_TICKS, 0);
        builder.add(INTIMACY, 0);
        builder.add(CARRY_MODE, 0);
        builder.add(SNIFF_TICKS, 0);
        builder.add(SNIFF_TARGET, BlockPos.ORIGIN);
        builder.add(STAYING, false);
        builder.add(BOSS_MODE, false);
        builder.add(BOSS_PHASE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.add(0, new FloatGoal(this));
        goalSelector.add(1, new TemptGoal(this, 0.9, stack -> isFood(stack), false));
        // 两个距离是“开始跟随”和“停止跟随”，不是瞬移距离。
        // 旧值 2.4/12 导致近距离启动后下一 tick 就停止，台阶边缘不断刹车。
        goalSelector.add(2, new FollowOwnerGoal(this, 1.0, 2.4F, 1.4F) {
            @Override
            public boolean canUse() {
                return !isStaying() && !isBossMode() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isStaying() && !isBossMode() && super.canContinueToUse();
            }
        });
        // 野生企鹅会在附近自由散步；认主后关闭本目标，只按主人指令跟随或待命。
        goalSelector.add(4, new WaterAvoidingRandomStrollGoal(this, 0.72, 0.015F) {
            @Override
            public boolean canUse() {
                return !isTame() && !isStaying() && !isBossMode() && carryMode() == 0
                    && expressionTicks() == 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isTame() && !isBossMode() && carryMode() == 0 && super.canContinueToUse();
            }
        });
        goalSelector.add(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.add(6, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------
    // 表情与情绪
    // ------------------------------------------------------------------

    public PetExpression expression() {
        return PetExpression.byIndex(entityData.get(EXPRESSION));
    }

    public int expressionTicks() {
        return entityData.get(EXPRESSION_TICKS);
    }

    public int intimacy() {
        return entityData.get(INTIMACY);
    }

    public PetMood mood() {
        return PetMood.of(intimacy());
    }

    public int carryMode() {
        return entityData.get(CARRY_MODE);
    }

    public int sniffTicks() {
        return entityData.get(SNIFF_TICKS);
    }

    public boolean isStaying() {
        return entityData.get(STAYING);
    }

    public boolean isBossMode() {
        return entityData.get(BOSS_MODE);
    }

    public int bossPhase() {
        return entityData.get(BOSS_PHASE);
    }

    /** 服务端：设置表情并持续 ticks，同时播放对应音效与粒子。 */
    public void setExpression(PetExpression expression, int ticks) {
        applyExpression(expression, ticks);
    }

    /** 已经由调用方播放过音效时，只切姿态，不重复播放同一音效。 */
    private void setExpressionSilent(PetExpression expression, int ticks) {
        applyExpression(expression, ticks, false);
    }

    private void applyExpression(PetExpression expression, int ticks, boolean withFeedback) {
        if (level().isClientSide()) {
            return;
        }
        PetExpression old = expression();
        entityData.set(EXPRESSION, expression.ordinal());
        entityData.set(EXPRESSION_TICKS, ticks);
        // 有演出就冻结移动目标：企鹅站定演完，不再一边演一边被 AI 带着乱跑。
        // 水里例外，否则游泳目标也被冻结会把企鹅按在水下。
        if (ticks > 0 && !isInWater()) {
            setMovementGoalsFrozen(true);
        }
        if (withFeedback && old != expression) {
            playExpressionSound(expression);
            spawnExpressionParticles(expression);
        } else if (old != expression) {
            spawnExpressionParticles(expression);
        }
    }

    /**
     * 服务端：关闭/打开 MOVE+JUMP 目标控制。关闭时当前正在跑的跟随、讨食等
     * 移动目标会被目标选择器自动停下，表情演完再恢复。LOOK 不动，头部仍会看向玩家。
     */
    private void setMovementGoalsFrozen(boolean frozen) {
        if (movementGoalsFrozen == frozen) {
            return;
        }
        movementGoalsFrozen = frozen;
        goalSelector.setControlFlag(Goal.Flag.MOVE, !frozen);
        goalSelector.setControlFlag(Goal.Flag.JUMP, !frozen);
        if (frozen) {
            getNavigation().stop();
            Vec3 vel = getDeltaMovement();
            setDeltaMovement(vel.x * 0.1, vel.y, vel.z * 0.1);
        }
    }

    private void playExpressionSound(PetExpression expression) {
        SoundEvent event = ModContent.resolve(expression.sfx);
        if (event == null) {
            return;
        }
        // 背负时音量降低（贴耳），寻钻成功等由调用方覆盖
        float volume = carryMode() > 0 ? 0.55F : 1.0F;
        level().playSound(null, this, event, SoundSource.NEUTRAL, volume, 1.0F);
    }

    private void spawnExpressionParticles(PetExpression expression) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        var effect = expression.particleEffect();
        if (effect == null) {
            return;
        }
        int count = switch (PetConfig.particleMode()) {
            case FULL -> 6;
            case LOW -> expression.keepInLowSpec() ? 4 : 0;
            case OFF -> 0;
        };
        if (count <= 0) {
            return;
        }
        double px = getX();
        double py = getY() + 1.45;
        double pz = getZ();
        server.sendParticles(effect, px, py, pz, count, 0.25, 0.2, 0.25, 0.01);
    }

    public void addIntimacy(int delta) {
        int old = intimacy();
        int next = Math.max(0, Math.min(100, old + delta));
        if (next != old) {
            entityData.set(INTIMACY, next);
            PetMood before = PetMood.of(old);
            PetMood after = PetMood.of(next);
            if (before != after && !level().isClientSide()) {
                LivingEntity owner = getOwner();
                if (owner instanceof Player player) {
                    player.sendOverlayMessage(Component.translatable("message.mod_b9992987.mood_up",
                        Component.translatable(after.translationKey())));
                }
                setExpression(after == PetMood.ANGRY ? PetExpression.ANGRY
                    : after == PetMood.EXCITED ? PetExpression.GREET : PetExpression.SMILE, 60);
            }
        }
    }

    // ------------------------------------------------------------------
    // 互动：抚摸 / 喂食驯服 / 携带 / 嗅探指令
    // ------------------------------------------------------------------

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.isOf(Items.COD) || stack.isOf(Items.SALMON)
            || stack.isOf(Items.TROPICAL_FISH) || stack.isOf(Items.COOKED_COD)
            || stack.isOf(Items.COOKED_SALMON);
    }

    /** 石头是企鹅喜欢的礼物；石头/圆石/砂砾/深板岩等都算“石头”。 */
    private static boolean isStoneGift(ItemStack stack) {
        return stack.isOf(Items.STONE) || stack.isOf(Items.COBBLESTONE)
            || stack.isOf(Items.GRAVEL) || stack.isOf(Items.DEEPSLATE)
            || stack.isOf(Items.ANDESITE) || stack.isOf(Items.DIORITE)
            || stack.isOf(Items.GRANITE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            boolean owner = isOwnedBy(player);

            // 肩上的企鹅：空手右键 = 放下来
            if (carryMode() > 0 && owner && stack.isEmpty()) {
                dropCarry();
                return InteractionResult.SUCCESS;
            }

            if (isFood(stack) && canEat(false)) {
                InteractionResult result = super.mobInteract(player, hand);
                if (result.consumesAction()) {
                    addIntimacy(6);
                    setExpression(PetExpression.SMILE, 60);
                    if (!isTame()) {
                        feedCount++;
                        if (feedCount >= 2) {
                            tame(player);
                            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.tamed"));
                            setExpression(PetExpression.CLINGY, 80);
                        }
                    }
                }
                return result;
            }

            // 石头礼物：没认主人前给石头会加好感；已认主后也能当礼物。
            if (isStoneGift(stack) && !isBossMode()) {
                addIntimacy(8);
                setExpression(PetExpression.CLINGY, 70);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                player.sendOverlayMessage(Component.translatable("message.mod_b9992987.stone_gift"));
                return InteractionResult.SUCCESS;
            }

            if (owner && player.isCrouching() && stack.isEmpty() && !isBossMode()) {
                // 肩上空手右键已经在上方处理；地上潜行空手右键只会上左肩。
                startCarry();
                return InteractionResult.SUCCESS;
            }

            if (owner && stack.isOf(Items.STICK)) {
                startSniff();
                return InteractionResult.SUCCESS;
            }

            if (owner && stack.isEmpty()) {
                if (petCooldown <= 0) {
                    petCooldown = 20;
                    PetExpression[] pool = mood().idlePool();
                    addIntimacy(2);
                    setExpression(pool[random.nextInt(pool.length)], 60);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------
    // 左肩携带（只有地面 0 / 左肩 1，不再提供背部状态）
    // ------------------------------------------------------------------

    private void startCarry() {
        entityData.set(CARRY_MODE, 1);
        setNoGravity(true);
        getNavigation().stop();
        SoundEvent event = ModContent.resolve(PetExpression.PetSfx.CARRY);
        if (event != null) {
            level().playSound(null, this, event, SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        setExpressionSilent(PetExpression.CLINGY, 60);
    }

    private void dropCarry() {
        LivingEntity owner = getOwner();
        entityData.set(CARRY_MODE, 0);
        setNoGravity(false);
        if (owner != null) {
            Vec3 at = new Vec3(owner.getX(), owner.getY(), owner.getZ()).add(owner.getViewVector(1.0F).multiply(0.8, 0, 0.8));
            setPos(at.x, owner.getY(), at.z);
            setDeltaMovement(Vec3.ZERO);
        }
        setExpression(PetExpression.CONFUSED, 40);
    }

    /** 服务端和渲染端共用肩部锚点，位置跟身体而不是视线旋转。 */
    public static Vec3 carryPosition(LivingEntity owner, Vec3 base, float yBodyRot) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        double crouch = owner.isCrouching() ? 0.20 : 0.0;
        return base.add(left.multiply(0.40)).add(forward.multiply(0.035))
            .add(0, 1.405 - crouch, 0);
    }

    private void tickCarry() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level()
            || distanceTo(owner) > 16.0) {
            dropCarry();
            return;
        }
        Vec3 target = carryPosition(owner, new Vec3(owner.getX(), owner.getY(), owner.getZ()), owner.yBodyRot);
        setPos(target.x, target.y, target.z);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0;
        setYRot(owner.getYRot());
        setYBodyRot(owner.yBodyRot);
        yHeadRot = owner.yHeadRot;
        // 玩家落水或熔岩时自动跳下
        if (owner.isInWater() || owner.isInLava()) {
            dropCarry();
        }
    }

    // ------------------------------------------------------------------
    // 实体碰撞：背负时不推玩家（否则每 tick 把玩家挤得自动往前走）
    // ------------------------------------------------------------------

    /** 自己主动推开别人：背负时不做，玩家就不会被企鹅撞着走。 */
    @Override
    protected void doPush(Entity entity) {
        if (carryMode() > 0) {
            return;
        }
        super.doPush(entity);
    }

    /** 背负时本身也不可被推动，彻底退出“实体互挤”判定。 */
    @Override
    public boolean isPushable() {
        return carryMode() == 0 && super.isPushable();
    }

    // ------------------------------------------------------------------
    // 钻石嗅探
    // ------------------------------------------------------------------

    private void startSniff() {
        if (sniffTicks() > 0) {
            return;
        }
        entityData.set(SNIFF_TICKS, 100);
        entityData.set(SNIFF_TARGET, BlockPos.ORIGIN);
        getNavigation().stop();
        SoundEvent event = ModContent.resolve(PetExpression.PetSfx.SNIFF);
        if (event != null) {
            level().playSound(null, this, event, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        setExpressionSilent(PetExpression.HUNGRY, 30);
    }

    private void tickSniff() {
        int ticks = sniffTicks();
        if (ticks <= 0) {
            return;
        }
        entityData.set(SNIFF_TICKS, ticks - 1);
        if (ticks == 60) {
            BlockPos found = findNearestDiamond(SNIFF_RANGE);
            if (found != null) {
                entityData.set(SNIFF_TARGET, found);
                BlockPos center = found.up();
                lookControl.lookAt(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                LivingEntity owner = getOwner();
                if (owner instanceof Player player) {
                    double dist = Math.sqrt(distanceToSqr(Vec3.atCenterOf(center)));
                    player.sendOverlayMessage(Component.translatable("message.mod_b9992987.diamond_found",
                        directionName(center), (int) Math.round(dist / 4) * 4));
                }
                // 寻钻成功：原声高亮 1 次 + 方向闪光
                SoundEvent event = ModContent.resolve(PetExpression.PetSfx.EXCITED);
                if (event != null) {
                    level().playSound(null, this, event, SoundSource.PLAYERS, 1.2F, 1.0F);
                }
                setExpression(PetExpression.GREET, 80);
                spawnSparklePath(center);
            } else {
                LivingEntity owner = getOwner();
                if (owner instanceof Player player) {
                    player.sendOverlayMessage(Component.translatable("message.mod_b9992987.diamond_none"));
                }
                setExpression(PetExpression.CONFUSED, 60);
            }
        }
        BlockPos target = entityData.get(SNIFF_TARGET);
        if (!target.equals(BlockPos.ORIGIN) && ticks < 60) {
            lookControl.lookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        }
    }

    /** 低配模式下这就是“钻石方向箭头”：朝目标方向每隔 2 格放一簇闪光。 */
    private void spawnSparklePath(BlockPos target) {
        if (!(level() instanceof ServerLevel server) || PetConfig.particleMode() == PetConfig.Mode.OFF) {
            return;
        }
        Vec3 from = new Vec3(getX(), getY() + 1.2, getZ());
        Vec3 to = Vec3.atCenterOf(target);
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.001) {
            return;
        }
        dir = dir.multiply(1 / len);
        int steps = (int) Math.min(8, Math.max(1, len / 2));
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(dir.multiply(i * 2.0));
            server.sendParticles(ModParticles.DIAMOND_SPARKLE, p.x, p.y, p.z,
                PetConfig.particleMode() == PetConfig.Mode.LOW ? 2 : 4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private BlockPos findNearestDiamond(int range) {
        return BlockPos.findClosestMatch(blockPosition(), range, range / 2,
            pos -> level().isLoaded(pos)
                && level().getBlockState(pos).is(BlockTags.DIAMOND_ORES)).orElse(null);
    }

    private String directionName(BlockPos target) {
        double dx = target.getX() + 0.5 - getX();
        double dz = target.getZ() + 0.5 - getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        double rel = (angle - (90 + getYRot())) % 360;
        if (rel < -180) rel += 360;
        if (rel > 180) rel -= 360;
        double a = (rel + 360) % 360;
        if (a < 22.5 || a >= 337.5) return "前方";
        if (a < 67.5) return "右前方";
        if (a < 112.5) return "右侧";
        if (a < 157.5) return "右后方";
        if (a < 202.5) return "后方";
        if (a < 247.5) return "左后方";
        if (a < 292.5) return "左侧";
        return "左前方";
    }

    // ------------------------------------------------------------------
    // 捡石头
    // ------------------------------------------------------------------

    /**
     * 服务端：发起一次捡石头。附近有掉落的石头就走过去捡；
     * 没有就原地把“弯腰—够地—抱石起身”完整演一遍，绝不借机乱跑。
     */
    private void startStonePick() {
        if (level().isClientSide() || stoneSeekTicks > 0) {
            return;
        }
        ItemEntity stone = carryMode() == 0 && !isInWater()
            ? findNearestStoneItem(STONE_SEEK_RANGE) : null;
        if (stone == null) {
            // 附近没石头：原地演一遍。applyExpression 会冻结移动目标，演完自动恢复。
            setExpression(PetExpression.PICKUP, 80);
            return;
        }
        stoneTarget = stone;
        stoneSeekTicks = 300;
        // 冻结 AI 的移动目标（跟随、讨食先让路），寻路由 tickStoneHunt 自己带。
        setMovementGoalsFrozen(true);
        lookControl.lookAt(stone.getX(), stone.getY() + 0.25, stone.getZ());
        getNavigation().moveTo(stone, 0.9);
        playSfx(PetExpression.PetSfx.SNIFF, 1.0F);
    }

    /** 服务端：捡石头寻路主循环，mobTick 里每 tick 调用。 */
    private void tickStoneHunt() {
        if (stoneSeekTicks <= 0) {
            return;
        }
        if (carryMode() > 0 || isInWater()) {
            // 被主人抱上肩/下水了：这趟不捡了。
            abandonStoneHunt();
            return;
        }
        // 途中被受伤等事件解冻过，这里重新接管，避免跟随目标与石头抢路。
        setMovementGoalsFrozen(true);
        stoneSeekTicks--;
        ItemEntity stone = stoneTarget;
        if (stone == null || stone.isRemoved()
            || !isStoneGift(stone.getItem()) || distanceToSqr(stone) > 400.0) {
            abandonStoneHunt();
            return;
        }
        lookControl.lookAt(stone.getX(), stone.getY() + 0.25, stone.getZ());
        LivingEntity owner = getOwner();
        if (owner != null && owner.isAlive() && distanceToSqr(owner) > 400.0) {
            // 主人跑远了：跟紧主人比捡石头重要。
            abandonStoneHunt();
            return;
        }
        if (getNavigation().isDone() || stoneSeekTicks % 10 == 0) {
            getNavigation().moveTo(stone, 0.9);
        }
        if (distanceTo(stone) < STONE_PICK_TOUCH) {
            completeStonePick(stone);
        } else if (stoneSeekTicks == 0) {
            abandonStoneHunt();
        }
    }

    /** 服务端：够到石头了——收走掉落物、闪光、开心叫一声，再演抱石起身。 */
    private void completeStonePick(ItemEntity stone) {
        stoneTarget = null;
        stoneSeekTicks = 0;
        double sx = stone.getX();
        double sy = stone.getY();
        double sz = stone.getZ();
        stone.discard();
        if (level() instanceof ServerLevel server
            && PetConfig.particleMode() != PetConfig.Mode.OFF) {
            server.sendParticles(ModParticles.DIAMOND_SPARKLE, sx, sy + 0.3, sz,
                PetConfig.particleMode() == PetConfig.Mode.LOW ? 3 : 6, 0.18, 0.15, 0.18, 0.01);
        }
        stonePickCooldown = 120;
        if (wasThrownAtFace(stone)) {
            addIntimacy(6);
            playSfx(PetExpression.PetSfx.HAPPY, 1.0F);
            LivingEntity owner = getOwner();
            if (owner instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("message.mod_b9992987.stone_face_gift"));
            } else if (!isTame()) {
                Player nearby = level().getNearestPlayer(this, 8.0);
                if (nearby != null) {
                    nearby.sendOverlayMessage(Component.translatable("message.mod_b9992987.stone_face_gift"));
                }
            }
        } else {
            playSfx(PetExpression.PetSfx.SNIFF, 0.8F);
        }
        // 弯腰—抱起—起身；ticks 归零后由 mobTick 自动解冻恢复跟随。
        setExpressionSilent(PetExpression.PICKUP, 50);
    }

    /** 服务端：石头被人先捡走或超时，改演一段“咦，没了”的疑问。 */
    private void abandonStoneHunt() {
        stoneTarget = null;
        stoneSeekTicks = 0;
        getNavigation().stop();
        setMovementGoalsFrozen(false);
        setExpression(PetExpression.CONFUSED, 40);
    }

    /** 服务端：找最近的掉落石头（石头/圆石/砂砾/深板岩等都算）。 */
    private ItemEntity findNearestStoneItem(double range) {
        AABB box = getBoundingBox().inflate(range, range, range);
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : level()
            .getEntities(EntityTypeTest.forClass(ItemEntity.class), box,
                it -> !it.isRemoved() && isStoneGift(it.getItem()))) {
            double d = distanceToSqr(item);
            if (d < bestDist) {
                bestDist = d;
                best = item;
            }
        }
        return best;
    }

    /** 路过就捡：跟随途中踩到石头堆，停下来低头捡起，不额外绕路。 */
    private void opportunisticStonePick() {
        if (stonePickCooldown > 0 || carryMode() > 0 || isBossMode()
            || isInWater() || stoneSeekTicks > 0 || expressionTicks() > 0 || isStaying()) {
            return;
        }
        ItemEntity stone = findNearestStoneItem(STONE_PICK_TOUCH + 0.6);
        if (stone != null) {
            completeStonePick(stone);
        }
    }

    /**
     * 玩家把石头面对面丢给企鹅才加好感：必须是玩家刚丢出、还在飞、
     * 并且朝企鹅脸的方向过来。随地一扔的掉落物只会被捡走，不加好感。
     */
    private boolean wasThrownAtFace(ItemEntity stone) {
        Entity thrower = stone.getOwner();
        if (!(thrower instanceof Player player)) {
            return false;
        }
        if (stone.getAge() > 40) {
            return false;
        }
        Vec3 toPenguin = getEyePosition().subtract(player.getEyePosition());
        if (toPenguin.lengthSqr() < 0.04) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F);
        double facing = look.normalize().dot(toPenguin.normalize());
        return facing > 0.55;
    }

    /** 服务端：按音效种类对周围播放一次叫声。 */
    private void playSfx(PetExpression.PetSfx sfx, float volume) {
        SoundEvent event = ModContent.resolve(sfx);
        if (event != null) {
            level().playSound(null, this, event, SoundSource.NEUTRAL, volume, 1.0F);
        }
    }

    // ------------------------------------------------------------------
    // 每 tick 逻辑
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && petCooldown > 0) {
            petCooldown--;
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (followTeleportCooldown > 0) {
            followTeleportCooldown--;
        }
        if (stonePickCooldown > 0) {
            stonePickCooldown--;
        }
        if (isBossMode()) {
            tickBoss(world);
            return;
        }
        if (isStaying()) {
            getNavigation().stop();
            setMovementGoalsFrozen(true);
        }
        catchUpWithOwner();
        int ticks = entityData.get(EXPRESSION_TICKS);
        if (ticks > 0) {
            entityData.set(EXPRESSION_TICKS, ticks - 1);
            if (ticks - 1 == 0) {
                entityData.set(EXPRESSION, 0);
                // 演完立刻解冻：恢复跟随等移动目标。
                setMovementGoalsFrozen(false);
            }
        }
        // 捡石头：先接续寻路，再看有没有顺路可捡的石头。
        tickStoneHunt();
        if ((tickCount & 3) == 0) {
            opportunisticStonePick();
        }
        if (carryMode() > 0) {
            tickCarry();
        }
        tickSniff();
        // 待机不再自动播放表情或叫声；明显动作和声音只由明确事件触发。
        // 跟随时的自动寻钻提示
        if (alertCooldown > 0) {
            alertCooldown--;
        } else if (isTame() && carryMode() == 0 && sniffTicks() == 0 && ticks == 0) {
            // 没找到也必须冷却；否则每只企鹅每秒重复扫描三十多万方块。
            alertCooldown = 100;
            BlockPos near = findNearestDiamond(ALERT_RANGE);
            if (near != null) {
                alertCooldown = 600;
                lookControl.lookAt(near.getX() + 0.5, near.getY() + 1.0, near.getZ() + 0.5);
                setExpression(PetExpression.GREET, 50);
                spawnSparklePath(near.up());
            }
        }
    }

    /** 玩家快速跑远、飞行或路径被水面/台阶打断时，把企鹅拉回主人身边。 */
    private void catchUpWithOwner() {
        if (!isTame() || carryMode() > 0 || followTeleportCooldown > 0 || isStaying() || isBossMode()) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            return;
        }
        // 正在捡石头、且还没离主人太远：先让它把这趟跑完，别硬拽回来。
        if (stoneSeekTicks > 0 && distanceToSqr(owner) < 484.0) {
            return;
        }
        double distanceSq = distanceToSqr(owner);
        // 普通走路不应触发瞬移；只有真正掉队或路径卡死时才拉回。
        if (distanceSq < 256.0) {
            return;
        }
        Vec3 forward = owner.getViewVector(1.0F);
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 target = new Vec3(owner.getX(), owner.getY(), owner.getZ())
            .subtract(forward.multiply(1.5, 0.0, 1.5))
            .add(side.multiply(0.8, 0.0, 0.8));
        setPos(target.x, target.y, target.z);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        getNavigation().stop();
        setYRot(owner.getYRot());
        setYBodyRot(owner.yBodyRot);
        yHeadRot = owner.yHeadRot;
        followTeleportCooldown = 20;
    }

    /**
     * 是否处于"跟着玩家"的状态：已驯服、主人在同一世界且存活。
     * 只有这个状态下才享受摔落免伤（野生企鹅、无主企鹅摔落照常掉血）。
     */
    public boolean isFollowingOwner() {
        if (!isTame()) {
            return false;
        }
        LivingEntity owner = getOwner();
        return owner != null && owner.isAlive() && owner.level() == level();
    }

    /**
     * 跟着玩家时摔落伤害恒为 0：从计算源头掐掉，
     * 不扣血、不播受伤音效、也不触发受伤表情/好感度下降。
     * 高处落地只安静着陆，野生企鹅不受影响。
     */
    @Override
    protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
        if (isFollowingOwner()) {
            return 0;
        }
        return super.calculateFallDamage(fallDistance, damageMultiplier);
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (isBossMode() && bossReviveTicks > 0) {
            return false;
        }
        if (!isTame() && !isBossMode() && source.getEntity() instanceof ServerPlayer player
            && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
            float remaining = getHealth() - amount;
            if (remaining <= 1.0F) {
                beginBossTransform(world, player);
                return false;
            }
        }
        return super.hurtServer(world, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide() && !isTame() && !isBossMode()
            && source.getEntity() instanceof ServerPlayer attacker
            && attacker.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
            && level() instanceof ServerLevel world) {
            setHealth(1.0F);
            beginBossTransform(world, attacker);
            return;
        }
        if (isBossMode()) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
        }
        super.die(source);
    }

    @Override
    protected void actuallyHurt(ServerLevel world, DamageSource source, float amount) {
        super.actuallyHurt(world, source, amount);
        if (carryMode() > 0) {
            dropCarry();
        }
        if (isBossMode()) {
            bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            return;
        }
        setExpression(PetExpression.ANGRY, 60);
        addIntimacy(-3);
    }

    private void beginBossTransform(ServerLevel world, Entity attacker) {
        if (isBossMode()) {
            return;
        }
        if (carryMode() > 0) {
            dropCarry();
        }
        entityData.set(BOSS_MODE, true);
        entityData.set(BOSS_PHASE, 1);
        entityData.set(STAYING, false);
        setOrderedToSit(false);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(BOSS_HEALTH);
        }
        setHealth(BOSS_HEALTH);
        bossBar.setProgress(1.0F);
        bossBar.setVisible(true);
        for (ServerPlayer player : world.players()) {
            if (distanceToSqr(player) < 4096.0) {
                bossBar.addPlayer(player);
            }
        }
        // 复活演出严格持续 5 秒。Boss 在演出结束前锁在原地、无敌且不攻击，
        // 音频本身也已裁成 5 秒，因此不会出现“已经开打但复活声还没停”。
        world.playSound(null, this, ModContent.SOUND_BOSS_THEME, SoundSource.HOSTILE, 1.4F, 1.0F);
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.9, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1.1, getZ(), 24, 0.35, 0.7, 0.35, 0.04);
        world.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.1, getZ(), 90, 0.65, 1.0, 0.65, 0.08);
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.9, getZ(), 45, 0.5, 0.75, 0.5, 0.04);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 35, 0.55, 0.7, 0.55, 0.03);
        setExpression(PetExpression.ANGRY, BOSS_REVIVE_TICKS);
        if (attacker instanceof Player player) {
            player.sendSystemMessage(Component.translatable("message.mod_b9992987.boss_awaken"));
        }
        bossAnchorY = getY() + 1.15;
        bossHoverTicks = 0;
        bossReviveTicks = BOSS_REVIVE_TICKS;
        bossThrowCooldown = 30;
        bossAttackCooldown = 20;
    }

    private void tickBoss(ServerLevel world) {
        entityData.set(EXPRESSION_TICKS, Math.max(20, expressionTicks()));
        entityData.set(EXPRESSION, PetExpression.ANGRY.ordinal());
        setMovementGoalsFrozen(true);
        getNavigation().stop();
        ServerPlayer target = null;
        double nearest = 48.0 * 48.0;
        for (ServerPlayer player : world.players()) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = distanceToSqr(player);
            if (distance < nearest) {
                nearest = distance;
                target = player;
            }
        }
        if (target != null) {
            lookControl.lookAt(target, 40.0F, 40.0F);
            // 飞行 Boss 没有普通行走控制来带动 yBodyRot；只调用 lookControl 会出现
            // “头看玩家、整个身体仍朝旧方向”。这里让身体和头一起平滑追踪目标。
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            float wantedYaw = (float) (Mth.atan2(dz, dx) * 57.295776) - 90.0F;
            float facing = Mth.approachDegrees(yBodyRot, wantedYaw, 14.0F);
            setYRot(facing);
            setYBodyRot(facing);
            setYHeadRot(facing);
            yHeadRot = facing;
            bossBar.addPlayer(target);
        }
        bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        bossHoverTicks++;

        // 凋零式飞行：用速度追踪而不是每 tick 瞬移或锁定高度。
        // Boss 会悬在目标上方约两格，带惯性转向；靠近后主动减速，所以不会一直向天上飘。
        setNoGravity(true);
        if (bossReviveTicks > 0) {
            bossReviveTicks--;
            setDeltaMovement(Vec3.ZERO);
            if ((bossReviveTicks & 3) == 0) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.1, getZ(),
                    8, 0.5, 0.75, 0.5, 0.04);
                world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.75, getZ(),
                    5, 0.35, 0.55, 0.35, 0.02);
                world.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.45, getZ(),
                    8, 0.22, 0.16, 0.22, 0.015);
            }
        } else if (target != null) {
            Vec3 horizontal = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
            double horizontalDistance = horizontal.length();
            double targetY = target.getY() + 2.35;
            double dy = Mth.clamp(targetY - getY(), -3.0, 3.0);
            Vec3 wanted = Vec3.ZERO;
            if (horizontalDistance > 5.0) {
                double speed = Math.min(0.34, 0.10 + (horizontalDistance - 5.0) * 0.025);
                wanted = horizontal.normalize().multiply(speed);
            } else if (horizontalDistance > 0.01 && horizontalDistance < 3.2) {
                wanted = horizontal.normalize().multiply(-0.10);
            }
            if (Math.abs(dy) > 0.35) {
                wanted = wanted.add(0.0, Mth.clamp(dy * 0.075, -0.16, 0.16), 0.0);
            }
            Vec3 current = getDeltaMovement();
            setDeltaMovement(current.multiply(0.68).add(wanted.multiply(0.32)));
        } else {
            // 没有生存玩家时刹停，只保留极轻的悬浮惯性。
            setDeltaMovement(getDeltaMovement().multiply(0.78));
        }

        // 复活的 5 秒结束后才真正开始打人。
        if (bossReviveTicks > 0) {
            return;
        }
        if (bossThrowCooldown > 0) {
            bossThrowCooldown--;
        }
        if (bossAttackCooldown > 0) {
            bossAttackCooldown--;
        }
        if (target != null && distanceToSqr(target) < 16.0 && bossAttackCooldown <= 0) {
            target.hurtServer(world, damageSources().mobAttack(this), 6.0F);
            bossAttackCooldown = 30;
        }
        if (bossThrowCooldown <= 0 && target != null) {
            throwBossRock(world, target);
            bossThrowCooldown = 45 + random.nextInt(20);
        }
        if ((tickCount & 15) == 0) {
            world.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.35, getZ(), 3, 0.18, 0.12, 0.18, 0.01);
        }
    }

    private void throwBossRock(ServerLevel world, LivingEntity target) {
        ThrownBlockEntity rock = new ThrownBlockEntity(world, this, new ItemStack(Items.COBBLESTONE));
        rock.markBossRock();
        Vec3 from = getEyePosition().add(0, 0.2, 0);
        Vec3 to = target.getEyePosition();
        Vec3 dir = to.subtract(from);
        rock.setPos(from.x, from.y, from.z);
        rock.shoot(dir.x, dir.y + 0.18, dir.z, 1.15F, 1.4F);
        world.addFreshEntity(rock);
        world.playSound(null, this, ModContent.SOUND_ANGRY, SoundSource.HOSTILE, 1.1F, 0.7F);
        setExpression(PetExpression.ANGRY, 30);
    }

    // ------------------------------------------------------------------
    // 声音与存档
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        // 禁止原版随机环境叫声，避免待机时无缘无故播放音效。
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SOUND_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SOUND_HURT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putInt("intimacy", intimacy());
        view.putInt("feed_count", feedCount);
        view.putBoolean("staying", isStaying());
        view.putBoolean("boss_mode", isBossMode());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        entityData.set(INTIMACY, Math.max(0, Math.min(100, view.getIntOr("intimacy", 0))));
        feedCount = view.getIntOr("feed_count", 0);
        entityData.set(CARRY_MODE, 0);
        entityData.set(SNIFF_TICKS, 0);
        entityData.set(STAYING, view.getBooleanOr("staying", false));
        boolean boss = view.getBooleanOr("boss_mode", false);
        entityData.set(BOSS_MODE, boss);
        if (boss) {
            AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(BOSS_HEALTH);
            }
            bossBar.setVisible(true);
            bossAnchorY = getY();
            bossReviveTicks = 0;
            setNoGravity(true);
        } else {
            setNoGravity(false);
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob parent) {
        PenguinPetEntity child = ModContent.PENGUIN.create(world, EntitySpawnReason.BREEDING);
        if (child != null && parent instanceof PenguinPetEntity pet) {
            child.setOwner(pet.getOwner());
        }
        return child;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
                                 EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, data);
        entityData.set(INTIMACY, 20 + random.nextInt(20));
        return result;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    /** 供网络包调用：轮盘选择表情。持续 4 秒，让整套浮夸演出能演完。 */
    public void requestExpression(PetExpression expression) {
        if (expression == PetExpression.PICKUP) {
            // 捡石头不是一段姿势那么简单：先找石头、走过去，再演抱石起身。
            startStonePick();
            return;
        }
        boolean refresh = expression() == expression && expressionTicks() > 0;
        setExpression(expression, 80);
        // 同一个表情连续触发时也要重新叫一次，否则轮盘第二次选择没有反馈。
        if (refresh && !level().isClientSide()) {
            playExpressionSound(expression);
        }
    }

    /** 认主后锁定当前位置：站住不跟、再按一次解除。 */
    public void requestStayToggle(Player player) {
        if (level().isClientSide() || isBossMode()) {
            return;
        }
        if (!isTame() || !isOwnedBy(player)) {
            return;
        }
        boolean next = !isStaying();
        entityData.set(STAYING, next);
        setOrderedToSit(next);
        if (next) {
            getNavigation().stop();
            setMovementGoalsFrozen(true);
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.stay_on"));
        } else {
            setMovementGoalsFrozen(false);
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.stay_off"));
        }
    }

    /** 供按键包调用：嗅探找钻石（服务端执行）。 */
    public void requestSniff() {
        if (!level().isClientSide()) {
            startSniff();
        }
    }

    /**
     * 供肩乘键包调用：地上 → 左肩 → 下来 循环。
     * 还没驯服的企鹅被按键指挥时会直接认该玩家为主，保证功能开箱即用。
     */
    public void requestCarryToggle(Player player) {
        if (level().isClientSide()) {
            return;
        }
        if (isBossMode()) {
            return;
        }
        if (!isTame() || getOwner() == null) {
            tame(player);
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.tamed"));
        } else if (!isOwnedBy(player)) {
            return;
        }
        if (carryMode() == 0) {
            startCarry();
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.carry_shoulder"));
        } else {
            dropCarry();
            player.sendOverlayMessage(Component.translatable("message.mod_b9992987.carry_down"));
        }
    }

    /** 供客户端渲染读取：当前表情 id。 */
    public int currentExpressionId() {
        return entityData.get(EXPRESSION);
    }

    /** 不主动繁殖，避免两只宠物互相追着跑。 */
    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    /** 背负时脚步声静音（贴在玩家身上不额外跺脚）。 */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (carryMode() == 0) {
            super.playStepSound(pos, state);
        }
    }
}
