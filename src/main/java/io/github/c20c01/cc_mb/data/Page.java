package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.util.TagData;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Page implements TagData<ListTag> {
    public static final byte BEATS_SIZE = 64;
    private final Beat[] BEATS = new Beat[BEATS_SIZE];

    public static Page ofBeats(Beat[] beats) {
        return new Page().loadBeats(beats);
    }

    public static Page ofCode(String codeOfPage) {
        return new Page().loadCode(codeOfPage);
    }

    public static Page ofTag(ListTag pageTag) {
        return new Page().loadTag(pageTag);
    }

    public Page loadBeats(Beat[] beats) {
        return setBeats(beats);
    }

    public Page loadCode(String codeOfPage) {
        String[] codesOfBeat = codeOfPage.split("\\.");
        ArrayList<Beat> beats = new ArrayList<>(codesOfBeat.length);
        for (String codeOfBeat : codesOfBeat) {
            beats.add(Beat.ofCode(codeOfBeat));
        }
        return setBeats(beats);
    }

    @Override
    public Page loadTag(ListTag pageTag) {
        ArrayList<Beat> beats = new ArrayList<>(pageTag.size());
        for (Tag beatTag : pageTag) {
            beats.add(Beat.ofTag((ByteArrayTag) beatTag));
        }
        return setBeats(beats);
    }

    @Override
    public ListTag toTag() {
        ListTag pageTag = new ListTag();
        for (byte beat = 0; beat < BEATS_SIZE; beat++) {
            pageTag.add(getBeat(beat).toTag());
        }
        return pageTag;
    }

    @Override
    public String toString() {
        return "Page:" + Arrays.toString(BEATS);
    }

    public Beat getBeat(byte index) {
        if (BEATS[index] == null) {
            BEATS[index] = new Beat();
        }
        return BEATS[index];
    }

    public Page setBeats(Beat[] beats) {
        System.arraycopy(beats, 0, BEATS, 0, Math.min(beats.length, BEATS_SIZE));
        return this;
    }

    public Page setBeats(Collection<Beat> beats) {
        return setBeats(beats.toArray(new Beat[0]));
    }
}
