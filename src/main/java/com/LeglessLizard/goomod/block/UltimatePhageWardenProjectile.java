package com.LeglessLizard.goomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class UltimatePhageWardenProjectile extends ShulkerBullet {
    private static final int MAX_LIFETIME = 100;
    private static final double SPEED = 0.6;
    private static final double HOMING_STRENGTH = 0.18;
    private static final float DAMAGE = 6.0F;

    private final BlockPos firingSeeker;
    private Warden target;

    public UltimatePhageWardenProjectile(ServerLevel level, BlockPos firingSeeker, Warden target) {
        super(EntityType.SHULKER_BULLET, level);
        this.firingSeeker = firingSeeker.immutable();
        this.target = target;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    public void tick() {
        baseTick();

        if (!(level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
            discard();
            return;
        }

        if (tickCount > MAX_LIFETIME) {
            discard();
            return;
        }

        Vec3 toTarget = target.getEyePosition().subtract(position());
        double distance = toTarget.length();

        if (distance <= 1.25) {
            hitWarden(level);
            return;
        }

        Vec3 desiredVelocity = toTarget.normalize().scale(SPEED);
        Vec3 velocity = getDeltaMovement()
                .scale(1.0 - HOMING_STRENGTH)
                .add(desiredVelocity.scale(HOMING_STRENGTH));

        if (velocity.lengthSqr() > SPEED * SPEED) {
            velocity = velocity.normalize().scale(SPEED);
        }

        setDeltaMovement(velocity);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(
                this,
                entity -> entity == target && entity.isAlive()
        );

        if (hit.getType() == HitResult.Type.BLOCK) {
            discard();
            return;
        }

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target) {
            hitWarden(level);
            return;
        }

        move(MoverType.SELF, velocity);

        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                getX(), getY(), getZ(),
                2, 0.03, 0.03, 0.03, 0.005
        );
    }

    private void hitWarden(ServerLevel level) {
        if (target != null && target.isAlive()) {
            target.hurt(level.damageSources().mobProjectile(this, null), DAMAGE);
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    18, 0.3, 0.5, 0.3, 0.08
            );
            level.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK,
                    SoundSource.HOSTILE,
                    1.2F,
                    1.4F
            );
        }
        discard();
    }
}
