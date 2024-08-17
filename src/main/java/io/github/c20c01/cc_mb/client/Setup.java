package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableScreen;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class Setup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(CCMain.PERFORATION_TABLE_MENU, PerforationTableScreen::new);
        InstrumentBlocksHelper.updateBlockList();

        ClientPlayNetworking.registerGlobalReceiver(NoteGridDataPacket.Reply.KEY, (client, handler, buf, responseSender) -> NoteGridDataPacket.Reply.handle(client, buf));
    }
}
