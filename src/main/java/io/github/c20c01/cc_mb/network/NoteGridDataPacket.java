package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.data.NoteGridDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public class NoteGridDataPacket {
    public record Request(int hash, BlockPos blockPos) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = CustomPacketPayload.codec(Request::encode, Request::decode);
        public static final CustomPacketPayload.Type<Request> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CCMain.ID, "note_grid_data_request"));

        public static Request decode(FriendlyByteBuf friendlyByteBuf) {
            return new Request(friendlyByteBuf.readInt(), friendlyByteBuf.readBlockPos());
        }

        private static void tryToReply(IPayloadContext context, int hash, BlockPos blockPos) {
            ServerLevel level = (ServerLevel) context.player().level();
            level.getBlockEntity(blockPos, CCMain.MUSIC_BOX_BLOCK_ENTITY.get())
                    .flatMap(MusicBoxBlockEntity::getPlayerData)
                    .ifPresent(noteGridData -> context.reply(new Reply(hash, noteGridData.toBytes())));
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeBlockPos(blockPos);
        }

        public static void handle(final Request packet, final IPayloadContext context) {
            context.enqueueWork(() -> tryToReply(context, packet.hash, packet.blockPos));
        }

        @Override
        public @Nonnull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Reply(int hash, byte[] data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, Reply> STREAM_CODEC = CustomPacketPayload.codec(Reply::encode, Reply::decode);
        public static final CustomPacketPayload.Type<Reply> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CCMain.ID, "note_grid_data_reply"));

        public static Reply decode(FriendlyByteBuf friendlyByteBuf) {
            return new Reply(friendlyByteBuf.readInt(), friendlyByteBuf.readByteArray());
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeByteArray(data);
        }

        public static void handle(final Reply packet, final IPayloadContext context) {
            context.enqueueWork(() -> NoteGridDataManager.getInstance().handleReply(packet.hash, packet.data));
        }

        @Override
        public @Nonnull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
