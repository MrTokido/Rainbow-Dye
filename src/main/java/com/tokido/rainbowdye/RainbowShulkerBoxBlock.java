package com.tokido.rainbowdye;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behaves exactly like a vanilla shulker box. As of 1.4.0 it also RENDERS like one:
 * a client renderer (RainbowShulkerBoxRenderer) draws the real shulker model with
 * lid animation using our animated rainbow sprite, so this block no longer supplies
 * a static model - render shape stays the vanilla (invisible) one and the block
 * entity renderer owns all visuals.
 *
 * The ticker below is what makes the lid actually move and the hitbox grow: vanilla
 * ShulkerBoxBlock only serves a ticker to the VANILLA block entity type, so our own
 * type must wire ShulkerBoxBlockEntity::tick itself (signature verified against
 * BetterBlockEntities' 26.2 source, which injects into exactly this method).
 */
public class RainbowShulkerBoxBlock extends ShulkerBoxBlock {

    public RainbowShulkerBoxBlock(Properties properties) {
        super(null, properties); // null colour = "uncoloured" shulker box
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RainbowShulkerBoxBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Vanilla's getTicker only matches BlockEntityTypes.SHULKER_BOX; serve the
        // same static tick for our own type so open/close progress advances.
        return type == RainbowDye.RAINBOW_SHULKER_BOX_BLOCK_ENTITY
                ? (BlockEntityTicker<T>) (BlockEntityTicker<ShulkerBoxBlockEntity>) ShulkerBoxBlockEntity::tick
                : null;
    }
}
