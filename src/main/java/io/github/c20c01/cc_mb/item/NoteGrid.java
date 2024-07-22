package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.data.NoteGridData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public class NoteGrid extends Item {
    public static final String NOTE_GRID_ID = "NoteGridId";

    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    /**
     * Get the id of the note grid.
     *
     * @see NoteGridData#ofId(MinecraftServer, int)
     * @see NoteGridData#ofId(int, Consumer)
     */
    @Nullable
    public static Integer getId(ItemStack noteGrid) {
        if (noteGrid.is(CCMain.NOTE_GRID_ITEM.get())) {
            CompoundTag compoundtag = noteGrid.getOrCreateTag();
            return compoundtag.contains(NOTE_GRID_ID, CompoundTag.TAG_ANY_NUMERIC) ? compoundtag.getInt(NOTE_GRID_ID) : -1;
        }
        return null;
    }

    public static void setId(ItemStack noteGrid, int id) {
        noteGrid.getOrCreateTag().putInt(NOTE_GRID_ID, id);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        NoteGridData data = NoteGridData.ofId(getId(itemStack), null);
        if (data == null) return Optional.of(new Tooltip(null, (byte) 0));
        return Optional.of(new Tooltip(data, data.size()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pUsedHand);
        String name = itemStack.getDisplayName().getString();
        try {
            int id = Integer.parseInt(name.substring(1, name.length() - 1));
            setId(itemStack, id);
            pPlayer.displayClientMessage(Component.literal("Set id to " + id), true);
        } catch (NumberFormatException e) {
            setId(itemStack, 0);
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    public record Tooltip(@Nullable NoteGridData data, Byte numberOfPages) implements TooltipComponent {
    }
}
