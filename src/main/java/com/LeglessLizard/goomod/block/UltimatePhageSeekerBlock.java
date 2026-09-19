package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.GooMod;
import com.LeglessLizard.goomod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UltimatePhageSeekerBlock extends Block {
    public static final Map<BlockPos, BlockPos> SEEKER_TO_CORE = new ConcurrentHashMap<>();
    public static final Set<BlockPos> ACTIVE_SEEKERS = ConcurrentHashMap.newKeySet();
    public static final Set<BlockPos> PANICKED = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> SPREAD_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> PANIC_DECAY = new ConcurrentHashMap<>();

    private static final Set<Block> SCULK_BLOCKS = Set.of(
            Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR,
            Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN
    );

    public UltimatePhageSeekerBlock(BlockBehaviour.Properties props) { super(props); }

    public static void registerSeeker(BlockPos seekerPos, BlockPos corePos) {
        SEEKER_TO_CORE.put(seekerPos.immutable(), corePos.immutable());
        ACTIVE_SEEKERS.add(seekerPos.immutable());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level.isClientSide()) return;
        BlockPos corePos = findNearestCore(level, pos, 20);
        if (corePos != null) {
            registerSeeker(pos, corePos);
            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof UltimatePhageCoreBlockEntity core) core.linkedSeekers.add(pos.immutable());
        }
        SPREAD_COOLDOWNS.put(pos, 8 + level.random.nextInt(8));
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide()) {
            ACTIVE_SEEKERS.remove(pos);
            SPREAD_COOLDOWNS.remove(pos);
            PANICKED.remove(pos);
            PANIC_DECAY.remove(pos);
            BlockPos corePos = SEEKER_TO_CORE.remove(pos);
            if (corePos != null) {
                BlockEntity be = level.getBlockEntity(corePos);
                if (be instanceof UltimatePhageCoreBlockEntity core) core.linkedSeekers.remove(pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;

        // Panic mode: core was broken during the show
        if (PANICKED.contains(pos)) {
            handlePanic(level, pos, random);
            return;
        }

        BlockPos corePos = SEEKER_TO_CORE.get(pos);
        if (corePos == null) {
            corePos = findNearestCore(level, pos, 20);
            if (corePos != null) {
                registerSeeker(pos, corePos);
                BlockEntity coreBe = level.getBlockEntity(corePos);
                if (coreBe instanceof UltimatePhageCoreBlockEntity c) c.linkedSeekers.add(pos.immutable());
            } else {
                level.scheduleTick(pos, this, 1);
                return;
            }
        }

        if (!level.isLoaded(corePos)) { level.scheduleTick(pos, this, 1); return; }

        BlockEntity coreBe = level.getBlockEntity(corePos);
        if (!(coreBe instanceof UltimatePhageCoreBlockEntity core)) {
            // Core gone — start panicking instead of just vanishing
            PANICKED.add(pos.immutable());
            level.scheduleTick(pos, this, 1);
            return;
        }

        // During convergence: do nothing, core handles us
        if (core.isReturning) {
            level.scheduleTick(pos, this, 1);
            return;
        }

        // Normal mode
        if (core.projectileCooldown <= 0 && random.nextInt(100) == 0) {
            BlockPos furthest = UltimatePhageCoreBlock.findFurthestUntargetedSculk(level, pos, 20, core);
            if (furthest != null) {
                UltimatePhageCoreBlock.spawnProjectile(core, pos, furthest);
                core.projectileCooldown = 40;
            }
        }

        if (GooMod.SPREADING_ENABLED) {
            Integer cd = SPREAD_COOLDOWNS.getOrDefault(pos, 0);
            if (cd > 0) {
                SPREAD_COOLDOWNS.put(pos, cd - 1);
            } else {
                BlockPos.MutableBlockPos np = new BlockPos.MutableBlockPos();
                boolean converted = false;
                for (int dx = -1; dx <= 1 && !converted; dx++) for (int dy = -1; dy <= 1 && !converted; dy++) for (int dz = -1; dz <= 1 && !converted; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    np.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.isLoaded(np)) continue;
                    BlockState neighbor = level.getBlockState(np);
                    if (SCULK_BLOCKS.contains(neighbor.getBlock())) {
                        level.setBlock(np, this.defaultBlockState(), 3);
                        registerSeeker(np, corePos);
                        core.linkedSeekers.add(np.immutable());
                        core.totalConverted++;
                        core.xpAccumulated += 1;
                        GooBlock.CONVERTED_COUNT.incrementAndGet();
                        converted = true;
                    }
                }
                SPREAD_COOLDOWNS.put(pos, 8 + random.nextInt(8));
            }
        }
        level.scheduleTick(pos, this, 1);
    }

    /** Panic: after the core is broken, seekers decay with lightning-phage effect. */
    private void handlePanic(ServerLevel level, BlockPos pos, RandomSource random) {
        Integer cd = PANIC_DECAY.get(pos);
        if (cd == null) {
            // Start the decay countdown
            PANIC_DECAY.put(pos, 20 + random.nextInt(80));
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.04);
            level.scheduleTick(pos, this, 1);
            return;
        }
        if (cd > 0) {
            PANIC_DECAY.put(pos, cd - 1);
            // Occasional nervous crackle
            if (random.nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.05);
            }
            level.scheduleTick(pos, this, 1);
            return;
        }

        // Detonate with lightning effect
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        int intensity = 2 + random.nextInt(3);
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 20 * intensity, 0.4, 0.4, 0.4, 0.2);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 15 * intensity, 0.5, 0.5, 0.5, 0.25);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 6 * intensity, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 3.0F, 0.8F + random.nextFloat() * 0.4F);

        // Damage nearby living entities
        AABB aabb = new AABB(pos).inflate(3.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        for (LivingEntity entity : entities) {
            entity.hurt(level.damageSources().lightningBolt(), 10.0f);
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private BlockPos findNearestCore(Level level, BlockPos pos, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos p : UltimatePhageCoreBlock.ACTIVE_CORES) {
            if (!level.isLoaded(p)) continue;
            double d = p.distSqr(pos);
            if (d < nearestDist && d <= radius * radius) { nearestDist = d; nearest = p; }
        }
        return nearest;
    }
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        processSeekerTick(level, pos, random);
    }

    public static void processSeekerTick(ServerLevel level, BlockPos pos, RandomSource random) {
        if (PANICKED.contains(pos)) {
            handlePanic(level, pos, random);
            return;
        }

        BlockPos corePos = SEEKER_TO_CORE.get(pos);
        if (corePos == null) {
            corePos = findNearestCore(level, pos, 20);
            if (corePos != null) {
                registerSeeker(pos, corePos);
                BlockEntity coreBe = level.getBlockEntity(corePos);
                if (coreBe instanceof UltimatePhageCoreBlockEntity c) c.linkedSeekers.add(pos.immutable());
            } else {
                return;
            }
        }

        if (!level.isLoaded(corePos)) return;

        BlockEntity coreBe = level.getBlockEntity(corePos);
        if (!(coreBe instanceof UltimatePhageCoreBlockEntity core)) {
            PANICKED.add(pos.immutable());
            return;
        }

        if (core.isReturning) return;

        if (GooMod.SPREADING_ENABLED) {
            Integer cd = SPREAD_COOLDOWNS.getOrDefault(pos, 0);
            if (cd > 0) {
                SPREAD_COOLDOWNS.put(pos, cd - 1);
            } else {
                BlockPos.MutableBlockPos np = new BlockPos.MutableBlockPos();
                boolean converted = false;
                for (int dx = -1; dx <= 1 && !converted; dx++) for (int dy = -1; dy <= 1 && !converted; dy++) for (int dz = -1; dz <= 1 && !converted; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    np.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.isLoaded(np)) continue;
                    BlockState neighbor = level.getBlockState(np);
                    if (SCULK_BLOCKS.contains(neighbor.getBlock())) {
                        level.setBlock(np, ModBlocks.ULTIMATE_PHAGE_SEEKER_BLOCK.get().defaultBlockState(), 3);
                        registerSeeker(np, corePos);
                        core.linkedSeekers.add(np.immutable());
                        core.totalConverted++;
                        core.xpAccumulated++;
                        GooBlock.CONVERTED_COUNT.incrementAndGet();
                        converted = true;
                    }
                }
                SPREAD_COOLDOWNS.put(pos, 8 + random.nextInt(8));
            }
        }
    }


    /** Panic: after the core is broken, seekers decay with lightning-phage effect. */
    private void handlePanic(ServerLevel level, BlockPos pos, RandomSource random) {
        Integer cd = PANIC_DECAY.get(pos);
        if (cd == null) {
            // Start the decay countdown
            PANIC_DECAY.put(pos, 20 + random.nextInt(80));
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.04);
            level.scheduleTick(pos, this, 1);
            return;
        }
        if (cd > 0) {
            PANIC_DECAY.put(pos, cd - 1);
            // Occasional nervous crackle
            if (random.nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.05);
            }
            level.scheduleTick(pos, this, 1);
            return;
        }

        // Detonate with lightning effect
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        int intensity = 2 + random.nextInt(3);
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 20 * intensity, 0.4, 0.4, 0.4, 0.2);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 15 * intensity, 0.5, 0.5, 0.5, 0.25);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 6 * intensity, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 3.0F, 0.8F + random.nextFloat() * 0.4F);

        // Damage nearby living entities
        AABB aabb = new AABB(pos).inflate(3.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        for (LivingEntity entity : entities) {
            entity.hurt(level.damageSources().lightningBolt(), 10.0f);
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private BlockPos findNearestCore(Level level, BlockPos pos, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos p : UltimatePhageCoreBlock.ACTIVE_CORES) {
            if (!level.isLoaded(p)) continue;
            double d = p.distSqr(pos);
            if (d < nearestDist && d <= radius * radius) { nearestDist = d; nearest = p; }
        }
        return nearest;
    }
}