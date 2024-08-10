package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableScreen;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.minecraft.client.gui.screens.MenuScreens;

@Mod.EventBusSubscriber(modid = CCMain.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Setup {
    @SubscribeEvent
    public static void setupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // register the perforation table menu and screen
            MenuScreens.register(CCMain.PERFORATION_TABLE_MENU.get(), PerforationTableScreen::new);

            // preload the music instrument blocks
            InstrumentBlocksHelper.updateBlockList();
        });
    }
}
