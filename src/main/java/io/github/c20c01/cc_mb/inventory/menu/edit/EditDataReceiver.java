package io.github.c20c01.cc_mb.inventory.menu.edit;

public class EditDataReceiver extends EditDataHandler {
    private final byte[] tempCode = new byte[2];// [page, beat] or [beat], mark is removed
    private byte tempCodeIndex = 0;

    public boolean receive(byte code) {
        if (isNote(code)) {
            if (tempCodeIndex > 0) {
                beatNum = tempCode[--tempCodeIndex];
                if (tempCodeIndex > 0) {
                    pageNum = tempCode[--tempCodeIndex];
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

    public byte getPageNum() {
        return pageNum;
    }

    public byte getBeatNum() {
        return beatNum;
    }
}
