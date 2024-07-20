package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

public class NoteGrid extends ComplexItem {
    public static final String NOTE_GRID_ID = "NoteGridId";

    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    @Nullable
    public static Integer getId(ItemStack noteGrid) {
        CompoundTag compoundtag = noteGrid.getTag();
        return compoundtag != null && compoundtag.contains(NOTE_GRID_ID, CompoundTag.TAG_ANY_NUMERIC) ? compoundtag.getInt(NOTE_GRID_ID) : null;
    }

    public static void setId(ItemStack noteGrid, int id) {
        noteGrid.getOrCreateTag().putInt(NOTE_GRID_ID, id);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        Page[] pages = NoteGridData.readFromTag(itemStack);
        return Optional.of(new Tooltip(pages[0], (byte) pages.length));
    }

    public record Tooltip(Page page, Byte numberOfPages) implements TooltipComponent {
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        setId(pPlayer.getItemInHand(pUsedHand), 0);
        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
