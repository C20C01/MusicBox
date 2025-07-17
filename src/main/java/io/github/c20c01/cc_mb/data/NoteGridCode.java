package io.github.c20c01.cc_mb.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import io.github.c20c01.cc_mb.CCMain;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Component for storing note grid data.
 */
public record NoteGridCode(byte[] code) implements TooltipProvider {
    public static final Codec<NoteGridCode> CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<NoteGridCode> read(DynamicOps<T> ops, T input) {
            return ops.getByteBuffer(input).map(byteBuffer -> new NoteGridCode(byteBuffer.array()));
        }

        @Override
        public <T> T write(DynamicOps<T> ops, NoteGridCode value) {
            return ops.createByteList(ByteBuffer.wrap(value.code));
        }
    };
    public static final StreamCodec<ByteBuf, NoteGridCode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY,
            NoteGridCode::code,
            NoteGridCode::new
    );

    public static NoteGridCode of(NoteGridData data) {
        return new NoteGridCode(new NoteGridCode.Encoder().encode(data));
    }

    public static NoteGridCode of(ItemStack itemStack) {
        return itemStack.getOrDefault(CCMain.NOTE_GRID_DATA.get(), NoteGridCode.of(NoteGridData.empty()));
    }

    public NoteGridData toData() {
        return new NoteGridCode.Decoder().decode(code);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(code);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteGridCode(byte[] other)) {
            return Arrays.equals(code, other);
        }
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
        if (code[code.length - 1] != 0) {
            tooltipAdder.accept(Component.translatable(CCMain.TEXT_PAGE_SIZE, 0).withStyle(ChatFormatting.RED));
        }
        byte size = 0;
        for (byte b : code) {
            if (b == 0) size++;
        }
        tooltipAdder.accept(Component.translatable(CCMain.TEXT_PAGE_SIZE, size).withStyle(ChatFormatting.GRAY));
    }

    private static class Decoder {
        final ArrayList<Page> PAGES = new ArrayList<>();
        final ArrayList<Beat> BEATS = new ArrayList<>(Page.BEATS_SIZE);
        final ByteArrayList NOTES = new ByteArrayList(5);

        NoteGridData decode(byte[] code) {
            if (code[code.length - 1] != 0) {
                // in case the player directly modifies the code and crashes
                LogUtils.getLogger().error("Wrong note grid code format!");
                return NoteGridData.empty();
            }
            for (byte b : code) {
                if (b > 0) {
                    handleNote(b);
                } else {
                    handleFlag(b);
                }
            }
            return new NoteGridData(PAGES);
        }

        void handleFlag(byte b) {
            finishBeat();
            if (b == 0) {
                finishPage();
            } else {
                byte emptyBeats = (byte) (-b - 1);
                for (byte i = 0; i < emptyBeats; i++) {
                    BEATS.add(null);
                }
            }
        }

        void handleNote(byte b) {
            byte note = (byte) (b - 1);
            if (Beat.isAvailableNote(note)) {
                NOTES.add(note);
            }
        }

        void finishBeat() {
            if (!NOTES.isEmpty()) {
                BEATS.add(Beat.ofNotes(NOTES));
                NOTES.clear();
            }
        }

        void finishPage() {
            PAGES.add(Page.ofBeats(BEATS));
            BEATS.clear();
        }
    }

    private static class Encoder {
        final ByteArrayList CODE = new ByteArrayList(1024);
        byte emptyBeats = 0;

        byte[] encode(NoteGridData data) {
            for (Page page : data.getPages()) {
                for (byte i = 0; i < Page.BEATS_SIZE; i++) {
                    if (page.isEmptyBeat(i)) {
                        emptyBeats++;
                    } else {
                        addFlag();
                        addBeat(page.getBeat(i));
                    }
                }
                finishPage();
            }
            return CODE.toByteArray();
        }

        void addBeat(Beat beat) {
            ByteArraySet notes = beat.getNotes();
            CODE.ensureCapacity(CODE.size() + notes.size());
            for (byte note : notes) {
                CODE.add((byte) (note + 1));
            }
        }

        void addFlag() {
            byte flag = (byte) (-emptyBeats - 1);
            CODE.add(flag);
            emptyBeats = 0;
        }

        void finishPage() {
            CODE.add((byte) 0);
            emptyBeats = 0;
        }
    }
}
