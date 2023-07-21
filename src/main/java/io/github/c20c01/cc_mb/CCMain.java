package io.github.c20c01.cc_mb;

import com.mojang.datafixers.DSL;
import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.block.PerforationTableBlock;
import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CCMain.ID)
public class CCMain {
    public static final String ID = "cc_mb";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 提示文本
    public static final String TEXT_PUNCH = "text." + ID + ".punch";
    public static final String TEXT_SUPERPOSE = "text." + ID + ".superpose";
    public static final String TEXT_CONNECT = "text." + ID + ".connect";
    public static final String TEXT_BOOK = "text." + ID + ".book";
    public static final String TEXT_SET_TICK_PER_BEAT = "text." + ID + ".set_tick_per_beat";
    public static final String TEXT_CHANGE_TICK_PER_BEAT = "text." + ID + ".change_tick_per_beat";
    public static final String TEXT_SHIFT_TO_PREVIEW = "text." + ID + ".shift_to_preview";

    // 音色文本
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

    // 网络相关
    public static final String NETWORK_VERSION = "1";
    public static final ResourceLocation CHANNEL_GRID_TO_S = new ResourceLocation(ID, "network_grid_to_s");

    // 注册器
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);


    // 物品
    public static final String NOTE_GRID_ITEM_ID = "note_grid";
    public static final RegistryObject<NoteGrid> NOTE_GRID_ITEM;
    public static final String AWL_ITEM_ID = "awl";
    public static final RegistryObject<Awl> AWL_ITEM;


    // 方块
    public static final String MUSIC_BOX_BLOCK_ID = "music_box_block";
    public static final RegistryObject<MusicBoxBlock> MUSIC_BOX_BLOCK;
    public static final RegistryObject<BlockItem> MUSIC_BOX_BLOCK_ITEM;
    public static final RegistryObject<BlockEntityType<MusicBoxBlockEntity>> MUSIC_BOX_BLOCK_ENTITY;

    public static final String PERFORATION_TABLE_BLOCK_ID = "perforation_table_block";
    public static final RegistryObject<PerforationTableBlock> PERFORATION_TABLE_BLOCK;
    public static final RegistryObject<BlockItem> PERFORATION_TABLE_BLOCK_ITEM;


    //GUI
    public static final String PERFORATION_TABLE_MENU_ID = "perforation_table_menu";
    public static final RegistryObject<MenuType<PerforationTableMenu>> PERFORATION_TABLE_MENU;


    static {
        NOTE_GRID_ITEM = ITEMS.register(NOTE_GRID_ITEM_ID, NoteGrid::new);
        AWL_ITEM = ITEMS.register(AWL_ITEM_ID, Awl::new);

        MUSIC_BOX_BLOCK = BLOCKS.register(MUSIC_BOX_BLOCK_ID, MusicBoxBlock::new);
        MUSIC_BOX_BLOCK_ITEM = ITEMS.register(MUSIC_BOX_BLOCK_ID, () -> new BlockItem(MUSIC_BOX_BLOCK.get(), new Item.Properties()));
        MUSIC_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(MUSIC_BOX_BLOCK_ID, () -> BlockEntityType.Builder.of(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_BLOCK = BLOCKS.register(PERFORATION_TABLE_BLOCK_ID, PerforationTableBlock::new);
        PERFORATION_TABLE_BLOCK_ITEM = ITEMS.register(PERFORATION_TABLE_BLOCK_ID, () -> new BlockItem(PERFORATION_TABLE_BLOCK.get(), new Item.Properties()));


        PERFORATION_TABLE_MENU = MENU_TYPES.register(PERFORATION_TABLE_MENU_ID, () -> new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET));


        CREATIVE_MODE_TABS.register(ID + "_tab", () -> CreativeModeTab.builder().icon(() -> MUSIC_BOX_BLOCK_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept(MUSIC_BOX_BLOCK_ITEM.get());
            output.accept(PERFORATION_TABLE_BLOCK_ITEM.get());
            output.accept(AWL_ITEM.get());
            output.accept(Items.SLIME_BALL);
            output.accept(Items.WRITABLE_BOOK);
            output.accept(NOTE_GRID_ITEM.get());
            output.accept(NoteGrid.changeToTestingGrid(new ItemStack(NOTE_GRID_ITEM.get())));

            output.accept(getNamedItem(Items.DARK_OAK_WOOD, Component.translatable(TEXT_SOUND_BASS)));
            output.accept(getNamedItem(Items.SAND, Component.translatable(TEXT_SOUND_SNARE)));
            output.accept(getNamedItem(Items.GREEN_STAINED_GLASS, Component.translatable(TEXT_SOUND_HAT)));
            output.accept(getNamedItem(Items.SMOOTH_STONE, Component.translatable(TEXT_SOUND_BASS_DRUM)));
            output.accept(getNamedItem(Items.GOLD_BLOCK, Component.translatable(TEXT_SOUND_BELL)));
            output.accept(getNamedItem(Items.CLAY, Component.translatable(TEXT_SOUND_FLUTE)));
            output.accept(getNamedItem(Items.PACKED_ICE, Component.translatable(TEXT_SOUND_CHIME)));
            output.accept(getNamedItem(Items.PINK_WOOL, Component.translatable(TEXT_SOUND_GUITAR)));
            output.accept(getNamedItem(Items.BONE_BLOCK, Component.translatable(TEXT_SOUND_XYLOPHONE)));
            output.accept(getNamedItem(Items.IRON_BLOCK, Component.translatable(TEXT_SOUND_IRON_XYLOPHONE)));
            output.accept(getNamedItem(Items.SOUL_SAND, Component.translatable(TEXT_SOUND_COW_BELL)));
            output.accept(getNamedItem(Items.PUMPKIN, Component.translatable(TEXT_SOUND_DIDGERIDOO)));
            output.accept(getNamedItem(Items.EMERALD_BLOCK, Component.translatable(TEXT_SOUND_BIT)));
            output.accept(getNamedItem(Items.HAY_BLOCK, Component.translatable(TEXT_SOUND_BANJO)));
            output.accept(getNamedItem(Items.GLOWSTONE, Component.translatable(TEXT_SOUND_PLING)));
            output.accept(getNamedItem(Items.MOSS_BLOCK, Component.translatable(TEXT_SOUND_HARP)));

        }).title(Component.translatable(MUSIC_BOX_BLOCK.get().getDescriptionId())).build());
    }

    public CCMain() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static ItemStack getNamedItem(ItemLike itemLike, Component component) {
        return new ItemStack(itemLike).setHoverName(component);
    }
}
