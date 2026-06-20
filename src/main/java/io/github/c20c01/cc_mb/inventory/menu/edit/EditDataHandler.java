package io.github.c20c01.cc_mb.inventory.menu.edit;

/**
 * The edit data is transmitted with several positive bytes.
 * <p>
 * For each byte, after the 1st sign bit, the 2nd bit is used to represent the content of the code:
 * <ul>
 *     <li>0: note</li>
 *     <li>1: beat or page</li>
 * </ul>
 * <p>
 * The page and beat are cached in both the sender and the receiver, they will only be transmitted when needed.
 * <p>
 * Because of the handler can not identify the page and beat by itself,
 * data must be transmitted in one of the following two sequences:
 * <ul>
 *     <li>page -> beat -> note</li>
 *     <li>beat -> note</li>
 * </ul>
 */
public class EditDataHandler {
    protected static final int MASK = 0b01000000;
    protected byte pageNum = -1;
    protected byte beatNum = -1;

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
        pageNum = -1;
        beatNum = -1;
    }

    public boolean dirty() {
        return pageNum != -1 || beatNum != -1;
    }
}
