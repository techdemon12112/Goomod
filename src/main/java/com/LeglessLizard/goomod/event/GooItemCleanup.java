//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.event;
import com.LeglessLizard.goomod.registry.ModBlocks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class GooItemCleanup {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        for(ServerLevel world : event.getServer().getAllLevels()) {
            for(ItemEntity item : world.getEntities(EntityTypeTest.forClass(ItemEntity.class), (it) -> {
                ItemStack i = it.getItem();
                return i.is((Item)ModBlocks.BASIC_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.VOLATILE_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.CLEAN_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.MINERS_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.PHILOSOPHERS_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.LIGHTNING_PHAGE_ITEM.get()) || i.is((Item)ModBlocks.ULTIMATE_PHAGE_CORE_ITEM.get()) || i.is((Item)ModBlocks.ULTIMATE_PHAGE_SEEKER_ITEM.get()) || i.is((Item)ModBlocks.DISRUPTOR_ITEM.get());
            })) {
                world.sendParticles(ParticleTypes.POOF, item.getX(), item.getY(), item.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
                item.discard();
            }
        }

    }
}
