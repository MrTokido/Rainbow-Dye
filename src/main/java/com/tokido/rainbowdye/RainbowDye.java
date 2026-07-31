package com.tokido.rainbowdye;

import com.tokido.rainbowdye.mixin.BlockEntityTypeAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class RainbowDye implements ModInitializer {

    public static final String MOD_ID = "rainbowdye";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // ------------------------------------------------------------------ block
    public static final ResourceKey<Block> RAINBOW_SHULKER_BOX_KEY =
            ResourceKey.create(Registries.BLOCK, id("rainbow_shulker_box"));

    public static final Block RAINBOW_SHULKER_BOX = registerBlock(
            RAINBOW_SHULKER_BOX_KEY,
            RainbowShulkerBoxBlock::new,
            // copy vanilla shulker box properties so hardness / sounds / behaviour match
            BlockBehaviour.Properties.ofFullCopy(Blocks.SHULKER_BOX)
    );

    public static final BlockEntityType<RainbowShulkerBoxBlockEntity> RAINBOW_SHULKER_BOX_BLOCK_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("rainbow_shulker_box"),
                    FabricBlockEntityTypeBuilder.create(RainbowShulkerBoxBlockEntity::new, RAINBOW_SHULKER_BOX).build());

    // ------------------------------------------------------------------ items
    public static final Item RAINBOW_DYE = registerItem(
            "rainbow_dye",
            RainbowDyeItem::new,
            // RARE renders the item name in aqua/cyan.
            // UNCOMMON = yellow, EPIC = light purple, COMMON = white.
            new Item.Properties().rarity(Rarity.RARE)
    );

    public static final Item RAINBOW_SHULKER_BOX_ITEM = registerItem(
            "rainbow_shulker_box",
            properties -> new BlockItem(RAINBOW_SHULKER_BOX, properties),
            // EPIC renders the item name in light purple.
            new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
    );

    @Override
    public void onInitialize() {
        // 26.2 validates block entities against their type in the CONSTRUCTOR.
        // Our BE runs the vanilla shulker super constructor, which hardcodes the
        // vanilla type - widen that type's valid-blocks set or placement crashes
        // with "Invalid block entity minecraft:shulker_box ... state".
        widenVanillaShulkerValidBlocks(RAINBOW_SHULKER_BOX);

        // Creative tabs (fabric-creative-tab-api-v1, the 26.x replacement for
        // the old ItemGroupEvents API)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(content -> content.accept(RAINBOW_DYE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(content -> content.accept(RAINBOW_SHULKER_BOX_ITEM));

        LOGGER.info("[Rainbow Dye] loaded");
    }

    private static void widenVanillaShulkerValidBlocks(Block block) {
        BlockEntityTypeAccessor accessor = (BlockEntityTypeAccessor) BlockEntityTypes.SHULKER_BOX;
        Set<Block> widened = new HashSet<>(accessor.rainbowdye$getValidBlocks());
        widened.add(block);
        accessor.rainbowdye$setValidBlocks(widened);
    }

    // --------------------------------------------------------------- helpers
    private static Block registerBlock(ResourceKey<Block> key,
                                       Function<BlockBehaviour.Properties, Block> factory,
                                       BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static Item registerItem(String name,
                                     Function<Item.Properties, Item> factory,
                                     Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /** True if the given state is our rainbow box (used by the dye to avoid re-dyeing). */
    public static boolean isRainbowBox(BlockState state) {
        return state.is(RAINBOW_SHULKER_BOX);
    }
}
