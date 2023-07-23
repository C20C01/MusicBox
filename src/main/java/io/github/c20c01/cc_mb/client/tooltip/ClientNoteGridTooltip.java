package io.github.c20c01.cc_mb.client.tooltip;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.NoteGridOnTableWidget;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
public class ClientNoteGridTooltip implements ClientTooltipComponent {
    private static final MutableComponent SHIFT_TO_PREVIEW = Component.translatable(CCMain.TEXT_SHIFT_TO_PREVIEW).withStyle(ChatFormatting.GRAY);
    private final NoteGrid.Page firstPage;
    private final String numberOfPages;

    public ClientNoteGridTooltip(NoteGrid.Tooltip tooltip) {
        this.firstPage = tooltip.page();
        this.numberOfPages = Component.translatable(CCMain.TEXT_NUMBER_OF_PAGES).getString() + tooltip.numberOfPages();
    }

    private static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    @Override
    public int getHeight() {
        return isShiftDown() ? NoteGridOnTableWidget.HEIGHT + 12 : 10;
    }

    @Override
    public int getWidth(Font font) {
        return isShiftDown() ? Math.max(NoteGridOnTableWidget.WIDTH, font.width(numberOfPages)) : font.width(SHIFT_TO_PREVIEW);
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        if (!Screen.hasShiftDown()) {
            font.drawInBatch(SHIFT_TO_PREVIEW, x, y, 16777215, true, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 16777215);
        }
        ClientTooltipComponent.super.renderText(font, x, y, matrix4f, bufferSource);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (Screen.hasShiftDown()) {
            NoteGridOnTableWidget.renderNoteGrid(guiGraphics, firstPage, x, y);
            guiGraphics.drawString(font, numberOfPages, x, (y + NoteGridOnTableWidget.HEIGHT + 2), 16777215);
        }
        ClientTooltipComponent.super.renderImage(font, x, y, guiGraphics);
    }
}
