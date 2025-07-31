package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MusicBoxPlayer extends BaseBoxPlayer {
    public final Ticker ticker = new Ticker(this);
    public final Sounder sounder = new Sounder();

    public MusicBoxPlayer(Listener listener) {
        super(listener);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ticker.setTickPerBeat(tag.getByte("tick_per_beat"));
        ticker.tickSinceLastBeat = tag.getByte("interval");
        sounder.setOctave(tag.getByte("octave"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("tick_per_beat", ticker.getTickPerBeat());
        tag.putByte("interval", ticker.tickSinceLastBeat);
        tag.putByte("octave", sounder.getOctave());
    }

    public void loadUpdateTag(CompoundTag tag) {
        byte[] data = tag.getByteArray("player_data");
        ticker.setTickPerBeat(data[0]);
        ticker.tickSinceLastBeat = data[1];
        beatNumber = data[2];
        pageNumber = data[3];
        sounder.setOctave(data[4]);
    }

    public void saveUpdateTag(CompoundTag tag) {
        byte[] data = new byte[5];
        data[0] = ticker.getTickPerBeat();
        data[1] = ticker.tickSinceLastBeat;
        data[2] = beatNumber;
        data[3] = pageNumber;
        data[4] = sounder.getOctave();
        tag.putByteArray("player_data", data);
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        update(level, blockPos, blockState);
        ticker.tick();
    }

    @Override
    protected void playBeat() {
        if (shouldPlay()) {
            level.gameEvent(null, GameEvent.NOTE_BLOCK_PLAY, blockPos);
            if (level.isClientSide) {
                playBeatOnClient();
            }
        }
        listener.onBeat();
    }

    /**
     * Update the sound and seed, and return whether the player should play the beat.
     */
    private boolean shouldPlay() {
        if (currentBeat.isEmpty()) {
            return false;
        }
        NoteBlockInstrument instrument = blockState.getValue(MusicBoxBlock.INSTRUMENT);
        if (instrument.hasCustomSound() && level.getBlockEntity(blockPos.below()) instanceof SoundBoxBlockEntity blockEntity) {
            sounder.sound = blockEntity.getSoundEvent();
            if (sounder.sound == null) {
                return false;
            }
            sounder.seed = blockEntity.getSoundSeed().orElse(level.random.nextLong());
        } else {
            sounder.sound = instrument.getSoundEvent();
            sounder.seed = level.random.nextLong();
        }
        return true;
    }

    private void playBeatOnClient() {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : currentBeat.getNotes()) {
            sounder.playInLevel(note, pos);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) currentBeat.getMinNote() / 24.0D;
            level.addParticle(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, d, 0.0D, 0.0D);
        }
    }

    @Override
    public void reset() {
        super.reset();
        ticker.tickSinceLastBeat = 0;
    }
}
