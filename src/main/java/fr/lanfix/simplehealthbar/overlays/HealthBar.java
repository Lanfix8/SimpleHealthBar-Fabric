package fr.lanfix.simplehealthbar.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.lanfix.simplehealthbar.SimpleHealthBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class HealthBar {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Identifier fullHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/full.png");
    private static final Identifier witherHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/wither.png");
    private static final Identifier poisonHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/poison.png");
    private static final Identifier frozenHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/frozen.png");
    private Identifier currentBar = fullHealthBar;
    private static final Identifier intermediateHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/intermediate.png");
    private static final Identifier emptyHealthBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/empty.png");
    private static final Identifier absorptionBar = new Identifier(SimpleHealthBar.MOD_ID, "textures/gui/healthbars/absorption.png");
    private static final Identifier guiIcons = new Identifier("minecraft", "textures/gui/icons.png");

    private float intermediateHealth = 0;

    public void render(MatrixStack matrixStack, float tickDelta) {
        if (mc.cameraEntity instanceof PlayerEntity player
                && !mc.options.hudHidden
                && mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            float x = (float) width / 2 - 91;
            float y = height - 39;
            TextRenderer textRenderer = mc.textRenderer;
            updateBarTextures(player);
            renderHealthValue(textRenderer, matrixStack, x, y, player);
            renderHealthBar(matrixStack, tickDelta, x, y, player);
            if (player.getAbsorptionAmount() > 0) {
                renderAbsorptionValue(textRenderer, matrixStack, x, y, player);
                renderAbsorptionBar(matrixStack, x, y, player);
            }
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

    private void renderHealthValue(TextRenderer textRenderer, MatrixStack matrixStack, float x, float y, PlayerEntity player) {
        double health = Math.ceil(player.getHealth() * 10) / 10;
        String text = health + "/" + (int) player.getMaxHealth();
        text = text.replace(".0", "");
        textRenderer.draw(matrixStack, text, x - textRenderer.getWidth(text) - 6, y + 1, 0xFF0000);
    }

    private void renderHealthBar(MatrixStack matrixStack, float tickDelta, float x, float y, PlayerEntity player) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
        int healthWidth = (int) Math.ceil(80 * healthProportion);
        int intermediateWidth = (int) Math.ceil(80 * intermediateProportion);
        // Display full part
        RenderSystem.setShaderTexture(0, currentBar);
        DrawableHelper.drawTexture(matrixStack,
                (int) x, (int) y,
                0, 0,
                healthWidth, 9,
                80, 9);
        // Display intermediate part
        RenderSystem.setShaderTexture(0, intermediateHealthBar);
        DrawableHelper.drawTexture(matrixStack,
                (int) x + healthWidth, (int) y,
                healthWidth, 0,
                intermediateWidth, 9,
                80, 9);
        // Display empty part
        RenderSystem.setShaderTexture(0, emptyHealthBar);
        DrawableHelper.drawTexture(matrixStack,
                (int) x + healthWidth + intermediateWidth, (int) y,
                healthWidth + intermediateWidth, 0,
                80 - healthWidth - intermediateWidth, 9,
                80, 9);
        // Update intermediate health
        this.intermediateHealth += (health - intermediateHealth) * tickDelta * 0.08;
        if (Math.abs(health - intermediateHealth) <= 0.25) {
            this.intermediateHealth = health;
        }
        // TODO hardcore bars
    }

    private void renderAbsorptionValue(TextRenderer textRenderer, MatrixStack matrixStack, float x, float y, PlayerEntity player) {
        double absorption = Math.ceil(player.getAbsorptionAmount());
        String text = String.valueOf(absorption / 2);
        text = text.replace(".0", "");
        textRenderer.draw(matrixStack, text, x - textRenderer.getWidth(text) - 16, y - 9, 0xFFFF00);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, guiIcons);
        // blit heart container
        DrawableHelper.drawTexture(matrixStack,
                (int) x - 16, (int) y - 10,
                16, 0,
                9, 9,
                256, 256);
        // blit heart
        RenderSystem.setShaderColor(255.0F, 255.0F, 0.0F, 0.0F);
        DrawableHelper.drawTexture(matrixStack,
                (int) x - 16, (int) y - 10,
                160, 0,
                9, 9,
                256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderAbsorptionBar(MatrixStack matrixStack, float x, float y, PlayerEntity player) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float absorption = player.getAbsorptionAmount();
        float maxHealth = player.getMaxHealth();
        // Calculate bar proportions
        float absorptionProportion = absorption / maxHealth;
        int absorptionWidth = (int) Math.ceil(80 * absorptionProportion);
        // Display full part
        RenderSystem.setShaderTexture(0, absorptionBar);
        DrawableHelper.drawTexture(matrixStack,
                (int) x, (int) y - 10,
                0, 0,
                absorptionWidth, 9,
                80, 9);
        // Display empty part
        RenderSystem.setShaderTexture(0, emptyHealthBar);
        DrawableHelper.drawTexture(matrixStack,
                (int) x + absorptionWidth, (int) y - 10,
                absorptionWidth, 0,
                80 - absorptionWidth, 9,
                80, 9);
    }

}
