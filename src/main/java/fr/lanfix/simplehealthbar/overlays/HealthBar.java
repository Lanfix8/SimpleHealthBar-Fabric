package fr.lanfix.simplehealthbar.overlays;

import fr.lanfix.simplehealthbar.SimpleHealthBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Random;

public class HealthBar {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    Random rng = new Random();

    private static final Identifier fullHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/full.png");
    private static final Identifier witherHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/wither.png");
    private static final Identifier poisonHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/poison.png");
    private static final Identifier frozenHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/frozen.png");
    private static final Identifier intermediateHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/intermediate.png");
    private static final Identifier emptyHealthBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/empty.png");
    private static final Identifier absorptionBar = Identifier.of(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/absorption.png");

    private static final Identifier heartContainer = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/container.png");
    private static final Identifier absorptionHeart = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/absorbing_full.png");

    private Identifier currentBar = fullHealthBar;
    private double intermediateHealth = 0;

    public void render(DrawContext context, PlayerEntity player, int x, int y, float tickDelta) {
        if (mc.cameraEntity instanceof PlayerEntity && !mc.options.hudHidden
                && mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
            TextRenderer textRenderer = mc.textRenderer;
            updateBarTextures(player);
            // Only render absorption when necessary
            if (player.getAbsorptionAmount() > 0) {
                renderAbsorptionBar(context, x, y, player);
                renderAbsorptionValue(textRenderer, context, x, y, player);
            }
            // We need to render the health bar after beacause they overlap a little
            renderHealthBar(context, tickDelta, x, y, player);
            renderHealthValue(textRenderer, context, x, y, player);
        }
    }

    public void updateBarTextures(PlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.WITHER)) {
            currentBar = witherHealthBar;
        } else if (player.hasStatusEffect(StatusEffects.POISON)) {
            currentBar = poisonHealthBar;
        } else if (player.isFrozen()) {
            currentBar = frozenHealthBar;
        } else {
            currentBar = fullHealthBar;
        }
    }

    private void renderHealthValue(TextRenderer textRenderer, DrawContext context, int x, int y, PlayerEntity player) {
        double health = Math.ceil(player.getHealth() * 10) / 10;
        float maxHealth = player.getMaxHealth();
        String text = health + "/" + (int) player.getMaxHealth();
        text = text.replace(".0", "");

        // Shake sprite and value and color value red when <=20% health
        boolean isLowHealth = health / maxHealth <= 0.2;

        // Color value green when regen
        boolean playerHasRegen = player.hasStatusEffect(StatusEffects.REGENERATION)
                || player.hasStatusEffect(StatusEffects.INSTANT_HEALTH);

        // Show a special string on the death screen (different if in hardcore)
        if (health <= 0) {
            boolean isHardcore = player.getWorld().getLevelProperties().isHardcore();
            text = Text.translatable("gui.simplehealthbar.zero_health" + (isHardcore ? ".hardcore" : "")).getString();
        }

        // Offset for text
        int offX = 4;
        int offY = -4;
        // Cause the value to shake when low health
        if (isLowHealth && health > 0)
            offY += rng.nextInt(-1, 2);

        // The health color: White when normal, pink when low health, green when regen (even if low health)
        int healthColor = 0xffffff;
        if (isLowHealth)
            healthColor = 0xffa1b0;
        if (playerHasRegen)
            healthColor = 0x6fff9a;

        // Draw health value + 4px outline
        context.drawText(textRenderer, text, x + offX + 1, y + offY, 0x000000, false);
        context.drawText(textRenderer, text, x + offX - 1, y + offY, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY + 1, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY - 1, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY, healthColor, false);

    }

    private void renderHealthBar(DrawContext context, float tickDelta, float x, float y, PlayerEntity player) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();

        // Calculate bar proportions
        double healthProportion;
        double intermediateProportion;
        if (health < intermediateHealth) {
            healthProportion = health / maxHealth;
            intermediateProportion = (intermediateHealth - health) / maxHealth;
        } else {
            healthProportion = intermediateHealth / maxHealth;
            intermediateProportion = 0;
        }
        if (healthProportion > 1) healthProportion = 1F;
        if (healthProportion + intermediateProportion > 1) intermediateProportion = 1 - healthProportion;
        int healthWidth = (int) Math.ceil(80 * healthProportion);
        int intermediateWidth = (int) Math.ceil(80 * intermediateProportion);

        // Display full part
        context.drawTexture(currentBar,
                (int) x, (int) y,
                0, 0,
                healthWidth, 9,
                80, 9);

        // Display intermediate part
        context.drawTexture(intermediateHealthBar,
                (int) x + healthWidth, (int) y,
                healthWidth, 0,
                intermediateWidth, 9,
                80, 9);

        // Display empty part
        context.drawTexture(emptyHealthBar,
                (int) x + healthWidth + intermediateWidth, (int) y,
                healthWidth + intermediateWidth, 0,
                80 - healthWidth - intermediateWidth, 9,
                80, 9);

        // Update intermediate health
        this.intermediateHealth += (health - intermediateHealth) * tickDelta * 0.08;
        if (Math.abs(health - intermediateHealth) <= 0.25) {
            this.intermediateHealth = health;
        }
    }

    private void renderAbsorptionValue(TextRenderer textRenderer, DrawContext context, int x, int y, PlayerEntity player) {
        double absorption = Math.ceil(player.getAbsorptionAmount() * 10) / 10;
        String text = String.valueOf(absorption);
        text = text.replace(".0", "");

        // Offset of sprite
        int offX = 1;
        int offY = -15;

        // blit heart container
        context.drawTexture(heartContainer,
                x + offX, y + offY,
                0, 0,
                9, 9,
                9, 9);
        // blit heart
        context.drawTexture(absorptionHeart,
                x + offX, y + offY,
                0, 0,
                9, 9,
                9, 9);

        // Text offset
        offX = 12;
        offY = -14;

        // Draw absorption value + 4px outline
        context.drawText(textRenderer, text, x + offX + 1, y + offY, 0x000000, false);
        context.drawText(textRenderer, text, x + offX - 1, y + offY, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY + 1, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY - 1, 0x000000, false);
        context.drawText(textRenderer, text, x + offX, y + offY, 0xffeba1, false);
    }

    private void renderAbsorptionBar(DrawContext context, float x, float y, PlayerEntity player) {
        float absorption = player.getAbsorptionAmount();
        float maxHealth = player.getMaxHealth();

        // Calculate bar proportions
        float absorptionProportion = absorption / maxHealth;
        if (absorptionProportion > 1) absorptionProportion = 1F;
        int absorptionWidth = (int) Math.ceil(80 * absorptionProportion);

        // Display full part
        context.drawTexture(absorptionBar,
                (int) x, (int) y - 10,
                0, 0,
                absorptionWidth, 9,
                80, 9);

        // Display empty part
        context.drawTexture(emptyHealthBar,
                (int) x + absorptionWidth, (int) y - 10,
                absorptionWidth, 0,
                80 - absorptionWidth, 9,
                80, 9);

    }

}
