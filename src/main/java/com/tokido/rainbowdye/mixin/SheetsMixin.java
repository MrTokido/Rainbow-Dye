package com.tokido.rainbowdye.mixin;

import com.tokido.rainbowdye.client.RainbowShulkerBoxRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While OUR renderer is submitting (ThreadLocal gate - no other shulker box in the
 * world is affected), swap the resolved sprite for our animated rainbow one. The
 * replacement reuses the atlas of the SpriteId vanilla just returned, so the atlas
 * id is always valid.
 *
 * Injected at RETURN so the original value is available to copy the atlas from.
 * require = 0: a rename in a future version means this silently does nothing and
 * the renderer's colour-stepping fallback takes over - never a crash.
 */
@Mixin(Sheets.class)
public class SheetsMixin {

    @Inject(method = "getShulkerBoxSprite", at = @At("RETURN"), cancellable = true, require = 0)
    private static void rainbowdye$rainbowSprite(DyeColor color, CallbackInfoReturnable<SpriteId> cir) {
        if (!RainbowShulkerBoxRenderer.isRainbowSubmitting()) {
            return;
        }
        SpriteId replacement = RainbowShulkerBoxRenderer.rainbowSpriteFrom(cir.getReturnValue());
        if (replacement != null) {
            cir.setReturnValue(replacement);
        }
    }
}
