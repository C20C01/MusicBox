package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.client.gui.NoteGridScreen;
import io.github.c20c01.cc_mb.data.NoteGridData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class NoteGrid extends Item {
    public NoteGrid(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openGui(NoteGridData data) {
        Minecraft.getInstance().setScreen(new NoteGridScreen(data));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemStack itemStack = player.getItemInHand(hand);
            openGui(NoteGridData.ofNoteGrid(itemStack));
        }
        return super.use(level, player, hand);
    }
}
