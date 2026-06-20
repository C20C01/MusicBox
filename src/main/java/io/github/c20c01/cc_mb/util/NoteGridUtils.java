package io.github.c20c01.cc_mb.util;

import io.github.c20c01.cc_mb.data.NoteGridData;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class NoteGridUtils {
    public static boolean canConnect(ItemStack noteGrid, ItemStack otherNoteGrid) {
        NoteGridData data = NoteGridData.ofNoteGrid(noteGrid);
        NoteGridData otherData = NoteGridData.ofNoteGrid(otherNoteGrid);
        return data.size() + otherData.size() <= NoteGridData.MAX_SIZE;
    }

    public static NoteGridData connect(NoteGridData first, @Nullable NoteGridData second) {
        if (second == null) return first;
        return first.withDataAdded(second);
    }

    public static boolean containsAll(NoteGridData main, NoteGridData help, int pageNum, int beatNum) {
        if (help.size() <= pageNum) return true;
        ByteList helpNotes = help.getPage(pageNum).getBeat(beatNum).getNotes();
        if (helpNotes.isEmpty()) return true;
        return main.getPage(pageNum).getBeat(beatNum).getNotes().containsAll(helpNotes);
    }

    /**
     * @param size the size of the first part, must be in [1, data.size())
     * @return {data[0, size), data[size, end)}
     */
    public static NoteGridData[] cut(NoteGridData data, int size) {
        NoteGridData[] result = new NoteGridData[2];
        result[0] = data.subData(0, size);
        result[1] = data.subData(size, data.size());
        return result;
    }
}
