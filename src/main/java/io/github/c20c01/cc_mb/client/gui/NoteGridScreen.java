package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.player.MindPlayer;
import io.github.c20c01.cc_mb.player.NoteGridDataHolder;
import io.github.c20c01.cc_mb.player.NoteGridIteratorListener;
import io.github.c20c01.cc_mb.player.SpeakerConfig;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;

public class NoteGridScreen extends Screen implements NoteGridDataHolder, NoteGridIteratorListener {
    protected static final int SELECTION_COLOR = 0x69000000;
    protected static final int PAPER_COLOR = 0xFFFDF7EA;
    protected static final int LINE_COLOR = 0x33000000;
    protected static final int MESSAGE_COLOR = 0xFFFFFFFF;
    protected static final int HALF_GRID_BACKGROUND_WIDTH = 202;// total width = 202 * 2 + 1 = 405
    protected static final int HALF_GRID_BACKGROUND_HEIGHT = 85;// total height = 85 * 2 + 1 = 171
    protected static final int GRID_CENTER_Y = HALF_GRID_BACKGROUND_HEIGHT + 8;// 8 is the top margin
    protected static final int HALF_GRID_WIDTH = 189;// total width = 189 * 2 + 1 = 379
    protected static final int HALF_GRID_HEIGHT = 72;// total height = 72 * 2 + 1 = 145
    protected static final int GRID_SIZE = 6;
    protected static final int PAGE_BUTTON_WIDTH = 23;

    protected final int inventoryKey;
    protected final MindPlayer player;

    @Nullable
    protected NoteGridData helpData;
    protected NoteGridData mainData;
    protected Page mainPage, helpPage;
    protected int centerX, gridLeft, gridRight;
    protected int pageNum = 0, beatNum = 0;
    protected boolean isPlaying = false;
    protected Component message = CommonComponents.EMPTY;
    protected PageButton forwardButton, backButton;

    public NoteGridScreen(NoteGridData mainData, @Nullable NoteGridData helpData, @Nullable SpeakerConfig config) {
        super(GameNarrator.NO_TITLE);
        this.inventoryKey = Minecraft.getInstance().options.keyInventory.getKey().getValue();
        this.mainData = mainData;
        this.helpData = helpData;
        this.player = new MindPlayer(this, this, config);
    }

    public static void openWithNoteGrid(ItemStack noteGrid, ItemStack otherItem) {
        NoteGridData mainData = NoteGridData.ofNoteGrid(noteGrid);
        NoteGridData helpData = NoteGridData.ofItemStack(otherItem);
        SpeakerConfig config = SpeakerConfig.ofItemStack(otherItem);
        Minecraft.getInstance().setScreen(new NoteGridScreen(mainData, helpData, config));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        setPlaying(false);
        super.onClose();
    }

    @Override
    protected void init() {
        centerX = width / 2;
        gridLeft = centerX - HALF_GRID_WIDTH;
        gridRight = centerX + HALF_GRID_WIDTH + 1;

        final int gridBottom = GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 1;
        final int pageButtonY = gridBottom + 4;

        forwardButton = addRenderableWidget(new PageButton(gridRight - PAGE_BUTTON_WIDTH, pageButtonY, true, (_) -> onForwardButton(), false));
        backButton = addRenderableWidget(new PageButton(gridLeft, pageButtonY, false, (_) -> onBackButton(), false));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (_) -> onClose()).bounds(centerX - Button.DEFAULT_WIDTH / 2, gridBottom + 32, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());

