package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.NoteGridScreen;
import io.github.c20c01.cc_mb.data.NoteGridData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class NoteGrid extends Item {
    public NoteGrid(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openGui(NoteGridData data) {
        Minecraft.getInstance().setScreen(new NoteGridScreen(data));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(CCMain.TEXT_PAGE_SIZE, NoteGridData.ofNoteGrid(stack).size()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ItemStack itemStack = player.getItemInHand(hand);
            openGui(NoteGridData.ofNoteGrid(itemStack));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
