package com.tokido.rainbowdye.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * 26.2 validates a block entity against its type IN THE CONSTRUCTOR
 * (BlockEntity.validateBlockState). Our BE subclass necessarily runs
 * ShulkerBoxBlockEntity's super constructor, which hardcodes the VANILLA
 * shulker type - so the vanilla type's validBlocks set must contain our
 * block or construction throws IllegalStateException on placement.
 *
 * Our own BlockEntityType (see RainbowShulkerBoxBlockEntity) still owns
 * rendering suppression and the save id; this mixin only exists to get
 * past the super-constructor check.
 *
 * Field name "validBlocks" verified against Fabric API's own accessor
 * (fabric-api-lookup-api-v1) on the 26.2 branch.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {

    @Accessor("validBlocks")
    Set<Block> rainbowdye$getValidBlocks();

    @Mutable
    @Accessor("validBlocks")
    void rainbowdye$setValidBlocks(Set<Block> validBlocks);
}
