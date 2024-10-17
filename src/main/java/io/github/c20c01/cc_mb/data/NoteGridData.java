package io.github.c20c01.cc_mb.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.CollectionUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class NoteGridData {
    public static final byte MAX_SIZE = 64;
    public static final Codec<NoteGridData> CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<NoteGridData> read(DynamicOps<T> ops, T input) {
            return ops.getByteBuffer(input).map(byteBuffer -> NoteGridData.ofBytes(byteBuffer.array()));
        }

        @Override
        public <T> T write(DynamicOps<T> ops, NoteGridData value) {
            return ops.createByteList(ByteBuffer.wrap(ByteBuffer.wrap(new Encoder().encode(value)).array()));
        }
    };
    public static final StreamCodec<ByteBuf, NoteGridData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @Nonnull NoteGridData decode(@Nonnull ByteBuf buffer) {
            return new Decoder().decode(ByteBufCodecs.BYTE_ARRAY.decode(buffer));
        }

        @Override
        public void encode(@Nonnull ByteBuf buffer, @Nonnull NoteGridData value) {
            ByteBufCodecs.BYTE_ARRAY.encode(buffer, new Encoder().encode(value));
        }
    };
    private final ArrayList<Page> PAGES;

    private NoteGridData() {
        this.PAGES = new ArrayList<>(List.of(new Page()));
    }

    private NoteGridData(ArrayList<Page> pages) {
        this.PAGES = pages;
        this.PAGES.replaceAll(page -> page == null ? new Page() : page);
    }

    public static NoteGridData empty() {
        return new NoteGridData();
    }

    public static NoteGridData ofBytes(byte[] bytes) {
        return new Decoder().decode(bytes);
    }

    public static NoteGridData ofPages(Page... pages) {
        return new NoteGridData(new ArrayList<>(Arrays.asList(pages)));
    }

    public static NoteGridData ofBook(ItemStack book) {
        Stream<String> codes = book.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).getPages(true);
        ArrayList<Page> pages = new ArrayList<>();
        codes.limit(MAX_SIZE).forEach(code -> pages.add(Page.ofCode(code)));
        return new NoteGridData(pages);
    }

    public static NoteGridData ofNoteGrid(ItemStack noteGrid) {
        return noteGrid.getOrDefault(CCMain.NOTE_GRID_DATA.get(), empty());
    }

    public NoteGridData deepCopy() {
        return NoteGridUtils.join(NoteGridData.ofPages(new Page[this.size()]), this);
    }

    public byte[] toBytes() {
        return new Encoder().encode(this);
    }

    public ItemStack saveToNoteGrid(ItemStack noteGrid) {
        noteGrid.set(CCMain.NOTE_GRID_DATA.get(), this.deepCopy());
        return noteGrid;
    }

    @Override
    public String toString() {
        return "NoteGrid:" + PAGES;
    }

    @Override
    public int hashCode() {
        return PAGES.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteGridData noteGridData) {
            return PAGES.equals(noteGridData.PAGES);
        }
        return super.equals(obj);
    }

    public Page getPage(byte index) {
        return PAGES.get(index);
    }

    public ArrayList<Page> getPages() {
        return PAGES;
    }

    public byte size() {
        return (byte) PAGES.size();
    }

    private static class Decoder {
        final ArrayList<Page> PAGES = new ArrayList<>();
        final ArrayList<Beat> BEATS = new ArrayList<>(Page.BEATS_SIZE);
        final ArrayList<Byte> NOTES = new ArrayList<>(5);

        NoteGridData decode(byte[] data) {
            for (byte b : data) {
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
        final ArrayList<Byte> DATA = new ArrayList<>(1024);
        byte emptyBeats = 0;

        byte[] encode(NoteGridData data) {
            for (Page page : data.PAGES) {
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
            return CollectionUtils.toArray(DATA);
        }

        void addBeat(Beat beat) {
            byte[] notes = beat.getNotes();
            DATA.ensureCapacity(DATA.size() + notes.length);
            for (byte note : notes) {
                DATA.add((byte) (note + 1));
            }
        }

        void addFlag() {
            byte flag = (byte) (-emptyBeats - 1);
            DATA.add(flag);
            emptyBeats = 0;
        }

        void finishPage() {
            DATA.add((byte) 0);
            emptyBeats = 0;
        }
    }
}
