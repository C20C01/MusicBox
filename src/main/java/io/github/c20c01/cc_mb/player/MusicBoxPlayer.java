package io.github.c20c01.cc_mb.player;

import com.mojang.serialization.Codec;
import io.github.c20c01.cc_mb.client.Speaker;
import io.github.c20c01.cc_mb.data.Beat;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Objects;

public class MusicBoxPlayer implements NoteGridIteratorListener {
    private final NoteGridTicker ticker;
    private final SpeakerConfig config;
    private final NoteGridIteratorListener listener;
    private final BlockPos blockPos;

    private Level level;

    public MusicBoxPlayer(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener, BlockPos blockPos) {
        this.config = new SpeakerConfig();
        this.listener = listener;
        this.ticker = new NoteGridTicker(dataHolder, this);
        this.blockPos = blockPos;
    }

    public void tick(Level level) {
        this.level = level;
        ticker.tick();
    }

    public void nextBeat(Level level) {
        this.level = level;
        ticker.nextBeat();
    }

    public void reset() {
        ticker.reset();
    }

    public byte getMinNote() {
        return ticker.getMinNote();
    }

    public byte getTickPerBeat() {
        return ticker.tickPerBeat;
    }

    public boolean tryToSetTickPerBeat(byte tickPerBeat) {
        if (ticker.tickPerBeat != tickPerBeat) {
            ticker.setTickPerBeat(tickPerBeat);
            return true;
        }
        return false;
    }

    public byte cycleOctave(boolean decrease) {
        byte currentOctave = config.getOctave();
        byte newOctave;
        if (decrease) {
            if (currentOctave <= Octave.MIN) newOctave = Octave.MAX;
            else newOctave = (byte) (currentOctave - 1);
        } else {
            if (currentOctave >= Octave.MAX) newOctave = Octave.MIN;
            else newOctave = (byte) (currentOctave + 1);
        }
        config.setOctave(newOctave);
        return newOctave;
    }

    public boolean tryToUpdateInstrument(@Nullable Identifier soundLocation, @Nullable Long soundSeed) {
        if (!Objects.equals(soundLocation, config.getSoundLocation()) || !Objects.equals(soundSeed, config.getSeed())) {
            config.setSoundLocation(soundLocation);
            config.setNullableSeed(soundSeed);
            return true;
        }
        return false;
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        listener.onBeat(pageNum, beatNum, beat);// always return true

        if (beat.isEmpty() || config.isSilent()) return true;
        level.gameEvent(null, GameEvent.NOTE_BLOCK_PLAY, blockPos);

        // client only: play the sound and spawn particles
        if (level.isClientSide()) Speaker.playBox(config, beat, level, blockPos);

        return true;
    }

    @Override
    public void onPageChanged() {
        listener.onPageChanged();
    }

    @Override
    public void onFinished() {
        listener.onFinished();
    }

    // region serialization
    public void loadAdditional(ValueInput input) {
        ticker.loadAdditional(input);
        config.setOctave(input.getByteOr("octave", config.getOctave()));
        loadSound(input);
    }

    public void loadSound(ValueInput input) {
        input.read("sound", Identifier.CODEC).ifPresentOrElse(config::setSoundLocation, config::removeSoundLocation);
        input.getLong("seed").ifPresentOrElse(config::setSeed, config::removeSeed);
    }

    public void loadSync(ValueInput input) {
        input.read("byte", Codec.list(Codec.BYTE)).ifPresent(data -> {
            ticker.setTickPerBeat(data.get(0));
            ticker.tickSinceLastBeat = data.get(1);
            ticker.beatNum = data.get(2);
            ticker.pageNum = data.get(3);
            config.setOctave(data.get(4));
        });
    }

    public void saveAdditional(ValueOutput output) {
        ticker.saveAdditional(output);
        output.putByte("octave", config.getOctave());
        saveSound(output);
    }

    public void saveSound(ValueOutput output) {
        if (config.getSoundLocation() != null) output.store("sound", Identifier.CODEC, config.getSoundLocation());
        if (config.hasSpecificSeed()) output.putLong("seed", config.getSpecificSeed());
    }

    public void saveSync(ValueOutput output) {
        output.store("byte", Codec.list(Codec.BYTE), ByteList.of(
                ticker.tickPerBeat,
                ticker.tickSinceLastBeat,
                ticker.beatNum,
                ticker.pageNum,
                config.getOctave()
        ));
    }
    // endregion
}
