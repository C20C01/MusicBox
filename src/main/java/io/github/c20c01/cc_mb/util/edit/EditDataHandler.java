package io.github.c20c01.cc_mb.util.edit;

/**
 * The edit data is transmitted with several positive bytes.
 * For each byte, after the 1st sign bit, the 2nd bit is used to represent the content of the code.
 * If the 2nd bit is 0, the code is a note, if it is 1, the code is a beat or a page.
 * The page and beat are cached in both the sender and the receiver, they will only be sent if there is a change.
 * The flow of the data is as follows: page -> beat -> note.
 */
public class EditDataHandler {
    protected static final int MASK = 0b01000000;
    protected byte page = -1;
    protected byte beat = -1;

    /**
     * Check if the code is a note.
     */
    protected boolean isNote(byte code) {
        return (code & MASK) == 0;
    }

    /**
     * Mark the code as a page or a beat.
     */
    protected byte mark(byte code) {
        return (byte) (code | MASK);
    }

    protected byte removeMark(byte code) {
        return (byte) (code & ~MASK);
    }

    public void reset() {
        page = -1;
        beat = -1;
    }

    public boolean dirty() {
        return page != -1 || beat != -1;
    }
}
