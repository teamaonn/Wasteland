package com.joshua.wasteland.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.joshua.wasteland.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

final class CreatureModel<T extends Mob & GeoAnimatable> extends GeoModel<T> {
    private static final DataTicket<Integer> SKIN=DataTicket.create("wasteland_skin",Integer.class);
    private final String name;
    CreatureModel(String name) { this.name=name; }
    @Override public void addAdditionalStateData(T mob,Object related,GeoRenderState state) { state.addGeckolibData(SKIN,mob instanceof Appearance a?a.appearance():0); }
    private String model(int skin) { return name.equals("deathclaw") && skin>0?"deathclaw_nuka":name; }
    @Override public Identifier getModelResource(GeoRenderState state) { return WastelandMod.id("entity/"+model(state.getOrDefaultGeckolibData(SKIN,0))); }
    @Override public Identifier getAnimationResource(T mob) { return WastelandMod.id("entity/"+model(mob instanceof Appearance a?a.appearance():0)); }
    @Override public Identifier getTextureResource(GeoRenderState state) {
        int skin=state.getOrDefaultGeckolibData(SKIN,0);
        return WastelandMod.id("textures/entity/"+(name.equals("deathclaw")?(skin==0?"deathclaw":"deathclaw_nuka_"+skin):name.equals("ghoul")?"ghoul_"+skin:name)+".png");
    }
}
