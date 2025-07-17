package io.github.c20c01.cc_mb.util.player;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.client.SoundPlayer;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MusicBoxPlayer extends AbstractNoteGridPlayer {
    private final Listener LISTENER;
    @Nullable
    private NoteGridData data;
    private Level level;
    private BlockPos blockPos;
    private BlockState blockState;

    public MusicBoxPlayer(Listener listener) {
        this.LISTENER = listener;
    }

    private void playBeatOnClient() {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : currentBeat.getNotes()) {
            float pitch = getPitchFromNote(note) * getPitchFactor();
            SoundPlayer.playInLevel(sound.value(), seed, 3.0F, pitch, pos);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) currentBeat.getMinNote() / 24.0D;
            level.addParticle(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, d, 0.0D, 0.0D);
        }
    }

    public void load(ValueInput input) {
        setTickPerBeat(input.getByteOr("tick_per_beat", TickPerBeat.DEFAULT));
        tickSinceLastBeat = input.getByteOr("interval", (byte) 0);
        beatNumber = input.getByteOr("beat", (byte) 0);
        pageNumber = input.getByteOr("page", (byte) 0);
        setOctave(input.getByteOr("octave", (byte) 0));
    }

    public void saveAdditional(ValueOutput output) {
        output.putByte("tick_per_beat", getTickPerBeat());
        output.putByte("interval", tickSinceLastBeat);
        output.putByte("beat", beatNumber);
        output.putByte("page", pageNumber);
        output.putByte("octave", getOctave());
    }

    public void loadUpdateTag(ValueInput input) {
        input.read("player_data", Codec.list(Codec.BYTE)).ifPresent(
                data -> {
                    setTickPerBeat(data.get(0));
                    tickSinceLastBeat = data.get(1);
                    beatNumber = data.get(2);
                    pageNumber = data.get(3);
                    setOctave(data.get(4));
                }
        );
    }

    public void saveUpdateTag(CompoundTag tag) {
        byte[] data = new byte[5];
        data[0] = getTickPerBeat();
        data[1] = tickSinceLastBeat;
        data[2] = beatNumber;
        data[3] = pageNumber;
        data[4] = getOctave();
        tag.putByteArray("player_data", data);
    }

    @Nullable
    public NoteGridData getData() {
        return this.data;
    }

    public void setData(@Nullable NoteGridData data) {
        this.data = data;
    }

    /**
     * Must be called before {@link #tick()}.
     */
    public void update(Level level, BlockPos blockPos, BlockState blockState) {
        this.level = level;
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState) {
        update(level, blockPos, blockState);
        nextBeat();
    }

    /**
     * Update the sound and seed, and return whether the player should play the beat.
     */
    private boolean shouldPlay(Level level, BlockPos blockPos, BlockState blockState) {
        if (currentBeat.isEmpty()) {
            return false;
        }
        NoteBlockInstrument instrument = blockState.getValue(MusicBoxBlock.INSTRUMENT);
        if (instrument.hasCustomSound() && level.getBlockEntity(blockPos.below()) instanceof SoundBoxBlockEntity blockEntity) {
            sound = blockEntity.getSoundEvent();
            if (sound == null) {
                return false;
            }
            seed = blockEntity.getSoundSeed().orElse(level.random.nextLong());
        } else {
            sound = instrument.getSoundEvent();
            seed = level.random.nextLong();
        }
        return true;
    }

    public byte getMinNote() {
        return currentBeat.getMinNote();
    }

    @Override
    public void reset() {
        super.reset();
        data = null;
    }

    @Override
    protected void playBeat() {
        if (shouldPlay(level, blockPos, blockState)) {
            level.gameEvent(null, GameEvent.NOTE_BLOCK_PLAY, blockPos);
            if (level.isClientSide) {
                playBeatOnClient();
            }
        }
    }

    @Override
    protected void updateCurrentBeat() {
        if (data == null) {
            currentBeat = Beat.EMPTY_BEAT;
        } else if (pageNumber < data.size()) {
            currentBeat = data.getPage(pageNumber).readBeat(beatNumber);
        } else {
            LogUtils.getLogger().warn("Page number: {} is out of range: {}", pageNumber, data.size());
            currentBeat = Beat.EMPTY_BEAT;
            pageNumber = 0;
        }
    }

    @Override
    protected byte dataSize() {
        return data == null ? 1 : data.size();
    }

    @Override
    protected boolean onBeat() {
        LISTENER.onBeat();
        return false;
    }

    @Override
    protected void onPageChange() {
        LISTENER.onPageChange();
    }

    @Override
    protected void onFinish() {
        LISTENER.onFinish();
    }

    public interface Listener {
        void onBeat();

        void onPageChange();

        void onFinish();
    }
}
