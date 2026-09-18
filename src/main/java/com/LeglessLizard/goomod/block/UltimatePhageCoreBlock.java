package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.GooMod;
import com.LeglessLizard.goomod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UltimatePhageCoreBlock extends Block implements EntityBlock {
    public static final Set<BlockPos> ACTIVE_CORES = ConcurrentHashMap.newKeySet();

    private static final int SEEKERS_PER_TICK = 4000;
    private static final int MAX_FLIGHTS = 1200;
    private static final int FLIGHT_DURATION = 25;
    private static final int FORMING_TICKS = 60;
    private static final int PULSE_TICKS = 40;

    private static final Set<Block> SCULK_BLOCKS = Set.of(
            Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR,
            Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN
    );

    public UltimatePhageCoreBlock(BlockBehaviour.Properties props) { super(props); }

    public static void setChunkForced(ServerLevel level, BlockPos pos, boolean forced) {
        ChunkPos c = new ChunkPos(pos);
        level.setChunkForced(c.x, c.z, forced);
        level.setChunkForced(c.x, c.z - 1, forced);
        level.setChunkForced(c.x, c.z + 1, forced);
        level.setChunkForced(c.x + 1, c.z, forced);
        level.setChunkForced(c.x - 1, c.z, forced);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UltimatePhageCoreBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel serverLevel) {
            ACTIVE_CORES.add(pos.immutable());
            setChunkForced(serverLevel, pos, true);
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ACTIVE_CORES.remove(pos);
            setChunkForced(serverLevel, pos, false);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof UltimatePhageCoreBlockEntity core) {
                for (BlockPos sp : core.linkedSeekers) {
                    UltimatePhageSeekerBlock.PANICKED.add(sp.immutable());
                }
                serverLevel.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.5F, 0.5F);

                if (!core.collectedOres.isEmpty()) {
                    Collections.shuffle(core.collectedOres);
                    int toDrop = Math.max(1, core.collectedOres.size() * 5 / 100);
                    for (int i = 0; i < toDrop && i < core.collectedOres.size(); i++) {
                        popResource(serverLevel, pos, core.collectedOres.get(i));
                    }
                }
                core.collectedOres.clear();

                int xp = core.xpAccumulated * 5 / 100;
                if (xp > 0) {
                    ExperienceOrb orb = new ExperienceOrb(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, xp);
                    serverLevel.addFreshEntity(orb);
                }
                core.xpAccumulated = 0;

                for (var f : core.particleFlights) {
                    if (f.bullet != null && !f.bullet.isRemoved()) f.bullet.discard();
                }
                core.particleFlights.clear();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;

        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                1, 0, 0.02, 0, 0.01);

        if (!GooMod.SPREADING_ENABLED) { level.scheduleTick(pos, this, 1); return; }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof UltimatePhageCoreBlockEntity core)) return;
        core.projectilesThisTick = 0;
        if (core.wardenAttackCooldown > 0) core.wardenAttackCooldown--;

        List<UltimatePhageCoreBlockEntity.PendingProjectile> projSnapshot = new ArrayList<>(core.pendingProjectiles);
        List<UltimatePhageCoreBlockEntity.PendingProjectile> projRemove = new ArrayList<>();
        for (var p : projSnapshot) {
            p.elapsedTicks++;
            double t = Math.min(1.0, (double) p.elapsedTicks / p.totalTicks);
            double x = p.from.x + (p.to.x - p.from.x) * t;
            double y = p.from.y + (p.to.y - p.from.y) * t;
            double z = p.from.z + (p.to.z - p.from.z) * t;
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 2, 0.05, 0.05, 0.05, 0);
            if (p.elapsedTicks == 1) level.playSound(null, p.from.x, p.from.y, p.from.z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2F, 0.8F);
            if (p.elapsedTicks >= p.totalTicks) {
                BlockState tgt = level.getBlockState(p.targetBlock);
                if (SCULK_BLOCKS.contains(tgt.getBlock())) {
                    level.setBlock(p.targetBlock, ModBlocks.ULTIMATE_PHAGE_SEEKER_BLOCK.get().defaultBlockState(), 3);
                    UltimatePhageSeekerBlock.registerSeeker(p.targetBlock, pos);
                    core.totalConverted++;
                    core.xpAccumulated += 1;
                    level.playSound(null, p.targetBlock, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
                }
                projRemove.add(p);
            }
        }
        core.pendingProjectiles.removeAll(projRemove);
        if (core.projectileCooldown > 0) core.projectileCooldown--;

        if (level.getGameTime() % 40 == 0) {
            core.linkedSeekers.removeIf(sp -> !level.isLoaded(sp) ||
                    !(level.getBlockState(sp).getBlock() instanceof UltimatePhageSeekerBlock));
        }

        if (!core.isReturning && core.totalConverted > 0 && core.pendingProjectiles.isEmpty()) {
            boolean anySeesSculk = false;
            for (BlockPos sp : core.linkedSeekers) {
                if (findFurthestSculk(level, sp, 20) != null) { anySeesSculk = true; break; }
            }
            if (!core.linkedSeekers.isEmpty() && !anySeesSculk) {
                beginConvergence(level, pos, core);
            }
        }

        if (core.isReturning) {
            if (core.phase == 0) beginConvergence(level, pos, core);
            core.phaseTicks++;
            switch (core.phase) {
                case 1 -> tickPulse(level, core);
                case 2 -> tickAbsorbing(level, core, random);
                case 3 -> tickForming(level, core);
                case 4 -> tickConsuming(level, pos, core);
                case 5 -> tickSlam(level, pos, core);
                default -> {}
            }
        }

        drawFlights(level, core);

        if (!core.isReturning && core.wardenAttackCooldown <= 0 && level.getGameTime() % 10 == 0) {
            fireAtNearbyWarden(level, core, random);
        }

        if (!core.isReturning && core.projectileCooldown <= 0) {
            if (random.nextInt(50) == 0) {
                BlockPos furthest = findFurthestUntargetedSculk(level, pos, 20, core);
                if (furthest != null) { spawnProjectile(core, pos, furthest); core.projectileCooldown = 40; }
            }
        }

        level.scheduleTick(pos, this, 1);
    }

    public static void beginConvergence(ServerLevel level, BlockPos corePos, UltimatePhageCoreBlockEntity core) {
        core.isReturning = true;
        core.phase = 1;
        core.phaseTicks = 0;

        int count = core.linkedSeekers.size();
        double r = Math.sqrt(count) / 6.0;
        r = Math.max(3.0, Math.min(16.0, r));
        core.sphereRadius = r;
        core.sphereCenter = Vec3.atCenterOf(corePos);

        core.spherePoints.clear();
        int N = Math.max(50, Math.min(2000, count / 50));
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < N; i++) {
            double y = (N == 1) ? 0.0 : 1.0 - (i * 2.0) / (N - 1);
            double phi = i * goldenAngle;
            double rad = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double px = Math.cos(phi) * rad;
            double pz = Math.sin(phi) * rad;
            core.spherePoints.add(core.sphereCenter.add(px * r, y * r, pz * r));
        }

        core.seekerSnapshot.clear();
        core.seekerSnapshot.addAll(core.linkedSeekers);
        Collections.shuffle(core.seekerSnapshot, new java.util.Random(level.getGameTime()));
        core.nextSeekerIndex = 0;
        core.particleFlights.clear();

        level.playSound(null, corePos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.6F);
    }

    private static void tickPulse(ServerLevel level, UltimatePhageCoreBlockEntity core) {
        level.sendParticles(ParticleTypes.END_ROD,
                core.sphereCenter.x, core.sphereCenter.y, core.sphereCenter.z,
                40, core.sphereRadius, core.sphereRadius, core.sphereRadius, 0.01);
        if (core.phaseTicks % 5 == 0) {
            level.playSound(null, core.sphereCenter.x, core.sphereCenter.y, core.sphereCenter.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.0F + (core.phaseTicks % 60) * 0.02F);
        }
        if (core.phaseTicks >= PULSE_TICKS) {
            core.phase = 2;
            core.phaseTicks = 0;
        }
    }

    private static void tickAbsorbing(ServerLevel level, UltimatePhageCoreBlockEntity core, RandomSource random) {
        int batch = Math.max(1, Math.min(20000, core.seekerSnapshot.size() / 80));
        int spawnEvery = Math.max(1, core.seekerSnapshot.size() / MAX_FLIGHTS);
        int deleted = 0;
        int slotCounter = 0;
        while (core.nextSeekerIndex < core.seekerSnapshot.size() && deleted < batch) {
            BlockPos p = core.seekerSnapshot.get(core.nextSeekerIndex);
            core.nextSeekerIndex++;
            deleted++;
            slotCounter++;
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof UltimatePhageSeekerBlock)) continue;

            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    4, 0.15, 0.15, 0.15, 0.02);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.01);

            if (spawnEvery > 0 && slotCounter % spawnEvery == 0 && !core.spherePoints.isEmpty()) {
                Vec3 target = core.spherePoints.get(core.particleFlights.size() % core.spherePoints.size());
                int delay = random.nextInt(4);
                UltimatePhageCoreBlockEntity.ParticleFlight flight =
                        new UltimatePhageCoreBlockEntity.ParticleFlight(
                                Vec3.atCenterOf(p), target, FLIGHT_DURATION, delay);

                try {
                    ShulkerBullet bullet = new ShulkerBullet(EntityType.SHULKER_BULLET, level);
                    bullet.setPos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                    bullet.setNoGravity(true);
                    bullet.setInvulnerable(true);
                    bullet.setDeltaMovement(0, 0, 0);
                    level.addFreshEntity(bullet);
                    flight.bullet = bullet;
                } catch (Throwable ignored) {}

                core.particleFlights.add(flight);
            }

            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }

        if (core.phaseTicks % 8 == 0) {
            float pitch = 1.0F + Math.min(1.5F, core.phaseTicks * 0.03F);
            level.playSound(null, core.sphereCenter.x, core.sphereCenter.y, core.sphereCenter.z,
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, pitch);
        }

        if (core.nextSeekerIndex >= core.seekerSnapshot.size()) {
            core.phase = 3;
            core.phaseTicks = 0;
        }
    }

    private static void tickForming(ServerLevel level, UltimatePhageCoreBlockEntity core) {
        for (Vec3 pt : core.spherePoints) {
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.001);
        }
        if (core.phaseTicks % 10 == 0) {
            float pitch = 1.2F + (core.phaseTicks / 10) * 0.08F;
            level.playSound(null, core.sphereCenter.x, core.sphereCenter.y, core.sphereCenter.z,
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, pitch);
        }

        boolean allArrived = core.particleFlights.isEmpty() && core.pendingProjectiles.isEmpty();
        if (allArrived && core.phaseTicks >= FORMING_TICKS) {
            core.phase = 4;
            core.phaseTicks = 0;
            level.playSound(null, core.sphereCenter.x, core.sphereCenter.y, core.sphereCenter.z,
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.8F);
        }
    }

    private static void tickConsuming(ServerLevel level, BlockPos corePos, UltimatePhageCoreBlockEntity core) {
        if (core.spherePoints.size() <= 1) {
            if (core.spherePoints.size() == 1) {
                Vec3 top = core.spherePoints.remove(0);
                core.slamFrom = top;
                core.slamTo = Vec3.atCenterOf(corePos);
                core.slamProgress = 0;
                core.slamTotalTicks = 15;
                core.phase = 5;
                core.phaseTicks = 0;
                level.playSound(null, top.x, top.y, top.z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 2.0F, 0.5F);
            } else {
                popCore(level, corePos, core);
            }
            return;
        }
        int consumePerTick = Math.max(2, core.spherePoints.size() / 60);
        int consumed = 0;

        while (consumed < consumePerTick && !core.spherePoints.isEmpty()) {
            int idx = core.spherePoints.size() - 1;
            Vec3 pt = core.spherePoints.remove(idx);
            consumed++;

            level.sendParticles(ParticleTypes.FLASH, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 6, 0.15, 0.15, 0.15, 0.03);

            level.playSound(null, pt.x, pt.y, pt.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS,
                    0.5F, 1.5F + level.random.nextFloat() * 0.4F);
        }

        for (Vec3 pt : core.spherePoints) {
            level.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.001);
        }

        if (core.spherePoints.isEmpty()) {
            Vec3 end = Vec3.atCenterOf(corePos);
            level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 3, 0, 0, 0, 0);
            popCore(level, corePos, core);
        }
    }

    private static void tickSlam(ServerLevel level, BlockPos corePos, UltimatePhageCoreBlockEntity core) {
        if (core.slamProgress >= core.slamTotalTicks) {
            Vec3 end = core.slamTo;
            level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 6, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 40, 0.6, 0.6, 0.6, 0.15);
            level.sendParticles(ParticleTypes.SCULK_SOUL, end.x, end.y, end.z, 60, 0.8, 0.8, 0.8, 0.2);
            level.playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F, 0.5F);
            popCore(level, corePos, core);
            return;
        }

        core.slamProgress++;
        double linear = (double) core.slamProgress / core.slamTotalTicks;
        double t = linear * linear;

        double x = core.slamFrom.x + (core.slamTo.x - core.slamFrom.x) * t;
        double y = core.slamFrom.y + (core.slamTo.y - core.slamFrom.y) * t;
        double z = core.slamFrom.z + (core.slamTo.z - core.slamFrom.z) * t;

        level.playSound(null, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.8F, 0.7F);

        level.sendParticles(ParticleTypes.SOUL, x, y, z, 14, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 18, 0.5, 0.5, 0.5, 0.08);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 12, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, 0.2, 0.2, 0.2, 0.02);
    }

    private static void drawFlights(ServerLevel level, UltimatePhageCoreBlockEntity core) {
        List<UltimatePhageCoreBlockEntity.ParticleFlight> done = new ArrayList<>();
        for (var f : core.particleFlights) {
            if (f.startDelay > 0) { f.startDelay--; continue; }
            f.ticksElapsed++;
            double t = Math.min(1.0, (double) f.ticksElapsed / f.totalTicks);
            double x = f.from.x + (f.to.x - f.from.x) * t;
            double y = f.from.y + (f.to.y - f.from.y) * t;
            double z = f.from.z + (f.to.z - f.from.z) * t;

            level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 2, 0.02, 0.02, 0.02, 0.001);

            if (f.bullet != null && !f.bullet.isRemoved()) {
                f.bullet.moveTo(x, y, z, 0, 0);
                f.bullet.setDeltaMovement(0, 0, 0);
            }

            if (f.ticksElapsed >= f.totalTicks) {
                level.sendParticles(ParticleTypes.END_ROD, f.to.x, f.to.y, f.to.z, 4, 0.1, 0.1, 0.1, 0.02);
                if (f.bullet != null && !f.bullet.isRemoved()) f.bullet.discard();
                done.add(f);
            }
        }
        core.particleFlights.removeAll(done);
    }

    private static void fireAtNearbyWarden(ServerLevel level, UltimatePhageCoreBlockEntity core, RandomSource random) {
        Warden chosenWarden = null;
        BlockPos firingSeeker = null;
        double bestDistance = Double.MAX_VALUE;

        for (Warden warden : level.getEntities(EntityType.WARDEN, Warden::isAlive)) {
            BlockPos nearestSeeker = null;
            double nearestDistance = Double.MAX_VALUE;

            for (BlockPos seekerPos : core.linkedSeekers) {
                if (!level.isLoaded(seekerPos)) continue;
                if (!(level.getBlockState(seekerPos).getBlock() instanceof UltimatePhageSeekerBlock)) continue;

                double distance = warden.distanceToSqr(
                        seekerPos.getX() + 0.5,
                        seekerPos.getY() + 0.5,
                        seekerPos.getZ() + 0.5
                );

                if (distance <= 20.0 * 20.0 && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestSeeker = seekerPos.immutable();
                }
            }

            if (nearestSeeker != null && nearestDistance < bestDistance) {
                bestDistance = nearestDistance;
                chosenWarden = warden;
                firingSeeker = nearestSeeker;
            }
        }

        if (chosenWarden == null || firingSeeker == null) return;

        UltimatePhageWardenProjectile projectile =
                new UltimatePhageWardenProjectile(level, firingSeeker, chosenWarden);
        projectile.setPos(
                firingSeeker.getX() + 0.5,
                firingSeeker.getY() + 0.5,
                firingSeeker.getZ() + 0.5
        );
        projectile.setDeltaMovement(
                chosenWarden.position().subtract(projectile.position()).normalize().scale(0.6)
        );
        level.addFreshEntity(projectile);

        level.sendParticles(ParticleTypes.SCULK_SOUL,
                firingSeeker.getX() + 0.5, firingSeeker.getY() + 0.5, firingSeeker.getZ() + 0.5,
                12, 0.25, 0.25, 0.25, 0.04);
        level.playSound(null, firingSeeker, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS,
                1.4F, 0.65F + random.nextFloat() * 0.15F);

        core.wardenAttackCooldown = 20;
    }

    public static int projectileBudget(UltimatePhageCoreBlockEntity core) {
        int size = core.linkedSeekers.size();
        return Math.max(4, Math.min(80, size / 200));
    }

    public static void spawnProjectile(UltimatePhageCoreBlockEntity core, BlockPos fromPos, BlockPos targetPos) {
        double dist = Math.sqrt(fromPos.distSqr(targetPos));
        int flightTicks = Math.max(5, (int)(dist * 1.5));
        core.pendingProjectiles.add(new UltimatePhageCoreBlockEntity.PendingProjectile(
                Vec3.atCenterOf(fromPos), Vec3.atCenterOf(targetPos), targetPos, flightTicks));
    }

    public static BlockPos findFurthestSculk(ServerLevel level, BlockPos origin, int radius) {
        BlockPos furthest = null;
        double furthestDist = -1;
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            BlockPos p = origin.offset(dx, dy, dz);
            if (!level.isLoaded(p)) continue;
            if (SCULK_BLOCKS.contains(level.getBlockState(p).getBlock())) {
                double d = origin.distSqr(p);
                if (d > furthestDist) { furthestDist = d; furthest = p; }
            }
        }
        return furthest;
    }

    public static BlockPos findFurthestUntargetedSculk(ServerLevel level, BlockPos origin, int radius, UltimatePhageCoreBlockEntity core) {
        BlockPos shrieker = null;
        double shriekerDist = -1;
        BlockPos furthest = null;
        double furthestDist = -1;

        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            BlockPos p = origin.offset(dx, dy, dz);
            if (!level.isLoaded(p)) continue;
            if (core.isTargeted(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!SCULK_BLOCKS.contains(s.getBlock())) continue;
            double d = origin.distSqr(p);
            if (s.getBlock() == Blocks.SCULK_SHRIEKER) {
                if (d > shriekerDist) { shriekerDist = d; shrieker = p; }
            } else {
                if (d > furthestDist) { furthestDist = d; furthest = p; }
            }
        }
        return shrieker != null ? shrieker : furthest;
    }

    private static void popCore(ServerLevel level, BlockPos pos, UltimatePhageCoreBlockEntity core) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.NONE);
        level.playSound(null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 1.5F, 0.5F);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.0F, 1.8F);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.5F, 0.5F);

        for (ItemStack stack : core.collectedOres) popResource(level, pos, stack);
        core.collectedOres.clear();

        int xp = (int)(core.xpAccumulated * 0.67);
        if (xp > 0) {
            ExperienceOrb orb = new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, xp);
            level.addFreshEntity(orb);
        }
        core.xpAccumulated = 0;

        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 200, 2, 2, 2, 0.2);
        setChunkForced(level, pos, false);
        ACTIVE_CORES.remove(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}