package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Packet to update the sound shard with specific sound event name.
 */
public record SoundShardUpdatePacket(int slot, String sound) implements FabricPacket {
    public static final ResourceLocation KEY = CCMain.getKey("sound_shard_update");
    public static final PacketType<SoundShardUpdatePacket> TYPE = PacketType.create(KEY, SoundShardUpdatePacket::new);

    public SoundShardUpdatePacket(FriendlyByteBuf friendlyByteBuf) {
        this(friendlyByteBuf.readInt(), friendlyByteBuf.readUtf());
    }

    public static void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        String sound = buf.readUtf();
        server.execute(() -> {
            if (!Inventory.isHotbarSlot(slot) && slot != Inventory.SLOT_OFFHAND) {
                return;
            }
            ItemStack soundShard = player.getInventory().getItem(slot);
            if (soundShard.is(CCMain.SOUND_SHARD_ITEM)) {
                CompoundTag tag = soundShard.getOrCreateTag();
                tag.putString(SoundShard.SOUND_EVENT, sound);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource(), 1.0F, 1.0F);
            }
        });
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
