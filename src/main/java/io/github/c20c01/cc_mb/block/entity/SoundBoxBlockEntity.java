package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.util.MobListenAndActHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * No sync inside, so make sure to change blockState or call music box to update instrument after changing sound info.
 */
public class SoundBoxBlockEntity extends SingleItemContainerBlockEntityImpl {
    @Nullable
    private Holder<SoundEvent> soundEvent;
    @Nullable
    private Long soundSeed = null;

    public SoundBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MusicBox.SOUND_BOX_BLOCK_ENTITY.get(), blockPos, blockState, "sound_shard", SoundBoxBlock.HAS_SOUND_SHARD);
    }

    /**
     * Change the sound seed of the sound shard in the sound box.
     *
     * @return True if the sound seed is successfully changed.
     */
    public static boolean tryToChangeSoundSeed(Level level, BlockPos blockPos) {
        if (!level.isClientSide() && level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity soundBox && soundBox.soundEvent != null) {
            ItemStack soundShard = soundBox.getItem();
            Long newSeed = SoundShard.tryToChangeSoundSeed(soundShard, level.getRandom());
            if (newSeed != null) {
                soundBox.setItem(soundShard);
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
    public void setItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            soundEvent = null;
            soundSeed = null;
        } else {
            SoundShard.SoundInfo.ofItemStack(itemStack).ifPresent(soundInfo -> {
                soundEvent = soundInfo.soundEvent();
                soundSeed = soundInfo.soundSeed().orElse(null);
            });
        }
        if (level != null && level.getBlockEntity(worldPosition.above()) instanceof MusicBoxBlockEntity musicBox) {
            musicBox.updateInstrument(getSoundLocation(), getSoundSeed());
        }
        super.setItem(itemStack);
    }

    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
        return into.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public boolean canPlaceItem(ItemStack itemStack) {
        return isEmpty() && itemStack.is(MusicBox.SOUND_SHARD_ITEM.get()) && SoundShard.containSound(itemStack);
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
