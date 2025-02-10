package io.github.c20c01.cc_mb.util.edit;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.data.NoteGridData;

import java.util.function.Supplier;

public class EditDataReceiver extends EditDataHandler {
    private final Supplier<NoteGridData> DATA_SUPPLIER;
    private final byte[] tempCode = new byte[2];// [page, beat] or [beat], mark is removed
    private byte tempCodeIndex = 0;

    public EditDataReceiver(Supplier<NoteGridData> dateSupplier) {
        this.DATA_SUPPLIER = dateSupplier;
    }

    public boolean receive(byte code) {
        if (isNote(code)) {
            if (tempCodeIndex > 0) {
                beat = tempCode[--tempCodeIndex];
                if (tempCodeIndex > 0) {
                    page = tempCode[--tempCodeIndex];
                }
            }
            final NoteGridData DATA = DATA_SUPPLIER.get();
            if (page >= DATA.size()) {
                LogUtils.getLogger().warn("Wrong page received, the data may be corrupted.");
            } else {
                DATA.getPage(page).getBeat(beat).addOneNote(code);
                return true;
            }
        } else {
            if (tempCodeIndex >= 2) {
                tempCodeIndex = 0;
                LogUtils.getLogger().warn("Too many beats or pages received, the data may be corrupted.");
            }
            tempCode[tempCodeIndex++] = removeMark(code);
        }
        return false;
    }
}
