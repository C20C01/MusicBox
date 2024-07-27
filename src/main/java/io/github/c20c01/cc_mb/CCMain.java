package io.github.c20c01.cc_mb;

import com.mojang.datafixers.DSL;
import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.PerforationTableBlock;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import io.github.c20c01.cc_mb.data.PresetNoteGrids;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.item.NoteGrid;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;

@Mod(CCMain.ID)
public class CCMain {
    public static final String ID = "cc_mb";

    // text
    public static final String TEXT_EMPTY = "text." + ID + ".empty";
    public static final String TEXT_PUNCH = "text." + ID + ".punch";
    public static final String TEXT_CONNECT = "text." + ID + ".connect";
    public static final String TEXT_CHECK = "text." + ID + ".check";
    public static final String TEXT_SET_TICK_PER_BEAT = "text." + ID + ".set_tick_per_beat";
    public static final String TEXT_CHANGE_TICK_PER_BEAT = "text." + ID + ".change_tick_per_beat";
    public static final String TEXT_SHARD_WITHOUT_SOUND = "text." + ID + ".shard_without_sound";

    // text-sound
    public static final String TEXT_SOUND_BASS = "text." + ID + ".bass";
    public static final String TEXT_SOUND_SNARE = "text." + ID + ".snare";
    public static final String TEXT_SOUND_HAT = "text." + ID + ".hat";
    public static final String TEXT_SOUND_BASS_DRUM = "text." + ID + ".bass_drum";
    public static final String TEXT_SOUND_BELL = "text." + ID + ".bell";
    public static final String TEXT_SOUND_FLUTE = "text." + ID + ".flute";
    public static final String TEXT_SOUND_CHIME = "text." + ID + ".chime";
    public static final String TEXT_SOUND_GUITAR = "text." + ID + ".guitar";
    public static final String TEXT_SOUND_XYLOPHONE = "text." + ID + ".xylophone";
    public static final String TEXT_SOUND_IRON_XYLOPHONE = "text." + ID + ".iron_xylophone";
    public static final String TEXT_SOUND_COW_BELL = "text." + ID + ".cow_bell";
    public static final String TEXT_SOUND_DIDGERIDOO = "text." + ID + ".didgeridoo";
    public static final String TEXT_SOUND_BIT = "text." + ID + ".bit";
    public static final String TEXT_SOUND_BANJO = "text." + ID + ".banjo";
    public static final String TEXT_SOUND_PLING = "text." + ID + ".pling";
    public static final String TEXT_SOUND_HARP = "text." + ID + ".harp";

    // network
    public static final String NETWORK_VERSION = "1";
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(ID, "network");

