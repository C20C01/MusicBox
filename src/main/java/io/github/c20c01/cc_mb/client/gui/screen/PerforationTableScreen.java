package io.github.c20c01.cc_mb.client.gui.screen;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
public class PerforationTableScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(CCMain.ID, "textures/gui/perforation_table_screen.png");

    public PerforationTableScreen(AbstractContainerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        CCMain.LOGGER.info(mouseX + ", " + mouseY + ", " + button);
        if (mouseX < 5 && mouseY < 5) {
            Minecraft.getInstance().setScreen(new PerforationScreen(menu.slots.get(0).getItem()));
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
