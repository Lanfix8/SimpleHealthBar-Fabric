package fr.lanfix.simplehealthbar.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.lanfix.simplehealthbar.SimpleHealthBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HealthBar {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Identifier fullHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/full.png");
    private static final Identifier witherHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/wither.png");
    private static final Identifier poisonHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/poison.png");
    private static final Identifier frozenHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/frozen.png");
    private static final Identifier intermediateHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/intermediate.png");
    private static final Identifier emptyHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/empty.png");
    private static final Identifier absorptionBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/absorption.png");
    private static final Identifier guiIcons = new Identifier("minecraft", "textures/gui/icons.png");

    private static final Identifier blinkIndicator = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/blink.png");
    private static final Identifier smallNumbers = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/font/num.png");

    private Identifier currentBar = fullHealthBar;
    private float intermediateHealth = 0;

    public void render(DrawContext context, float tickDelta) {
        // If the HUD is not hidden and has status bars (like health), render!
        if (mc.cameraEntity instanceof PlayerEntity player
                && !mc.options.hudHidden
                && mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();

            float x = (float) width / 2 - 91;
            float y = height - 39;

            TextRenderer textRenderer = mc.textRenderer;
            updateBarTextures(player);

            // Only render absorption when necessary
            if (player.getAbsorptionAmount() > 0) {
                renderAbsorptionBar(context, x, y, player);
                renderAbsorptionValue(textRenderer, context, (int) x, (int) y, player);
            }

            renderHealthBar(context, tickDelta, x, y, player);
            renderHealthValue(textRenderer, context, (int) x, (int) y, player);

        }
    }

    public void updateBarTextures(PlayerEntity player) {
        // Update bar texture to correspond to status of the player
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
        // TODO: Configurable low health value

        double health = Math.ceil(player.getHealth() * 10) / 10;
        float maxHealth = player.getMaxHealth();

        String text = String.valueOf(health);
        text = text.replace(".0", "");

        // Offset for sprites
        int offX = 1;
        int offY = -5;

        boolean isLowHealth = health / maxHealth <= 0.2;
        boolean playerHasRegen = player.hasStatusEffect(StatusEffects.REGENERATION)
                || player.hasStatusEffect(StatusEffects.INSTANT_HEALTH);

        // Cause sprite shaking when low health
        if (isLowHealth)
            offY += (Math.random() * 2) - 1;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, guiIcons);

        // Draw heart background
        context.drawTexture(guiIcons,
                x + offX, y + offY,
                16, 0,
                9, 9,
                256, 256);

        // Full heart if in good health, half heart if low
        if (isLowHealth)
            context.drawTexture(guiIcons,
                    x + offX, y + offY,
                    61, 0,
                    9, 9,
                    256, 256);
        else if (health > 0)
            context.drawTexture(guiIcons,
                    x + offX, y + offY,
                    52, 0,
                    9, 9,
                    256, 256);

        // Offset for text
        offX = 12;
        offY = -4;

        // Cause the value to shake when low health
        if (isLowHealth) {
            offY += (Math.random() * 2) - 1;
        }

        // Show a special string on the death screen
        if (health <= 0) {
            text = Text.translatable("gui.simplehealthbar.zero_health").getString();
        }

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

        // Only draw max number if our max health is not 20 (default)
        if (maxHealth != 20) {
            renderMaxHPValue(context, x, y, maxHealth);
        }
    }

    private void renderHealthBar(DrawContext context, float tickDelta, float x, float y, PlayerEntity player) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();

        // Calculate bar proportions
        float healthProportion;
        float intermediateProportion;

        if (health < intermediateHealth) {
            healthProportion = health / maxHealth;
            intermediateProportion = (intermediateHealth - health) / maxHealth;
        } else {
            healthProportion = intermediateHealth / maxHealth;
            intermediateProportion = 0;
        }

        if (healthProportion > 1)
            healthProportion = 1F;

        if (healthProportion + intermediateProportion > 1)
            intermediateProportion = 1 - healthProportion;

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

        // Update intermediate health with delta time
        this.intermediateHealth += (health - intermediateHealth) * mc.getLastFrameDuration() * 0.24;

        // Snap value when close enough
        if (Math.abs(health - intermediateHealth) <= 0.15) {
            this.intermediateHealth = health;
        }

        // Render blink indicator upon healing or taking damage
        if (health != intermediateHealth) {
            RenderSystem.setShaderTexture(0, blinkIndicator);
            context.drawTexture(blinkIndicator,
                    (int) x - 1, (int) y - 1,
                    0, 0,
                    82, 11,
                    82, 11);
        }
    }

    private void renderAbsorptionValue(TextRenderer textRenderer, DrawContext context, int x, int y, PlayerEntity player) {
        double absorption = Math.ceil(player.getAbsorptionAmount() * 10) / 10;

        String text = String.valueOf(absorption);
        text = text.replace(".0", "");

        // Offset of sprite
        int offX = 1;
        int offY = -15;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, guiIcons);

        // Draw heart background
        context.drawTexture(guiIcons,
                x + offX, y + offY,
                16, 0,
                9, 9,
                256, 256);

        // Draw absorption heart
        context.drawTexture(guiIcons,
                x + offX, y + offY,
                160, 0,
                9, 9,
                256, 256);

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

        if (absorptionProportion > 1)
            absorptionProportion = 1F;

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

    private void drawOutlinedTexture(DrawContext context, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, Identifier texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, texture);

        // Draw four black copies of the texture in a + pattern
        RenderSystem.setShaderColor(0F, 0F, 0F, 1F);
        context.drawTexture(texture, x, y + 1, u, v, width, height, textureWidth, textureHeight);
        context.drawTexture(texture, x, y - 1, u, v, width, height, textureWidth, textureHeight);
        context.drawTexture(texture, x + 1, y, u, v, width, height, textureWidth, textureHeight);
        context.drawTexture(texture, x - 1, y, u, v, width, height, textureWidth, textureHeight);

        // Draw the regular texture in the center
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(texture,
                x, y,
                u, v,
                width, height,
                textureWidth, textureHeight);
    }

    private void renderMaxHPValue(DrawContext context, float x, float y, float maxHealth) {
        // Individual characters of the amount of max health
        char[] maxHealthDigits = String.valueOf(maxHealth).toCharArray();

        // Text offset (anchored to right of bar)
        int offX = 69 - 5 * Math.max(maxHealthDigits.length - 3, 0);
        int offY = -7;

        // Draw the slash
        drawOutlinedTexture(context,
                (int) x + offX, (int) y + offY,
                10 * 8, 0, 8, 8,
                128, 128,
                smallNumbers);

        // Draw the max value
        for (int i = 0; i < maxHealthDigits.length - 1; i++) {
            int digit = maxHealthDigits[i];
            drawOutlinedTexture(context,
                    (int) x + offX + ((i + 1) * 5),
                    (int) y + offY,
                    8 * digit,
                    0, 8, 8, 128, 128, smallNumbers);
        }
    }
}
