package com.LeglessLizard.goomod.registry;

import com.LeglessLizard.goomod.GooMod;
import com.LeglessLizard.goomod.block.PhageDisruptorBlockEntity;
import com.LeglessLizard.goomod.block.UltimatePhageCoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GooMod.MOD_ID);

    public static final Supplier<BlockEntityType<PhageDisruptorBlockEntity>> PHAGE_DISRUPTOR_ENTITY =
            BLOCK_ENTITIES.register("phage_disruptor",
                    () -> BlockEntityType.Builder.of(PhageDisruptorBlockEntity::new,
                            ModBlocks.DISRUPTOR_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<UltimatePhageCoreBlockEntity>> ULTIMATE_PHAGE_CORE_ENTITY =
            BLOCK_ENTITIES.register("ultimate_phage_core",
                    () -> BlockEntityType.Builder.of(UltimatePhageCoreBlockEntity::new,
                            ModBlocks.ULTIMATE_PHAGE_CORE_BLOCK.get()).build(null));
}
