package fr.lanfix.simplehealthbar.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.lanfix.simplehealthbar.overlays.HealthBar;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.minecraft.client.gui.DrawableHelper.GUI_ICONS_TEXTURE;

@Mixin(InGameHud.class)
public class HealthBarMixin {

    @Unique
    private static final HealthBar healthBar = new HealthBar();

    @Unique
    private int lastTicks;

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"))
    public void replaceVanillaHealthBar(InGameHud instance, MatrixStack matrices, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        healthBar.render(matrices, player, x, y, instance.getTicks() - lastTicks);
        lastTicks = instance.getTicks();
        RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), method = "renderStatusBars")
    public int rowHeight(int a, int b) {
        // The height of a health bar is 10 at Math.max(10 - (q - 2), 3)
        // We want the renderer to think there's only one bar, but we set the height to 5
        // at Math.max(j, i) we want the renderer to think there is only one line of hearts, so we return 5
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
        // so the armor is displayed at the right place
        return (player.getAbsorptionAmount() > 0) ? 40 : 0;
    }
}
