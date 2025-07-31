package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.client.gui.NoteGridScreen;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.item.SoundShard;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;


/**
 * A player that plays music in the player's mind.
 * Used in the {@link NoteGridScreen}.
 */
public class MindPlayer extends BasePlayer {
    private static final MindPlayer INSTANCE = new MindPlayer();
    public final Ticker ticker = new Ticker(this);
    public final Sounder sounder = new Sounder();
    private Listener listener;
    private LocalPlayer player;
    private ItemStack itemStackOnLastBeat = null;

    private MindPlayer() {
        // use getInstance() to create an instance
    }

    public static MindPlayer getInstance(NoteGridData data, Listener listener) {
        INSTANCE.reset();
        INSTANCE.data = data;
        INSTANCE.listener = listener;
        INSTANCE.player = Minecraft.getInstance().player;
        return INSTANCE;
    }

    @Override
    protected void onPageChange() {
        listener.onPageChange();
    }

    @Override
    protected void onFinish() {
        listener.onFinish();
    }

    @Override
    protected boolean shouldPause() {
        return listener.shouldPause(beatNumber);
    }

    @Override
    protected void playBeat() {
        if (shouldPlay()) {
            playNotes(currentBeat.getNotes());
        }
    }

    /**
     * Update the sound and seed, and return whether the player should play the beat.
     */
    private boolean shouldPlay() {
        if (currentBeat.isEmpty()) {
            return false;
        }
        ItemStack itemStack = player.getOffhandItem();
        if (itemStack != itemStackOnLastBeat) {
            updateSounder(itemStack);
        }
        return true;
    }

    private void updateSounder(ItemStack itemStack) {
        itemStackOnLastBeat = itemStack;
        Optional<SoundShard.SoundInfo> soundInfo = SoundShard.SoundInfo.ofItemStack(itemStack);
        if (soundInfo.isPresent()) {
            sounder.sound = soundInfo.get().soundEvent();
            sounder.seed = soundInfo.get().soundSeed().orElse(player.level().random.nextLong());
        } else {
            sounder.sound = SoundEvents.NOTE_BLOCK_HARP;
            sounder.seed = player.level().random.nextLong();
        }
    }

    public void playNotes(ByteArraySet notes) {
        for (byte note : notes) {
            sounder.playInMind(note);
        }
    }

    public void jumpPageTo(byte pageNumber) {
        this.pageNumber = (byte) Mth.clamp(pageNumber, 0, data != null ? data.size() : 0);
        this.beatNumber = 0;
        this.ticker.tickSinceLastBeat = 0;
    }

    @Override
    public void reset() {
        super.reset();
        itemStackOnLastBeat = null;
        ticker.tickSinceLastBeat = 0;
    }

    public interface Listener {
        /**
         * @return Whether the player should pause instead of playing the beat
         */
        boolean shouldPause(byte currentBeat);

        void onPageChange();

        void onFinish();
    }
}
