package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.util.GuiUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.AbstractNoteGridPlayer;
import io.github.c20c01.cc_mb.util.player.MindPlayer;
import io.github.c20c01.cc_mb.util.player.PlayerListener;
import io.github.c20c01.cc_mb.util.punch.PunchDataSender;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoteGridScreen extends Screen implements PlayerListener {
    private static final int PAPER_COLOR = 0xFFFDF7EA;
    private static final int LINE_COLOR = 0x33000000;
    private static final int SELECTION_COLOR = 0x69000000;
    private static final int EDIT_PROGRESS_COLOR = 0x66FF2233;
    private static final String[] NOTE_NAME = new String[]{"1", "#1", "2", "#2", "3", "4", "#4", "5", "#5", "6", "#6", "7"};
    private static final int HALF_GRID_BACKGROUND_WIDTH = 202;// total width = 202 * 2 + 1 = 405
    private static final int HALF_GRID_BACKGROUND_HEIGHT = 85;// total height = 85 * 2 + 1 = 171
    private static final int HALF_GRID_WIDTH = 189;// total width = 189 * 2 + 1 = 379
    private static final int HALF_GRID_HEIGHT = 72;// total height = 72 * 2 + 1 = 145
    private static final int GRID_CENTER_Y = HALF_GRID_BACKGROUND_HEIGHT + 8;// 8 is the top margin
    private static final int GRID_SIZE = 6;
    private static final int HALF_GRID_SIZE = GRID_SIZE / 2;
    private static final byte JUDGMENT_INTERVAL_TICK = 5;// max tick you can early the beat (1tick = 50ms)

    private final byte[] MOUSE_POS = new byte[]{-1, -1};// {x, y}, {0, 0} at bottom left
    private final PunchDataSender PUNCH_DATA_SENDER;
    private final NoteGridData MAIN_DATA;// play or edit on this data
    private final NoteGridData HELP_DATA;// show a translucent note grid to help player to copy notes from this data
    private PerforationTableScreen tableScreen = null;
    private Component tip = CommonComponents.EMPTY;
    private byte currentPage;
    private byte beat;
    private int playProgressLineColor = SELECTION_COLOR;
    private PageButton forwardButton;
    private PageButton backButton;
    private boolean canEdit = false;// When the tool is broken it will be set to false, which should only happen once.
    private boolean playing = false;
    private boolean paused = false;
    private boolean punchFail = false;// Fail to punch at current beat, avoid punching fail repeatedly.

    /**
     * Opened from a note grid item.
     */
    public NoteGridScreen(NoteGridData mainData) {
        super(GameNarrator.NO_TITLE);
        MAIN_DATA = mainData;
        HELP_DATA = null;
        PUNCH_DATA_SENDER = null;
        MindPlayer.getInstance().init(mainData, this);
    }

    /**
     * Opened from a perforation table screen.
     */
    public NoteGridScreen(PerforationTableScreen screen) {
        super(GameNarrator.NO_TITLE);
        MAIN_DATA = screen.getMenu().data;
        HELP_DATA = screen.getMenu().helpData;
        PUNCH_DATA_SENDER = new PunchDataSender(screen.getMenu().containerId);
        tableScreen = screen;
        currentPage = (byte) Math.min(screen.currentPage, getNumPages() - 1);
        canEdit = screen.getMenu().mode == MenuMode.PUNCH;
        playProgressLineColor = canEdit ? EDIT_PROGRESS_COLOR : SELECTION_COLOR;
        MindPlayer.getInstance().init(MAIN_DATA, this);
    }

    @Override
    public void onClose() {
        playing = false;
        MindPlayer.getInstance().reset();
        if (tableScreen != null) {
            GuiUtils.sendCodeToMenu(tableScreen.getMenu().containerId, PerforationTableMenu.CODE_SAVE_NOTE_GRID);
            tableScreen.noteGridScreen = null;
        }
        if (PUNCH_DATA_SENDER != null) {
            PUNCH_DATA_SENDER.reset();
        }
        super.onClose();
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
    }

    protected void createMenuControls() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (b) -> onClose()).bounds((this.width - Button.DEFAULT_WIDTH) / 2, GRID_CENTER_Y + HALF_GRID_HEIGHT + 32, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());
    }

    protected void createPageControlButtons() {
        final int GRID_CENTER_X = width / 2;
        final int Y = GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT + 4;
        // 23 is the width of the button
        forwardButton = this.addRenderableWidget(new PageButton(GRID_CENTER_X + HALF_GRID_BACKGROUND_WIDTH - 23, Y, true, (b) -> this.pageForward(true), false));
        backButton = this.addRenderableWidget(new PageButton(GRID_CENTER_X - HALF_GRID_BACKGROUND_WIDTH, Y, false, (b) -> this.pageBack(), false));
        updatePageStuff(true);
    }

    private int getNumPages() {
        return MAIN_DATA.size();
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
        if (canEdit) {
            updateMousePosAndTip(x, y);
        }
    }

    private void updateMousePosAndTip(double x, double y) {
        final int LEFT = width / 2 - HALF_GRID_WIDTH - HALF_GRID_SIZE;
        final int BOTTOM = GRID_CENTER_Y + HALF_GRID_HEIGHT + HALF_GRID_SIZE + 1;// +1 for line width
        int posX = (int) Math.floor((x - LEFT) / GRID_SIZE);
        int posY = (int) Math.floor((BOTTOM - y) / GRID_SIZE);
        if (posX < 0 || posX >= 64 || posY < 0 || posY >= 25) {
            if (MOUSE_POS[0] != -1) {
                MOUSE_POS[0] = -1;
                updateTip();
            }
            return;
        }
        if (MOUSE_POS[0] != posX || MOUSE_POS[1] != posY) {
            MOUSE_POS[0] = (byte) posX;
            MOUSE_POS[1] = (byte) posY;
            updateTip();
        }
    }

    private void updateTip() {
        if (MOUSE_POS[0] == -1 || playing) {
            updatePageTip();
        } else {
            String note = NOTE_NAME[(MOUSE_POS[1] + 6) % 12] + " (" + MOUSE_POS[1] + ")";
            tip = Component.literal("Beat: " + MOUSE_POS[0] + ", Note: " + note);
        }
    }

    private void updatePageTip() {
        tip = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderNoteGrid(graphics);
    }

    protected void renderNoteGrid(GuiGraphics graphics) {
        final int GRID_CENTER_X = width / 2;

        // background
        graphics.fill(GRID_CENTER_X - HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y - HALF_GRID_BACKGROUND_HEIGHT, GRID_CENTER_X + HALF_GRID_BACKGROUND_WIDTH, GRID_CENTER_Y + HALF_GRID_BACKGROUND_HEIGHT, PAPER_COLOR);

        // hLines
        for (byte y = 0; y < 25; y++) {
            graphics.hLine(GRID_CENTER_X - HALF_GRID_WIDTH, GRID_CENTER_X + HALF_GRID_WIDTH, GRID_CENTER_Y - HALF_GRID_HEIGHT + y * GRID_SIZE, LINE_COLOR);
        }

        // vLines & notes
        for (byte x = 0; x < 64; x++) {
            graphics.vLine(GRID_CENTER_X - HALF_GRID_WIDTH + x * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, LINE_COLOR);
            Beat mainBeat = MAIN_DATA.getPage(currentPage).getBeat(x);
            final int CROSS_RECTANGLE_LEFT = GRID_CENTER_X - HALF_GRID_WIDTH + x * GRID_SIZE;
            for (byte note : mainBeat.getNotes()) {
                final int CROSS_RECTANGLE_TOP = GRID_CENTER_Y + HALF_GRID_HEIGHT - note * GRID_SIZE;
                // Note size: 3x3 Line width: 1
                graphics.fill(CROSS_RECTANGLE_LEFT - 1, CROSS_RECTANGLE_TOP - 1, CROSS_RECTANGLE_LEFT + 2, CROSS_RECTANGLE_TOP + 2, GuiUtils.BLACK);
            }
            if (HELP_DATA != null && HELP_DATA.size() > currentPage) {
                Beat helpBeat = HELP_DATA.getPage(currentPage).getBeat(x);
                for (byte note : helpBeat.getNotes()) {
                    final int CROSS_RECTANGLE_TOP = GRID_CENTER_Y + HALF_GRID_HEIGHT - note * GRID_SIZE;
                    // Note size: 3x3 Line width: 1
                    graphics.fill(CROSS_RECTANGLE_LEFT - 1, CROSS_RECTANGLE_TOP - 1, CROSS_RECTANGLE_LEFT + 2, CROSS_RECTANGLE_TOP + 2, GuiUtils.HELP_NOTE_COLOR);
                }
            }
        }

        if (playing) {
            // playing progress line
            graphics.vLine(GRID_CENTER_X - HALF_GRID_WIDTH + beat * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, playProgressLineColor);
        } else if (canEdit && MOUSE_POS[0] != -1) {
            // selection lines
            graphics.hLine(GRID_CENTER_X - HALF_GRID_WIDTH, GRID_CENTER_X + HALF_GRID_WIDTH, GRID_CENTER_Y + HALF_GRID_HEIGHT - MOUSE_POS[1] * GRID_SIZE, SELECTION_COLOR);
            graphics.vLine(GRID_CENTER_X - HALF_GRID_WIDTH + MOUSE_POS[0] * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, SELECTION_COLOR);
        }

        // tip
        graphics.drawCenteredString(font, tip, GRID_CENTER_X, GRID_CENTER_Y + HALF_GRID_HEIGHT + 16, PAPER_COLOR);
    }

    /**
     * Set playing state and reset beat.
     * Usually call {@link #updateTip()} after this method.
     */
    private void setPlaying(boolean playing) {
        this.playing = playing;
        beat = 0;
        MindPlayer.getInstance().setPageNumber(currentPage);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        switch (pKeyCode) {
            case 90, 88 -> {
                // Z, X: punch with help data
                tryToPunchWithHelpData();
                return true;
            }
            case 32 -> {
                // SPACE: play/pause
                setPlaying(!playing);
                updateTip();
                updatePauseState();
                return true;
            }
            default -> {
                if (pKeyCode == Minecraft.getInstance().options.keyInventory.getKey().getValue()) {
                    // INVENTORY: close
                    onClose();
                    return true;
                }
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (playing) {
            if (pDelta > 0) {
                // roll up: speed up
                MindPlayer.getInstance().setTickPerBeat((byte) Math.max(AbstractNoteGridPlayer.MIN_TICK_PER_BEAT, MindPlayer.getInstance().getTickPerBeat() - 1));
            } else {
                // roll down: speed down
                MindPlayer.getInstance().setTickPerBeat((byte) Math.min(AbstractNoteGridPlayer.MAX_TICK_PER_BEAT, MindPlayer.getInstance().getTickPerBeat() + 1));
            }
        } else {
            if (pDelta > 0) {
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
            MindPlayer.getInstance().tick(Minecraft.getInstance().level, null, null);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            tryToPunch();
        }
        tryToPunchWithHelpData();
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private void tryToPunch() {
        final byte PAGE = currentPage;
        if (canEdit && !playing && MOUSE_POS[0] != -1) {
            // punch a note on client and send the punch to server
            if (MAIN_DATA.getPage(PAGE).getBeat(MOUSE_POS[0]).addOneNote(MOUSE_POS[1])) {
                PUNCH_DATA_SENDER.send(PAGE, MOUSE_POS[0], MOUSE_POS[1]);
            }
        }
    }

    private void tryToPunchWithHelpData() {
        if (canEdit && playing && HELP_DATA != null) {
            if (!punchWithHelpData() && !punchFail) {
                // punched at wrong beat, damage the tool as a punishment
                punchFail = true;
                GuiUtils.sendCodeToMenu(tableScreen.getMenu().containerId, PerforationTableMenu.CODE_PUNCH_FAIL);
                GuiUtils.playSound(SoundEvents.VILLAGER_NO);
            }
        }
    }

    /**
     * @return True if player punched successfully at beat in judgment interval.
     */
    private boolean punchWithHelpData() {
        final byte PAGE = currentPage;
        final byte BEAT = beat;
        if (HELP_DATA.size() <= PAGE) {
            // no help data
            return false;
        }
        if (punchWithHelpData(PAGE, BEAT, true)) {
            // filled successfully at current beat
            return true;
        }
        if (MindPlayer.getInstance().tickToNextBeat() > JUDGMENT_INTERVAL_TICK) {
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
        MindPlayer player = MindPlayer.getInstance();
        int beatsInInterval = 1 + (JUDGMENT_INTERVAL_TICK - player.tickToNextBeat()) / player.getTickPerBeat();
        // check next several beats
        for (int i = 0; i < beatsInInterval; i++) {
            if (++beat >= Page.BEATS_SIZE) {
                beat = 0;
                if (++page >= MAIN_DATA.size()) {
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
        if (HELP_DATA.getPage(page).isEmptyBeat(beat)) {
            return false;
        }
        Beat mainBeat = MAIN_DATA.getPage(page).getBeat(beat);
        Beat helpBeat = HELP_DATA.getPage(page).getBeat(beat);
        byte[] helpBeatNotes = helpBeat.getNotes();
        boolean punchNewNote = false;
        for (byte note : helpBeatNotes) {
            if (mainBeat.addOneNote(note)) {
                PUNCH_DATA_SENDER.send(page, beat, note);
                if (!canEdit) {
                    break;
                }
                punchNewNote = true;
            }
        }
        if (punchNewNote) {
            // punched successfully or !canEdit
            if (skipWaiting) {
                MindPlayer.getInstance().skipWaiting();
            }
            paused = false;
            return true;
        }
        return false;
    }

    private void updatePauseState() {
        if (HELP_DATA == null) {
            paused = false;
            return;
        }
        punchFail = false;
        paused = canEdit && !NoteGridUtils.containsAll(MAIN_DATA, HELP_DATA, currentPage, beat);
    }

    /**
     * Called by {@link PerforationTableScreen#onItemChanged}
     * to exit edit mode because of the broken tool.
     */
    protected void exitEditMode() {
        if (canEdit) {
            canEdit = false;
            paused = false;
            playProgressLineColor = SELECTION_COLOR;
            updateTip();
        }
    }

    @Override
    public void onFinish(Level level, BlockPos blockPos, BlockState blockState) {
        setPlaying(false);
        updateTip();
    }

    @Override
    public boolean onBeat(Level level, BlockPos blockPos, BlockState blockState, Beat lastBeat, Beat currentBeat) {
        beat = MindPlayer.getInstance().getBeat();
        updatePauseState();
        return paused;
    }

    @Override
    public void onPageChange(Level level, BlockPos blockPos) {
        pageForward(false);
    }
}
