package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.data.Beat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MusicBoxPlayer extends AbstractNoteGridPlayer {
    public MusicBoxPlayer(PlayerListener listener) {
        super(listener);
    }

    @OnlyIn(Dist.CLIENT)
    private static void playBeatOnClient(ClientLevel level, BlockPos blockPos, Holder<SoundEvent> sound, long seed, Beat beat) {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(Minecraft.getInstance().player, pos.x, pos.y, pos.z, sound, SoundSource.RECORDS, 3.0F, pitch, seed);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) beat.getMinNote() / 24.0D;
            level.addParticle(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, d, 0.0D, 0.0D);
        }
    }

    private static void playBeatOnServer(ServerLevel level, BlockPos blockPos, Holder<SoundEvent> sound, long seed, Beat beat) {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(null, pos.x, pos.y, pos.z, sound, SoundSource.RECORDS, 3.0F, pitch, seed);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) beat.getMinNote() / 24.0D;
            level.sendParticles(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, 0, d, 0.0D, 0.0D, 1.0D);
        }
    }

    public void handleUpdateTag(CompoundTag tag) {
        byte[] data = tag.getByteArray("PlayerData");
        setTickPerBeat(data[0]);
        tickSinceLastBeat = data[1];
        beatNumber = data[2];
        pageNumber = data[3];
    }

    public void getUpdateTag(CompoundTag tag) {
        byte[] data = new byte[4];
        data[0] = tickPerBeat;
        data[1] = tickSinceLastBeat;
        data[2] = beatNumber;
        data[3] = pageNumber;
        tag.putByteArray("PlayerData", data);
    }

    public void load(CompoundTag tag) {
        setTickPerBeat(tag.getByte("TickPerBeat"));
        tickSinceLastBeat = tag.getByte("Interval");
        beatNumber = tag.getByte("Beat");
        pageNumber = tag.getByte("Page");
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putByte("TickPerBeat", tickPerBeat);
        tag.putByte("Interval", tickSinceLastBeat);
        tag.putByte("Beat", beatNumber);
        tag.putByte("Page", pageNumber);
    }

    /**
     * Play the beat on the client or server side.
     */
    protected void playBeat(Level level, BlockPos blockPos, BlockState blockState, Beat beat) {
        if (beat.isEmpty()) {
            return;
        }
        NoteBlockInstrument instrument = blockState.getValue(MusicBoxBlock.INSTRUMENT);
        Holder<SoundEvent> soundEvent;
        long soundSeed;
        // Sound event & Sound seed
        if (instrument.hasCustomSound()) {
            if (level.getBlockEntity(blockPos.below()) instanceof SoundBoxBlockEntity blockEntity) {
                if (blockEntity.containSound()) {
                    soundEvent = blockEntity.getSoundEvent();
                    soundSeed = blockEntity.getSoundSeed(level.random);
                } else {
                    return;
                }
            } else {
                return;
            }
        } else {
            soundEvent = instrument.getSoundEvent();
            soundSeed = level.random.nextLong();
        }
        if (level.isClientSide) {
            playBeatOnClient((ClientLevel) level, blockPos, soundEvent, soundSeed, beat);
        } else {
            playBeatOnServer((ServerLevel) level, blockPos, soundEvent, soundSeed, beat);
        }
    }
}
