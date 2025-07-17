package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.NoteGridScreen;
import io.github.c20c01.cc_mb.data.NoteGridCode;
import io.github.c20c01.cc_mb.data.NoteGridData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class NoteGrid extends Item {
    public NoteGrid(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        stack.getOrDefault(CCMain.NOTE_GRID_DATA.get(), new NoteGridCode(new byte[]{0})).addToTooltip(context, tooltipAdder, flag, stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ItemStack itemStack = player.getItemInHand(hand);
            NoteGridScreen.open(NoteGridData.ofNoteGrid(itemStack));
        }
        return InteractionResult.SUCCESS;
    }
}
