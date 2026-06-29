package io.github.c20c01.cc_mb.network.sync.edit;

import io.github.c20c01.cc_mb.client.gui.GuiUtils;

public class EditDataSender extends EditDataHandler {
    private final int containerId;

    public EditDataSender(int containerId) {
        this.containerId = containerId;
    }

    public void send(byte pageNum, byte beatNum, byte note) {
        boolean pageChanged = pageNum != this.pageNum;
        if (pageChanged) {
            this.pageNum = pageNum;
            GuiUtils.sendCodeToMenu(containerId, mark(pageNum));
        }
        if (beatNum != this.beatNum || pageChanged) {
            this.beatNum = beatNum;
            GuiUtils.sendCodeToMenu(containerId, mark(beatNum));
        }
        GuiUtils.sendCodeToMenu(containerId, note);
    }
}
