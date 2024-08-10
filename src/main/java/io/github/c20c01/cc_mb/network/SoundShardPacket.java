package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Packet to update the sound shard with specific sound event name.
 */
public record SoundShardPacket(int slot, String sound) {
    public static SoundShardPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new SoundShardPacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readUtf());
    }

    private static void saveSoundEvent(@Nullable ServerPlayer player, int slot, String sound) {
        if (player != null) {
            ItemStack soundShard = player.getInventory().getItem(slot);
            if (soundShard.is(CCMain.SOUND_SHARD_ITEM.get())) {
                CompoundTag tag = soundShard.getOrCreateTag();
                tag.putString(SoundShard.SOUND_EVENT, sound);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(slot);
        friendlyByteBuf.writeUtf(sound);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> saveSoundEvent(context.getSender(), slot, sound));
        context.setPacketHandled(true);
    }
}
