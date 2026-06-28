package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.inventory.menu.MenuChangedListener;
import io.github.c20c01.cc_mb.inventory.menu.MenuMode;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import io.github.c20c01.cc_mb.player.NoteGridDataHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PerforationTableScreen extends AbstractContainerScreen<PerforationTableMenu> implements NoteGridDataHolder, MenuChangedListener, EditScreenCloseListener {
    protected static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(MusicBox.ID, "textures/gui/perforation_table_screen.png");

    protected PageButton forwardButton, backButton;
    protected NoteGridWidget noteGridWidget;

    @Nullable
    protected NoteGridData mainData, helpData;
    protected MenuMode mode = MenuMode.EMPTY;
    protected int pageNum = 0;

    private MenuModeChangedListener listener;

    public PerforationTableScreen(PerforationTableMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        menu.setListener(this);
    }

    @Override
    public void onMenuItemChanged(@Nullable NoteGridData mainData, @Nullable NoteGridData helpData, MenuMode mode) {
        this.mainData = mainData;
        this.helpData = helpData;
        this.pageNum = mainData == null ? 0 : Math.min(pageNum, mainData.size() - 1);
        boolean modeChanged = mode != this.mode;
        this.mode = mode;
        updateWidgets();
        if (modeChanged && listener != null) listener.onMenuModeChanged(mode);
    }

    @Override
    public void onEditScreenClose(NoteGridData mainData, int pageNum) {
        this.mainData = mainData;
        this.pageNum = pageNum;
        setListener(null);
        updateWidgets();
    }

    public void setListener(MenuModeChangedListener listener) {
        this.listener = listener;
    }

    private void onClick() {
        switch (mode) {
            case PUNCH, CHECK, FIX -> NoteGridEditScreen.openWithPerforationTable(this);
            case CONNECT -> GuiUtils.sendCodeToMenu(menu.containerId, PerforationTableMenu.CODE_CONNECT_NOTE_GRID);
            case CUT -> {
                if (pageNum < getDataSize() - 1) GuiUtils.sendCodeToMenu(menu.containerId, (byte) pageNum);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        final int pageButtonY = topPos + 57;

        forwardButton = this.addRenderableWidget(new PageButton(leftPos + 145, pageButtonY, true, (_) -> pageForward(), true));
        backButton = this.addRenderableWidget(new PageButton(leftPos + 57, pageButtonY, false, (_) -> pageBack(), true));
        noteGridWidget = this.addRenderableWidget(new NoteGridWidget(leftPos + 79, topPos + 15, this::onClick));

        updateWidgets();
    }

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    private void updateWidgets() {
        updatePageButtons();
        noteGridWidget.update(mainData, helpData, mode, pageNum);
    }

    public void updatePageButtons() {
        final int pageFromOne = pageNum + 1;
        final int pageCount = getDataSize();

        boolean frontVisible = pageNum < pageCount - 1;
        forwardButton.visible = frontVisible;
        if (frontVisible) {
            forwardButton.setTooltip(Tooltip.create(Component.literal(pageFromOne + " → " + (pageFromOne + 1) + " / " + pageCount)));
        }

        boolean backVisible = pageNum > 0;
        backButton.visible = backVisible;
        if (backVisible) {
            backButton.setTooltip(Tooltip.create(Component.literal((pageFromOne - 1) + " ← " + pageFromOne + " / " + pageCount)));
        }
    }

    public void pageForward() {
        if (pageNum < getDataSize() - 1) {
            ++pageNum;
            updateWidgets();
        }
    }

    public void pageBack() {
        if (pageNum > 0) {
            --pageNum;
            updateWidgets();
        }
    }

    @Override
    public @Nullable NoteGridData getData() {
        return mainData;
    }

    @Override
    public void setData(@Nullable NoteGridData data) {
        this.mainData = data;
    }
}
