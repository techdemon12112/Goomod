package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UltimatePhageCoreBlockEntity extends BlockEntity {
    public int totalConverted = 0;
    public int xpAccumulated = 0;
    public final List<ItemStack> collectedOres = new ArrayList<>();
    public boolean isReturning = false;
    public int phase = 0;
    public int phaseTicks = 0;
    public int projectileCooldown = 0;
    public int projectilesThisTick = 0;
    public int wardenAttackCooldown = 0;

    public final Set<BlockPos> linkedSeekers = new HashSet<>();
    public final List<PendingProjectile> pendingProjectiles = new ArrayList<>();
    public final List<WardenFlight> wardenFlights = new ArrayList<>();

    public Vec3 sphereCenter = Vec3.ZERO;
    public double sphereRadius = 0;
    public final List<Vec3> spherePoints = new ArrayList<>();
    public final List<BlockPos> seekerSnapshot = new ArrayList<>();
    public int nextSeekerIndex = 0;
    public final List<ParticleFlight> particleFlights = new ArrayList<>();
    public Vec3 slamFrom = Vec3.ZERO;
    public Vec3 slamTo = Vec3.ZERO;
    public int slamProgress = 0;
    public int slamTotalTicks = 0;

    public UltimatePhageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTIMATE_PHAGE_CORE_ENTITY.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            UltimatePhageCoreBlock.setChunkForced(serverLevel, worldPosition, true);
        }
    }

    public void addOre(ItemStack stack) {
        collectedOres.add(stack);
        setChanged();
    }

    public boolean isTargeted(BlockPos p) {
        for (PendingProjectile proj : pendingProjectiles) {
            if (proj.targetBlock.equals(p)) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("TotalConverted", totalConverted);
        tag.putInt("XpAccumulated", xpAccumulated);
        tag.putBoolean("IsReturning", isReturning);
        ListTag oreList = new ListTag();
        for (ItemStack stack : collectedOres) {
            CompoundTag oreTag = new CompoundTag();
            stack.save(provider, oreTag);
            oreList.add(oreTag);
        }
        tag.put("CollectedOres", oreList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        totalConverted = tag.getInt("TotalConverted");
        xpAccumulated = tag.getInt("XpAccumulated");
        isReturning = tag.getBoolean("IsReturning");
        collectedOres.clear();
        ListTag oreList = tag.getList("CollectedOres", Tag.TAG_COMPOUND);
        for (Tag t : oreList) {
            ItemStack.parse(provider, (CompoundTag)t).ifPresent(collectedOres::add);
        }
    }

    public static class PendingProjectile {
        public final Vec3 from;
        public final Vec3 to;
        public final BlockPos targetBlock;
        public final int totalTicks;
        public int elapsedTicks = 0;

        public PendingProjectile(Vec3 from, Vec3 to, BlockPos targetBlock, int totalTicks) {
            this.from = from;
            this.to = to;
            this.targetBlock = targetBlock;
            this.totalTicks = totalTicks;
        }
    }

    public static class WardenFlight {
        public Vec3 position;
        public Vec3 velocity;
        public final Warden target;
        public final int maxTicks;
        public int ticksElapsed = 0;

        public WardenFlight(Vec3 position, Vec3 velocity, Warden target, int maxTicks) {
            this.position = position;
            this.velocity = velocity;
            this.target = target;
            this.maxTicks = maxTicks;
        }
    }

    /** A single glowing clump flying from a seeker to a sphere slot. */
    public static class ParticleFlight {
        public final Vec3 from;
        public final Vec3 to;
        public final int totalTicks;
        public int startDelay;
        public int ticksElapsed = 0;
        public transient ShulkerBullet bullet;

        public ParticleFlight(Vec3 from, Vec3 to, int totalTicks, int startDelay) {
            this.from = from;
            this.to = to;
            this.totalTicks = totalTicks;
            this.startDelay = startDelay;
        }
    }
}