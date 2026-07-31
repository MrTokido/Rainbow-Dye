package com.tokido.rainbowdye.client;

import com.tokido.rainbowdye.RainbowDye;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class RainbowDyeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Typed local sidesteps a javac inference failure: with a bare method
        // reference, inference tries to equate E with both ShulkerBoxBlockEntity
        // (from the renderer's generics) and RainbowShulkerBoxBlockEntity (from
        // the type argument) and gives up. Pinning the provider's generics makes
        // the `? super E` wildcard do its job.
        BlockEntityRendererProvider<ShulkerBoxBlockEntity, ShulkerBoxRenderState> provider =
                RainbowShulkerBoxRenderer::new;
        BlockEntityRendererRegistry.register(RainbowDye.RAINBOW_SHULKER_BOX_BLOCK_ENTITY, provider);
    }
}
