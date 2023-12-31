package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableScreen;
import io.github.c20c01.cc_mb.client.tooltip.ClientNoteGridTooltip;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Function;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Setup {
    @SubscribeEvent
    public static void setupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册菜单页面
            MenuScreens.register(CCMain.PERFORATION_TABLE_MENU.get(), PerforationTableScreen::new);
        });
    }

    @SubscribeEvent
    public static void TooltipComponentFactoriesEvent(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(NoteGrid.Tooltip.class, (Function<NoteGrid.Tooltip, ClientNoteGridTooltip>) ClientNoteGridTooltip::new);
    }
}
