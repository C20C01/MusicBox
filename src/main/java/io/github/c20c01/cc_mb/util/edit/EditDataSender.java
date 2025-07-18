package io.github.c20c01.cc_mb.util.edit;

import io.github.c20c01.cc_mb.client.GuiUtils;

public class EditDataSender extends EditDataHandler {
    private final int CONTAINER_ID;

    public EditDataSender(int containerId) {
        this.CONTAINER_ID = containerId;
    }

    public void send(byte page, byte beat, byte note) {
        if (page != this.page) {
            this.page = page;
            GuiUtils.sendCodeToMenu(CONTAINER_ID, mark(page));
        }
        if (beat != this.beat) {
            this.beat = beat;
            GuiUtils.sendCodeToMenu(CONTAINER_ID, mark(beat));
        }
        GuiUtils.sendCodeToMenu(CONTAINER_ID, note);
    }
}
