package io.github.c20c01.cc_mb.util;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Find all blocks that provide a tunable instrument according to {@link NoteBlockInstrument},
 * also support new instruments added by other mods.
 */
public class InstrumentBlocksHelper {
    private static final ArrayList<Block> INSTRUMENT_BLOCKS = new ArrayList<>(16);
    private static final HashMap<NoteBlockInstrument, String> INSTRUMENT_TRANSLATION_KEY_MAP = new HashMap<>(16);

    static {
        add(Blocks.CHERRY_LEAVES, CCMain.TEXT_SOUND_HARP);
        add(Blocks.DARK_OAK_WOOD, CCMain.TEXT_SOUND_BASS);
        add(Blocks.SAND, CCMain.TEXT_SOUND_SNARE);
        add(Blocks.GREEN_STAINED_GLASS, CCMain.TEXT_SOUND_HAT);
        add(Blocks.SMOOTH_STONE, CCMain.TEXT_SOUND_BASS_DRUM);
        add(Blocks.GOLD_BLOCK, CCMain.TEXT_SOUND_BELL);
        add(Blocks.CLAY, CCMain.TEXT_SOUND_FLUTE);
        add(Blocks.PACKED_ICE, CCMain.TEXT_SOUND_CHIME);
        add(Blocks.PINK_WOOL, CCMain.TEXT_SOUND_GUITAR);
        add(Blocks.BONE_BLOCK, CCMain.TEXT_SOUND_XYLOPHONE);
        add(Blocks.IRON_BLOCK, CCMain.TEXT_SOUND_IRON_XYLOPHONE);
        add(Blocks.SOUL_SAND, CCMain.TEXT_SOUND_COW_BELL);
        add(Blocks.PUMPKIN, CCMain.TEXT_SOUND_DIDGERIDOO);
        add(Blocks.EMERALD_BLOCK, CCMain.TEXT_SOUND_BIT);
        add(Blocks.HAY_BLOCK, CCMain.TEXT_SOUND_BANJO);
        add(Blocks.GLOWSTONE, CCMain.TEXT_SOUND_PLING);
    }

    /**
     * Add the block that provide a tunable instrument and the translation key for the instrument.
     */
    public static void add(Block block, String translationKey) {
        NoteBlockInstrument instrument = block.defaultBlockState().instrument();
        if (instrument.isTunable() && !INSTRUMENT_TRANSLATION_KEY_MAP.containsKey(instrument)) {
            INSTRUMENT_BLOCKS.add(block);
            INSTRUMENT_TRANSLATION_KEY_MAP.put(instrument, translationKey);
        }
    }

    public static ArrayList<ItemStack> getItems() {
        updateBlockList();
        ArrayList<ItemStack> items = new ArrayList<>(INSTRUMENT_BLOCKS.size());
        for (Block block : INSTRUMENT_BLOCKS) {
            items.add(getItem(block));
        }
        return items;
    }

    private static ItemStack getItem(Block block) {
        NoteBlockInstrument instrument = block.defaultBlockState().instrument();
        Component hoverName;
        if (INSTRUMENT_TRANSLATION_KEY_MAP.containsKey(instrument)) {
            hoverName = Component.translatable(INSTRUMENT_TRANSLATION_KEY_MAP.get(instrument));
        } else {
            hoverName = Component.literal(instrument.getSerializedName());
        }
        return new ItemStack(block).setHoverName(hoverName);
    }

    public static void updateBlockList() {
        ArrayList<NoteBlockInstrument> instruments = new ArrayList<>();
        for (NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
            if (instrument.isTunable()) {
                instruments.add(instrument);
            }
        }
        for (Block block : INSTRUMENT_BLOCKS) {
            instruments.remove(block.defaultBlockState().instrument());
        }
        if (instruments.isEmpty()) {
            return;
        }
        // There are some instruments not found, iterate all blocks to find them
        LogUtils.getLogger().info("Iterating all blocks for instruments{}", instruments);
        for (Block block : ForgeRegistries.BLOCKS) {
            NoteBlockInstrument instrument = block.defaultBlockState().instrument();
            if (instruments.contains(instrument)) {
                instruments.remove(instrument);
                INSTRUMENT_BLOCKS.add(block);
            }
        }
    }
}