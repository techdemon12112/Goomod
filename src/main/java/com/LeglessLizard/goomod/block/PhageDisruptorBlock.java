package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PhageDisruptorBlock extends Block implements EntityBlock {

    public static boolean GLOBAL_COMMAND_DISRUPT = false;
    public static final Set<BlockPos> DISRUPTOR_POSITIONS = ConcurrentHashMap.newKeySet();

    public PhageDisruptorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhageDisruptorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PhageDisruptorBlockEntity disruptor) {
            if (!disruptor.hasStar() && stack.is(Items.NETHER_STAR)) {
                stack.shrink(1);
                disruptor.insertStar();
                level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 0.5F);
                return ItemInteractionResult.SUCCESS;
            } else if (disruptor.hasStar()) {
                ItemStack star = disruptor.extractStar();
                if (!player.getInventory().add(star)) {
                    player.drop(star, false);
                }
                level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 1.0F, 1.5F);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide()) {
            if (ModConfig.ONLY_ONE_DISRUPTOR.get() && !DISRUPTOR_POSITIONS.isEmpty()) {
                popResource(level, pos, new ItemStack(this));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            DISRUPTOR_POSITIONS.add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide()) {
            DISRUPTOR_POSITIONS.remove(pos);
            if (!state.is(newState.getBlock())) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PhageDisruptorBlockEntity disruptor && disruptor.hasStar()) {
                    popResource(level, pos, disruptor.extractStar());
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static void tickDisruptor(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        if (PhageDisruptorBlockEntity.getActiveCount() <= 0 && !GLOBAL_COMMAND_DISRUPT) return;

        // Gather all phage positions from the active sets (global)
        Set<BlockPos> all = ConcurrentHashMap.newKeySet();
        all.addAll(GooBlock.ACTIVE_PHAGES);
        all.addAll(VolatileGooBlock.ACTIVE_PHAGES);
        all.addAll(CleanPhageBlock.ACTIVE_PHAGES);
        all.addAll(MinersPhageBlock.ACTIVE_PHAGES);
        all.addAll(PhilosophersPhageBlock.ACTIVE_PHAGES);
        all.addAll(LightningPhageBlock.ACTIVE_PHAGES);
        all.addAll(UltimatePhageCoreBlock.ACTIVE_CORES);
        all.addAll(UltimatePhageSeekerBlock.ACTIVE_SEEKERS);

        for (ServerLevel world : level.getServer().getAllLevels()) {
            for (BlockPos p : all) {
                if (world.isLoaded(p)) {
                    BlockState bs = world.getBlockState(p);
                    if (bs.getBlock() instanceof GooBlock || bs.getBlock() instanceof VolatileGooBlock ||
                        bs.getBlock() instanceof CleanPhageBlock || bs.getBlock() instanceof MinersPhageBlock ||
                        bs.getBlock() instanceof PrimedGooBlock || bs.getBlock() instanceof PhilosophersPhageBlock || bs.getBlock() instanceof LightningPhageBlock || bs.getBlock() instanceof UltimatePhageCoreBlock || bs.getBlock() instanceof UltimatePhageSeekerBlock) {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static void toggleCommandDisrupt() {
        GLOBAL_COMMAND_DISRUPT = !GLOBAL_COMMAND_DISRUPT;
    }

    public static void disruptOnce(ServerLevel level) {
        Set<BlockPos> all = ConcurrentHashMap.newKeySet();
        all.addAll(GooBlock.ACTIVE_PHAGES);
        all.addAll(VolatileGooBlock.ACTIVE_PHAGES);
        all.addAll(CleanPhageBlock.ACTIVE_PHAGES);
        all.addAll(MinersPhageBlock.ACTIVE_PHAGES);
        all.addAll(PhilosophersPhageBlock.ACTIVE_PHAGES);
        all.addAll(LightningPhageBlock.ACTIVE_PHAGES);
        all.addAll(UltimatePhageCoreBlock.ACTIVE_CORES);
        all.addAll(UltimatePhageSeekerBlock.ACTIVE_SEEKERS);
        for (ServerLevel world : level.getServer().getAllLevels()) {
            for (BlockPos p : all) {
                if (world.isLoaded(p)) {
                    BlockState bs = world.getBlockState(p);
                    if (bs.getBlock() instanceof GooBlock || bs.getBlock() instanceof VolatileGooBlock ||
                        bs.getBlock() instanceof CleanPhageBlock || bs.getBlock() instanceof MinersPhageBlock ||
                        bs.getBlock() instanceof PrimedGooBlock || bs.getBlock() instanceof PhilosophersPhageBlock || bs.getBlock() instanceof LightningPhageBlock || bs.getBlock() instanceof UltimatePhageCoreBlock || bs.getBlock() instanceof UltimatePhageSeekerBlock) {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static int countDisruptors(net.minecraft.server.MinecraftServer server) {
        return DISRUPTOR_POSITIONS.size();
    }
}
