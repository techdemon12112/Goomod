//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.entity;
import com.LeglessLizard.goomod.registry.ModBlocks;

import com.LeglessLizard.goomod.block.VolatileGooBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PrimedGooEffectEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_TARGET;
    private int fuse = 100;

    public PrimedGooEffectEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setTarget(BlockPos pos) {
        this.entityData.set(DATA_TARGET, pos);
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, BlockPos.ZERO);
    }

    public void tick() {
        super.tick();
        BlockPos target = (BlockPos)this.entityData.get(DATA_TARGET);
        if (!this.level().isClientSide()) {
            --this.fuse;
            if (this.fuse <= 0) {
                if (this.level().getBlockState(target).getBlock() == ModBlocks.PRIMED_GOO_BLOCK.get()) {
                    this.level().removeBlock(target, false);
                }

                this.level().explode(this, (double)target.getX() + (double)0.5F, (double)target.getY() + (double)0.5F, (double)target.getZ() + (double)0.5F, 3.0F, ExplosionInteraction.BLOCK);
                this.flingVolatileGoo(target);
                this.discard();
            }
        } else if (this.tickCount % 5 == 0) {
            this.level().playLocalSound(target, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }

    }

    private void flingVolatileGoo(BlockPos pos) {
        int radius = 5;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for(int dx = -radius; dx <= radius; ++dx) {
            for(int dy = -radius; dy <= radius; ++dy) {
                for(int dz = -radius; dz <= radius; ++dz) {
                    checkPos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (this.level().isLoaded(checkPos)) {
                        BlockState nearState = this.level().getBlockState(checkPos);
                        if (nearState.getBlock() instanceof VolatileGooBlock) {
                            this.level().removeBlock(checkPos, false);
                            FallingBlockEntity falling = FallingBlockEntity.fall(this.level(), checkPos, nearState);
                            Vec3 dir = (new Vec3((double)(checkPos.getX() - pos.getX()), (double)(checkPos.getY() - pos.getY()), (double)(checkPos.getZ() - pos.getZ()))).normalize().scale((double)2.0F + this.random.nextDouble() * (double)2.0F);
                            falling.setDeltaMovement(dir);
                            this.level().addFreshEntity(falling);
                        }
                    }
                }
            }
        }

    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Target")) {
            int[] arr = tag.getIntArray("Target");
            this.entityData.set(DATA_TARGET, new BlockPos(arr[0], arr[1], arr[2]));
        }

        this.fuse = tag.getInt("Fuse");
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
        BlockPos p = (BlockPos)this.entityData.get(DATA_TARGET);
        tag.putIntArray("Target", new int[]{p.getX(), p.getY(), p.getZ()});
        tag.putInt("Fuse", this.fuse);
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    static {
        DATA_TARGET = SynchedEntityData.defineId(PrimedGooEffectEntity.class, EntityDataSerializers.BLOCK_POS);
    }
}
