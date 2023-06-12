package fr.lanfix.simplehealthbar.mixins;

import fr.lanfix.simplehealthbar.overlays.HealthBar;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HealthBarMixin {

    private static final HealthBar healthBar = new HealthBar();

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void renderHealthBar(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        healthBar.render(matrices, tickDelta);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"), method = "renderStatusBars")
    public void disableVanillaHealthBar(InGameHud instance, MatrixStack matrices, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        // do nothing, vanilla health bar is not rendered anymore
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), method = "renderStatusBars")
    public int rowHeight(int a, int b) {
        // The height of a health bar is 10 at Math.max(10 - (q - 2), 3)
        // at Math.max(j, i) we want the renderer to think there is only one line of hearts, so we return 10
        return 10;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "renderStatusBars")
    public float fakeHealth(float a, float b) {
        // The renderer should there is only one health row so the armor is displayed at the right place
        return 20;
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAbsorptionAmount()F"))
    public float fakeAbsorption(PlayerEntity player) {
        // The renderer should think there is only one absorption row if the player has absorption
        // so the armor is displayed at the right place
        return (player.getAbsorptionAmount() > 0) ? 20 : 0;
    }
}
