package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.NoteGridScreen;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A player that plays music in the player's mind.
 * Used in the {@link NoteGridScreen}.
 */
@OnlyIn(Dist.CLIENT)
public class MindPlayer extends AbstractNoteGridPlayer {
    private static MindPlayer instance;
    private LocalPlayer player;
    private Holder<SoundEvent> sound;
    private boolean noSpecificSeed;
    private long seed;

    private MindPlayer() {
        super(null);
    }

    /**
     * Call {@link #init(NoteGridData, PlayerListener)} before using the instance.
     */
    public static MindPlayer getInstance() {
        if (instance == null) {
            instance = new MindPlayer();
        }
        return instance;
    }

    public void init(NoteGridData data, PlayerListener listener) {
        player = Minecraft.getInstance().player;
        if (player != null) {
            this.noteGridData = data;
            this.listener = listener;
            loadSound();
        }
    }

    private void loadSound() {
        // Get the sound shard in the player's MAIN_HAND or OFF_HAND
        ItemStack soundShard = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!soundShard.is(CCMain.SOUND_SHARD_ITEM.get())) {
            soundShard = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!soundShard.is(CCMain.SOUND_SHARD_ITEM.get())) {
                soundShard = ItemStack.EMPTY;
            }
        }
        // Load the sound event and seed from the sound shard
        Holder<SoundEvent> sound = SoundShard.getSoundEvent(soundShard);
        this.sound = sound == null ? NoteBlockInstrument.HARP.getSoundEvent() : sound;
        Long seed = SoundShard.getSoundSeed(soundShard);
        if (seed == null) {
            noSpecificSeed = true;
        } else {
            this.seed = seed;
            noSpecificSeed = false;
        }
    }

    @Override
    public void reset() {
        pageNumber = 0;
        beatNumber = 0;
        tickSinceLastBeat = 0;
    }

    public void setPageNumber(byte pageNumber) {
        this.pageNumber = (byte) Mth.clamp(pageNumber, 0, noteGridData == null ? 0 : noteGridData.size() - 1);
        this.beatNumber = 0;
        this.tickSinceLastBeat = 0;
    }

    public byte getBeat() {
        return beatNumber;
    }

    public void skipWaiting() {
        tickSinceLastBeat = tickPerBeat;
    }

    public byte tickToNextBeat() {
        return (byte) (tickPerBeat - tickSinceLastBeat);
    }

    @Override
    protected void playBeat(Level level, BlockPos blockPos, BlockState blockState, Beat beat) {
        if (beat.isEmpty()) {
            return;
        }
        if (noSpecificSeed) {
            seed = level.random.nextLong();
        }
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.RECORDS, 3.0F, pitch, seed);
        }
    }
}
