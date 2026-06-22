package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.inventory.menu.MenuMode;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import io.github.c20c01.cc_mb.inventory.menu.edit.EditDataSender;
import io.github.c20c01.cc_mb.player.MindPlayer;
import io.github.c20c01.cc_mb.player.NoteGridIteratorListener;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;

// TODO 重构

public class NoteGridScreen extends Screen implements NoteGridIteratorListener {
    private static final int PAPER_COLOR = 0xFFFDF7EA;
    private static final int LINE_COLOR = 0x33000000;
    private static final int SELECTION_COLOR = 0x69000000;
    private static final int EDIT_PROGRESS_COLOR = 0x66FF2233;
    private static final int TIP_COLOR = 0xFFFFFFFF;
    private static final String[] NOTE_NAME = new String[]{"1", "#1", "2", "#2", "3", "4", "#4", "5", "#5", "6", "#6", "7"};
    private static final int HALF_GRID_BACKGROUND_WIDTH = 202;// total width = 202 * 2 + 1 = 405
    private static final int HALF_GRID_BACKGROUND_HEIGHT = 85;// total height = 85 * 2 + 1 = 171
    private static final int HALF_GRID_WIDTH = 189;// total width = 189 * 2 + 1 = 379
    private static final int HALF_GRID_HEIGHT = 72;// total height = 72 * 2 + 1 = 145
    private static final int GRID_CENTER_Y = HALF_GRID_BACKGROUND_HEIGHT + 8;// 8 is the top margin
    private static final int GRID_SIZE = 6;
    private static final int HALF_GRID_SIZE = GRID_SIZE / 2;
    private static final byte JUDGMENT_INTERVAL_TICK = 5;// max tick you can early the beat (1tick = 50ms)

    private final byte[] mousePos = new byte[]{-1, -1};// {x, y}, {0, 0} at bottom left
    private final EditDataSender editDataSender;
    private final MindPlayer player;
    @Nullable
    private final NoteGridData helpData;// show a translucent note grid to help player to copy notes from this data
    private NoteGridData mainData;// play or edit on this data
    private PerforationTableScreen tableScreen = null;
    private Component tip = CommonComponents.EMPTY;
    private int currentPage;
    private int beatNumber;
    private int playProgressLineColor = SELECTION_COLOR;
    private PageButton forwardButton;
    private PageButton backButton;
    private boolean editMode = false;// punch or fix mode
    private boolean playing = false;
    private boolean paused = false;// Whether the player is waiting for the player to punch notes.
    private boolean punchFail = false;// Fail to punch at current beat, avoid punching fail repeatedly.
    private MenuMode mode = MenuMode.CHECK;

    /**
     * Opened from a note grid item.
     */
    public NoteGridScreen(NoteGridData mainData) {
        super(GameNarrator.NO_TITLE);
        this.mainData = mainData;
        helpData = null;
        editDataSender = null;
        player = new MindPlayer(mainData, this, Minecraft.getInstance().player);
    }

    /**
     * Opened from a perforation table screen.
     */
    public NoteGridScreen(PerforationTableScreen screen) {
        super(GameNarrator.NO_TITLE);
        mainData = screen.getMenu().getData();
        helpData = screen.getMenu().getHelpData();
        editDataSender = new EditDataSender(screen.getMenu().containerId);
        tableScreen = screen;
        currentPage = Math.min(screen.currentPage, getNumPages() - 1);
        mode = screen.getMenu().getMode();
        editMode = mode == MenuMode.PUNCH || mode == MenuMode.FIX;
        playProgressLineColor = mode == MenuMode.PUNCH ? EDIT_PROGRESS_COLOR : SELECTION_COLOR;
        player = new MindPlayer(mainData, this, Minecraft.getInstance().player);
    }

    public static void open(NoteGridData data) {
        Minecraft.getInstance().setScreen(new NoteGridScreen(data));
    }

    @Override
    public void onClose() {
        playing = false;
        if (tableScreen != null) {
            GuiUtils.sendCodeToMenu(tableScreen.getMenu().containerId, PerforationTableMenu.CODE_SAVE_NOTE_GRID);
            tableScreen.noteGridScreen = null;
        }
        if (editDataSender != null) {
            editDataSender.reset();
        }
        super.onClose();
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
    }

