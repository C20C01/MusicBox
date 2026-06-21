package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.block.entity.NoteGridBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public interface NoteGridBoxBlock {
    BooleanProperty HAS_NOTE_GRID = BooleanProperty.create("has_note_grid");

    default InteractionResult takeOutNoteGrid(Level level, NoteGridBoxBlockEntity box, Inventory inventory) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        inventory.add(box.removeItem());
        return InteractionResult.CONSUME;
    }

    default InteractionResult putInNoteGrid(Level level, BlockPos blockPos, NoteGridBoxBlockEntity box, ItemStack noteGrid) {
        if (!box.canPlaceItem(noteGrid)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        box.setItem(noteGrid.copy());
        noteGrid.shrink(1);// creative mode also need to shrink
        level.playSound(null, blockPos, SoundEvents.BOOK_PUT, SoundSource.PLAYERS);
        return InteractionResult.CONSUME;
    }

    default int getOutputSignal(Level level, BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(HAS_NOTE_GRID) && level.getBlockEntity(blockPos) instanceof NoteGridBoxBlockEntity box) {
            byte minNote = box.getMinNote();
            return minNote > 13 ? 15 : minNote + 2;
        } else {
            return 0;
        }
    }
}
