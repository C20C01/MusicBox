package io.github.c20c01.cc_mb.util;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NoteGridUtils {
    public static boolean canConnect(ItemStack noteGrid, ItemStack otherNoteGrid) {
        NoteGridData data = NoteGridData.ofNoteGrid(noteGrid);
        NoteGridData otherData = NoteGridData.ofNoteGrid(otherNoteGrid);
        return data.size() + otherData.size() <= NoteGridData.MAX_SIZE;
    }

    public static NoteGridData connect(NoteGridData data, @Nullable NoteGridData otherData) {
        if (otherData == null) {
            return data;
        }
        data.getPages().addAll(otherData.getPages());
        return data;
    }

    public static boolean containsAll(NoteGridData main, NoteGridData help, byte page, byte beat) {
        if (help.size() <= page) {
            return true;
        }
        Beat mainBeat = main.getPage(page).getBeat(beat);
        Beat helpBeat = help.getPage(page).getBeat(beat);
        return mainBeat.getNotes().containsAll(helpBeat.getNotes());
    }

    /**
     * Join the other data to the data.
     */
    public static NoteGridData join(NoteGridData data, NoteGridData otherData) {
        byte size = (byte) Math.min(data.size(), otherData.size());
        for (byte page = 0; page < size; page++) {
            for (byte beat = 0; beat < Page.BEATS_SIZE; beat++) {
                if (otherData.getPage(page).isEmptyBeat(beat)) {
                    continue;
                }
                for (byte note : otherData.getPage(page).getBeat(beat).getNotes()) {
                    data.getPage(page).getBeat(beat).addNote(note);
                }
            }
        }
        return data;
    }

    /**
     * Cut the data to two parts.
     *
     * @param page cut the end of which page
     */
    public static NoteGridData[] cut(NoteGridData data, byte page) {
        page += 1;
        NoteGridData[] res = new NoteGridData[2];
        res[0] = NoteGridData.ofPages(data.getPages().subList(0, page));
        res[1] = NoteGridData.ofPages(data.getPages().subList(page, data.size()));
        return res;
    }
}