        updateWidgets();
    }

    private void updateWidgets() {
        showPageInfo();
        updatePageButtons();
        updateNoteGridPage();
    }

    protected void showPageInfo() {
        message = Component.translatable("book.pageIndicator", pageNum + 1, Math.max(mainData.size(), 1));
    }

    // region render
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        renderNoteGrid(graphics);
        renderHelpLines(graphics);
        graphics.centeredText(font, message, centerX, GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 8, MESSAGE_COLOR);
    }

    private void renderNotes(GuiGraphicsExtractor graphics, ByteList notes, int noteLeft, int color) {
        final int noteRight = noteLeft + 3;
        for (int i = 0; i < notes.size(); i++) {
            final int noteTop = GRID_CENTER_Y + HALF_GRID_HEIGHT - GRID_SIZE * notes.getByte(i) - 1;
            final int noteBottom = noteTop + 3;
            graphics.fill(noteLeft, noteTop, noteRight, noteBottom, color);
        }
    }

    private void renderNoteGrid(GuiGraphicsExtractor graphics) {
        // background
        graphics.fill(centerX - HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y - HALF_GRID_BACKGROUND_HEIGHT, centerX + HALF_GRID_BACKGROUND_WIDTH + 1, GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 1, PAPER_COLOR);

        // hLines
        for (int y = 0; y < Beat.NOTES_SIZE; y++) {
            final int lienTop = GRID_CENTER_Y - HALF_GRID_HEIGHT + GRID_SIZE * y;
            graphics.fill(gridLeft, lienTop, gridRight, lienTop + 1, LINE_COLOR);
        }

        // vLines & notes, note size: 3x3, line width: 1
        for (int x = 0; x < Page.BEATS_SIZE; x++) {
            final int lienLeft = gridLeft + GRID_SIZE * x;
            final int noteLeft = lienLeft - 1;
            graphics.fill(lienLeft, GRID_CENTER_Y - HALF_GRID_HEIGHT, lienLeft + 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, LINE_COLOR);
            if (helpPage != null) {
                renderNotes(graphics, helpPage.getBeat(x).getNotes(), noteLeft, GuiUtils.HELP_NOTE_COLOR);
            }
            renderNotes(graphics, mainPage.getBeat(x).getNotes(), noteLeft, GuiUtils.BLACK);
        }
    }

    protected void renderHelpLines(GuiGraphicsExtractor graphics) {
        if (isPlaying) renderPlayingLine(graphics, beatNum, SELECTION_COLOR);
    }

    protected void renderPlayingLine(GuiGraphicsExtractor graphics, int beatNum, int color) {
        final int lineLeft = gridLeft + GRID_SIZE * beatNum;
        graphics.fill(lineLeft, GRID_CENTER_Y - HALF_GRID_HEIGHT, lineLeft + 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, color);
    }
    // endregion

    // region page
    public void onForwardButton() {
        pageForward();
        setPlaying(false);
    }

    public void onBackButton() {
        pageBack();
        setPlaying(false);
    }

    public void pageForward() {
        if (pageNum < mainData.size() - 1) {
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

    public void updatePageButtons() {
        forwardButton.visible = pageNum < mainData.size() - 1;
        backButton.visible = pageNum > 0;
    }

    private void updateNoteGridPage() {
        this.mainPage = mainData.getPage(pageNum);
        this.helpPage = helpData != null && helpData.size() > pageNum ? helpData.getPage(pageNum) : null;
    }
    // endregion

    // region input
    @Override
    public boolean keyPressed(KeyEvent event) {
        final int key = event.key();
        if (key == inventoryKey) {
            onClose();
            return true;
        }

        if (key == 32) {// space
            setPlaying(!isPlaying);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {// roll up
            if (isPlaying) player.changeTickPerBeat(-1);
            else onBackButton();
        } else {// roll down
            if (isPlaying) player.changeTickPerBeat(+1);
            else onForwardButton();
        }
        return true;
    }
    // endregion

    // region play & data
    @Override
    public void tick() {
        if (isPlaying) player.tick();
    }

    protected void setPlaying(boolean playing) {
        if (playing) {
            player.jumpPageTo(pageNum);
            beatNum = 0;
        }
        isPlaying = playing;
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        this.beatNum = (byte) beatNum;
        return true;
    }

    @Override
    public void onPageChanged() {
        pageForward();
    }

    @Override
    public void onFinished() {
        setPlaying(false);
    }

    @Override
    public NoteGridData getData() {
        return mainData;
    }

    @Override
    public void setData(NoteGridData data) {
        assert data != null;
        this.mainData = data;
        this.mainPage = data.getPage(pageNum);
    }
    // endregion
}
