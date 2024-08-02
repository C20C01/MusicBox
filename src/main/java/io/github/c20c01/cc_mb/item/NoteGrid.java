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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
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
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(itemStack, level, components, flag);
        components.add(Component.translatable(CCMain.TEXT_PAGE_SIZE, NoteGridData.ofNoteGrid(itemStack).size()).withStyle(ChatFormatting.GRAY));
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
