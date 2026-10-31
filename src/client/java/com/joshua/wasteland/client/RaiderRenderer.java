package com.joshua.wasteland.client;

import com.joshua.wasteland.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Human poses with vanilla held-item and armor rendering. */
final class RaiderRenderer extends HumanoidMobRenderer<Raider,RaiderRenderer.State,HumanoidModel<RaiderRenderer.State>> {
    static final class State extends HumanoidRenderState { int skin; }
    RaiderRenderer(EntityRendererProvider.Context context) {
        super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),.5F);
        addLayer(new HumanoidArmorLayer<>(this,ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR,context.getModelSet(),HumanoidModel<State>::new),context.getEquipmentRenderer()));
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(Raider mob,State state,float partialTick) { super.extractRenderState(mob,state,partialTick);state.skin=mob.appearance(); }
    @Override public Identifier getTextureLocation(State state) { return WastelandMod.id("textures/entity/raider_"+state.skin+".png"); }
}
