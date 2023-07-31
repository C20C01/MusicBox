package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class SoundBoxBlockEntity extends BlockEntity implements ContainerSingleItem {
    public static final Holder<SoundEvent> EMPTY = Holder.direct(SoundEvents.EMPTY);
    private Holder<SoundEvent> instrument = EMPTY;
    private long soundSeed = 0;
    private ItemStack soundShard = ItemStack.EMPTY;

    public SoundBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.SOUND_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public Holder<SoundEvent> getInstrument() {
        return instrument;
    }

    public long getSoundSeed() {
        return soundSeed;
    }

    public boolean setSoundShard(ItemStack itemStack) {
        if (soundShard.isEmpty()) {
            soundShard = itemStack.copy();
            CompoundTag tag = soundShard.getOrCreateTag();
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("SoundEvent"));
            soundSeed = tag.getLong("SoundSeed");
            if (location == null) return false;
            instrument = Holder.direct(SoundEvent.createVariableRangeEvent(location));
            if (getLevel() != null) {
                CCUtil.changeProperty(getLevel(), getBlockPos(), getBlockState(), SoundBoxBlock.EMPTY, Boolean.FALSE);
            }
            setChanged();
            return true;
        }
        return false;
    }

    public ItemStack outSoundShard() {
        ItemStack oldItemStack = soundShard.copy();
        soundShard = ItemStack.EMPTY;
        instrument = EMPTY;
        soundSeed = 0;
        if (getLevel() != null) {
            CCUtil.changeProperty(getLevel(), getBlockPos(), getBlockState(), MusicBoxBlock.EMPTY, Boolean.TRUE);
        }
        setChanged();
        return oldItemStack;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        ItemStack itemStack = ItemStack.of(compoundTag.getCompound("SoundShard"));
        if (itemStack.is(CCMain.SOUND_SHARD_ITEM.get())) setSoundShard(itemStack);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.put("SoundShard", soundShard.save(new CompoundTag()));
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? soundShard : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot == 0 ? outSoundShard() : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot != 0) return;

        if (itemStack.isEmpty()) {
            outSoundShard();
        } else if (itemStack.is(CCMain.SOUND_SHARD_ITEM.get())) {
            setSoundShard(itemStack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return itemStack.is(CCMain.SOUND_SHARD_ITEM.get()) && this.getItem(slot).isEmpty();
    }

    @Override
    public boolean canTakeItem(Container container, int slot, ItemStack itemStack) {
        return false;
    }
}