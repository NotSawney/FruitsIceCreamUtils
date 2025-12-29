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

        // Botón "Collect" centrado en la parte inferior
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonX = leftPos + (imageWidth / 2) - (buttonWidth / 2);
        int buttonY = topPos + 90; // Ajustado para la nueva altura

        collectButton = Button.builder(
                        Component.translatable("gui.fruitsicecreamutilities.collect"),
                        button -> {
                            // Enviar paquete al servidor para recolectar XP
                            ModNetworking.sendToServer(new CollectExperiencePacket(menu.getBlockEntity().getBlockPos()));
                            onClose(); // Cerrar GUI después de recolectar
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

        // Renderizar el número de XP almacenado centrado
        int storedXP = menu.getStoredExperience();
        String xpText = String.valueOf(storedXP);
        int textX = leftPos + (imageWidth / 2) - (font.width(xpText) / 2);
        int textY = topPos + 30;
        guiGraphics.drawString(font, xpText, textX, textY, 0x3FFF3F, false);

        // Opcional: Mostrar "XP" debajo del número
        String xpLabel = "XP";
        int labelX = leftPos + (imageWidth / 2) - (font.width(xpLabel) / 2);
        int labelY = topPos + 40;
        guiGraphics.drawString(font, xpLabel, labelX, labelY, 0xFFFFFF, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Título del GUI
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
}