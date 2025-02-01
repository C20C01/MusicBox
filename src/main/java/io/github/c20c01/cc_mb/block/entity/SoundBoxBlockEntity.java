package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SoundBoxBlockEntity extends AbstractItemLoaderBlockEntity {
    public static final String SOUND_SHARD = "sound_shard";
    private Holder<SoundEvent> soundEvent = null;
    private Long soundSeed = null;

    public SoundBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.SOUND_BOX_BLOCK_ENTITY, blockPos, blockState, SOUND_SHARD);
    }

    public static void tryToPlaySound(Level level, BlockPos blockPos) {
        if (!level.isClientSide && level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity blockEntity) {
            blockEntity.playSound(level, blockPos);
        }
    }

    /**
     * Change the sound seed of the sound shard in the sound box.
     *
     * @return True if the sound seed is successfully changed.
     */
    public static boolean tryToChangeSoundSeed(Level level, BlockPos blockPos) {
        if (!level.isClientSide && level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity blockEntity && blockEntity.containSound()) {
            Long newSeed = SoundShard.tryToChangeSoundSeed(blockEntity.getItem(), level.random);
            if (newSeed != null) {
                blockEntity.soundSeed = newSeed;
                blockEntity.setChanged();
                return true;
            }
        }
        return false;
    }

    private void playSound(Level level, BlockPos blockPos) {
        if (getSoundEvent() != null) {
            Vec3 pos = blockPos.getCenter();
            level.gameEvent(null, GameEvent.INSTRUMENT_PLAY, pos);
            level.playSeededSound(null, pos.x, pos.y, pos.z, getSoundEvent(), SoundSource.BLOCKS, 3.0F, 1.0F, getSoundSeed().orElse(level.random.nextLong()));
        }
    }

    @Override
    protected void loadItem(ItemStack soundShard) {
        SoundShard.Info info = SoundShard.Info.ofItemStack(soundShard);
        soundEvent = info.sound();
        soundSeed = info.seed();
        if (level != null) {
            BlockUtils.changeProperty(level, worldPosition, getBlockState(), SoundBoxBlock.HAS_SOUND_SHARD, true);
        }
    }

    @Override
    protected void unloadItem() {
        soundEvent = null;
        soundSeed = null;
        if (level != null) {
            BlockUtils.changeProperty(level, worldPosition, getBlockState(), SoundBoxBlock.HAS_SOUND_SHARD, false);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("empty")) {
            if (!isEmpty()) {
                removeItem();
            }
        } else {
            super.load(tag);
        }
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Only send update packet when the sound box is under a music box.
        return getBlockState().getValue(SoundBoxBlock.UNDER_MUSIC_BOX) ? ClientboundBlockEntityDataPacket.create(this) : super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        if (tag.isEmpty()) {
            tag.putByte("empty", (byte) 0);
        }
        return tag;
    }

    @Override
    public boolean canPlaceItem(ItemStack itemStack) {
        return itemStack.is(CCMain.SOUND_SHARD_ITEM) && SoundShard.containSound(itemStack);
    }

    public boolean containSound() {
        return soundEvent != null;
    }

    @Nullable
    public Holder<SoundEvent> getSoundEvent() {
        return soundEvent;
    }

    public Optional<Long> getSoundSeed() {
        return Optional.ofNullable(soundSeed);
    }
}
