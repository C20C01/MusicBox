package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableScreen;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CCMain.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class Setup {
    @SubscribeEvent
    public static void setupMenuScreen(RegisterMenuScreensEvent event) {
        // register the perforation table menu and screen
        event.register(CCMain.PERFORATION_TABLE_MENU.get(), PerforationTableScreen::new);
    }

    @SubscribeEvent
    public static void setupEvent(FMLClientSetupEvent event) {
        // preload the music instrument blocks
        event.enqueueWork(InstrumentBlocksHelper::updateBlockList);
    }
}
