package com.fruitsicecream.fruitsicecreamutilities.features.experience.client;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCoreMenu;
import com.fruitsicecream.fruitsicecreamutilities.network.ModNetworking;
import com.fruitsicecream.fruitsicecreamutilities.network.packets.CollectExperiencePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExperienceCoreScreen extends AbstractContainerScreen<ExperienceCoreMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FruitsIceCreamUtilities.MOD_ID, "textures/gui/experience-core-gui.png");

    private Button collectButton;

    public ExperienceCoreScreen(ExperienceCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 128;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonX = leftPos + (imageWidth / 2) - (buttonWidth / 2);
        int buttonY = topPos + 90;

        collectButton = Button.builder(
                        Component.translatable("gui.fruitsicecreamutilities.collect"),
                        button -> {
                            ModNetworking.sendToServer(new CollectExperiencePacket(menu.getBlockEntity().getBlockPos()));
                            onClose();
                        })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        addRenderableWidget(collectButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // XP almacenado
        int storedXP = menu.getStoredExperience();
        String xpText = String.valueOf(storedXP);
        int textX = leftPos + (imageWidth / 2) - (font.width(xpText) / 2);
        int textY = topPos + 30;

        // Color verde si no está lleno, amarillo si está cerca, rojo si está lleno
        int color = getXPColor(menu.getFillPercentage());
        guiGraphics.drawString(font, xpText, textX, textY, color, false);

        // Label "XP"
        String xpLabel = "XP";
        int labelX = leftPos + (imageWidth / 2) - (font.width(xpLabel) / 2);
        int labelY = topPos + 40;
        guiGraphics.drawString(font, xpLabel, labelX, labelY, 0xFFFFFF, false);

        // Mostrar capacidad y porcentaje
        int maxCap = menu.getMaxCapacity();
        float fillPercent = menu.getFillPercentage();
        String capacityText = String.format("%d / %d (%.1f%%)", storedXP, maxCap, fillPercent);
        int capX = leftPos + (imageWidth / 2) - (font.width(capacityText) / 2);
        int capY = topPos + 55;
        guiGraphics.drawString(font, capacityText, capX, capY, 0xAAAAAA, false);

        // Mensaje si está lleno
        if (fillPercent >= 100.0f) {
            String fullText = "FULL";
            int fullX = leftPos + (imageWidth / 2) - (font.width(fullText) / 2);
            int fullY = topPos + 67;
            guiGraphics.drawString(font, fullText, fullX, fullY, 0xFF0000, false);
        }

        // Barra de progreso visual
        renderProgressBar(guiGraphics, fillPercent);
    }

    private void renderProgressBar(GuiGraphics guiGraphics, float fillPercent) {
        int barWidth = 140;
        int barHeight = 4;
        int barX = leftPos + (imageWidth / 2) - (barWidth / 2);
        int barY = topPos + 75;

        // Fondo de la barra (gris oscuro)
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

        // Barra de progreso (color según llenado)
        int fillWidth = (int) (barWidth * (fillPercent / 100.0f));
        int barColor = getBarColor(fillPercent);
        guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, barColor);

        // Borde de la barra
        guiGraphics.fill(barX, barY, barX + barWidth, barY + 1, 0xFF000000); // Top
        guiGraphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFF000000); // Bottom
        guiGraphics.fill(barX, barY, barX + 1, barY + barHeight, 0xFF000000); // Left
        guiGraphics.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFF000000); // Right
    }

    private int getXPColor(float fillPercent) {
        if (fillPercent >= 100.0f) return 0xFF0000; // Rojo - lleno
        if (fillPercent >= 75.0f) return 0xFFAA00; // Naranja - casi lleno
        return 0x3FFF3F; // Verde - normal
    }

    private int getBarColor(float fillPercent) {
        if (fillPercent >= 100.0f) return 0xFFFF0000; // Rojo brillante
        if (fillPercent >= 75.0f) return 0xFFFFAA00; // Naranja
        if (fillPercent >= 50.0f) return 0xFFFFFF00; // Amarillo
        return 0xFF00FF00; // Verde
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
}