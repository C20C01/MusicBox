package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.block.entity.NoteGridBoxBlockEntity;
import io.github.c20c01.cc_mb.block.entity.SingleItemContainerBlockEntityImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public interface NoteGridBoxBlock extends SingleItemContainerBlock {
    BooleanProperty POWERED = BlockStateProperties.POWERED;
    BooleanProperty HAS_NOTE_GRID = BooleanProperty.create("has_note_grid");

    static InteractionResult putInNoteGrid(Level level, BlockPos blockPos, SingleItemContainerBlockEntityImpl container, ItemStack noteGrid) {
        if (!container.canPlaceItem(noteGrid)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        container.setItem(noteGrid.copy());
        noteGrid.shrink(1);// creative mode also need to shrink
        level.playSound(null, blockPos, SoundEvents.BOOK_PUT, SoundSource.PLAYERS);
        return InteractionResult.CONSUME;
    }

    static int getOutputSignal(Level level, BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(HAS_NOTE_GRID) && level.getBlockEntity(blockPos) instanceof NoteGridBoxBlockEntity box) {
            byte minNote = box.getMinNote();
            return minNote > 13 ? 15 : minNote + 2;
        } else {
            return 0;
        }
    }
}
