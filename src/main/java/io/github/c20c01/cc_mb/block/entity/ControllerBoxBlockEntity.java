package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.player.ControllerBoxPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ControllerBoxBlockEntity extends BaseBoxPlayerBlockEntity<ControllerBoxPlayer> {
    private final ControllerBoxPlayer PLAYER;

    public ControllerBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.CONTROLLER_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
        PLAYER = new ControllerBoxPlayer(this);
    }

    @Override
    ControllerBoxPlayer getPlayer() {
        return PLAYER;
    }

    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState) {
        PLAYER.nextBeat(level, blockPos, blockState);
    }
}