    // register
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);


    // item
    public static final String NOTE_GRID_ITEM_ID = "note_grid";
    public static final RegistryObject<NoteGrid> NOTE_GRID_ITEM;
    public static final String AWL_ITEM_ID = "awl";
    public static final RegistryObject<Awl> AWL_ITEM;
    public static final String SOUND_SHARD_ITEM_ID = "sound_shard";
    public static final RegistryObject<Item> SOUND_SHARD_ITEM;

    // block
    public static final String MUSIC_BOX_BLOCK_ID = "music_box_block";
    public static final RegistryObject<MusicBoxBlock> MUSIC_BOX_BLOCK;
    public static final RegistryObject<BlockItem> MUSIC_BOX_BLOCK_ITEM;
    public static final RegistryObject<BlockEntityType<MusicBoxBlockEntity>> MUSIC_BOX_BLOCK_ENTITY;

    public static final String PERFORATION_TABLE_BLOCK_ID = "perforation_table_block";
    public static final RegistryObject<PerforationTableBlock> PERFORATION_TABLE_BLOCK;
    public static final RegistryObject<BlockItem> PERFORATION_TABLE_BLOCK_ITEM;

    public static final String SOUND_BOX_BLOCK_ID = "sound_box_block";
    public static final RegistryObject<SoundBoxBlock> SOUND_BOX_BLOCK;
    public static final RegistryObject<BlockItem> SOUND_BOX_BLOCK_ITEM;
    public static final RegistryObject<BlockEntityType<SoundBoxBlockEntity>> SOUND_BOX_BLOCK_ENTITY;


    // GUI
    public static final String PERFORATION_TABLE_MENU_ID = "perforation_table_menu";
    public static final RegistryObject<MenuType<PerforationTableMenu>> PERFORATION_TABLE_MENU;


    static {
        NOTE_GRID_ITEM = ITEMS.register(NOTE_GRID_ITEM_ID, () -> new NoteGrid(new Item.Properties().stacksTo(1)));
        AWL_ITEM = ITEMS.register(AWL_ITEM_ID, () -> new Awl(new Item.Properties().durability(512)));
        SOUND_SHARD_ITEM = ITEMS.register(SOUND_SHARD_ITEM_ID, () -> new SoundShard(new Item.Properties().stacksTo(1)));

        MUSIC_BOX_BLOCK = BLOCKS.register(MUSIC_BOX_BLOCK_ID, () -> new MusicBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()));
        MUSIC_BOX_BLOCK_ITEM = ITEMS.register(MUSIC_BOX_BLOCK_ID, () -> new BlockItem(MUSIC_BOX_BLOCK.get(), new Item.Properties()));
        MUSIC_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(MUSIC_BOX_BLOCK_ID, () -> BlockEntityType.Builder.of(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_BLOCK = BLOCKS.register(PERFORATION_TABLE_BLOCK_ID, () -> new PerforationTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()));
        PERFORATION_TABLE_BLOCK_ITEM = ITEMS.register(PERFORATION_TABLE_BLOCK_ID, () -> new BlockItem(PERFORATION_TABLE_BLOCK.get(), new Item.Properties()));

        SOUND_BOX_BLOCK = BLOCKS.register(SOUND_BOX_BLOCK_ID, () -> new SoundBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.CUSTOM_HEAD).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()));
        SOUND_BOX_BLOCK_ITEM = ITEMS.register(SOUND_BOX_BLOCK_ID, () -> new BlockItem(SOUND_BOX_BLOCK.get(), new Item.Properties()));
        SOUND_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(SOUND_BOX_BLOCK_ID, () -> BlockEntityType.Builder.of(SoundBoxBlockEntity::new, SOUND_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_MENU = MENU_TYPES.register(PERFORATION_TABLE_MENU_ID, () -> new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET));

        CREATIVE_MODE_TABS.register(ID + "_tab", () -> CreativeModeTab.builder()
                .icon(() -> MUSIC_BOX_BLOCK_ITEM.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(MUSIC_BOX_BLOCK_ITEM.get());
                    output.accept(PERFORATION_TABLE_BLOCK_ITEM.get());
                    output.accept(SOUND_BOX_BLOCK_ITEM.get());
                    output.accept(AWL_ITEM.get());
                    output.accept(SOUND_SHARD_ITEM.get());
                    output.accept(Items.SLIME_BALL);
                    output.accept(Items.WRITABLE_BOOK);
                    output.accept(NOTE_GRID_ITEM.get());
                    output.acceptAll(new PresetNoteGrids().get());
                    output.acceptAll(InstrumentBlocksHelper.getItems());
                })
                .title(Component.translatable(MUSIC_BOX_BLOCK.get().getDescriptionId()))
                .build()
        );
    }

    public CCMain() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    /**
     * Find all blocks that provide a tunable instrument according to {@link NoteBlockInstrument},
     * also support new instruments added by other mods.
     */
    public static class InstrumentBlocksHelper {
        private static final ArrayList<Block> INSTRUMENT_BLOCKS = new ArrayList<>(16);
        private static final HashMap<NoteBlockInstrument, String> INSTRUMENT_TRANSLATION_KEY_MAP = new HashMap<>(16);

        static {
            add(Blocks.CHERRY_LEAVES, TEXT_SOUND_HARP);
            add(Blocks.DARK_OAK_WOOD, TEXT_SOUND_BASS);
            add(Blocks.SAND, TEXT_SOUND_SNARE);
            add(Blocks.GREEN_STAINED_GLASS, TEXT_SOUND_HAT);
            add(Blocks.SMOOTH_STONE, TEXT_SOUND_BASS_DRUM);
            add(Blocks.GOLD_BLOCK, TEXT_SOUND_BELL);
            add(Blocks.CLAY, TEXT_SOUND_FLUTE);
            add(Blocks.PACKED_ICE, TEXT_SOUND_CHIME);
            add(Blocks.PINK_WOOL, TEXT_SOUND_GUITAR);
            add(Blocks.BONE_BLOCK, TEXT_SOUND_XYLOPHONE);
            add(Blocks.IRON_BLOCK, TEXT_SOUND_IRON_XYLOPHONE);
            add(Blocks.SOUL_SAND, TEXT_SOUND_COW_BELL);
            add(Blocks.PUMPKIN, TEXT_SOUND_DIDGERIDOO);
            add(Blocks.EMERALD_BLOCK, TEXT_SOUND_BIT);
            add(Blocks.HAY_BLOCK, TEXT_SOUND_BANJO);
            add(Blocks.GLOWSTONE, TEXT_SOUND_PLING);
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
}
