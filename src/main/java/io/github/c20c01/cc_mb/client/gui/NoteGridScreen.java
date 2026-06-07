package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.client.GuiUtils;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.inventory.menu.MenuMode;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import io.github.c20c01.cc_mb.inventory.menu.edit.EditDataSender;
import io.github.c20c01.cc_mb.player.MindPlayer;
import io.github.c20c01.cc_mb.player.NoteGridIteratorListener;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
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
    private final NoteGridData mainData;// play or edit on this data
    private final NoteGridData helpData;// show a translucent note grid to help player to copy notes from this data
    private final MindPlayer player;
    private PerforationTableScreen tableScreen = null;
    private Component tip = CommonComponents.EMPTY;
    private byte currentPage;
    private byte beatNumber;
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
        currentPage = (byte) Math.min(screen.currentPage, getNumPages() - 1);
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
        final int GRID_CENTER_X = width / 2;
        final int Y = GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 4;
        // 23 is the width of the button
        forwardButton = this.addRenderableWidget(new PageButton(GRID_CENTER_X + HALF_GRID_BACKGROUND_WIDTH - 23, Y, true, (_) -> this.pageForward(true), false));
        backButton = this.addRenderableWidget(new PageButton(GRID_CENTER_X - HALF_GRID_BACKGROUND_WIDTH, Y, false, (_) -> this.pageBack(), false));
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

    protected void pageForward(boolean stopPlaying) {
        if (this.currentPage < this.getNumPages() - 1) {
            ++this.currentPage;
        }

        this.updatePageStuff(stopPlaying);
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
        final int LEFT = width / 2 - HALF_GRID_WIDTH - HALF_GRID_SIZE;
        final int BOTTOM = GRID_CENTER_Y + HALF_GRID_HEIGHT + HALF_GRID_SIZE + 1;// +1 for line width
        int posX = (int) Math.floor((x - LEFT) / GRID_SIZE);
        int posY = (int) Math.floor((BOTTOM - y) / GRID_SIZE);
        if (posX < 0 || posX >= 64 || posY < 0 || posY >= 25) {
            if (mousePos[0] != -1) {
                mousePos[0] = -1;
                updateTip();
            }
            return;
        }
        if (mousePos[0] != posX || mousePos[1] != posY) {
            mousePos[0] = (byte) posX;
            mousePos[1] = (byte) posY;
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
        final int GRID_CENTER_X = width / 2;

        // background
        graphics.fill(GRID_CENTER_X - HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y - HALF_GRID_BACKGROUND_HEIGHT, GRID_CENTER_X + HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT, PAPER_COLOR);

        // hLines
        for (byte y = 0; y < 25; y++) {
            graphics.horizontalLine(GRID_CENTER_X - HALF_GRID_WIDTH, GRID_CENTER_X + HALF_GRID_WIDTH, GRID_CENTER_Y - HALF_GRID_HEIGHT + y * GRID_SIZE, LINE_COLOR);
        }

        // vLines & notes
        for (byte x = 0; x < 64; x++) {
            graphics.verticalLine(GRID_CENTER_X - HALF_GRID_WIDTH + x * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, LINE_COLOR);
            Beat mainBeat = mainData.getPage(currentPage).getBeat(x);
            final int CROSS_RECTANGLE_LEFT = GRID_CENTER_X - HALF_GRID_WIDTH + x * GRID_SIZE;
            for (byte note : mainBeat.getNotes()) {
                final int CROSS_RECTANGLE_TOP = GRID_CENTER_Y + HALF_GRID_HEIGHT - note * GRID_SIZE;
                // Note size: 3x3 Line width: 1
                graphics.fill(CROSS_RECTANGLE_LEFT - 1, CROSS_RECTANGLE_TOP - 1, CROSS_RECTANGLE_LEFT + 2, CROSS_RECTANGLE_TOP + 2, GuiUtils.BLACK);
            }
            if (helpData != null && helpData.size() > currentPage) {
                Beat helpBeat = helpData.getPage(currentPage).getBeat(x);
                for (byte note : helpBeat.getNotes()) {
                    final int CROSS_RECTANGLE_TOP = GRID_CENTER_Y + HALF_GRID_HEIGHT - note * GRID_SIZE;
                    // Note size: 3x3 Line width: 1
                    graphics.fill(CROSS_RECTANGLE_LEFT - 1, CROSS_RECTANGLE_TOP - 1, CROSS_RECTANGLE_LEFT + 2, CROSS_RECTANGLE_TOP + 2, GuiUtils.HELP_NOTE_COLOR);
                }
            }
        }

        if (playing) {
            // playing progress line
            graphics.verticalLine(GRID_CENTER_X - HALF_GRID_WIDTH + beatNumber * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, playProgressLineColor);
        } else if (editMode && pointNote()) {
            // selection lines
            graphics.horizontalLine(GRID_CENTER_X - HALF_GRID_WIDTH, GRID_CENTER_X + HALF_GRID_WIDTH, GRID_CENTER_Y + HALF_GRID_HEIGHT - mousePos[1] * GRID_SIZE, SELECTION_COLOR);
            graphics.verticalLine(GRID_CENTER_X - HALF_GRID_WIDTH + mousePos[0] * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, SELECTION_COLOR);
        }

        // tip
        graphics.centeredText(font, tip, GRID_CENTER_X, GRID_CENTER_Y + HALF_GRID_HEIGHT + 16, TIP_COLOR);
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
                pageForward(true);
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
     * @param mode true: add pointed note, false: remove pointed note.
     */
    private void previewBeat(boolean mode) {
        ByteArraySet notes = mainData.getPage(currentPage).getBeat(mousePos[0]).getNotes().clone();
        byte pointedNote = mousePos[1];
        if (mode) {
            notes.add(pointedNote);
        } else {
            notes.remove(pointedNote);
        }
        player.playNotes(notes);
    }

    private void tryToFix() {
        // remove a note on client and send the fix to server
        final byte PAGE = currentPage;
        if (mainData.getPage(PAGE).getBeat(mousePos[0]).removeNote(mousePos[1])) {
            editDataSender.send(PAGE, mousePos[0], mousePos[1]);
            GuiUtils.playSound(SoundEvents.SLIME_BLOCK_FALL);
        }
    }

    private void tryToPunch() {
        // punch a note on client and send the punch to server
        final byte PAGE = currentPage;
        if (mainData.getPage(PAGE).getBeat(mousePos[0]).addNote(mousePos[1])) {
            editDataSender.send(PAGE, mousePos[0], mousePos[1]);
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
        final byte PAGE = currentPage;
        final byte BEAT = beatNumber;
        if (helpData.size() <= PAGE) {
            // no help data
            return false;
        }
        if (punchWithHelpData(PAGE, BEAT, true)) {
            // filled successfully at current beat
            return true;
        }
        if (player.getTickToNextBeat() > JUDGMENT_INTERVAL_TICK) {
            // out of judgment interval
            return false;
        }
        return punchWithHelpDataInInterval(PAGE, BEAT);
    }

    /**
     * Punch the nearest beat's notes in judgment interval.
     * The beat can be nonadjacent to the current beat.
     *
     * @return True if player punched successfully at beat in judgment interval.
     */
    private boolean punchWithHelpDataInInterval(byte page, byte beat) {
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
        if (helpData.getPage(page).isEmptyBeat(beat)) {
            return false;
        }
        Beat mainBeat = mainData.getPage(page).getBeat(beat);
        Beat helpBeat = helpData.getPage(page).getBeat(beat);
        ByteArraySet helpBeatNotes = helpBeat.getNotes();
        boolean punchNewNote = false;
        for (byte note : helpBeatNotes) {
            if (mainBeat.addNote(note)) {
                editDataSender.send(page, beat, note);
                if (mode != MenuMode.PUNCH) {
                    break;
                }
                punchNewNote = true;
            }
        }
        if (punchNewNote) {
            if (skipWaiting) {
                player.skipWaiting();
            }
            paused = false;
            return true;
        }
        return false;
    }

    private void updatePauseState() {
        if (helpData == null) {
            paused = false;
            return;
        }
        punchFail = false;
        paused = mode == MenuMode.PUNCH && !NoteGridUtils.containsAll(mainData, helpData, currentPage, beatNumber);
    }

    /**
     * Called by {@link PerforationTableScreen#onItemChanged}
     * to exit edit mode because of the broken tool.
     */
    protected void exitEditMode() {
        if (editMode) {
            editMode = false;
            paused = false;
            playProgressLineColor = SELECTION_COLOR;
            updateTip();
        }
    }

    @Override
    public boolean onBeat(Beat beat, byte beatNumber) {
        this.beatNumber = beatNumber;
        updatePauseState();
        return paused;
    }

    @Override
    public void onPageChanged(byte pageNum) {
        pageForward(false);
    }

    @Override
    public void onFinish() {
        setPlaying(false);
        updateTip();
    }
}
