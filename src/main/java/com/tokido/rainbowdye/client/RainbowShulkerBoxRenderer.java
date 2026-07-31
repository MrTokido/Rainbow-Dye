package com.tokido.rainbowdye.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokido.rainbowdye.RainbowDye;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

/**
 * Vanilla shulker renderer, with the texture redirected to our animated rainbow.
 *
 * Two layers, and the second is a safety net for the first:
 *
 *  1. SheetsMixin swaps the colour->sprite resolution while this renderer is
 *     submitting, giving a smooth interpolated gradient. The replacement SpriteId
 *     is built from VANILLA'S OWN atlas id (copied off the SpriteId vanilla just
 *     returned) rather than a guessed constant - guessing produced
 *     "Invalid atlas texture id: minecraft:shulker_boxes".
 *
 *  2. If the mixin never fires, the DyeColor field on the render state is stepped
 *     through the 16 wool colours, 25 ticks each: a literal jeb_ cycle.
 */
public class RainbowShulkerBoxRenderer extends ShulkerBoxRenderer {

    /** Sprite name inside the shulker atlas: rainbowdye:entity/shulker/rainbow */
    public static final Identifier RAINBOW_TEXTURE = RainbowDye.id("entity/shulker/rainbow");

    private static final DyeColor[] JEB_ORDER = DyeColor.values();
    private static final ThreadLocal<Boolean> SUBMITTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static SpriteId rainbowSprite;      // built once from a real vanilla SpriteId
    private static boolean atlasResolveFailed;

    private static boolean searched;
    private static Field colorField;

    public RainbowShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /** True only while THIS renderer is inside super.submit on the current thread. */
    public static boolean isRainbowSubmitting() {
        return SUBMITTING.get();
    }

    /**
     * Build (once) a SpriteId pointing at our rainbow texture, reusing the atlas of
     * a SpriteId vanilla produced. Returns null if the atlas can't be read, in which
     * case the caller leaves vanilla's value alone.
     */
    public static SpriteId rainbowSpriteFrom(SpriteId vanillaSprite) {
        if (rainbowSprite != null) {
            return rainbowSprite;
        }
        if (atlasResolveFailed || vanillaSprite == null) {
            return null;
        }
        Identifier atlas = extractAtlas(vanillaSprite);
        if (atlas == null) {
            atlasResolveFailed = true;
            RainbowDye.LOGGER.warn("[Rainbow Dye] Could not read atlas id from {} - falling back to colour stepping",
                    vanillaSprite.getClass().getName());
            return null;
        }
        rainbowSprite = new SpriteId(atlas, RAINBOW_TEXTURE);
        RainbowDye.LOGGER.info("[Rainbow Dye] Rainbow sprite bound to atlas {}", atlas);
        return rainbowSprite;
    }

    /** SpriteId holds (atlas, texture); find the atlas component whatever it's called. */
    private static Identifier extractAtlas(SpriteId sprite) {
        try {
            RecordComponent[] comps = SpriteId.class.getRecordComponents();
            if (comps != null) {
                for (RecordComponent rc : comps) {
                    if (rc.getType() == Identifier.class && !rc.getName().toLowerCase().contains("texture")) {
                        Method acc = rc.getAccessor();
                        acc.setAccessible(true);
                        return (Identifier) acc.invoke(sprite);
                    }
                }
            }
        } catch (Exception ignored) { }
        try {
            for (Field f : SpriteId.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())
                        && f.getType() == Identifier.class
                        && !f.getName().toLowerCase().contains("texture")) {
                    f.setAccessible(true);
                    return (Identifier) f.get(sprite);
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Override
    public void submit(ShulkerBoxRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector,
                       CameraRenderState camera) {
        SUBMITTING.set(Boolean.TRUE);
        try {
            super.submit(state, poseStack, collector, camera);
        } finally {
            SUBMITTING.set(Boolean.FALSE);
        }
    }

    @Override
    public void extractRenderState(ShulkerBoxBlockEntity blockEntity,
                                   ShulkerBoxRenderState state,
                                   float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        stepColour(state);
    }

    /** Fallback path: cycle the state's DyeColor like a jeb_ sheep. */
    private static void stepColour(ShulkerBoxRenderState state) {
        if (!searched) {
            searched = true;
            outer:
            for (Class<?> c = state.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) && f.getType() == DyeColor.class) {
                        try {
                            f.setAccessible(true);
                            colorField = f;
                            RainbowDye.LOGGER.info("[Rainbow Dye] Colour field: {}.{}", c.getSimpleName(), f.getName());
                            break outer;
                        } catch (Exception ignored) { }
                    }
                }
            }
        }
        if (colorField == null) {
            return;
        }
        try {
            long time = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime()
                    : System.currentTimeMillis() / 50L;
            colorField.set(state, JEB_ORDER[(int) ((time / 25L) % JEB_ORDER.length)]);
        } catch (ReflectiveOperationException e) {
            colorField = null;
            RainbowDye.LOGGER.warn("[Rainbow Dye] Colour stepping failed", e);
        }
    }
}
