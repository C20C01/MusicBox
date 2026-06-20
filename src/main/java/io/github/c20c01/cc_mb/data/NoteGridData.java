package io.github.c20c01.cc_mb.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.c20c01.cc_mb.MusicBox;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.WritableBookContent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class NoteGridData implements TooltipProvider {
    public static final NoteGridData EMPTY = new NoteGridData();
    public static final byte MAX_SIZE = 64;
    public static final Codec<NoteGridData> CODEC = Codec.BYTE_BUFFER.xmap(buffer -> NoteGridData.ofBytes(buffer.array()), data -> ByteBuffer.wrap(data.toBytes()));
    public static final StreamCodec<ByteBuf, NoteGridData> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, NoteGridData::toBytes, NoteGridData::ofBytes);

    private final List<Page> pages;
    private int hashCode;
    private boolean dirty = true;
    private boolean cow = true;

    public NoteGridData() {
        this.pages = List.of(Page.EMPTY);
    }

    public NoteGridData(int size) {
        int realSize = Math.max(1, Math.min(size, MAX_SIZE));
        Page[] pageArray = new Page[realSize];
        Arrays.fill(pageArray, Page.EMPTY);
        this.pages = List.of(pageArray);
    }

    private NoteGridData(List<Page> pages) {
        this.pages = pages;
    }

    /**
     * Copy constructor (cow -> false)
     */
    private NoteGridData(NoteGridData data) {
        this.pages = new ArrayList<>(data.pages);
        this.hashCode = data.hashCode;
        this.dirty = data.dirty;// will be dirty in other methods, so just copy it.
        this.cow = false;
    }

    public static NoteGridData ofBook(ItemStack book) {
        WritableBookContent content = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (content == null) return EMPTY;

        List<Filterable<String>> codes = content.pages();
        if (codes.isEmpty()) return EMPTY;

        int size = Math.min(codes.size(), MAX_SIZE);
        Page[] pageArray = new Page[size];
        for (int i = 0; i < size; i++) pageArray[i] = Page.ofCode(codes.get(i).get(false));
        return new NoteGridData(List.of(pageArray));
    }

    public static NoteGridData ofNoteGrid(ItemStack noteGrid) {
        return noteGrid.getOrDefault(MusicBox.NOTE_GRID_DATA.get(), EMPTY);
    }

    public static NoteGridData ofBytes(byte[] bytes) {
        if (bytes[bytes.length - 1] != 0) {
            // in case the player directly modifies the code and crashes
            LogUtils.getLogger().error("Wrong note grid code format!");
            return NoteGridData.EMPTY;
        }

        final List<Page> pages = new ArrayList<>(64);
        final List<Beat> beats = new ArrayList<>(Page.BEATS_SIZE);
        final ByteList notes = new ByteArrayList(8);
        for (byte b : bytes) {
            if (b > 0) {
                // handle note
                byte note = (byte) (b - 1);
                if (Beat.isValidNote(note)) notes.add(note);
            } else {
                // finish beat
                if (!notes.isEmpty()) {
                    beats.add(new Beat(notes));
                    notes.clear();
                }
                // handle flag
                if (b == 0) {
                    pages.add(new Page(beats));
                    beats.clear();
                } else {
                    byte emptyBeats = (byte) (-b - 1);
                    for (byte j = 0; j < emptyBeats; j++) {
                        beats.add(Beat.EMPTY);
                    }
                }
            }
        }
        return new NoteGridData(pages);
    }

    public byte[] toBytes() {
        final ByteArrayList bytes = new ByteArrayList(128 * pages.size());
        byte emptyBeats = 0;

        for (int p = 0; p < pages.size(); p++) {
            Page page = pages.get(p);
            for (int b = 0; b < Page.BEATS_SIZE; b++) {
                ByteList notes = page.getBeat(b).getNotes();
                if (notes.isEmpty()) {
                    emptyBeats++;
                } else {
                    // add flag for empty beats
                    bytes.add((byte) (-emptyBeats - 1));
                    emptyBeats = 0;
                    // add beat
                    for (int n = 0; n < notes.size(); n++) bytes.add((byte) (notes.getByte(n) + 1));
                }
            }
            // add flag for end of page
            bytes.add((byte) 0);
            emptyBeats = 0;
        }
        return bytes.toByteArray();
    }

    public void saveToNoteGrid(ItemStack noteGrid) {
        noteGrid.set(MusicBox.NOTE_GRID_DATA.get(), makeCow());
    }

    public int size() {
        return pages.size();
    }

    public Page getPage(int index) {
        return pages.get(index);
    }

    private NoteGridData withPage(int pageNum, Page page) {
        if (this.pages.get(pageNum) != page) {
            final NoteGridData result = this.cow ? new NoteGridData(this) : this;
            result.pages.set(pageNum, page);
            return result;
        }
        return this;
    }

    /**
     * @return a new data with pages [fromPage, toPage) of this data.
     */
    public NoteGridData subData(int fromPage, int toPage) {
        if (fromPage >= toPage || fromPage < 0 || toPage > this.pages.size()) return this;
        List<Page> subPages = this.pages.subList(fromPage, toPage);
        if (!this.cow) {
            // make sure all the shared data will cow in other methods.
            for (int i = 0; i < subPages.size(); i++) subPages.get(i).makeCow();
        }
        // pages in new data(cow = true) will be copied with new array list when modified, so just use the sub list directly.
        return new NoteGridData(subPages);
    }

    /**
     * @return edited data, or null if no change is made.
     */
    @Nullable
    public NoteGridData withNoteChanged(int pageNum, int beatNum, byte note, boolean add) {
        Page currentPage = this.pages.get(pageNum);
        Beat currentBeat = currentPage.getBeat(beatNum);
        Beat resultBeat = add ? currentBeat.withNoteAdded(note) : currentBeat.withNoteRemoved(note);
        if (resultBeat != null) {
            NoteGridData edited = withPage(pageNum, currentPage.withBeat(beatNum, resultBeat));
            edited.dirty = true;
            return edited;
        }
        return null;
    }

    /**
     * @param other the data to be added, pages will {@link Page#makeCow() makeCow} if the data is not cow.
     */
    public NoteGridData withDataAdded(NoteGridData other) {
        List<Page> otherPages = other.pages;
        if (otherPages.isEmpty()) return this;

        int availableSize = NoteGridData.MAX_SIZE - this.pages.size();
        if (availableSize <= 0) return this;

        int size = Math.min(otherPages.size(), availableSize);
        List<Page> pagesToAdd = otherPages.subList(0, size);
        if (!other.cow) {
            // make sure all the shared data will cow in other methods.
            for (int i = 0; i < size; i++) otherPages.get(i).makeCow();
        }

        final NoteGridData result = this.cow ? new NoteGridData(this) : this;
        result.pages.addAll(pagesToAdd);
        result.dirty = true;
        return result;
    }

    public NoteGridData withDataMerged(NoteGridData other) {
        List<Page> otherPages = other.pages;
        int size = Math.min(this.pages.size(), otherPages.size());
        if (size == 0) return this;

        boolean cow = this.cow;
        NoteGridData result = this;
        for (int i = 0; i < size; i++) {
            Page currentPage = this.pages.get(i);
            Page resultPage = currentPage.withPageMerged(otherPages.get(i));
            if (resultPage == currentPage) continue;

            if (cow) {
                cow = false;
                result = new NoteGridData(this);
            }

            result.pages.set(i, resultPage);
        }
        result.dirty = true;// hard to say whether the data is changed, just set dirty to true to be safe.
        return result;
    }

    public NoteGridData makeCow() {
        if (!cow) {
            for (int i = 0; i < pages.size(); i++) pages.get(i).makeCow();
            cow = true;
        }
        return this;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
        tooltipAdder.accept(Component.translatable(MusicBox.TEXT_PAGE_SIZE, pages.size()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NoteGridData data)) return false;
        return pages.equals(data.pages);
    }

    @Override
    public int hashCode() {
        if (dirty) {
            hashCode = pages.hashCode();
            dirty = false;
        }
        return hashCode;
    }
}
