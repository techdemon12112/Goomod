//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.registry;

import com.LeglessLizard.goomod.entity.PrimedGooEffectEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;
    public static final Supplier<EntityType<PrimedGooEffectEntity>> PRIMED_GOO_EFFECT;

    static {
        ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "goomod");
        PRIMED_GOO_EFFECT = ENTITY_TYPES.register("primed_goo_effect", () -> Builder.of(PrimedGooEffectEntity::new, MobCategory.MISC).sized(0.0F, 0.0F).clientTrackingRange(64).updateInterval(1).build("primed_goo_effect"));
    }
}
