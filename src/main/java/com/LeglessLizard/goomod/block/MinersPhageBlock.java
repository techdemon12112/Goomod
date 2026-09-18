//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.config.ModConfig;import com.LeglessLizard.goomod.GooMod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MinersPhageBlock extends Block {
    public static final Set<BlockPos> ACTIVE_PHAGES = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> SPREAD_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> ISOLATION_START = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> DECAY_DEADLINE = new ConcurrentHashMap<>();
    private static final Set<Block> SCULK_BLOCKS;

    public MinersPhageBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            ACTIVE_PHAGES.add(pos.immutable());
            SPREAD_COOLDOWNS.put(pos, this.randomCooldown(level.random));
            level.scheduleTick(pos, this, 1);
        }

    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            ACTIVE_PHAGES.remove(pos);
            SPREAD_COOLDOWNS.remove(pos);
            ISOLATION_START.remove(pos);
            DECAY_DEADLINE.remove(pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide()) {
            boolean hasNeighbor = this.hasSculkNeighbor(level, pos);
            long currentTick = level.getGameTime();
            if (!hasNeighbor) {
                ISOLATION_START.putIfAbsent(pos, currentTick);
                long start = ISOLATION_START.get(pos);
                long graceEnd = start + ModConfig.DECAY_GRACE_PERIOD.get();
                if (currentTick >= graceEnd) {
                    DECAY_DEADLINE.putIfAbsent(pos, currentTick + (long)this.randomDecayDelay(random));
                    Long deadline = DECAY_DEADLINE.get(pos);
                    if (deadline != null && currentTick >= deadline) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        if (level.getBrightness(LightLayer.BLOCK, pos) == 0) {
                            this.placeLightSource(level, pos);
                        }

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

                    for(int dx = -1; dx <= 1 && !converted; ++dx) {
                        for(int dy = -1; dy <= 1 && !converted; ++dy) {
                            for(int dz = -1; dz <= 1 && !converted; ++dz) {
                                if (dx != 0 || dy != 0 || dz != 0) {
                                    neighborPos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                                    if (level.isLoaded(neighborPos)) {
                                        BlockState neighbor = level.getBlockState(neighborPos);
                                        if (SCULK_BLOCKS.contains(neighbor.getBlock())) {
                                            level.setBlock(neighborPos, this.defaultBlockState(), 3);
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

    private void placeLightSource(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (level.isLoaded(below)) {
            BlockState belowState = level.getBlockState(below);
            if (belowState.isSolid() && !this.isPhage(belowState.getBlock())) {
                if (level.getBlockState(pos).getFluidState().isEmpty()) { if (!this.isPositionSurroundedByFluid(level, pos)) { level.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3); } }
                return;
            }
        }

        for(Direction dir : Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (level.isLoaded(neighbor)) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.isSolid() && !this.isPhage(neighborState.getBlock())) {
                    Direction facing = dir.getOpposite();
                    BlockState wallTorch = (BlockState)Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
                    if (level.getBlockState(pos).getFluidState().isEmpty()) { if (!this.isPositionSurroundedByFluid(level, pos)) { level.setBlock(pos, wallTorch, 3); } }
                    return;
                }
            }
        }

        if (level.isLoaded(below) && level.getBlockState(below).isSolid() && !this.isPhage(level.getBlockState(below).getBlock())) {
            if (level.getBlockState(pos).getFluidState().isEmpty()) { if (!this.isPositionSurroundedByFluid(level, pos)) { level.setBlock(pos, Blocks.LANTERN.defaultBlockState(), 3); } }
        } else {
            BlockPos above = pos.above();
            if (level.isLoaded(above)) {
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.isSolid() && !this.isPhage(aboveState.getBlock())) {
                    BlockState hangingLantern = (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
                    if (level.getBlockState(pos).getFluidState().isEmpty()) { if (!this.isPositionSurroundedByFluid(level, pos)) { level.setBlock(pos, hangingLantern, 3); } }
                }
            }

        }
    }

    private boolean isPhage(Block block) {
        return block instanceof GooBlock || block instanceof CleanPhageBlock || block instanceof MinersPhageBlock || block instanceof VolatileGooBlock || block instanceof PrimedGooBlock;
    }

    private boolean hasSculkNeighbor(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
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
        return ModConfig.SPREAD_MIN_DELAY.get() + random.nextInt(ModConfig.SPREAD_MAX_DELAY.get() - ModConfig.SPREAD_MIN_DELAY.get() + 1);
    }

    private int randomDecayDelay(RandomSource random) {
        return ModConfig.DECAY_RANDOM_MIN.get() + random.nextInt(ModConfig.DECAY_RANDOM_MAX.get() - ModConfig.DECAY_RANDOM_MIN.get() + 1);
    }

    static {
        SCULK_BLOCKS = Set.of(Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN);
    }

    private boolean isPositionSurroundedByFluid(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).getFluidState().isEmpty()) return true;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            BlockPos neighborPos = pos.relative(dir);
            if (level.isLoaded(neighborPos) && !level.getBlockState(neighborPos).getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }}
