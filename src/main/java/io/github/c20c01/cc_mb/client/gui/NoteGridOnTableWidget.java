package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
public class NoteGridOnTableWidget extends AbstractWidget {
    private final PerforationTableScreen screen;
    public static final int WIDTH = 68;
    public static final int HEIGHT = 53;

    public NoteGridOnTableWidget(int x, int y, PerforationTableScreen screen) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.visible = !screen.isEditMode();
        this.screen = screen;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (screen.pages == null) return;
        renderNoteGrid(guiGraphics, screen.pages[screen.page], getX(), getY());
    }

    public static void renderNoteGrid(GuiGraphics guiGraphics, NoteGrid.Page page, int x, int y) {
        guiGraphics.blit(PerforationTableScreen.GUI_BACKGROUND, x, y, 0, 168, WIDTH, HEIGHT);
        for (byte beat = 0; beat < 64; beat++) {
            NoteGrid.Beat oneBeat = page.getBeat(beat);
            for (byte note : oneBeat.getNotes()) {
                drawNoteOnTable(guiGraphics, x + 2, y + 50, beat, note);
            }
        }
    }

    private static void drawNoteOnTable(GuiGraphics guiGraphics, int noteGridLeft, int noteGridBottom, byte beat, byte note) {
        int x = noteGridLeft + beat;
        int y = noteGridBottom - note * 2;
        guiGraphics.fill(x, y, x + 1, y + 1, PerforationTableScreen.BLACK);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private void sendIntToServer(int i) {
        var gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(screen.getMenu().containerId, i);
        }
    }

    @Override
    public void onClick(double x, double y) {
        super.onClick(x, y);
        switch (screen.getMenu().mode) {
            case SUPERPOSE -> sendIntToServer(0);
            case CONNECT -> sendIntToServer(1);
            case BOOK -> sendIntToServer(2);
            case PUNCH -> screen.changeEditMode(Boolean.TRUE);
        }
    }

    public void setTip(PerforationTableMenu.Mode mode) {
        Component component = null;
        switch (mode) {
            case SUPERPOSE -> component = Component.translatable(CCMain.TEXT_SUPERPOSE);
            case CONNECT -> component = Component.translatable(CCMain.TEXT_CONNECT);
            case BOOK -> component = Component.translatable(CCMain.TEXT_BOOK);
            case PUNCH -> component = Component.translatable(CCMain.TEXT_PUNCH);
        }
        setTooltip(component == null ? null : Tooltip.create(component));
    }
}
