package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.inventory.SingleItemContainer;
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.MobListenAndActHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SoundBoxBlockEntity extends BlockEntity implements SingleItemContainer.SingleItemContainerBlockEntity {
    private ItemStack soundShard = ItemStack.EMPTY;

    @Nullable
    private Holder<SoundEvent> soundEvent;
    @Nullable
    private Long soundSeed = null;

    public SoundBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MusicBox.SOUND_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    /**
     * Change the sound seed of the sound shard in the sound box.
     *
     * @return True if the sound seed is successfully changed.
     */
    public static boolean tryToChangeSoundSeed(Level level, BlockPos blockPos) {
        if (!level.isClientSide() && level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity soundBox && soundBox.soundEvent != null) {
            Long newSeed = SoundShard.tryToChangeSoundSeed(soundBox.getItem(), level.getRandom());
            if (newSeed != null) {
                soundBox.soundSeed = newSeed;
                soundBox.setChanged();
                // Change seed may not change the blockState and will not trigger music box to update instrument.
                // So we need to manually update instrument of music box above if exists.
                if (level.getBlockEntity(blockPos.above()) instanceof MusicBoxBlockEntity musicBox) {
                    musicBox.updateInstrumentFromBelow(level, blockPos);
                }
                return true;
            }
        }
        return false;
    }

    public void playSound(Level level, BlockPos blockPos) {
        if (soundEvent != null) {
            Vec3 pos = blockPos.getCenter();
            if (!level.isClientSide()) {
                MobListenAndActHelper.nearbyMobsListen(level, blockPos, soundEvent.value().location());
            }
            level.gameEvent(null, GameEvent.INSTRUMENT_PLAY, pos);
            level.playSeededSound(null, pos.x, pos.y, pos.z, soundEvent, SoundSource.BLOCKS, 3.0F, 1.0F, soundSeed == null ? level.getRandom().nextLong() : soundSeed);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("sound_shard", ItemStack.CODEC).ifPresent(this::setItem);
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        if (!soundShard.isEmpty()) output.store("sound_shard", ItemStack.CODEC, soundShard);
        super.saveAdditional(output);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public ItemStack getItem() {
        return soundShard;
    }

    @Override
    public void setItem(ItemStack itemStack) {
        this.soundShard = itemStack;
        boolean hasSoundShard = !itemStack.isEmpty();
        if (hasSoundShard) {
            SoundShard.SoundInfo.ofItemStack(soundShard).ifPresent(soundInfo -> {
                soundEvent = soundInfo.soundEvent();
                soundSeed = soundInfo.soundSeed().orElse(null);
            });
        } else {
            soundEvent = null;
            soundSeed = null;
        }
        if (level != null) {
            BlockUtils.changeProperty(level, worldPosition, getBlockState(), SoundBoxBlock.HAS_SOUND_SHARD, hasSoundShard);
            setChanged(level, worldPosition, getBlockState());
        }
    }

    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
        return into.hasAnyMatching(ItemStack::isEmpty);
    }

    public boolean canPlaceItem(ItemStack itemStack) {
        return soundShard.isEmpty() && itemStack.is(MusicBox.SOUND_SHARD_ITEM.get()) && SoundShard.containSound(itemStack);
    }

    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == 0 && canPlaceItem(itemStack);
    }

    @Nullable
    public Identifier getSoundLocation() {
        return soundEvent == null ? null : soundEvent.value().location();
    }

    @Nullable
    public Long getSoundSeed() {
        return soundSeed;
    }
}
