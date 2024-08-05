package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class PerforationTableScreen extends AbstractContainerScreen<PerforationTableMenu> {
    protected static final ResourceLocation GUI_BACKGROUND = ResourceLocation.fromNamespaceAndPath(CCMain.ID, "textures/gui/perforation_table_screen.png");
    protected NoteGridScreen noteGridScreen;
    protected byte currentPage = 0;
    private PageButton backButton;
    private PageButton forwardButton;
    private NoteGridWidget gridOnTableWidget;

    public PerforationTableScreen(PerforationTableMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        menu.screen = this;
    }

    @Override
    protected void init() {
        super.init();
        int top = this.topPos + 57;
        this.backButton = this.addRenderableWidget(new PageButton(this.leftPos + 57, top, false, (button) -> pageBack(), true));
        this.forwardButton = this.addRenderableWidget(new PageButton(this.leftPos + 145, top, true, (button) -> pageForward(), true));
        this.gridOnTableWidget = this.addRenderableWidget(new NoteGridWidget(this.leftPos + 79, this.topPos + 15, this));
        updatePageButtonVisibility();
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (Minecraft.getInstance().screen == this) {
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Called when the item in the {@link PerforationTableMenu} changes.
     */
    protected void onItemChanged() {
        gridOnTableWidget.setTooltip(Tooltip.create(menu.mode.getTip()));
        currentPage = 0;
        updatePageButtonVisibility();
        if (noteGridScreen != null && menu.mode != MenuMode.PUNCH) {
            noteGridScreen.exitEditMode();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        int left = (this.width - this.imageWidth) / 2;
        int up = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_BACKGROUND, left, up, 0, 0, this.imageWidth, this.imageHeight);
    }

    protected void openNoteGridScreen() {
        noteGridScreen = new NoteGridScreen(this);
        Minecraft.getInstance().pushGuiLayer(noteGridScreen);
    }

    private void pageBack() {
        if (currentPage > 0) {
            --currentPage;
        }
        updatePageButtonVisibility();
    }

    private void pageForward() {
        if (currentPage < getPageSize() - 1) {
            ++this.currentPage;
        }
        updatePageButtonVisibility();
    }

    private int getPageSize() {
        switch (menu.mode) {
            case PUNCH, CHECK -> {
                return menu.data == null ? 0 : menu.data.size();
            }
            case CONNECT -> {
                return menu.displayData == null ? 0 : menu.displayData.size();
            }
            default -> {
                return 0;
            }
        }
    }

    private void updatePageButtonVisibility() {
        backButton.visible = currentPage > 0;
        forwardButton.visible = currentPage < getPageSize() - 1;
    }
}
