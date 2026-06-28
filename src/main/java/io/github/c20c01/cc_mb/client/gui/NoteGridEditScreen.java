package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.inventory.menu.MenuMode;
import io.github.c20c01.cc_mb.inventory.menu.PerforationTableMenu;
import io.github.c20c01.cc_mb.inventory.menu.edit.EditDataSender;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class NoteGridEditScreen extends NoteGridScreen implements MenuModeChangedListener {
    protected static final int PUNCH_WITH_HELP_PLAYING_LINE_COLOR = 0x66FF2233;
    protected static final String[] NOTE_NAME = new String[]{"1", "#1", "2", "#2", "3", "4", "#4", "5", "#5", "6", "#6", "7"};
    protected static final byte JUDGMENT_INTERVAL_TICK = 5;// max tick you can early the beat (1tick = 50ms)

    protected final int containerId;
    protected final EditDataSender sender;

    protected MenuMode mode = MenuMode.CHECK;
    protected byte mouseX = -1, mouseY = -1;// -1 for not pointing to any note
    protected boolean isEditing = false;
    protected boolean isPunchingWithHelpData = false;// mode == punch && helpData != null
    protected boolean shouldTick = true;// whether the current beat has been punched fully when punching with help data.
    protected boolean punchFail = false;// fail to punch at current beat, avoid punching fail repeatedly.

    private EditScreenCloseListener listener;

    public NoteGridEditScreen(NoteGridData mainData, @Nullable NoteGridData helpData, int containerId, MenuMode mode) {
        super(mainData, helpData, null);
        this.containerId = containerId;
        this.sender = new EditDataSender(containerId);
        this.onMenuModeChanged(mode);
    }

    public static void openWithPerforationTable(PerforationTableScreen menuScreen) {
        assert menuScreen.mainData != null;
        NoteGridEditScreen editScreen = new NoteGridEditScreen(menuScreen.mainData, menuScreen.helpData, menuScreen.getMenu().containerId, menuScreen.mode);
        editScreen.pageNum = menuScreen.pageNum;
        editScreen.listener = menuScreen;
        menuScreen.setListener(editScreen);
        Minecraft.getInstance().pushGuiLayer(editScreen);
    }

    @Override
    public void onMenuModeChanged(MenuMode mode) {
        this.mode = mode;
        shouldTick = true;
        isPunchingWithHelpData = mode == MenuMode.PUNCH && helpData != null;
        if (isPunchingWithHelpData) punchFail = false;
        isEditing = mode == MenuMode.PUNCH || mode == MenuMode.FIX;
        if (!isEditing) clearMousePos();
    }

    @Override
    public void onClose() {
        GuiUtils.sendCodeToMenu(containerId, PerforationTableMenu.CODE_EDIT_SCREEN_CLOSE);
        if (listener != null) listener.onEditScreenClose(mainData, pageNum);
        super.onClose();
    }

    // region render
    private void renderSelectionLine(GuiGraphicsExtractor graphics, int x, int y) {
        // hLine
        final int hLineTop = GRID_CENTER_Y + HALF_GRID_HEIGHT - GRID_SIZE * y;
        graphics.fill(gridLeft, hLineTop, gridRight, hLineTop + 1, SELECTION_COLOR);
        // vLine
        final int vLineLeft = gridLeft + x * GRID_SIZE;
        final int gridTop = GRID_CENTER_Y - HALF_GRID_HEIGHT;
        final int gridBottom = GRID_CENTER_Y + HALF_GRID_HEIGHT + 1;
        graphics.fill(vLineLeft, gridTop, vLineLeft + 1, gridBottom, SELECTION_COLOR);
    }

    @Override
    protected void renderHelpLines(GuiGraphicsExtractor graphics) {
        if (isPlaying) {
            renderPlayingLine(graphics, beatNum, isPunchingWithHelpData ? PUNCH_WITH_HELP_PLAYING_LINE_COLOR : SELECTION_COLOR);
        } else if (isEditing && isMouseInsideGrid()) {
            renderSelectionLine(graphics, mouseX, mouseY);
        }
    }
    // endregion

    // region mouse pos
    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
        if (isEditing) updateMousePos(x, y);
    }

    public void showMousePos() {
        if (isMouseInsideGrid() && !isPlaying) {
            String note = NOTE_NAME[(mouseY + 6) % 12] + " (" + mouseY + ")";
            message = Component.literal("Beat: " + mouseX + ", Note: " + note);
        } else {
            showPageInfo();
        }
    }

    private void updateMousePos(double x, double y) {
        final float halfGridSize = GRID_SIZE / 2f;
        final float gridCenterY = GRID_CENTER_Y + 0.5f;
        final float bottom = gridCenterY + HALF_GRID_HEIGHT + halfGridSize;
        if (y < gridCenterY - HALF_GRID_HEIGHT - halfGridSize || y > bottom) {
            clearMousePos();
            return;
        }

        final float gridCenterX = width / 2f + 0.5f;
        final float left = gridCenterX - HALF_GRID_WIDTH - halfGridSize;
        if (x < left || x > gridCenterX + HALF_GRID_WIDTH + halfGridSize) {
            clearMousePos();
            return;
        }

        byte newMouseX = (byte) ((x - left) / GRID_SIZE);
        byte newMouseY = (byte) ((bottom - y) / GRID_SIZE);
        if (newMouseX != mouseX || newMouseY != mouseY) {
            mouseX = newMouseX;
            mouseY = newMouseY;
            showMousePos();
        }
    }

    public boolean isMouseInsideGrid() {
        return mouseY != -1 && mouseX != -1;
    }

    private void clearMousePos() {
        if (isMouseInsideGrid()) {
            mouseX = -1;
            mouseY = -1;
            showPageInfo();
        }
    }
    // endregion

    // region input
    @Override
    public boolean keyPressed(KeyEvent event) {
        final int key = event.key();
        if (isPlaying && isPunchingWithHelpData && (key == 90 || key == 88)) {// Z, X
            handlePunchWithHelpInput((byte) pageNum, (byte) beatNum);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!isEditing) return false;

        final int button = event.button();
        if (isPlaying) {
            if (isPunchingWithHelpData && (button == 0 || button == 1)) {
                handlePunchWithHelpInput((byte) pageNum, (byte) beatNum);
                return true;
            }
            return false;
        }

        if (isMouseInsideGrid()) {
            if (button == 0) {// left click
                handleEditInput((byte) pageNum, mouseX, mouseY, mode == MenuMode.PUNCH);
                return true;
            }
            if (button == 1) {// right click
                handlePreviewInput(pageNum, mouseX, mouseY, mode == MenuMode.PUNCH);
                return true;
            }
        }

        return false;
    }
    // endregion

    // region edit
    private void handleEditInput(byte pageNum, byte beatNum, byte note, boolean punchMode) {
        // edit a note on client and send the edit to server
        if (editNote(pageNum, beatNum, note, punchMode)) {
            sender.send(pageNum, beatNum, note);
            GuiUtils.playSound(punchMode ? SoundEvents.BOOK_PUT : SoundEvents.SLIME_BLOCK_FALL);
        }
    }

    /**
     * Preview the sound of the beat at current mouse position.
     *
     * @param punchMode true: add pointed note, false: remove pointed note.
     */
    private void handlePreviewInput(int pageNum, byte beatNum, byte note, boolean punchMode) {
        Beat beat = getBeat(pageNum, beatNum);
        ByteList previewNotes = punchMode ? beat.getAddPreviewNotes(note) : beat.getRemovePreviewNotes(note);
        player.playNotes(previewNotes);
    }

    private void handlePunchWithHelpInput(byte pageNum, byte beatNum) {
        assert helpData != null;
        if (!tryToPunchWithHelp(pageNum, beatNum) && !punchFail) {
            punchFail = true;
            GuiUtils.sendCodeToMenu(containerId, PerforationTableMenu.CODE_PUNCH_FAIL);
            GuiUtils.playSound(SoundEvents.VILLAGER_NO);
        }
    }

    /**
     * @return whether punched successfully.
     */
    private boolean tryToPunchWithHelp(byte pageNum, byte beatNum) {
        assert helpData != null;
        if (helpData.size() <= pageNum) {
            // no help data
            return false;
        }
        if (doPunchWithHelp(pageNum, beatNum, true)) {
            // filled successfully at current beat
            return true;
        }
        if (player.getTickToNextBeat() > JUDGMENT_INTERVAL_TICK) {
            // no other beat in judgment interval
            return false;
        }
        return tryToPunchWithHelpInInterval(pageNum, beatNum);
    }

    /**
     * Punch the nearest beat's notes in judgment interval.
     *
     * @return whether punched successfully at a beat in judgment interval.
     */
    private boolean tryToPunchWithHelpInInterval(byte pageNum, byte beatNum) {
        assert helpData != null;
        final int beatNumToCheck = player.getBeatNumInInterval(JUDGMENT_INTERVAL_TICK);
        for (int i = 0; i < beatNumToCheck; i++) {
            if (++beatNum >= Page.BEATS_SIZE) {
                ++pageNum;
                if (pageNum >= helpData.size() || pageNum >= mainData.size()) return false;
                beatNum = 0;
            }
            if (doPunchWithHelp(pageNum, beatNum, false)) return true;
        }
        return false;
    }

    /**
     * Punch notes from help data to main data for whole beat.
     * <p>
     * Even if the awl broken during the punching, the whole beat will still be punched successfully.
     *
     * @param skipWaiting Whether to skip waiting after punched successfully
     *                    to restore the playing fast.
     * @return whether punched successfully at given beat.
     */
    private boolean doPunchWithHelp(byte pageNum, byte beatNum, boolean skipWaiting) {
        assert helpData != null;
        ByteList notes = helpData.getPage(pageNum).getBeat(beatNum).getNotes();
        if (notes.isEmpty()) return false;

        boolean punchedAtLeastOneNote = false;
        for (int i = 0; i < notes.size(); i++) {
            byte note = notes.getByte(i);
            if (editNote(pageNum, beatNum, note, true)) {
                punchedAtLeastOneNote = true;
                sender.send(pageNum, beatNum, note);
            }
        }
        if (punchedAtLeastOneNote) {
            GuiUtils.playSound(SoundEvents.BOOK_PUT);
            if (skipWaiting) player.skipWaiting();
            shouldTick = true;
            return true;
        }
        return false;
    }
    // endregion

    // region play
    @Override
    protected void setPlaying(boolean playing) {
        super.setPlaying(playing);
        shouldTick = true;
    }

    @Override
    public void tick() {
        if (isPlaying && shouldTick) player.tick();
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        super.onBeat(pageNum, beatNum, beat);
        if (isPunchingWithHelpData) {
            assert helpData != null;
            punchFail = false;
            shouldTick = NoteGridUtils.containsAll(mainData, helpData, pageNum, beatNum);
            return shouldTick;
        } else {
            return true;
        }
    }
    // endregion
}
