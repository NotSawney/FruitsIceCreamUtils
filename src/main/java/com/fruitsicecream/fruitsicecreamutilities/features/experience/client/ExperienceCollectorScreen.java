package com.fruitsicecream.fruitsicecreamutilities.features.experience.client;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCollectorMenu;
import com.fruitsicecream.fruitsicecreamutilities.network.ModNetworking;
import com.fruitsicecream.fruitsicecreamutilities.network.packets.CollectCollectorExperiencePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExperienceCollectorScreen extends AbstractContainerScreen<ExperienceCollectorMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FruitsIceCreamUtilities.MOD_ID, "textures/gui/experience-collector-gui.png");

    // Dimensiones de la GUI (ajustadas para mejor layout)
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 170;

    // Posiciones de los paneles (ajustadas para evitar solapamiento)
    // Panel Izquierdo - Connected Cores
    private static final int LEFT_PANEL_X = 10;
    private static final int LEFT_PANEL_Y = 28;
    private static final int LEFT_PANEL_WIDTH = 70;
    private static final int LEFT_PANEL_HEIGHT = 115;

    // Panel Central - XP Storage
    private static final int CENTER_PANEL_X = 82;
    private static final int CENTER_PANEL_Y = 28;
    private static final int CENTER_PANEL_WIDTH = 65;
    private static final int CENTER_PANEL_HEIGHT = 115;

    // Panel Derecho - Production Stats
    private static final int RIGHT_PANEL_X = 153;
    private static final int RIGHT_PANEL_Y = 28;
    private static final int RIGHT_PANEL_WIDTH = 70;
    private static final int RIGHT_PANEL_HEIGHT = 115;

    private Button collectButton;

    public ExperienceCollectorScreen(ExperienceCollectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000; // Esconder label de inventario
    }

    @Override
    protected void init() {
        super.init();

        // Botón centrado en el panel central, más pequeño para evitar overlap
        int buttonWidth = 50;
        int buttonHeight = 18;
        int buttonX = leftPos + CENTER_PANEL_X + (CENTER_PANEL_WIDTH / 2) - (buttonWidth / 2);
        int buttonY = topPos + CENTER_PANEL_Y + 90;

        collectButton = Button.builder(
                        Component.translatable("gui.fruitsicecreamutilities.collect"),
                        button -> {
                            ModNetworking.sendToServer(new CollectCollectorExperiencePacket(menu.getBlockEntity().getBlockPos()));
                            onClose();
                        })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        addRenderableWidget(collectButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Renderizar la textura PNG de fondo
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Renderizar contenido de los tres paneles
        renderLeftPanel(guiGraphics);
        renderCenterPanel(guiGraphics);
        renderRightPanel(guiGraphics);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Panel izquierdo - Lista de cores conectados
     */
    private void renderLeftPanel(GuiGraphics guiGraphics) {
        int panelX = leftPos + LEFT_PANEL_X;
        int panelY = topPos + LEFT_PANEL_Y;

        int y = panelY + 3;

        // Mostrar cada tier con su icono y cantidad (texto más pequeño)
        int[] coreCounts = {
                menu.getCoresMKI(),
                menu.getCoresMKII(),
                menu.getCoresMKIII(),
                menu.getCoresMKIV(),
                menu.getCoresMKV()
        };

        boolean hasCores = false;
        for (int tier = 1; tier <= 5; tier++) {
            int count = coreCounts[tier - 1];
            if (count > 0) {
                hasCores = true;
                // Formato más compacto para evitar overflow
                String icon = getTierIcon(tier);
                String text = icon + " MK-" + toRoman(tier) + " x" + count;

                // Limitar longitud del texto
                if (font.width(text) > LEFT_PANEL_WIDTH - 8) {
                    text = icon + " " + toRoman(tier) + " x" + count;
                }

                guiGraphics.drawString(font, text, panelX + 4, y, getTierColor(tier), false);
                y += 11;
            }
        }

        // Si no hay cores, mostrar mensaje
        if (!hasCores) {
            guiGraphics.drawString(font, "No cores", panelX + 4, y, 0x888888, false);
            y += 11;
            guiGraphics.drawString(font, "connected", panelX + 4, y, 0x888888, false);
        }

        // Total de cores (al final del panel)
        int totalY = panelY + LEFT_PANEL_HEIGHT - 12;

        String totalText = "Total: " + menu.getTotalConnectedCores();
        guiGraphics.drawString(font, totalText, panelX + 4, totalY, 0xFFFFFF, false);
    }

    /**
     * Panel central - Display de XP y botón collect
     */
    private void renderCenterPanel(GuiGraphics guiGraphics) {
        int panelX = leftPos + CENTER_PANEL_X;
        int panelY = topPos + CENTER_PANEL_Y;

        // XP número (más compacto)
        int storedXP = menu.getStoredExperience();
        String xpText = formatNumber(storedXP);
        int xpX = panelX + (CENTER_PANEL_WIDTH / 2) - (font.width(xpText) / 2);
        int xpY = panelY + 15;

        int xpColor = getXPColor(menu.getFillPercentage());
        guiGraphics.drawString(font, xpText, xpX, xpY, xpColor, false);

        // Label "XP"
        String xpLabel = "XP";
        int labelX = panelX + (CENTER_PANEL_WIDTH / 2) - (font.width(xpLabel) / 2);
        guiGraphics.drawString(font, xpLabel, labelX, xpY + 10, 0xFFFFFF, false);

        // Barra de progreso (más pequeña)
        int barWidth = CENTER_PANEL_WIDTH - 10;
        renderProgressBar(guiGraphics, panelX + 5, panelY + 40, barWidth, menu.getFillPercentage());

        // Porcentaje debajo de la barra
        String percentText = String.format("%.0f%%", menu.getFillPercentage());
        int percentX = panelX + (CENTER_PANEL_WIDTH / 2) - (font.width(percentText) / 2);
        guiGraphics.drawString(font, percentText, percentX, panelY + 50, 0xAAAAAA, false);

        // El botón COLLECT ya está renderizado por el widget system
        collectButton.active = storedXP > 0;
    }

    /**
     * Panel derecho - Estadísticas de producción
     */
    private void renderRightPanel(GuiGraphics guiGraphics) {
        int panelX = leftPos + RIGHT_PANEL_X;
        int panelY = topPos + RIGHT_PANEL_Y;

        int y = panelY + 3;

        // Total Rate
        guiGraphics.drawString(font, "Total Rate:", panelX + 4, y, 0xCCCCCC, false);
        y += 9;

        int totalRate = menu.getTotalProductionRate();
        String rateText = formatNumber(totalRate) + " XP/h";

        // Asegurar que el texto no se salga
        if (font.width(rateText) > RIGHT_PANEL_WIDTH - 8) {
            rateText = formatNumber(totalRate);
            guiGraphics.drawString(font, rateText, panelX + 6, y, 0x55FF55, false);
            y += 9;
            guiGraphics.drawString(font, "XP/h", panelX + 6, y, 0x55FF55, false);
            y += 11;
        } else {
            guiGraphics.drawString(font, rateText, panelX + 6, y, 0x55FF55, false);
            y += 13;
        }

        // Capacity
        guiGraphics.drawString(font, "Capacity:", panelX + 4, y, 0xCCCCCC, false);
        y += 9;

        int stored = menu.getStoredExperience();
        int max = menu.getMaxCapacity();
        float percent = menu.getFillPercentage();

        // Primera línea: stored / max (más compacto)
        String cap1 = formatNumber(stored);
        guiGraphics.drawString(font, cap1 + " /", panelX + 6, y, 0xFFFFFF, false);
        y += 9;

        String cap2 = formatNumber(max);
        guiGraphics.drawString(font, cap2, panelX + 6, y, 0xFFFFFF, false);
        y += 9;

        // Porcentaje
        String percentText = String.format("(%.1f%%)", percent);
        guiGraphics.drawString(font, percentText, panelX + 6, y, getCapacityColor(percent), false);
        y += 13;

        // Avg per Core
        guiGraphics.drawString(font, "Avg/Core:", panelX + 4, y, 0xCCCCCC, false);
        y += 9;

        int avgRate = menu.getAverageProductionPerCore();
        String avgText = formatNumber(avgRate) + " XP/h";

        if (font.width(avgText) > RIGHT_PANEL_WIDTH - 8) {
            avgText = formatNumber(avgRate);
            guiGraphics.drawString(font, avgText, panelX + 6, y, 0xFFAA00, false);
            y += 9;
            guiGraphics.drawString(font, "XP/h", panelX + 6, y, 0xFFAA00, false);
        } else {
            guiGraphics.drawString(font, avgText, panelX + 6, y, 0xFFAA00, false);
        }
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, float fillPercent) {
        int barHeight = 4;

        // Fondo de la barra
        guiGraphics.fill(x, y, x + width, y + barHeight, 0xFF333333);

        // Barra de progreso
        int fillWidth = (int) (width * (fillPercent / 100.0f));
        int barColor = getBarColor(fillPercent);
        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, barColor);

        // Borde negro
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF000000); // Top
        guiGraphics.fill(x, y + barHeight - 1, x + width, y + barHeight, 0xFF000000); // Bottom
        guiGraphics.fill(x, y, x + 1, y + barHeight, 0xFF000000); // Left
        guiGraphics.fill(x + width - 1, y, x + width, y + barHeight, 0xFF000000); // Right
    }

    private String getTierIcon(int tier) {
        return switch (tier) {
            case 1 -> "I";   // Más simple para evitar problemas de fuente
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "-";
        };
    }

    private int getTierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xAAAAAA; // Gris
            case 2 -> 0xFFFFFF; // Blanco
            case 3 -> 0xFFAA00; // Naranja
            case 4 -> 0x55FFFF; // Cian
            case 5 -> 0xFF55FF; // Magenta
            default -> 0xFFFFFF;
        };
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 10000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number >= 1000) {
            return String.format("%.2fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private int getXPColor(float fillPercent) {
        if (fillPercent >= 100.0f) return 0xFF0000;
        if (fillPercent >= 75.0f) return 0xFFAA00;
        return 0x55FF55;
    }

    private int getCapacityColor(float fillPercent) {
        if (fillPercent >= 100.0f) return 0xFF0000;
        if (fillPercent >= 75.0f) return 0xFFAA00;
        if (fillPercent >= 50.0f) return 0xFFFF00;
        return 0x55FF55;
    }

    private int getBarColor(float fillPercent) {
        if (fillPercent >= 100.0f) return 0xFFFF0000;
        if (fillPercent >= 75.0f) return 0xFFFFAA00;
        if (fillPercent >= 50.0f) return 0xFFFFFF00;
        return 0xFF00FF00;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título principal centrado en la parte superior
        int titleX = (imageWidth / 2) - (font.width(this.title) / 2);
        guiGraphics.drawString(this.font, this.title, titleX, 8, 0xFFD700, false);
    }
}