package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Packet to update the sound shard with specific sound event name.
 */
public record SoundShardUpdatePacket(int slot, ResourceLocation sound) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, SoundShardUpdatePacket> STREAM_CODEC = CustomPacketPayload.codec(SoundShardUpdatePacket::encode, SoundShardUpdatePacket::decode);
    public static final CustomPacketPayload.Type<SoundShardUpdatePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CCMain.ID, "sound_shard_update"));

    public static SoundShardUpdatePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new SoundShardUpdatePacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readResourceLocation());
    }

    private static void saveSoundEvent(ServerPlayer player, int slot, ResourceLocation location) {
        if (!Inventory.isHotbarSlot(slot) && slot != Inventory.SLOT_OFFHAND) {
            return;
        }
        ItemStack soundShard = player.getInventory().getItem(slot);
        if (soundShard.is(CCMain.SOUND_SHARD_ITEM.get())) {
            Holder<SoundEvent> sound = Holder.direct(SoundEvent.createVariableRangeEvent(location));
            soundShard.set(CCMain.SOUND_INFO.get(), new SoundShard.SoundInfo(sound, Optional.empty()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource(), 1.0F, 1.0F);
        }
    }

    public static void handle(final SoundShardUpdatePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> saveSoundEvent((ServerPlayer) context.player(), packet.slot(), packet.sound()));
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(slot);
        friendlyByteBuf.writeResourceLocation(sound);
    }

    @Override
    public @Nonnull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
