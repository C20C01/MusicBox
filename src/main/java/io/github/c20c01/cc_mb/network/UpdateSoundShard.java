package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class UpdateSoundShard {
    public static void toServer(Player player, String sound) {
        CCNetwork.CHANNEL.sendToServer(new SoundShardPacket(player.getInventory().selected, sound));
    }

    public static void onServer(ServerPlayer player, int slot, String sound) {
        ItemStack soundShard = player.getInventory().getItem(slot);
        if (soundShard.is(CCMain.SOUND_SHARD_ITEM.get())) {
            CompoundTag tag = soundShard.getOrCreateTag();
            tag.putString("SoundEvent", sound);
            tag.putLong("SoundSeed", player.getRandom().nextLong());
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource());
        }
    }
}
