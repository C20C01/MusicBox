package io.github.c20c01.cc_mb;

import com.mojang.datafixers.DSL;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.PerforationTableBlock;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import io.github.c20c01.cc_mb.data.PresetNoteGridData;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.item.NoteGrid;
import io.github.c20c01.cc_mb.item.PaperPaste;
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(ID, "network");

    // register
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    // item
    public static final RegistryObject<NoteGrid> NOTE_GRID_ITEM;
    public static final RegistryObject<Awl> AWL_ITEM;
    public static final RegistryObject<Item> SOUND_SHARD_ITEM;
    public static final RegistryObject<Item> PAPER_PASTE_ITEM;

    // block
    public static final RegistryObject<MusicBoxBlock> MUSIC_BOX_BLOCK;
    public static final RegistryObject<BlockItem> MUSIC_BOX_BLOCK_ITEM;
    public static final RegistryObject<BlockEntityType<MusicBoxBlockEntity>> MUSIC_BOX_BLOCK_ENTITY;

    public static final RegistryObject<PerforationTableBlock> PERFORATION_TABLE_BLOCK;
    public static final RegistryObject<BlockItem> PERFORATION_TABLE_BLOCK_ITEM;

    public static final RegistryObject<SoundBoxBlock> SOUND_BOX_BLOCK;
    public static final RegistryObject<BlockItem> SOUND_BOX_BLOCK_ITEM;
    public static final RegistryObject<BlockEntityType<SoundBoxBlockEntity>> SOUND_BOX_BLOCK_ENTITY;

    // menu
    public static final RegistryObject<MenuType<PerforationTableMenu>> PERFORATION_TABLE_MENU;


    static {
        NOTE_GRID_ITEM = ITEMS.register("note_grid", () -> new NoteGrid(new Item.Properties().stacksTo(1)));
        AWL_ITEM = ITEMS.register("awl", () -> new Awl(new Item.Properties().durability(1024)));
        SOUND_SHARD_ITEM = ITEMS.register("sound_shard", () -> new SoundShard(new Item.Properties().stacksTo(1)));
        PAPER_PASTE_ITEM = ITEMS.register("paper_paste", () -> new PaperPaste(new Item.Properties()));

        MUSIC_BOX_BLOCK = BLOCKS.register("music_box_block", () -> new MusicBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()));
        MUSIC_BOX_BLOCK_ITEM = ITEMS.register("music_box_block", () -> new BlockItem(MUSIC_BOX_BLOCK.get(), new Item.Properties()));
        MUSIC_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("music_box_block", () -> BlockEntityType.Builder.of(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_BLOCK = BLOCKS.register("perforation_table_block", () -> new PerforationTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()));
        PERFORATION_TABLE_BLOCK_ITEM = ITEMS.register("perforation_table_block", () -> new BlockItem(PERFORATION_TABLE_BLOCK.get(), new Item.Properties()));

        SOUND_BOX_BLOCK = BLOCKS.register("sound_box_block", () -> new SoundBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.CUSTOM_HEAD).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()));
        SOUND_BOX_BLOCK_ITEM = ITEMS.register("sound_box_block", () -> new BlockItem(SOUND_BOX_BLOCK.get(), new Item.Properties()));
        SOUND_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("sound_box_block", () -> BlockEntityType.Builder.of(SoundBoxBlockEntity::new, SOUND_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_MENU = MENU_TYPES.register("perforation_table_menu", () -> new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET));

        CREATIVE_MODE_TABS.register(ID + "_tab", () -> CreativeModeTab.builder()
                .icon(() -> MUSIC_BOX_BLOCK_ITEM.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(MUSIC_BOX_BLOCK_ITEM.get());
                    output.accept(PERFORATION_TABLE_BLOCK_ITEM.get());
                    output.accept(SOUND_BOX_BLOCK_ITEM.get());
                    output.accept(SOUND_SHARD_ITEM.get());
                    output.accept(AWL_ITEM.get());
                    output.accept(PAPER_PASTE_ITEM.get());
                    output.accept(Items.SLIME_BALL);
                    output.accept(Items.SHEARS);
                    output.accept(NOTE_GRID_ITEM.get());
                    output.acceptAll(PresetNoteGridData.getItems());
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
}
