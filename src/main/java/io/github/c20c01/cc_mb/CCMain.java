package io.github.c20c01.cc_mb;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.PerforationTableBlock;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import io.github.c20c01.cc_mb.data.NoteGridCode;
import io.github.c20c01.cc_mb.data.PresetNoteGridData;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.item.NoteGrid;
import io.github.c20c01.cc_mb.item.PaperPaste;
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CCMain.ID)
public class CCMain {
    public static final String ID = "cc_mb";

    // text
    public static final String TEXT_EMPTY = "text." + ID + ".empty";
    public static final String TEXT_PUNCH = "text." + ID + ".punch";
    public static final String TEXT_CONNECT = "text." + ID + ".connect";
    public static final String TEXT_CHECK = "text." + ID + ".check";
    public static final String TEXT_FIX = "text." + ID + ".fix";
    public static final String TEXT_CUT = "text." + ID + ".cut";
    public static final String TEXT_CANNOT_CUT = "text." + ID + ".cannot_cut";
    public static final String TEXT_TICK_PER_BEAT = "text." + ID + ".tick_per_beat";
    public static final String TEXT_CHANGE_TICK_PER_BEAT = "text." + ID + ".change_tick_per_beat";
    public static final String TEXT_CHANGE_OCTAVE = "text." + ID + ".change_octave";
    public static final String TEXT_SHARD_WITHOUT_SOUND = "text." + ID + ".shard_without_sound";
    public static final String TEXT_PAGE_SIZE = "text." + ID + ".page_size";

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

    // register
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    // component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NoteGridCode>> NOTE_GRID_DATA;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Byte>> TICK_PER_BEAT;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SoundShard.SoundInfo>> SOUND_INFO;

    // item
    public static final DeferredItem<NoteGrid> NOTE_GRID_ITEM;
    public static final DeferredItem<Awl> AWL_ITEM;
    public static final DeferredItem<Item> SOUND_SHARD_ITEM;
    public static final DeferredItem<PaperPaste> PAPER_PASTE_ITEM;

    // block
    public static final DeferredBlock<MusicBoxBlock> MUSIC_BOX_BLOCK;
    public static final DeferredItem<BlockItem> MUSIC_BOX_BLOCK_ITEM;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MusicBoxBlockEntity>> MUSIC_BOX_BLOCK_ENTITY;

    public static final DeferredBlock<PerforationTableBlock> PERFORATION_TABLE_BLOCK;
    public static final DeferredItem<BlockItem> PERFORATION_TABLE_BLOCK_ITEM;

    public static final DeferredBlock<SoundBoxBlock> SOUND_BOX_BLOCK;
    public static final DeferredItem<BlockItem> SOUND_BOX_BLOCK_ITEM;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoundBoxBlockEntity>> SOUND_BOX_BLOCK_ENTITY;

    // menu
    public static final DeferredHolder<MenuType<?>, MenuType<PerforationTableMenu>> PERFORATION_TABLE_MENU;


    static {
        NOTE_GRID_DATA = DATA_COMPONENTS.registerComponentType("notes", builder -> builder.persistent(NoteGridCode.CODEC).networkSynchronized(NoteGridCode.STREAM_CODEC).cacheEncoding());
        TICK_PER_BEAT = DATA_COMPONENTS.registerComponentType("tick_per_beat", builder -> builder.persistent(TickPerBeat.CODEC).networkSynchronized(ByteBufCodecs.BYTE).cacheEncoding());
        SOUND_INFO = DATA_COMPONENTS.registerComponentType("sound_info", builder -> builder.persistent(SoundShard.SoundInfo.CODEC).networkSynchronized(SoundShard.SoundInfo.STREAM_CODEC).cacheEncoding());

        NOTE_GRID_ITEM = ITEMS.registerItem("note_grid", NoteGrid::new, new Item.Properties().stacksTo(1));
        AWL_ITEM = ITEMS.registerItem("awl", Awl::new, new Item.Properties().durability(1024));
        SOUND_SHARD_ITEM = ITEMS.registerItem("sound_shard", SoundShard::new, new Item.Properties().stacksTo(1));
        PAPER_PASTE_ITEM = ITEMS.registerItem("paper_paste", PaperPaste::new);

        MUSIC_BOX_BLOCK = BLOCKS.registerBlock("music_box_block", MusicBoxBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        MUSIC_BOX_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("music_box_block", MUSIC_BOX_BLOCK);
        MUSIC_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("music_box_block", () -> new BlockEntityType<>(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK.get()));

        PERFORATION_TABLE_BLOCK = BLOCKS.registerBlock("perforation_table_block", PerforationTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
        PERFORATION_TABLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("perforation_table_block", PERFORATION_TABLE_BLOCK);

        SOUND_BOX_BLOCK = BLOCKS.registerBlock("sound_box_block", SoundBoxBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.CUSTOM_HEAD).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        SOUND_BOX_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("sound_box_block", SOUND_BOX_BLOCK);
        SOUND_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("sound_box_block", () -> new BlockEntityType<>(SoundBoxBlockEntity::new, SOUND_BOX_BLOCK.get()));

        PERFORATION_TABLE_MENU = MENU_TYPES.register("perforation_table_menu", () -> new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET));

        CREATIVE_MODE_TABS.register(ID + "_tab", () -> CreativeModeTab.builder()
                .icon(MUSIC_BOX_BLOCK_ITEM::toStack)
                .displayItems((parameters, output) -> {
                    output.accept(MUSIC_BOX_BLOCK_ITEM);
                    output.accept(PERFORATION_TABLE_BLOCK_ITEM);
                    output.accept(SOUND_BOX_BLOCK_ITEM);
                    output.accept(SOUND_SHARD_ITEM);
                    output.accept(AWL_ITEM);
                    output.accept(PAPER_PASTE_ITEM);
                    output.accept(Items.SLIME_BALL);
                    output.accept(Items.SHEARS);
                    output.accept(NOTE_GRID_ITEM);
                    output.acceptAll(PresetNoteGridData.getItems());
                    output.acceptAll(InstrumentBlocksHelper.getItems());
                })
                .title(Component.translatable(MUSIC_BOX_BLOCK.get().getDescriptionId()))
                .build()
        );
    }

    public CCMain(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
