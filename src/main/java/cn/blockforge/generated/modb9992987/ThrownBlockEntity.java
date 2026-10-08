package cn.blockforge.generated.modb9992987;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 弹弓射出的方块，以及 Boss 企鹅丢出的大石头。
 * 弹弓打中 Boss 石头时会把它原路弹回去。
 */
public class ThrownBlockEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> BOSS_ROCK =
        SynchedEntityData.defineId(ThrownBlockEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean bossRock;
    private boolean bounced;
    private boolean impactHandled;
    private int life = 120;

    public ThrownBlockEntity(EntityType<? extends ThrownBlockEntity> type, Level world) {
        super(type, world);
    }

    public ThrownBlockEntity(Level world, LivingEntity owner, ItemStack stack) {
        super(ModContent.THROWN_BLOCK, owner, world, stack);
    }

    public ThrownBlockEntity(Level world, double x, double y, double z, ItemStack stack) {
        super(ModContent.THROWN_BLOCK, x, y, z, world, stack);
    }

    public void markBossRock() {
        this.bossRock = true;
        this.entityData.set(BOSS_ROCK, true);
    }

    public boolean isBossRock() {
        return bossRock || this.entityData.get(BOSS_ROCK);
    }

    public float renderScale() {
        return isBossRock() ? 1.85F : 1.35F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.add(BOSS_ROCK, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && --life <= 0) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (target == getOwner()) {
            return;
        }
        if (bossRock && !bounced && target instanceof ThrownBlockEntity other && !other.bossRock) {
            bounceBack(other);
            other.discard();
            return;
        }
        if (!bossRock && target instanceof ThrownBlockEntity other && other.bossRock && !other.bounced) {
            other.bounceBack(this);
            discard();
            return;
        }
        Level world = level();
        if (world instanceof ServerLevel server) {
            float amount = bossRock ? 8.0F : 4.0F;
            target.hurtServer(server, damageSources().thrown(this, getOwner()), amount);
            spawnImpact(server);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        Level world = level();
        if (world instanceof ServerLevel server && !isRemoved()) {
            spawnImpact(server);
            discard();
        }
    }

    /** 被弹弓打中后，石头掉头飞向原来的投掷者（Boss）。 */
    public void bounceBack(ThrownBlockEntity fromSling) {
        bounced = true;
        Entity original = getOwner();
        Entity shooter = fromSling.getOwner();
        if (shooter != null) {
            setOwner(shooter);
        }
        Vec3 reverse;
        if (original != null) {
            reverse = original.getEyePosition().subtract(position()).normalize();
        } else {
            reverse = getDeltaMovement().scale(-1.0);
            if (reverse.lengthSqr() < 0.0001) {
                reverse = new Vec3(0, 0.2, 0);
            } else {
                reverse = reverse.normalize();
            }
        }
        shoot(reverse.x, reverse.y + 0.12, reverse.z, 1.35F, 1.0F);
        life = 80;
        Level world = level();
        if (world instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.08);
            world.playSound(null, this, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    private void spawnImpact(ServerLevel server) {
        if (impactHandled) {
            return;
        }
        impactHandled = true;
        ItemStack stack = getItem();
        if (stack.isEmpty()) {
            stack = new ItemStack(getDefaultItem());
        }
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack),
            getX(), getY(), getZ(), bossRock ? 24 : 12, 0.25, 0.25, 0.25, 0.12);
        if (bossRock) {
            // Boss 巨石落地挖出明显坑洞，并保留爆炸粒子与冲击伤害。
            server.explode(this, getX(), getY(), getZ(), 2.0F, false,
                Level.ExplosionInteraction.MOB);
        }
        server.playSound(null, this, SoundEvents.STONE_BREAK,
            SoundSource.NEUTRAL, bossRock ? 1.3F : 0.9F, 0.9F);
    }
}
