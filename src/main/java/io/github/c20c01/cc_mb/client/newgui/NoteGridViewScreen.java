package io.github.c20c01.cc_mb.client.newgui;

import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.NoteGridPunchPacket;
import io.github.c20c01.cc_mb.util.player.AbstractNoteGridPlayer;
import io.github.c20c01.cc_mb.util.player.MindPlayer;
import io.github.c20c01.cc_mb.util.player.PlayerListener;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class NoteGridViewScreen extends Screen implements PlayerListener {
    private static final int PAPER_COLOR = 0xFFFDF7EA;
    private static final int BLACK = 0xFF000000;
    private static final int LINE_COLOR = 0x33000000;
    private static final int SELECTION_COLOR = 0x69000000;
    private static final String[] NOTE_NAME = new String[]{"1", "#1", "2", "#2", "3", "4", "#4", "5", "#5", "6", "#6", "7"};
    private static final int HALF_GRID_BACKGROUND_WIDTH = 202; // total width = 202 * 2 + 1 = 405
    private static final int HALF_GRID_BACKGROUND_HEIGHT = 85; // total height = 85 * 2 + 1 = 171
    private static final int HALF_GRID_WIDTH = 189; // total width = 189 * 2 + 1 = 379
    private static final int HALF_GRID_HEIGHT = 72; // total height = 72 * 2 + 1 = 145
    private static final int GRID_CENTER_Y = HALF_GRID_BACKGROUND_HEIGHT + 8; // 8 is the top margin
    private static final int GRID_SIZE = 6;
    private static final int HALF_GRID_SIZE = GRID_SIZE / 2;

    private final byte[] MOUSE_POS = new byte[]{-1, -1};// {x, y}, {0, 0} at bottom left
    private final boolean WITH_HELPER;
    private final int MAIN_DATA_ID;
    private PerforationTableMenu menu = null;
    private NoteGridData mainData = null;// play or edit on this data
    private NoteGridData helperData = null;// show a translucent note grid to help player to copy notes from this data
    private Component tip = CommonComponents.EMPTY;
    private byte currentPage;
    private byte beat;
    private PageButton forwardButton;
    private PageButton backButton;
    private boolean canEdit = false;
    private boolean playing = false;

    /**
     * Read only mode. Available: view, play
     */
    public NoteGridViewScreen(int noteGridId) {
        super(GameNarrator.NO_TITLE);
        MAIN_DATA_ID = noteGridId;
        WITH_HELPER = helperData != null;
    }

    /**
     * Edit mode. Available: punch, view, play
     */
    public NoteGridViewScreen(int noteGridId, PerforationTableMenu menu) {
        this(noteGridId);
        this.menu = menu;
        this.canEdit = noteGridId >= 0;
    }

    /**
     * Edit mode. Available: punch with help, punch, view, play
     */
    public NoteGridViewScreen(int noteGridId, PerforationTableMenu menu, NoteGridData helperData) {
        this(noteGridId, menu);
        this.helperData = helperData;
    }

    public void loadData(@Nullable NoteGridData noteGridData) {
        if (noteGridData != null) {
            mainData = noteGridData;
            MindPlayer.getInstance().init(mainData, this);
            updatePageStuff(true);
        }
    }

    @Override
    public void onClose() {
        playing = false;
        MindPlayer.getInstance().close();
        super.onClose();
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
        loadData(NoteGridData.ofId(MAIN_DATA_ID, this::loadData));
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
        return mainData == null ? 0 : mainData.size();
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
        } else {
            updateTip();
        }
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
            MOUSE_POS[0] = -1;
            updateTip();
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
            tip = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
        } else {
            String note = NOTE_NAME[(MOUSE_POS[1] + 6) % 12] + " (" + MOUSE_POS[1] + ")";
            tip = Component.literal("Beat: " + MOUSE_POS[0] + ", Note: " + note);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        if (mainData == null) {
            // loading screen
            graphics.drawCenteredString(this.font, Component.translatable("mco.download.downloading"), width / 2, height / 2, PAPER_COLOR);
        } else {
            // note grid
            renderNoteGrid(graphics, mainData.getPage(currentPage));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderNoteGrid(GuiGraphics graphics, Page page) {
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
            Beat oneBeat = page.getBeat(x);
            for (byte note : oneBeat.getNotes()) {
                final int CROSS_RECTANGLE_LEFT = GRID_CENTER_X - HALF_GRID_WIDTH + x * GRID_SIZE;
                final int CROSS_RECTANGLE_TOP = GRID_CENTER_Y + HALF_GRID_HEIGHT - note * GRID_SIZE;
                // Note size: 3x3 Line width: 1
                graphics.fill(CROSS_RECTANGLE_LEFT - 1, CROSS_RECTANGLE_TOP - 1, CROSS_RECTANGLE_LEFT + 2, CROSS_RECTANGLE_TOP + 2, BLACK);
            }
        }

        if (playing) {
            // playing progress line
            graphics.vLine(GRID_CENTER_X - HALF_GRID_WIDTH + beat * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, SELECTION_COLOR)
            ;
        } else if (MOUSE_POS[0] != -1) {
            // selection lines
            graphics.hLine(GRID_CENTER_X - HALF_GRID_WIDTH, GRID_CENTER_X + HALF_GRID_WIDTH, GRID_CENTER_Y + HALF_GRID_HEIGHT - MOUSE_POS[1] * GRID_SIZE, SELECTION_COLOR);
            graphics.vLine(GRID_CENTER_X - HALF_GRID_WIDTH + MOUSE_POS[0] * GRID_SIZE, GRID_CENTER_Y - HALF_GRID_HEIGHT - 1, GRID_CENTER_Y + HALF_GRID_HEIGHT + 1, SELECTION_COLOR);
        }

        // tip
        graphics.drawCenteredString(font, tip, GRID_CENTER_X, GRID_CENTER_Y + HALF_GRID_HEIGHT + 16, PAPER_COLOR);
    }

    private void setPlaying(boolean playing) {
        this.playing = mainData != null && playing;
        beat = 0;
        MindPlayer.getInstance().setPageNumber(currentPage);
        updateTip();
    }

    /**
     * @param pKeyCode See {@link org.lwjgl.glfw.GLFW}
     */
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 32) {
            // SPACE: play/pause
            setPlaying(!playing);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (playing) {
            if (pDelta > 0) {
                // roll uo: speed down
                MindPlayer.getInstance().setTickPerBeat((byte) Math.min(AbstractNoteGridPlayer.MAX_TICK_PER_BEAT, MindPlayer.getInstance().getTickPerBeat() + 1));
            } else {
                // roll down: speed up
                MindPlayer.getInstance().setTickPerBeat((byte) Math.max(AbstractNoteGridPlayer.MIN_TICK_PER_BEAT, MindPlayer.getInstance().getTickPerBeat() - 1));
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
        if (playing) {
            MindPlayer.getInstance().tick(Minecraft.getInstance().level, null, null);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (canEdit && !playing && MOUSE_POS[0] != -1 && pButton == 0) {
            // punch a note on client and send a packet to server
            mainData.getPage(currentPage).getBeat(MOUSE_POS[0]).addOneNote(MOUSE_POS[1]);


            CCNetwork.CHANNEL.sendToServer(new NoteGridPunchPacket(MAIN_DATA_ID, currentPage, MOUSE_POS[0], MOUSE_POS[1]));
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void onFinish(Level level, BlockPos blockPos, BlockState blockState) {
        setPlaying(false);
    }

    @Override
    public void onBeat(Level level, BlockPos blockPos, BlockState blockState, Beat lastBeat, Beat currentBeat) {
        beat = MindPlayer.getInstance().getBeat();
    }

    @Override
    public void onPageChange(Level level, BlockPos blockPos, BlockState blockState, byte newPageNumber) {
        pageForward(false);
    }
}
