package fr.lanfix.simplehealthbar.mixins;

import fr.lanfix.simplehealthbar.overlays.HealthBar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HealthBarMixin {
    @Shadow private int ticks;
    @Unique
    private static final HealthBar healthBar = new HealthBar();

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"), method = "renderStatusBars")
    public void renderHealthBar(InGameHud instance, DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        HealthBar.setTicks(ticks);
        healthBar.render(context);
    }

//    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"), method = "renderStatusBars")
//    public void disableVanillaHealthBar(InGameHud instance, DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
//        // Do nothing, vanilla health bar is not rendered anymore
//    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), method = "renderStatusBars")
    public int rowHeight(int a, int b) {
        // The height of a health bar is 10 at Math.max(10 - (q - 2), 3)
        // We want the renderer to think there's only one bar, but we set the height to 5
        // as explained under fakeHealth and fakeAbsorption.
        return 5;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "renderStatusBars")
    public float fakeHealth(float a, float b) {
        // We trick the renderer into thinking there are two health bars in order to make space for
        // the health text under the armor bar. Each health bar is at half height because of rowHeight being 5,
        // so the height in total is 10 (first) + 5 (second).
        return 40;
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAbsorptionAmount()F"))
    public float fakeAbsorption(PlayerEntity player) {
        // The renderer should think we have two absorption bars to add another 10 pixels to the
        // bar height, making space for the absorption bar + its value text.
        return (player.getAbsorptionAmount() > 0) ? 40 : 0;
    }
}
