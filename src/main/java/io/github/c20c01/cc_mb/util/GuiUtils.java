package io.github.c20c01.cc_mb.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiUtils {
    public static final int BLACK = 0xFF000000;
    public static final int HELP_NOTE_COLOR = 0x441122BB;

    /**
     * Send a byte to the menu at {@link net.minecraft.world.inventory.AbstractContainerMenu#clickMenuButton clickMenuButton}.
     *
     * @param containerId {@link net.minecraft.world.inventory.AbstractContainerMenu#containerId Menu's containerId}
     */
    public static void sendCodeToMenu(int containerId, byte code) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(containerId, code);
        }
    }

    public static void playSound(SoundEvent sound) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(sound);
        }
    }
}
