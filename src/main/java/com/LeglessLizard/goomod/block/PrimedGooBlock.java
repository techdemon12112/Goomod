//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.block;

import com.LeglessLizard.goomod.entity.PrimedGooEffectEntity;
import com.LeglessLizard.goomod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PrimedGooBlock extends Block {
    public PrimedGooBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            PrimedGooEffectEntity effect = new PrimedGooEffectEntity((EntityType)ModEntities.PRIMED_GOO_EFFECT.get(), level);
            effect.setTarget(pos);
            effect.setPos((double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F);
            level.addFreshEntity(effect);

            for(Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (level.isEmptyBlock(neighbor) && !level.isClientSide()) {
                    level.setBlock(neighbor, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }

    }
}
