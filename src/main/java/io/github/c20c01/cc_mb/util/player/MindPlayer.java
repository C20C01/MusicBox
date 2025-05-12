package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.SoundPlayer;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.item.SoundShard;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A player that plays music in the player's mind.
 * Used in the {@link io.github.c20c01.cc_mb.client.gui.NoteGridScreen}.
 */
@Environment(EnvType.CLIENT)
public class MindPlayer extends AbstractNoteGridPlayer {
    private static MindPlayer instance;
    protected Level level;
    private NoteGridData data;
    private Listener listener;
    private LocalPlayer player;
    private boolean noSpecificSeed;

    private MindPlayer(NoteGridData data, Listener listener) {
        this.data = data;
        this.listener = listener;
    }

    public static MindPlayer getInstance(NoteGridData data, Listener listener) {
        if (instance == null) {
            instance = new MindPlayer(data, listener);
        }
        instance.data = data;
        instance.listener = listener;
        instance.player = Minecraft.getInstance().player;
        instance.updateSound();
        return instance;
    }

    public void jumpPageTo(byte pageNumber) {
        this.pageNumber = (byte) Mth.clamp(pageNumber, 0, dataSize());
        this.beatNumber = 0;
        this.tickSinceLastBeat = 0;
    }

    public int tickToNextBeat() {
        return getTickPerBeat() - tickSinceLastBeat;
    }

    public void skipWaiting() {
        tickSinceLastBeat = getTickPerBeat();
    }

    private void updateSound() {
        // Get the sound shard in the player's MAIN_HAND or OFF_HAND
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack soundShard = mainHand.is(CCMain.SOUND_SHARD_ITEM) ? mainHand : offHand.is(CCMain.SOUND_SHARD_ITEM) ? offHand : ItemStack.EMPTY;

        // Load the sound event and seed from the sound shard
        SoundShard.Info info = SoundShard.Info.ofItemStack(soundShard);
        this.sound = info.sound() == null ? SoundEvents.NOTE_BLOCK_HARP : info.sound();
        if (info.seed() == null) {
            noSpecificSeed = true;
        } else {
            this.seed = info.seed();
            noSpecificSeed = false;
        }
    }

    @Override
    protected void playBeat() {
        playNotes(currentBeat.getNotes());
    }

    public void playNotes(ByteArraySet notes) {
        if (notes.isEmpty()) {
            return;
        }
        level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (noSpecificSeed) {
            seed = level.random.nextLong();
        }
        for (byte note : notes) {
            float pitch = getPitchFromNote(note);
            SoundPlayer.playInMind(sound.value(), seed, 3.0F, pitch);
        }
    }

    @Override
    protected void updateCurrentBeat() {
        currentBeat = data.getPage(pageNumber).readBeat(beatNumber);
    }

    @Override
    protected byte dataSize() {
        return data.size();
    }

    @Override
    protected boolean onBeat() {
        return listener.onBeat(beatNumber);
    }

    @Override
    protected void onPageChange() {
        listener.onPageChange();
    }

    @Override
    protected void onFinish() {
        listener.onFinish();
    }

    public interface Listener {
        boolean onBeat(byte beatNumber);

        void onPageChange();

        void onFinish();
    }
}
