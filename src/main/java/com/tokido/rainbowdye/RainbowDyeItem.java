package com.tokido.rainbowdye;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately NOT a DyeItem.
 *
 * Extending DyeItem would automatically wire this into every vanilla dye interaction:
 * sheep, wool crafting, beds, candles, banners, signs, collars, the lot. We only want
 * shulker boxes, so this is a plain Item that implements exactly one interaction.
 */
public class RainbowDyeItem extends Item {

    public RainbowDyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Only vanilla-family shulker boxes, and not one we already converted.
        if (!(state.getBlock() instanceof ShulkerBoxBlock) || RainbowDye.isRainbowBox(state)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // ---- snapshot the old box -----------------------------------------
        List<ItemStack> contents = new ArrayList<>();
        Component customName = null;

        if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity oldBox) {
            for (int i = 0; i < oldBox.getContainerSize(); i++) {
                contents.add(oldBox.getItem(i).copy());
            }
            customName = oldBox.getCustomName();
            // Clear before replacing so nothing can be scattered or duplicated.
            oldBox.clearContent();
        }

        Direction facing = state.hasProperty(ShulkerBoxBlock.FACING)
                ? state.getValue(ShulkerBoxBlock.FACING)
                : Direction.UP;

        // ---- swap in the rainbow box -------------------------------------
        BlockState rainbow = RainbowDye.RAINBOW_SHULKER_BOX.defaultBlockState()
                .setValue(ShulkerBoxBlock.FACING, facing);
        level.setBlock(pos, rainbow, Block.UPDATE_ALL);

        if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newBox) {
            // Feed contents + name through the item-component path. BaseContainerBlockEntity
            // reads CONTAINER and CUSTOM_NAME back out of the stack in
            // applyComponentsFromItemStack, which is public - verified on 26.2, unlike
            // any direct name setter.
            ItemStack carrier = new ItemStack(RainbowDye.RAINBOW_SHULKER_BOX_ITEM);
            carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            if (customName != null) {
                carrier.set(DataComponents.CUSTOM_NAME, customName);
            }
            newBox.applyComponentsFromItemStack(carrier);
            newBox.setChanged();
        }

        // ---- feedback -----------------------------------------------------
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    12, 0.35, 0.35, 0.35, 0.0);
        }

        Player player = context.getPlayer();
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
