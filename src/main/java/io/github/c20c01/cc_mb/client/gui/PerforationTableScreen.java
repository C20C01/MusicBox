package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.inventory.MenuMode;
import io.github.c20c01.cc_mb.inventory.PerforationTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

public class PerforationTableScreen extends AbstractContainerScreen<PerforationTableMenu> {
    protected static final Identifier GUI_BACKGROUND = Identifier.fromNamespaceAndPath(MusicBox.ID, "textures/gui/perforation_table_screen.png");
    protected NoteGridScreen noteGridScreen;
    protected byte currentPage = 0;
    private PageButton backButton;
    private PageButton forwardButton;
    private NoteGridWidget gridOnTableWidget;

    public PerforationTableScreen(PerforationTableMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        menu.setItemChangedCallback(this::onItemChanged);
    }

    @Override
    protected void init() {
        super.init();
        int top = this.topPos + 57;
        this.backButton = this.addRenderableWidget(new PageButton(this.leftPos + 57, top, false, (_) -> pageBack(), true));
        this.forwardButton = this.addRenderableWidget(new PageButton(this.leftPos + 145, top, true, (_) -> pageForward(), true));
        this.gridOnTableWidget = this.addRenderableWidget(new NoteGridWidget(this.leftPos + 79, this.topPos + 15, this));
        updateWidget();
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        if (Minecraft.getInstance().screen == this) {
            super.extractRenderState(guiGraphics, mouseX, mouseY, a);
            this.extractTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Called when the item in the {@link PerforationTableMenu} changes.
     */
    protected void onItemChanged() {
        currentPage = 0;
        updateWidget();
        if (noteGridScreen != null && menu.getMode() != MenuMode.PUNCH && menu.getMode() != MenuMode.FIX) {
            noteGridScreen.exitEditMode();
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        int left = (this.width - this.imageWidth) / 2;
        int up = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_BACKGROUND, left, up, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    protected void openNoteGridScreen() {
        noteGridScreen = new NoteGridScreen(this);
        Minecraft.getInstance().pushGuiLayer(noteGridScreen);
    }

    private void pageBack() {
        if (currentPage > 0) {
            --currentPage;
        }
        if (menu.getMode() == MenuMode.CUT && currentPage == getPageSize() - 2) {
            gridOnTableWidget.setTooltip(Tooltip.create(menu.getMode().getTip()));
        }
        updateWidget();
    }

    private void pageForward() {
        if (currentPage < getPageSize() - 1) {
            ++this.currentPage;
        }
        if (menu.getMode() == MenuMode.CUT && currentPage == getPageSize() - 1) {
            gridOnTableWidget.setTooltip(Tooltip.create(Component.translatable(MusicBox.TEXT_CANNOT_CUT)));
        }
        updateWidget();
    }

    private int getPageSize() {
        switch (menu.getMode()) {
            case PUNCH, CHECK, CUT -> {
                return menu.getData() == null ? 0 : menu.getData().size();
            }
            case CONNECT -> {
                return menu.getDisplayData() == null ? 0 : menu.getDisplayData().size();
            }
            default -> {
                return 0;
            }
        }
    }

    private void updateWidget() {
        // page buttons
        backButton.visible = currentPage > 0;
        if (backButton.visible) {
            backButton.setTooltip(Tooltip.create(Component.literal(currentPage + " ←")));
        }
        boolean hasNextPage = hasNextPage();
        forwardButton.visible = hasNextPage;
        if (hasNextPage) {
            forwardButton.setTooltip(Tooltip.create(Component.literal("→ " + (currentPage + 2))));
        }

        // tooltip
        if (menu.getMode() == MenuMode.CUT && !hasNextPage) {
            gridOnTableWidget.setTooltip(Tooltip.create(Component.translatable(MusicBox.TEXT_CANNOT_CUT)));
        } else {
            gridOnTableWidget.setTooltip(Tooltip.create(menu.getMode().getTip()));
        }
    }

    protected boolean hasNextPage() {
        return currentPage < getPageSize() - 1;
    }
}
