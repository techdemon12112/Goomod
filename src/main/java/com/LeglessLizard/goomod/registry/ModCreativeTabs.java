//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS;
    public static final Supplier<CreativeModeTab> PHAGE_TAB;

    static {
        CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "goomod");
        PHAGE_TAB = CREATIVE_MODE_TABS.register("phage_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.goomod.goo_tab")).icon(() -> new ItemStack((ItemLike)ModBlocks.BASIC_PHAGE_ITEM.get())).displayItems((params, output) -> {
            output.accept((ItemLike)ModBlocks.BASIC_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.CLEAN_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.MINERS_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.VOLATILE_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.PHILOSOPHERS_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.LIGHTNING_PHAGE_ITEM.get());
            output.accept((ItemLike)ModBlocks.ULTIMATE_PHAGE_CORE_ITEM.get());
            output.accept((ItemLike)ModBlocks.DISRUPTOR_ITEM.get());
        }).build());
    }
}
