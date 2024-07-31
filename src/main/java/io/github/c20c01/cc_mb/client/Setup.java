package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableScreen;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
