//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.LeglessLizard.goomod.client;

import com.LeglessLizard.goomod.entity.PrimedGooEffectEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PrimedGooEffectRenderer extends EntityRenderer<PrimedGooEffectEntity> {
    public PrimedGooEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public ResourceLocation getTextureLocation(PrimedGooEffectEntity entity) {
        return null;
    }

    public boolean shouldRender(PrimedGooEffectEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }
}
