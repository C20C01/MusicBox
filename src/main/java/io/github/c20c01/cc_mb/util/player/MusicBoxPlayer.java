package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.sync.DataHolder;
import io.github.c20c01.cc_mb.data.sync.NoteGridDataManager;
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
    private final Listener LISTENER;
    public DataHolder<NoteGridData> dataHolder = DataHolder.empty();
    private Level level;
    private BlockPos blockPos;
    private BlockState blockState;

    public MusicBoxPlayer(Listener listener) {
        this.LISTENER = listener;
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

    public void load(CompoundTag tag) {
        setTickPerBeat(tag.getByte("tick_per_beat"));
        tickSinceLastBeat = tag.getByte("interval");
        beatNumber = tag.getByte("beat");
        pageNumber = tag.getByte("page");
        if (tag.contains("note_grid_hash")) {
            dataHolder = NoteGridDataManager.INSTANCE.get(tag.getInt("note_grid_hash"));
        } else {
            dataHolder.clear();
        }
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putByte("tick_per_beat", getTickPerBeat());
        tag.putByte("interval", tickSinceLastBeat);
        tag.putByte("beat", beatNumber);
        tag.putByte("page", pageNumber);
        dataHolder.getHashCode().ifPresent(hash -> tag.putInt("note_grid_hash", hash));
    }

    /**
     * Client side only. The player will not play the beat on the server side by ticking.
     */
    public void update(Level level, BlockPos blockPos, BlockState blockState) {
        this.level = level;
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    /**
     * The only way to play the beat on the server side.
     */
    public void hitOneBeat(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        nextBeat();
        if (shouldPlay(level, blockPos, blockState)) {
            playBeatOnServer(level, blockPos, sound, seed, currentBeat);
        }
    }

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
        dataHolder.clear();
    }

    @Override
    protected void playBeat() {
        if (level != null && shouldPlay(level, blockPos, blockState)) {
            playBeatOnClient((ClientLevel) level, blockPos, sound, seed, currentBeat);
        }
    }

    @Override
    protected void updateCurrentBeat() {
        dataHolder.get().ifPresentOrElse(data -> currentBeat = data.getPage(pageNumber).readBeat(beatNumber), () -> currentBeat = Beat.EMPTY_BEAT);
    }

    @Override
    protected byte dataSize() {
        return dataHolder.get().map(NoteGridData::size).orElse((byte) 1);
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
