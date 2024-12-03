package io.github.c20c01.cc_mb.network;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class NoteGridDataPacket {
    public record ToServer(int hash, BlockPos blockPos) {
        public static NoteGridDataPacket.ToServer decode(FriendlyByteBuf friendlyByteBuf) {
            return new NoteGridDataPacket.ToServer(friendlyByteBuf.readInt(), friendlyByteBuf.readBlockPos());
        }

        private static boolean isValid(BlockPos targetPos, @Nullable Player player) {
            if (player == null) {
                return false;
            }
            BlockPos playerPos = player.blockPosition();
            double disSqr = playerPos.distSqr(targetPos);
            if (disSqr >= 4096) {
                LogUtils.getLogger().warn("{} at {} requested data from {} which is too far away ({}).",
                        player.getDisplayName(), playerPos, targetPos, Math.sqrt(disSqr));
                return false;
            }
            return true;
        }

        private static void tryToReply(NetworkEvent.Context context, int hash, BlockPos blockPos) {
            ServerPlayer player = context.getSender();
            assert player != null;
            player.serverLevel().getBlockEntity(blockPos, CCMain.MUSIC_BOX_BLOCK_ENTITY.get())
                    .flatMap(MusicBoxBlockEntity::getPlayerData)
                    .ifPresent(noteGridData -> CCNetwork.CHANNEL.reply(new ToClient(hash, noteGridData.toBytes()), context));
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeBlockPos(blockPos);
        }

        public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (isValid(blockPos, context.getSender())) {
                    tryToReply(context, hash, blockPos);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ToClient(int hash, byte[] data) {
        public static NoteGridDataPacket.ToClient decode(FriendlyByteBuf friendlyByteBuf) {
            return new NoteGridDataPacket.ToClient(friendlyByteBuf.readInt(), friendlyByteBuf.readByteArray());
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeByteArray(data);
        }

        public void handleOnClient(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> NoteGridDataManager.getInstance().handleReply(hash, data));
            context.setPacketHandled(true);
        }
    }
}
