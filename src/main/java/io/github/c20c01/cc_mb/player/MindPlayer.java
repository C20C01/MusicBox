package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.client.Speaker;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.item.SoundShard;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Client side only!
 * <p>
 * A player that plays music in the player's mind.
 * Used in the {@link io.github.c20c01.cc_mb.client.gui.NoteGridScreen}.
 */
public class MindPlayer implements NoteGridDataHolder, NoteGridIteratorListener {
    public final NoteGridTicker ticker;

    private final NoteGridData data;
    private final NoteGridIteratorListener listener;
    private final SpeakerConfig config;

    public MindPlayer(NoteGridData data, NoteGridIteratorListener listener, @Nullable Player player) {
        this.data = data;
        this.listener = listener;
        this.config = new SpeakerConfig();

        // Get the sound shard in the player's MAIN_HAND or OFF_HAND
        ItemStack soundShard = ItemStack.EMPTY;
        if (player != null) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            soundShard = mainHand.is(MusicBox.SOUND_SHARD_ITEM.get()) ? mainHand : offHand.is(MusicBox.SOUND_SHARD_ITEM.get()) ? offHand : ItemStack.EMPTY;
        }

        // Load the sound event and seed from the sound shard.
        SoundShard.SoundInfo.ofItemStack(soundShard).ifPresentOrElse(info -> {
            config.setSoundLocation(info.soundEvent().value().location());
            info.soundSeed().ifPresent(config::setSeed);
        }, () -> {
            config.setSoundLocation(SoundEvents.NOTE_BLOCK_HARP.key().identifier());
            config.setSeed(0); // harp need no random
        });

        this.ticker = new NoteGridTicker(this, this);
    }

    public byte getBeatNum() {
        return ticker.beatNum;
    }

    public void jumpPageTo(int pageNumber) {
        ticker.pageNum = (byte) Math.max(0, Math.min(pageNumber, data.size() - 1));
        ticker.beatNum = 0;
        ticker.tickSinceLastBeat = 0;
    }

    public int getTickToNextBeat() {
        return ticker.getTickPerBeat() - ticker.tickSinceLastBeat;
    }

    public void skipWaiting() {
        ticker.tickSinceLastBeat = ticker.getTickPerBeat();
    }

    @Override
    public boolean onBeat(Beat beat, int beatNumber) {
        boolean pause = listener.onBeat(beat, beatNumber);
        if (pause) return false;
        if (beat.isEmpty()) return true;
        playNotes(beat.getNotes());
        return true;
    }

    public void playNotes(ByteList notes) {
        Speaker.playMind(config, notes);
    }

    @Override
    public void onPageChanged(int pageNum) {
        listener.onPageChanged(pageNum);
    }

    @Override
    public void onFinish() {
        listener.onFinish();
    }

    @Override
    public NoteGridData getData() {
        return data;
    }

    @Override
    public void setData(@Nullable NoteGridData data) {
        // final data, do nothing
    }

    @Override
    public int getDataSize() {
        return data.size();
    }

    @Override
    public Beat getBeat(int pageNum, int beatNum) {
        return data.getPage(pageNum).getBeat(beatNum);
    }
}
