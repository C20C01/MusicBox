package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Packet to update the sound shard with specific sound event name.
 */
public record SoundShardPacket(int slot, String sound) implements FabricPacket {
    public static final ResourceLocation KEY = CCMain.getKey("sound_shard_update");
    public static final PacketType<SoundShardPacket> TYPE = PacketType.create(KEY, SoundShardPacket::new);

    public SoundShardPacket(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readInt(), friendlyByteBuf.readUtf());
    }

    public static void handle(ServerPlayer player, FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        String sound = buf.readUtf();
        ItemStack soundShard = player.getInventory().getItem(slot);
        if (soundShard.is(CCMain.SOUND_SHARD_ITEM)) {
            CompoundTag tag = soundShard.getOrCreateTag();
            tag.putString(SoundShard.SOUND_EVENT, sound);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource(), 1.0F, 1.0F);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeUtf(sound);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
