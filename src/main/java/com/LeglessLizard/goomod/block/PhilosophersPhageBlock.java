package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.config.ModConfig;
import com.LeglessLizard.goomod.GooMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PhilosophersPhageBlock extends Block {
    public static final Set<BlockPos> ACTIVE_PHAGES = ConcurrentHashMap.newKeySet();

    private static final Map<BlockPos, Integer> SPREAD_COOLDOWNS = new HashMap<>();
    private static final Map<BlockPos, Long> ISOLATION_START = new HashMap<>();
    private static final Map<BlockPos, Long> DECAY_DEADLINE = new HashMap<>();

    private static final Map<Block, Double> ORE_BASE_CHANCES = Map.ofEntries(
        Map.entry(Blocks.EMERALD_ORE, 0.6), Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 0.6),
        Map.entry(Blocks.DIAMOND_ORE, 0.6), Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 0.6),
        Map.entry(Blocks.IRON_ORE, 0.8), Map.entry(Blocks.DEEPSLATE_IRON_ORE, 0.8),
        Map.entry(Blocks.GOLD_ORE, 0.8), Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 0.8),
        Map.entry(Blocks.REDSTONE_ORE, 0.9), Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 0.9),
        Map.entry(Blocks.COAL_ORE, 0.9), Map.entry(Blocks.DEEPSLATE_COAL_ORE, 0.9),
        Map.entry(Blocks.COPPER_ORE, 0.9), Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 0.9)
    );
    
    
    

    private static final Set<Block> SCULK_BLOCKS = Set.of(
            Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR,
            Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN
    );

    public PhilosophersPhageBlock(Properties props) {
        super(props);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide()) return;
        ACTIVE_PHAGES.add(pos.immutable());
        SPREAD_COOLDOWNS.put(pos, randomCooldown(level.random));
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level.isClientSide()) return;
        ACTIVE_PHAGES.remove(pos);
        SPREAD_COOLDOWNS.remove(pos);
        ISOLATION_START.remove(pos);
        DECAY_DEADLINE.remove(pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide()) return;

        if (!hasSculkNeighbor(level, pos)) {
            long now = level.getGameTime();
            ISOLATION_START.putIfAbsent(pos, now);
            long start = ISOLATION_START.get(pos);
            if (now >= start + ModConfig.DECAY_GRACE_PERIOD.get()) {
                DECAY_DEADLINE.putIfAbsent(pos, now + randomDecayDelay(random));
                Long deadline = DECAY_DEADLINE.get(pos);
                if (deadline != null && now >= deadline) {
                    if (!tryDuplicateOre(level, pos, random)) {
                        level.removeBlock(pos, false);
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
            return;
        }

        Integer cooldown = SPREAD_COOLDOWNS.getOrDefault(pos, 0);
        if (cooldown > 0) {
            SPREAD_COOLDOWNS.put(pos, cooldown - 1);
            level.scheduleTick(pos, this, 1);
            return;
        }

        BlockPos target = findSculkToSpread(level, pos, random);
        if (target != null) {
            level.setBlock(target, this.defaultBlockState(), Block.UPDATE_ALL);
        }

        SPREAD_COOLDOWNS.put(pos, randomCooldown(level.random));
        level.scheduleTick(pos, this, 1);
    }

    private boolean tryDuplicateOre(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos n : getNeighbors(pos)) {
            if (level.isLoaded(n) && isOre(level.getBlockState(n))) {
                ores.add(n.immutable());
            }
        }
        if (ores.isEmpty()) return false;

        Collections.shuffle(ores, new java.util.Random(random.nextLong()));
        VeinTracker tracker = VeinTracker.get(level);

        for (BlockPos orePos : ores) {
            if (tracker.isPlayerPlaced(orePos)) continue;

            BlockState oreState = level.getBlockState(orePos);
            int veinId = tracker.getVeinId(orePos);
            if (veinId == -1) {
                veinId = registerVein(level, orePos, oreState);
                if (veinId == -1) continue;
            }
            int dupCount = tracker.getDuplicationCount(veinId);
            int veinSize = tracker.getVeinSize(veinId);
            double base = getOreBaseChance(oreState.getBlock());
            double chance = base / (1.0 + dupCount * getDuplicationDecay() + veinSize * getSizeFactor());
            if (random.nextDouble() < chance) {
                level.setBlock(pos, oreState, Block.UPDATE_ALL);
                tracker.incrementDuplicationCount(veinId);
                return true;
            }
        }
        return false;
    }

    private int registerVein(ServerLevel level, BlockPos start, BlockState oreState) {
        Set<BlockPos> vein = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        vein.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos n : getNeighbors(current)) {
                if (!vein.contains(n) && level.isLoaded(n) && level.getBlockState(n).equals(oreState)) {
                    vein.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        if (vein.size() < 2) return -1;

        VeinTracker tracker = VeinTracker.get(level);
        int veinId = tracker.getNextVeinId();
        for (BlockPos p : vein) {
            tracker.registerOre(p, veinId);
        }
        tracker.registerVeinSize(veinId, vein.size());   // <-- store the vein size
        return veinId;
    }

    // ... rest of methods unchanged (findSculkToSpread, hasSculkNeighbor, getNeighbors, isOre, randoms) ...
    private BlockPos findSculkToSpread(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos n : getNeighbors(pos)) {
            if (level.isLoaded(n) && SCULK_BLOCKS.contains(level.getBlockState(n).getBlock())) {
                candidates.add(n.immutable());
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean hasSculkNeighbor(ServerLevel level, BlockPos pos) {
        for (BlockPos n : getNeighbors(pos)) {
            if (level.isLoaded(n) && SCULK_BLOCKS.contains(level.getBlockState(n).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    list.add(pos.offset(dx, dy, dz));
                }
            }
        }
        return list;
    }

    private boolean isOre(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE ||
               block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE ||
               block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
               block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.NETHER_GOLD_ORE;
    }

    private int randomCooldown(RandomSource random) { return ModConfig.SPREAD_MIN_DELAY.get() + random.nextInt(ModConfig.SPREAD_MAX_DELAY.get() - ModConfig.SPREAD_MIN_DELAY.get() + 1); }
    private int randomDecayDelay(RandomSource random) { return ModConfig.DECAY_RANDOM_MIN.get() + random.nextInt(ModConfig.DECAY_RANDOM_MAX.get() - ModConfig.DECAY_RANDOM_MIN.get() + 1); }

    private static double getOreBaseChance(Block block) {
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE ||
            block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return ModConfig.PPHAGE_DIAMOND_EMERALD_CHANCE.get();
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
                   block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return ModConfig.PPHAGE_IRON_GOLD_CHANCE.get();
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE ||
                   block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
                   block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return ModConfig.PPHAGE_REDSTONE_COAL_COPPER_CHANCE.get();
        } else {
            return ModConfig.PPHAGE_NETHER_MODDED_CHANCE.get();
        }
    }

    private static double getDuplicationDecay() {
        return ModConfig.PPHAGE_DUPLICATION_DECAY.get();
    }

    private static double getSizeFactor() {
        return ModConfig.PPHAGE_SIZE_FACTOR.get();
    }
}