    protected void createMenuControls() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (_) -> onClose()).bounds((this.width - Button.DEFAULT_WIDTH) / 2, GRID_CENTER_Y + HALF_GRID_HEIGHT + 32, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());
    }

    protected void createPageControlButtons() {
        final int gridCenterX = width / 2;
        final int y = GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 4;
        // 23 is the width of the button
        forwardButton = this.addRenderableWidget(new PageButton(gridCenterX + HALF_GRID_BACKGROUND_WIDTH - 23, y, true, (_) -> this.pageForward(), false));
        backButton = this.addRenderableWidget(new PageButton(gridCenterX - HALF_GRID_BACKGROUND_WIDTH, y, false, (_) -> this.pageBack(), false));
        updatePageStuff(true);
    }

    private int getNumPages() {
        return mainData.size();
    }

    protected void pageBack() {
        if (this.currentPage > 0) {
            --this.currentPage;
        }

        this.updatePageStuff(true);
    }

    protected void pageForward() {
        if (this.currentPage < this.getNumPages() - 1) {
            ++this.currentPage;
        }

        this.updatePageStuff(true);
    }

    private void updatePageStuff(boolean stopPlaying) {
        forwardButton.visible = this.currentPage < this.getNumPages() - 1;
        backButton.visible = this.currentPage > 0;
        if (stopPlaying) {
            setPlaying(false);
        }
        updatePageTip();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
        if (editMode) {
            updateMousePosAndTip(x, y);
        }
    }

    private void updateMousePosAndTip(double x, double y) {
        final int left = width / 2 - HALF_GRID_WIDTH - HALF_GRID_SIZE;
        final int bottom = GRID_CENTER_Y + HALF_GRID_HEIGHT + HALF_GRID_SIZE + 1;// +1 for line width
        byte posX = (byte) Math.floor((x - left) / GRID_SIZE);
        byte posY = (byte) Math.floor((bottom - y) / GRID_SIZE);
        if (posX < 0 || posX >= 64 || posY < 0 || posY >= 25) {
            if (mousePos[0] != -1) {
                mousePos[0] = -1;
                updateTip();
            }
            return;
        }
        if (mousePos[0] != posX || mousePos[1] != posY) {
            mousePos[0] = posX;
            mousePos[1] = posY;
            updateTip();
        }
    }

    private boolean pointNote() {
        return mousePos[0] != -1;
    }

    private void updateTip() {
        if (!pointNote() || playing) {
            updatePageTip();
        } else {
            String note = NOTE_NAME[(mousePos[1] + 6) % 12] + " (" + mousePos[1] + ")";
            tip = Component.literal("Beat: " + mousePos[0] + ", Note: " + note);
        }
    }

    private void updatePageTip() {
        tip = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        renderNoteGrid(graphics);
    }

    protected void renderNoteGrid(GuiGraphicsExtractor graphics) {
        final int gridCenterX = width / 2;

        // background
        graphics.fill(gridCenterX - HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y - HALF_GRID_BACKGROUND_HEIGHT, gridCenterX + HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT, PAPER_COLOR);

        // hLines
        for (int y = 0; y < 25; y++) {
            graphics.horizontalLine(gridCenterX - HALF_GRID_WIDTH, gridCenterX + HALF_GRID_WIDTH, GRID_CENTER_Y - HALF_GRID_HEIGHT + y * GRID_SIZE, LINE_COLOR);
        }

        // vLines & notes
        Page currentMainPage = mainData.getPage(currentPage);
        final boolean invisibleHelpData = helpData == null || helpData.size() <= currentPage;
        Page currentHelpPage = invisibleHelpData ? null : helpData.getPage(currentPage);
        for (int x = 0; x < 64; x++) {
            // vLine
            graphics.verticalLine(gridCenterX - HALF_GRID_WIDTH + x * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, LINE_COLOR);
            // main data notes
            final int crossRectangleLeft = gridCenterX - HALF_GRID_WIDTH + x * GRID_SIZE;
            ByteList notes = currentMainPage.getBeat(x).getNotes();
            for (int i = 0; i < notes.size(); i++) {
                final int crossRectangleTop = GRID_CENTER_Y + HALF_GRID_HEIGHT - notes.getByte(i) * GRID_SIZE;
                // Note size: 3x3 Line width: 1
                graphics.fill(crossRectangleLeft - 1, crossRectangleTop - 1, crossRectangleLeft + 2, crossRectangleTop + 2, GuiUtils.BLACK);
            }
            // help data notes
            if (invisibleHelpData) continue;
            ByteList helpNotes = currentHelpPage.getBeat(x).getNotes();
            for (int i = 0; i < helpNotes.size(); i++) {
                final int crossRectangleTop = GRID_CENTER_Y + HALF_GRID_HEIGHT - helpNotes.getByte(i) * GRID_SIZE;
                // Note size: 3x3 Line width: 1
                graphics.fill(crossRectangleLeft - 1, crossRectangleTop - 1, crossRectangleLeft + 2, crossRectangleTop + 2, GuiUtils.HELP_NOTE_COLOR);
            }
        }

        if (playing) {
            // playing progress line
            graphics.verticalLine(gridCenterX - HALF_GRID_WIDTH + beatNumber * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, playProgressLineColor);
        } else if (editMode && pointNote()) {
            // selection lines
            graphics.horizontalLine(gridCenterX - HALF_GRID_WIDTH, gridCenterX + HALF_GRID_WIDTH, GRID_CENTER_Y + HALF_GRID_HEIGHT - mousePos[1] * GRID_SIZE, SELECTION_COLOR);
            graphics.verticalLine(gridCenterX - HALF_GRID_WIDTH + mousePos[0] * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, SELECTION_COLOR);
        }

        // tip
        graphics.centeredText(font, tip, gridCenterX, GRID_CENTER_Y + HALF_GRID_HEIGHT + 16, TIP_COLOR);
    }

    /**
     * Set playing state and reset beat.
     * Usually call {@link #updateTip()} after this method.
     */
    private void setPlaying(boolean playing) {
        this.playing = playing;
        beatNumber = 0;
        player.jumpPageTo(currentPage);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        switch (key) {
            case 90, 88 -> {
                // Z, X: punch with help data
                if (mode == MenuMode.PUNCH && playing) {
                    tryToPunchWithHelpData();
                    return true;
                }
                return super.keyPressed(event);
            }
            case 32 -> {
                // SPACE: play/pause
                setPlaying(!playing);
                updateTip();
                updatePauseState();
                return true;
            }
            default -> {
                if (key == Minecraft.getInstance().options.keyInventory.getKey().getValue()) {
                    // INVENTORY: close
                    onClose();
                    return true;
                }
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (playing) {
            if (scrollY > 0) {
                // roll up: speed up
                player.ticker.setTickPerBeat(player.ticker.getTickPerBeat() - 1);
            } else {
                // roll down: speed down
                player.ticker.setTickPerBeat(player.ticker.getTickPerBeat() + 1);
            }
        } else {
            if (scrollY > 0) {
                // roll up: page back
                pageBack();
            } else {
                // roll down: page forward
                pageForward();
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (playing && !paused) {
            player.ticker.tick();
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int button = event.button();
        if (mode == MenuMode.PUNCH) {
            if (playing) {
                tryToPunchWithHelpData();
            } else if (pointNote()) {
                if (button == 0) {
                    tryToPunch();
                } else if (button == 1) {
                    previewBeat(true);
                }
            }
            return true;
        }
        if (mode == MenuMode.FIX) {
            if (!playing && pointNote()) {
                if (button == 0) {
                    tryToFix();
                } else if (button == 1) {
                    previewBeat(false);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Preview the sound of the beat at current mouse position.
     *
     * @param addMode true: add pointed note, false: remove pointed note.
     */
    private void previewBeat(boolean addMode) {
        Beat beat = mainData.getPage(currentPage).getBeat(mousePos[0]);
        byte pointedNote = mousePos[1];
        ByteList previewNotes = addMode ? beat.getAddPreviewNotes(pointedNote) : beat.getRemovePreviewNotes(pointedNote);
        player.playNotes(previewNotes);
    }

    private void tryToFix() {
        // remove a note on client and send the fix to server
        final byte page = (byte) currentPage;
        final byte beat = mousePos[0];
        final byte note = mousePos[1];
        NoteGridData editedData = mainData.withNoteChanged(page, beat, note, false);
        if (editedData != null) {
            mainData = editedData;
            editDataSender.send(page, beat, note);
            GuiUtils.playSound(SoundEvents.SLIME_BLOCK_FALL);
        }
    }

    private void tryToPunch() {
        // punch a note on client and send the punch to server
        final byte page = (byte) currentPage;
        final byte beat = mousePos[0];
        final byte note = mousePos[1];
        NoteGridData editedData = mainData.withNoteChanged(page, beat, note, true);
        if (editedData != null) {
            mainData = editedData;
            editDataSender.send(page, beat, note);
            GuiUtils.playSound(SoundEvents.BOOK_PUT);
        }
    }

    private void tryToPunchWithHelpData() {
        if (helpData != null && !punchWithHelpData() && !punchFail) {
            // punched at wrong beat, damage the tool as a punishment
            punchFail = true;
            GuiUtils.sendCodeToMenu(tableScreen.getMenu().containerId, PerforationTableMenu.CODE_PUNCH_FAIL);
            GuiUtils.playSound(SoundEvents.VILLAGER_NO);
        }
    }

    /**
     * @return True if player punched successfully at beat in judgment interval.
     */
    private boolean punchWithHelpData() {
        assert helpData != null;
        final byte page = (byte) currentPage;
        final byte beat = (byte) beatNumber;
        if (helpData.size() <= page) {
            // no help data
            return false;
        }
        if (punchWithHelpData(page, beat, true)) {
            // filled successfully at current beat
            return true;
        }
        if (player.getTickToNextBeat() > JUDGMENT_INTERVAL_TICK) {
            // out of judgment interval
            return false;
        }
        return punchWithHelpDataInInterval(page, beat);
    }

    /**
     * Punch the nearest beat's notes in judgment interval.
     * The beat can be nonadjacent to the current beat.
     *
     * @return True if player punched successfully at beat in judgment interval.
     */
    private boolean punchWithHelpDataInInterval(byte page, byte beat) {
        assert helpData != null;
        int beatsInInterval = 1 + (JUDGMENT_INTERVAL_TICK - player.getTickToNextBeat()) / player.ticker.getTickPerBeat();
        // check next several beats
        for (int i = 0; i < beatsInInterval; i++) {
            if (++beat >= Page.BEATS_SIZE) {
                beat = 0;
                page++;
                if (page >= helpData.size() || page >= mainData.size()) {
                    return false;
                }
            }
            if (punchWithHelpData(page, beat, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Punch notes from help data to main data for whole beat.
     *
     * @param skipWaiting Whether to skip waiting after punched successfully
     *                    to restore the playing fast.
     * @return True if player punched successfully at beat in judgment interval.
     */
    private boolean punchWithHelpData(byte page, byte beat, boolean skipWaiting) {
        assert helpData != null;
        ByteList notes = helpData.getPage(page).getBeat(beat).getNotes();
        if (notes.isEmpty()) return false;

        boolean punchedAtLeastOneNote = false;
        for (int i = 0; i < notes.size(); i++) {
            byte note = notes.getByte(i);
            NoteGridData editedData = mainData.withNoteChanged(page, beat, note, true);
            if (editedData != null) {
                mainData = editedData;
                editDataSender.send(page, beat, note);
                GuiUtils.playSound(SoundEvents.BOOK_PUT);
                punchedAtLeastOneNote = true;
            }
            if (!editMode) break;
        }
        if (punchedAtLeastOneNote) {
            if (skipWaiting) player.skipWaiting();
            paused = false;
            return true;
        }
        return false;
    }

    private void updatePauseState() {
        if (helpData == null) {
            paused = false;
        } else {
            punchFail = false;
            paused = mode == MenuMode.PUNCH && !NoteGridUtils.containsAll(mainData, helpData, currentPage, beatNumber);
        }
    }

    /**
     * Called by {@link PerforationTableScreen#onItemChanged}
     * to exit edit mode because of the broken tool.
     */
    protected void exitEditMode() {
        // TODO 纯靠这里来维护状态感觉很麻烦
        if (editMode) {
            editMode = false;
            mousePos[0] = -1;// make mouse position invalid
            paused = false;
            playProgressLineColor = SELECTION_COLOR;
            mode = MenuMode.CHECK;
            updateTip();
        }
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        this.beatNumber = beatNum;
        updatePauseState();
        return paused;
    }

    @Override
    public void onPageChanged(int pageNum) {
        currentPage = pageNum;
        updatePageStuff(false);
    }

    @Override
    public void onFinish() {
        setPlaying(false);
        updateTip();
    }
}
