package com.LeglessLizard.goomod.client;

import com.LeglessLizard.goomod.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber({Dist.CLIENT})
public class ClientEvents {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == ModBlocks.BASIC_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.basic_phage"));
        } else if (item == ModBlocks.VOLATILE_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.volatile_phage"));
        } else if (item == ModBlocks.CLEAN_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.clean_phage"));
        } else if (item == ModBlocks.MINERS_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.miners_phage"));
        } else if (item == ModBlocks.PHILOSOPHERS_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.philosophers_phage"));
        } else if (item == ModBlocks.LIGHTNING_PHAGE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.lightning_phage"));
        } else if (item == ModBlocks.ULTIMATE_PHAGE_CORE_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.ultimate_phage_core"));
        } else if (item == ModBlocks.DISRUPTOR_ITEM.get()) {
            event.getToolTip().add(Component.translatable("tooltip.goomod.phage_disruptor"));
        }
    }
}
