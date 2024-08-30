package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * The widget that displays the note grid, used in the {@link PerforationTableScreen screen}.
 * And the {@link #onClick(double, double, int) button} to trigger note grid handling.
 */
@OnlyIn(Dist.CLIENT)
public class NoteGridWidget extends AbstractWidget {
    public static final int WIDTH = 68;
    public static final int HEIGHT = 53;
    private final PerforationTableScreen SCREEN;
    private final PerforationTableMenu MENU;

    public NoteGridWidget(int x, int y, PerforationTableScreen screen) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.SCREEN = screen;
        this.MENU = screen.getMenu();
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        switch (MENU.mode) {
            case PUNCH, CHECK -> renderPunch(guiGraphics);
            case CONNECT -> renderConnect(guiGraphics);
        }
    }

    private void renderBg(GuiGraphics guiGraphics) {
        guiGraphics.blit(PerforationTableScreen.GUI_BACKGROUND, getX(), getY(), 0, 168, WIDTH, HEIGHT);
    }

    private void renderOneBeat(GuiGraphics guiGraphics, NoteGridData data, byte page, byte beat, int color) {
        Beat oneBeat = data.getPage(page).getBeat(beat);
        int x = getX() + 2 + beat;
        for (byte note : oneBeat.getNotes()) {
            int y = getY() + 50 - note * 2;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void renderPunch(GuiGraphics guiGraphics) {
        renderBg(guiGraphics);
        for (byte beat = 0; beat < 64; beat++) {
            renderOneBeat(guiGraphics, MENU.data, SCREEN.currentPage, beat, GuiUtils.BLACK);
            if (MENU.helpData != null && MENU.helpData.size() > SCREEN.currentPage) {
                renderOneBeat(guiGraphics, MENU.helpData, SCREEN.currentPage, beat, GuiUtils.HELP_NOTE_COLOR);
            }
        }
    }

    private void renderConnect(GuiGraphics guiGraphics) {
        renderBg(guiGraphics);
        for (byte beat = 0; beat < 64; beat++) {
            renderOneBeat(guiGraphics, MENU.displayData, SCREEN.currentPage, beat, GuiUtils.BLACK);
        }
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void onClick(double x, double y, int button) {
        super.onClick(x, y, button);
        switch (SCREEN.getMenu().mode) {
            case PUNCH, CHECK -> SCREEN.openNoteGridScreen();
            case CONNECT -> GuiUtils.sendCodeToMenu(MENU.containerId, PerforationTableMenu.CODE_CONNECT_NOTE_GRID);
        }
    }
}
