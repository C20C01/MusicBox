package io.github.c20c01.cc_mb.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoteGridEditWidget extends AbstractWidget {
    private static final int TRANSLUCENT_BLACK = 838860800;
    private static final int PAPER_COLOR = -133142;
    private static final String[] NOTE_NAME = new String[]{"1", "#1", "2", "#2", "3", "4", "#4", "5", "#5", "6", "#6", "7"};
    private final byte[] mousePosOnGird = new byte[]{-1, -1};
    private final PerforationTableScreen screen;
    private final int noteGridBottom;
    private Component tip = Component.empty();

    public NoteGridEditWidget(int width, int height, PerforationTableScreen screen) {
        super(width / 2 - 202, height / 2 - 125, 405, 171, Component.empty());
        noteGridBottom = getY() + 157;
        this.visible = screen.isEditMode();
        this.screen = screen;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (screen.data == null) return;
        drawNoteGrid(guiGraphics);
        if (mousePosOnGird[0] != -1) {
            drawMousePosOnGird(guiGraphics);
            drawTip(guiGraphics);
        }
    }

    private void drawNoteGrid(GuiGraphics guiGraphics) {
        int leftLine = getX() + 13;

        // 纸带
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, PAPER_COLOR);

        // 横线
        drawHLines(guiGraphics, leftLine);

        // 竖线&音符
        for (byte beat = 0; beat < 64; beat++) {
            guiGraphics.vLine(leftLine + beat * 6, getY() + 12, noteGridBottom + 1, TRANSLUCENT_BLACK);
//            Beat oneBeat = screen.data[screen.page].getBeat(beat);
            int noteLeft = getX() + 12;
            int noteBottom = getY() + 156;
//            for (byte note : oneBeat.getNotes()) {
//                drawOneNote(guiGraphics, noteLeft, noteBottom, beat, note, PerforationTableScreen.BLACK);
//            }
        }
    }

    private void drawHLines(GuiGraphics guiGraphics, int leftLine) {
        int rightLine = getX() + 391;
        for (byte note = 0; note < 25; note++) {
            guiGraphics.hLine(leftLine, rightLine, noteGridBottom - note * 6, TRANSLUCENT_BLACK);
        }
    }

    private void drawOneNote(GuiGraphics guiGraphics, int noteLeft, int noteBottom, byte beat, byte note, int color) {
        int x = noteLeft + beat * 6;
        int y = noteBottom - note * 6;
        guiGraphics.fill(x, y, x + 3, y + 3, color);
    }

    private void drawMousePosOnGird(GuiGraphics guiGraphics) {
        drawOneNote(guiGraphics, getX() + 12, getX() + 391, mousePosOnGird[0], mousePosOnGird[1], TRANSLUCENT_BLACK);
        guiGraphics.hLine(getX() + 13, getX() + 391, noteGridBottom - mousePosOnGird[1] * 6, TRANSLUCENT_BLACK);
        guiGraphics.vLine(getX() + 13 + mousePosOnGird[0] * 6, getY() + 12, noteGridBottom + 1, TRANSLUCENT_BLACK);
    }

    private void drawTip(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, tip, getX() + 202, getY() + 180, PAPER_COLOR);
    }

    @Override
    public void onClick(double x, double y) {
        if (mousePosOnGird[0] != -1) {
            byte page = screen.page;
            byte beat = mousePosOnGird[0];
            byte note = mousePosOnGird[1];
            if (screen.getMenu().punchGrid(page, beat, note)) {
//                var packet = new NoteGridPacket(screen.getMenu().noteGridId, page, beat, note);
//                CCNetwork.CHANNEL.sendToServer(packet);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    protected void setMousePosOnGird(double x, double y) {
        int right = (int) Math.floor((x - (getX() + 10)) / 6);
        int top = (int) Math.floor((noteGridBottom + 3 - y) / 6);

        if (right >= 0 && right < 64) {
            mousePosOnGird[0] = (byte) (right);
        } else {
            mousePosOnGird[0] = -1;
            tip = Component.empty();
            return;
        }
        if (top >= 0 && top < 25) {
            mousePosOnGird[1] = (byte) (top);
        } else {
            mousePosOnGird[0] = -1;
            tip = Component.empty();
            return;
        }

        setTip();
    }

    private void setTip() {
        String note = NOTE_NAME[(mousePosOnGird[1] + 6) % 12] + " (" + mousePosOnGird[1] + ")";
        tip = Component.literal("Beat: " + mousePosOnGird[0] + ", Note: " + note);
    }
}
