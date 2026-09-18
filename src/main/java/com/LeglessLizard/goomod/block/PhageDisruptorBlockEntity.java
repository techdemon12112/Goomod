package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PhageDisruptorBlockEntity extends BlockEntity {
    private static int ACTIVE_COUNT = 0;
    private static int TOTAL_COUNT = 0;
    private ItemStack storedStar = ItemStack.EMPTY;
    private boolean active = false;

    public PhageDisruptorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHAGE_DISRUPTOR_ENTITY.get(), pos, state);
        TOTAL_COUNT++;
        TOTAL_COUNT++;
    }

    public boolean hasStar() {
        return !storedStar.isEmpty();
    }

    public void insertStar() {
        storedStar = new ItemStack(Items.NETHER_STAR);
        active = true;
        ACTIVE_COUNT++;
        setChanged();
    }

    public ItemStack extractStar() {
        ItemStack star = storedStar.copy();
        storedStar = ItemStack.EMPTY;
        active = false;
        ACTIVE_COUNT = Math.max(0, ACTIVE_COUNT - 1);
        setChanged();
        return star;
    }

    public boolean isActive() {
        return active && hasStar();
    }

    public static int getTotalCount() { return TOTAL_COUNT; }


    public static int getActiveCount() {
        return ACTIVE_COUNT;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!storedStar.isEmpty()) {
            CompoundTag starTag = new CompoundTag();
            storedStar.save(provider, starTag);
            tag.put("Star", starTag);
        }
        tag.putBoolean("Active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Star")) {
            storedStar = ItemStack.parse(provider, tag.getCompound("Star")).orElse(ItemStack.EMPTY);
        }
        active = tag.getBoolean("Active");
        if (active && hasStar()) {
            ACTIVE_COUNT++;
        }
    }

    @Override
    public void setRemoved() {
        TOTAL_COUNT--;
        TOTAL_COUNT--;
        if (active && hasStar()) {
            ACTIVE_COUNT = Math.max(0, ACTIVE_COUNT - 1);
        }
        super.setRemoved();
    }
}
