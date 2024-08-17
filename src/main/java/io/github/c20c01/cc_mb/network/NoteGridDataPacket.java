package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class NoteGridDataPacket {
    public record Request(int hash, BlockPos blockPos) implements FabricPacket {
        public static final ResourceLocation KEY = CCMain.getKey("note_grid_data_request");
        public static final PacketType<Request> TYPE = PacketType.create(KEY, Request::new);

        public Request(FriendlyByteBuf friendlyByteBuf) {
            this(friendlyByteBuf.readInt(), friendlyByteBuf.readBlockPos());
        }

        public static void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf, PacketSender responseSender) {
            int hash = buf.readInt();
            BlockPos blockPos = buf.readBlockPos();
            server.execute(() -> player.serverLevel().getBlockEntity(blockPos, CCMain.MUSIC_BOX_BLOCK_ENTITY)
                    .flatMap(MusicBoxBlockEntity::getPlayerData)
                    .ifPresent(noteGridData -> responseSender.sendPacket(new Reply(hash, noteGridData.toBytes())))
            );
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeInt(hash);
            buf.writeBlockPos(blockPos);
        }

        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
    }

    public record Reply(int hash, byte[] data) implements FabricPacket {
        public static final ResourceLocation KEY = CCMain.getKey("note_grid_data_reply");
        public static final PacketType<Reply> TYPE = PacketType.create(KEY, Reply::new);

        public Reply(FriendlyByteBuf friendlyByteBuf) {
            this(friendlyByteBuf.readInt(), friendlyByteBuf.readByteArray());
        }

        public static void handle(Minecraft client, FriendlyByteBuf buf) {
            int hash = buf.readInt();
            byte[] data = buf.readByteArray();
            client.execute(() -> NoteGridDataManager.getInstance().handleReply(hash, data));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeInt(hash);
            buf.writeByteArray(data);
        }

        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
    }
}
