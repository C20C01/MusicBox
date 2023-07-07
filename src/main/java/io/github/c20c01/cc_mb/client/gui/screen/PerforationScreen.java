package io.github.c20c01.cc_mb.client.gui.screen;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class PerforationScreen extends Screen {
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(CCMain.ID, "textures/gui/perforation_screen.png");
    private Button doneButton;
    private final ItemStack noteGrid;
    private final NoteGrid.Beat[] beats;

    protected PerforationScreen(ItemStack noteGrid) {
        super(Component.empty());
        this.noteGrid = noteGrid;
        beats = NoteGrid.readFromTag(noteGrid);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        int w = (this.width - 256) / 2;
        int h = (this.height - 124) / 2;
        //guiGraphics.drawString(this.font, "@", mouseX, mouseY, 16777215);
        for (int i = 0; i < beats.length; i++) {
            if (beats[i] == null) continue;
            byte[] notes = beats[i].getNotes();
            for (byte note : notes) {
                guiGraphics.drawString(this.font, ".", w + i * 2, h + 120 - note * 4, 16777215);
            }
        }

        guiGraphics.blit(GUI_BACKGROUND, w, h, 0, 0, 256, 124, 256, 124);
        //guiGraphics.drawString(this.font, Arrays.toString(NoteGrid.readFromTag(noteGrid)), 0, 0, 16777215);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CCMain.LOGGER.info(mouseX + ", " + mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
