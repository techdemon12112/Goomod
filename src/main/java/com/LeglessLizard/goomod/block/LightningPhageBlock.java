package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.config.ModConfig;
import com.LeglessLizard.goomod.GooMod;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LightningPhageBlock extends Block {
    public static final Set<BlockPos> ACTIVE_PHAGES = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> SPREAD_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> ISOLATION_START = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> DECAY_DEADLINE = new ConcurrentHashMap<>();
    private static final Set<Block> SCULK_BLOCKS;

    public LightningPhageBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            ACTIVE_PHAGES.add(pos.immutable());
            SPREAD_COOLDOWNS.put(pos, this.randomCooldown(level.random));
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            ACTIVE_PHAGES.remove(pos);
            SPREAD_COOLDOWNS.remove(pos);
            ISOLATION_START.remove(pos);
            DECAY_DEADLINE.remove(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide()) {
            boolean hasNeighbor = this.hasSculkNeighbor(level, pos);
            long currentTick = level.getGameTime();

            if (!hasNeighbor) {
                ISOLATION_START.putIfAbsent(pos, currentTick);
                long start = ISOLATION_START.get(pos);
                long graceEnd = start + 40;
                if (currentTick >= graceEnd) {
                    DECAY_DEADLINE.putIfAbsent(pos, currentTick + 10 + random.nextInt(20));
                    Long deadline = DECAY_DEADLINE.get(pos);
                    if (deadline != null && currentTick >= deadline) {
                        // 1 in 50 chance to shock on dissipate
                        if (random.nextInt(50) == 0) {
                            this.spawnShock(level, pos, random);
                        }
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        return;
                    }
                }
            } else {
                ISOLATION_START.remove(pos);
                DECAY_DEADLINE.remove(pos);
            }

            if (!GooMod.SPREADING_ENABLED) {
                level.scheduleTick(pos, this, 1);
            } else {
                Integer cooldownObj = SPREAD_COOLDOWNS.get(pos);
                int cooldown = cooldownObj != null ? cooldownObj : 0;
                if (cooldown > 0) {
                    SPREAD_COOLDOWNS.put(pos, cooldown - 1);
                    level.scheduleTick(pos, this, 1);
                } else {
                    BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
                    boolean converted = false;

                    for (int dx = -1; dx <= 1 && !converted; ++dx) {
                        for (int dy = -1; dy <= 1 && !converted; ++dy) {
                            for (int dz = -1; dz <= 1 && !converted; ++dz) {
                                if (dx != 0 || dy != 0 || dz != 0) {
                                    neighborPos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                                    if (level.isLoaded(neighborPos)) {
                                        BlockState neighbor = level.getBlockState(neighborPos);
                                        if (SCULK_BLOCKS.contains(neighbor.getBlock())) {
                                            level.setBlock(neighborPos, this.defaultBlockState(), 3);
                                            GooBlock.CONVERTED_COUNT.incrementAndGet();
                                            converted = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SPREAD_COOLDOWNS.put(pos, this.randomCooldown(level.random));
                    level.scheduleTick(pos, this, 1);
                }
            }
        }
    }

    private void spawnShock(ServerLevel level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        int intensity = 2 + random.nextInt(4);

        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 30 * intensity, 0.4, 0.4, 0.4, 0.2);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 20 * intensity, 0.5, 0.5, 0.5, 0.25);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10 * intensity, 0.4, 0.4, 0.4, 0.1);

        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 5.0F, 0.8F + random.nextFloat() * 0.4F);

        double radius = 3.0;
        AABB aabb = new AABB(pos).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        float damage = 10.0f;
        for (LivingEntity entity : entities) {
            entity.hurt(level.damageSources().lightningBolt(), damage);
        }
    }

    private boolean hasSculkNeighbor(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        checkPos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                        if (level.isLoaded(checkPos) && SCULK_BLOCKS.contains(level.getBlockState(checkPos).getBlock())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private int randomCooldown(RandomSource random) {
        // Lightning Phage is fast - uses fixed quick cooldowns instead of config
        return 4 + random.nextInt(5); // 4-8 ticks
    }

    private int randomDecayDelay(RandomSource random) {
        return ModConfig.DECAY_RANDOM_MIN.get() + random.nextInt(ModConfig.DECAY_RANDOM_MAX.get() - ModConfig.DECAY_RANDOM_MIN.get() + 1);
    }

    static {
        SCULK_BLOCKS = Set.of(Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN);
    }
}