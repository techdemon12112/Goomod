//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.registry;

import com.LeglessLizard.goomod.block.CleanPhageBlock;
import com.LeglessLizard.goomod.block.GooBlock;
import com.LeglessLizard.goomod.block.LightningPhageBlock;
import com.LeglessLizard.goomod.block.UltimatePhageCoreBlock;
import com.LeglessLizard.goomod.block.UltimatePhageSeekerBlock;
import com.LeglessLizard.goomod.block.MinersPhageBlock;
import com.LeglessLizard.goomod.block.PhageDisruptorBlock;
import com.LeglessLizard.goomod.block.PrimedGooBlock;
import com.LeglessLizard.goomod.block.VolatileGooBlock;
import com.LeglessLizard.goomod.block.PhilosophersPhageBlock;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("goomod");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("goomod");
    public static final DeferredBlock<Block> BASIC_PHAGE_BLOCK;
    public static final Supplier<Item> BASIC_PHAGE_ITEM;
    public static final DeferredBlock<Block> VOLATILE_PHAGE_BLOCK;
    public static final Supplier<Item> VOLATILE_PHAGE_ITEM;
    public static final DeferredBlock<Block> CLEAN_PHAGE_BLOCK;
    public static final Supplier<Item> CLEAN_PHAGE_ITEM;
    public static final DeferredBlock<Block> MINERS_PHAGE_BLOCK;
    public static final Supplier<Item> MINERS_PHAGE_ITEM;
    public static final DeferredBlock<Block> DISRUPTOR_BLOCK;
    public static final Supplier<Item> DISRUPTOR_ITEM;
    public static final DeferredBlock<Block> PRIMED_GOO_BLOCK;

    static {
        BASIC_PHAGE_BLOCK = BLOCKS.register("basic_phage", () -> new GooBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().isViewBlocking((state, level, pos) -> false)));
        BASIC_PHAGE_ITEM = ITEMS.register("basic_phage", () -> new BlockItem((Block)BASIC_PHAGE_BLOCK.get(), new Item.Properties()));
        VOLATILE_PHAGE_BLOCK = BLOCKS.register("volatile_phage", () -> new VolatileGooBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().isViewBlocking((state, level, pos) -> false)));
        VOLATILE_PHAGE_ITEM = ITEMS.register("volatile_phage", () -> new BlockItem((Block)VOLATILE_PHAGE_BLOCK.get(), new Item.Properties()));
        CLEAN_PHAGE_BLOCK = BLOCKS.register("clean_phage", () -> new CleanPhageBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().isViewBlocking((state, level, pos) -> false)));
        CLEAN_PHAGE_ITEM = ITEMS.register("clean_phage", () -> new BlockItem((Block)CLEAN_PHAGE_BLOCK.get(), new Item.Properties()));
        MINERS_PHAGE_BLOCK = BLOCKS.register("miners_phage", () -> new MinersPhageBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().isViewBlocking((state, level, pos) -> false)));
        MINERS_PHAGE_ITEM = ITEMS.register("miners_phage", () -> new BlockItem((Block)MINERS_PHAGE_BLOCK.get(), new Item.Properties()));
        DISRUPTOR_BLOCK = BLOCKS.register("phage_disruptor", () -> new PhageDisruptorBlock(Properties.of().strength(2.0F).sound(SoundType.STONE).noOcclusion()));
        DISRUPTOR_ITEM = ITEMS.register("phage_disruptor", () -> new BlockItem((Block)DISRUPTOR_BLOCK.get(), new Item.Properties()));
        PRIMED_GOO_BLOCK = BLOCKS.register("primed_goo", () -> new PrimedGooBlock(Properties.of().strength(0.0F).sound(SoundType.GRASS).noOcclusion().noLootTable().instabreak().ignitedByLava()));
    }

    // Philosopher's Phage
    public static final DeferredBlock<Block> PHILOSOPHERS_PHAGE_BLOCK = BLOCKS.register("philosophers_phage",
            () -> new PhilosophersPhageBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().isViewBlocking((a,b,c) -> false)));
    public static final Supplier<Item> PHILOSOPHERS_PHAGE_ITEM = ITEMS.register("philosophers_phage",
            () -> new BlockItem(PHILOSOPHERS_PHAGE_BLOCK.get(), new Item.Properties()));

    // Lightning Phage
    public static final DeferredBlock<Block> LIGHTNING_PHAGE_BLOCK = BLOCKS.register("lightning_phage",
            () -> new LightningPhageBlock(Properties.of().strength(0.5F).sound(SoundType.SLIME_BLOCK).noOcclusion().noLootTable().isViewBlocking((s,l,p) -> false)));
    public static final Supplier<Item> LIGHTNING_PHAGE_ITEM = ITEMS.register("lightning_phage",
            () -> new BlockItem(LIGHTNING_PHAGE_BLOCK.get(), new Item.Properties()));

    // Ultimate Phage Core
    public static final DeferredBlock<Block> ULTIMATE_PHAGE_CORE_BLOCK = BLOCKS.register("ultimate_phage_core",
            () -> new UltimatePhageCoreBlock(Properties.of().strength(1.0F).sound(SoundType.SCULK).noOcclusion().noLootTable()));
    public static final Supplier<Item> ULTIMATE_PHAGE_CORE_ITEM = ITEMS.register("ultimate_phage_core",
            () -> new BlockItem(ULTIMATE_PHAGE_CORE_BLOCK.get(), new Item.Properties()));

    // Ultimate Phage Seeker
    public static final DeferredBlock<Block> ULTIMATE_PHAGE_SEEKER_BLOCK = BLOCKS.register("ultimate_phage_seeker",
            () -> new UltimatePhageSeekerBlock(Properties.of().strength(1.0F).sound(SoundType.SCULK).noOcclusion().noLootTable()));
    public static final Supplier<Item> ULTIMATE_PHAGE_SEEKER_ITEM = ITEMS.register("ultimate_phage_seeker",
            () -> new BlockItem(ULTIMATE_PHAGE_SEEKER_BLOCK.get(), new Item.Properties()));
}
