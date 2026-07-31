package com.tokido.rainbowdye;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same behaviour as a vanilla shulker box block entity, but registered under its own
 * BlockEntityType. That single fact does two jobs:
 *
 * 1. The vanilla ShulkerBoxRenderer is registered against the VANILLA type, so it no
 *    longer renders our box at all. This kills the "purple shulker under a rainbow
 *    coat" double-render (and the purple lid popping out on open) - the renderer was
 *    drawing the default uncoloured shulker on top of our block model.
 *
 * 2. The type's valid-blocks set is just our block, so the chunk-load validity check
 *    passes natively - the BlockEntityType mixin from 1.1.0 is deleted entirely.
 */
public class RainbowShulkerBoxBlockEntity extends ShulkerBoxBlockEntity {

    public RainbowShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return RainbowDye.RAINBOW_SHULKER_BOX_BLOCK_ENTITY;
    }

    @Override
    protected Component getDefaultName() {
        // GUI title: "Rainbow Shulker Box" instead of vanilla's "Shulker Box"
        return Component.translatable("container.rainbowdye.rainbow_shulker_box");
    }
}
