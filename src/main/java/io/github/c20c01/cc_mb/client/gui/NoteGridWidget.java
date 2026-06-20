package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.client.GuiUtils;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The widget that displays the note grid, used in the {@link PerforationTableScreen screen}.
 * And the {@link #onClick(MouseButtonEvent, boolean) button} to trigger note grid handling.
 */
public class NoteGridWidget extends AbstractWidget {
    public static final int WIDTH = 68;
    public static final int HEIGHT = 53;
    private final PerforationTableScreen screen;
    private final PerforationTableMenu menu;

    public NoteGridWidget(int x, int y, PerforationTableScreen screen) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.screen = screen;
        this.menu = screen.getMenu();
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        switch (menu.getMode()) {
            case PUNCH, CHECK, FIX -> renderPunch(guiGraphics, screen.currentPage, menu.getData(), menu.getHelpData());
            case CONNECT -> renderConnect(guiGraphics, screen.currentPage, menu.getDisplayData());
            case CUT -> renderCut(guiGraphics, screen.currentPage, screen.hasNextPage(), menu.getData());
        }
    }

    private void renderBg(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PerforationTableScreen.GUI_BACKGROUND, getX(), getY(), 0, 168, WIDTH, HEIGHT, 256, 256);
    }

    private void renderOneBeat(GuiGraphicsExtractor guiGraphics, Page page, int beatNum, int color) {
        int x = getX() + 2 + beatNum;
        int baseY = getY() + 50;
        ByteList notes = page.getBeat(beatNum).getNotes();
        for (int i = 0; i < notes.size(); i++) {
            int y = baseY - notes.getByte(i) * 2;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void renderPunch(GuiGraphicsExtractor guiGraphics, int pageNum, NoteGridData data, @Nullable NoteGridData helpData) {
        renderBg(guiGraphics);
        Page page = data.getPage(pageNum);
        for (int b = 0; b < Page.BEATS_SIZE; b++) {
            renderOneBeat(guiGraphics, page, b, GuiUtils.BLACK);
        }
        if (helpData != null && helpData.size() > pageNum) {
            Page helpPage = helpData.getPage(pageNum);
            for (int b = 0; b < Page.BEATS_SIZE; b++) {
                renderOneBeat(guiGraphics, helpPage, b, GuiUtils.HELP_NOTE_COLOR);
            }
        }
    }

    private void renderConnect(GuiGraphicsExtractor guiGraphics, int pageNum, NoteGridData displayData) {
        renderBg(guiGraphics);
        Page page = displayData.getPage(pageNum);
        for (int b = 0; b < Page.BEATS_SIZE; b++) {
            renderOneBeat(guiGraphics, page, b, GuiUtils.BLACK);
        }
    }

    private void renderCut(GuiGraphicsExtractor guiGraphics, int pageNum, boolean hasNextPage, NoteGridData data) {
        renderBg(guiGraphics);
        Page page = data.getPage(pageNum);
        for (int b = 0; b < Page.BEATS_SIZE; b++) {
            renderOneBeat(guiGraphics, page, b, GuiUtils.BLACK);
        }
        if (hasNextPage) {
            guiGraphics.verticalLine(getX() + WIDTH - 1, getY() - 1, getY() + HEIGHT, 0xFFCC2001);
        }
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        switch (menu.getMode()) {
            case PUNCH, CHECK, FIX -> screen.openNoteGridScreen();
            case CONNECT -> GuiUtils.sendCodeToMenu(menu.containerId, PerforationTableMenu.CODE_CONNECT_NOTE_GRID);
            case CUT -> {
                if (screen.hasNextPage()) {
                    GuiUtils.sendCodeToMenu(menu.containerId, screen.currentPage);
                }
            }
        }
    }
}
