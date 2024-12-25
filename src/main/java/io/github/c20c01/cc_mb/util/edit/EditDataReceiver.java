package io.github.c20c01.cc_mb.util.edit;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;

public class EditDataReceiver extends EditDataHandler {
    private final byte[] tempCode = new byte[2];// [page, beat] or [beat], mark is removed
    private byte tempCodeIndex = 0;

    public boolean receive(byte code) {
        if (isNote(code)) {
            if (tempCodeIndex > 0) {
                beat = tempCode[--tempCodeIndex];
                if (tempCodeIndex > 0) {
                    page = tempCode[--tempCodeIndex];
                }
            }
            return true;
        } else {
            if (tempCodeIndex >= 2) {
                tempCodeIndex = 0;
            }
            tempCode[tempCodeIndex++] = removeMark(code);
        }
        return false;
    }

    public Beat getBeat(NoteGridData data) {
        return data.getPage(page).getBeat(beat);
    }
}
