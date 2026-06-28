package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.inventory.menu.MenuMode;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The widget that displays the note grid and trigger note grid handling, used in the {@link io.github.c20c01.cc_mb.client.gui.PerforationTableScreen PerforationTableScreen}.
 */
public class NoteGridWidget extends AbstractWidget {
    private static final Component CANNOT_CUT = Component.translatable(MusicBox.TEXT_CANNOT_CUT);
    private static final int WIDTH = 68;
    private static final int HEIGHT = 53;

    private final Runnable onClick;

    @Nullable
    private Page mainPage, helpPage;
    private LineType lineType = LineType.NO_LINE;

    public NoteGridWidget(int x, int y, Runnable onClick) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.onClick = onClick;
    }

    public void update(@Nullable NoteGridData mainData, @Nullable NoteGridData helpData, MenuMode mode, int pageNum) {
        switch (mode) {
            case PUNCH, CHECK, FIX -> {
                assert mainData != null;
                mainPage = mainData.getPage(pageNum);
                helpPage = helpData != null && helpData.size() > pageNum ? helpData.getPage(pageNum) : null;
                lineType = LineType.NO_LINE;
                setTooltip(mode.message);
            }
            case CONNECT -> {
                assert mainData != null && helpData != null;
                mainPage = mainData.getPage(pageNum);
                helpPage = null;
                lineType = pageNum == mainData.size() - helpData.size() - 1 ? LineType.CONNECT_LINE : LineType.NO_LINE;
                setTooltip(mode.message);
            }
            case CUT -> {
                assert mainData != null;
                mainPage = mainData.getPage(pageNum);
                helpPage = null;
                boolean notLastPage = pageNum < mainData.size() - 1;
                lineType = notLastPage ? LineType.CUT_LINE : LineType.NO_LINE;
                setTooltip(notLastPage ? mode.message : CANNOT_CUT);
            }
            default -> {
                mainPage = null;
                helpPage = null;
                lineType = LineType.NO_LINE;
                setTooltip(mode.message);
            }
        }
    }

    private void setTooltip(Component message) {
        setTooltip(Tooltip.create(message));
    }

    protected void renderNotes(GuiGraphicsExtractor graphics, ByteList notes, int noteLeft, int color) {
        final int gridBottom = getY() + 50;
        for (int i = 0; i < notes.size(); i++) {
            final int noteTop = gridBottom - 2 * notes.getByte(i);
            graphics.fill(noteLeft, noteTop, noteLeft + 1, noteTop + 1, color);
        }
    }

    @Override
    protected void extractWidgetRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        if (mainPage == null) return;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PerforationTableScreen.BACKGROUND, getX(), getY(), 0, 168, WIDTH, HEIGHT, 256, 256);

        for (int x = 0; x < Page.BEATS_SIZE; x++) {
            final int noteLeft = getX() + 2 + x;
            if (helpPage != null) {
                renderNotes(guiGraphics, helpPage.getBeat(x).getNotes(), noteLeft, GuiUtils.HELP_NOTE_COLOR);
            }
            renderNotes(guiGraphics, mainPage.getBeat(x).getNotes(), noteLeft, GuiUtils.BLACK);
        }

        if (lineType != LineType.NO_LINE) {
            final int cutLineRight = getX() + WIDTH;
            guiGraphics.fill(cutLineRight - 1, getY(), cutLineRight, getY() + HEIGHT, lineType.color);
        }
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        onClick.run();
    }

    private enum LineType {
        NO_LINE(0),
        CUT_LINE(0xFFCC2001),
        CONNECT_LINE(0xFF01CC20);

        private final int color;

        LineType(int color) {
            this.color = color;
        }
    }
}
