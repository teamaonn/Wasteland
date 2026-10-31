package com.joshua.wasteland.client;

import com.joshua.wasteland.*;
import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class WastelandClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(WastelandMod.DEATHCLAW, ctx -> new GeoEntityRenderer<>(ctx, new CreatureModel<Deathclaw>("deathclaw")));
        EntityRendererRegistry.register(NewCreatures.RAIDER,RaiderRenderer::new);
        EntityRendererRegistry.register(NewCreatures.GHOUL,ctx->new GeoEntityRenderer<>(ctx,new CreatureModel<Ghoul>("ghoul")));
        EntityRendererRegistry.register(NewCreatures.PROTECTRON,ctx->new GeoEntityRenderer<>(ctx,NewCreatures.PROTECTRON));
        EntityRendererRegistry.register(NewCreatures.MOTHMAN,ctx->new GeoEntityRenderer<>(ctx,NewCreatures.MOTHMAN));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused()) return;
            var player = client.player;
            if (player.hasEffect(CrowdControl.DAZED)) {
                // Change aim by an oscillating offset, with zero long-term drift.
                float t = player.tickCount;
                player.setYRot(player.getYRot() + (float)(1.3 * (Math.sin(t * 1.5) - Math.sin((t - 1) * 1.5))));
                player.setXRot(net.minecraft.util.Mth.clamp(player.getXRot() + (float)(.9 * (Math.cos(t * 1.7) - Math.cos((t - 1) * 1.7))), -90, 90));
            }
            if (player.hasEffect(CrowdControl.STUNNED)) {
                player.setSprinting(false);
                client.options.keyAttack.setDown(false); client.options.keyUse.setDown(false);
                client.options.keyJump.setDown(false); client.options.keyUp.setDown(false);
                client.options.keyDown.setDown(false); client.options.keyLeft.setDown(false); client.options.keyRight.setDown(false);
            }
        });
    }
}
