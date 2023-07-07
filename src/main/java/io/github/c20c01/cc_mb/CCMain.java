package io.github.c20c01.cc_mb;

import com.mojang.datafixers.DSL;
import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.block.PerforationTableBlock;
import io.github.c20c01.cc_mb.client.gui.menu.PerforationTableMenu;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

    // 注册器
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);


    // 物品
    public static final String NOTE_GRID_ITEM_ID = "note_grid";
    public static final RegistryObject<NoteGrid> NOTE_GRID_ITEM;


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

        MUSIC_BOX_BLOCK = BLOCKS.register(MUSIC_BOX_BLOCK_ID, MusicBoxBlock::new);
        MUSIC_BOX_BLOCK_ITEM = ITEMS.register(MUSIC_BOX_BLOCK_ID, () -> new BlockItem(MUSIC_BOX_BLOCK.get(), new Item.Properties()));
        MUSIC_BOX_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(MUSIC_BOX_BLOCK_ID, () -> BlockEntityType.Builder.of(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK.get()).build(DSL.remainderType()));

        PERFORATION_TABLE_BLOCK = BLOCKS.register(PERFORATION_TABLE_BLOCK_ID, PerforationTableBlock::new);
        PERFORATION_TABLE_BLOCK_ITEM = ITEMS.register(PERFORATION_TABLE_BLOCK_ID, () -> new BlockItem(PERFORATION_TABLE_BLOCK.get(), new Item.Properties()));


        PERFORATION_TABLE_MENU = MENU_TYPES.register(PERFORATION_TABLE_MENU_ID, () -> new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET));
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
