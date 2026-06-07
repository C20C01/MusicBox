package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.client.GuiUtils;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

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
            case PUNCH, CHECK, FIX -> renderPunch(guiGraphics, menu.getData(), menu.getHelpData());
            case CONNECT -> renderConnect(guiGraphics, menu.getDisplayData());
            case CUT -> renderCut(guiGraphics, menu.getData());
        }
    }

    private void renderBg(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PerforationTableScreen.GUI_BACKGROUND, getX(), getY(), 0, 168, WIDTH, HEIGHT, 256, 256);
    }

    private void renderOneBeat(GuiGraphicsExtractor guiGraphics, NoteGridData data, byte page, byte beat, int color) {
        Beat oneBeat = data.getPage(page).getBeat(beat);
        int x = getX() + 2 + beat;
        for (byte note : oneBeat.getNotes()) {
            int y = getY() + 50 - note * 2;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void renderPunch(GuiGraphicsExtractor guiGraphics, NoteGridData data, NoteGridData helpData) {
        renderBg(guiGraphics);
        for (byte beat = 0; beat < 64; beat++) {
            renderOneBeat(guiGraphics, data, screen.currentPage, beat, GuiUtils.BLACK);
            if (helpData != null && helpData.size() > screen.currentPage) {
                renderOneBeat(guiGraphics, helpData, screen.currentPage, beat, GuiUtils.HELP_NOTE_COLOR);
            }
        }
    }

    private void renderConnect(GuiGraphicsExtractor guiGraphics, NoteGridData displayData) {
        renderBg(guiGraphics);
        for (byte beat = 0; beat < 64; beat++) {
            renderOneBeat(guiGraphics, displayData, screen.currentPage, beat, GuiUtils.BLACK);
        }
    }

    private void renderCut(GuiGraphicsExtractor guiGraphics, NoteGridData data) {
        renderBg(guiGraphics);
        for (byte beat = 0; beat < 64; beat++) {
            renderOneBeat(guiGraphics, data, screen.currentPage, beat, GuiUtils.BLACK);
        }
        if (screen.hasNextPage()) {
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
