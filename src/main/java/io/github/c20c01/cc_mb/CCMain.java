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
import io.github.c20c01.cc_mb.item.SoundShard;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import io.github.c20c01.cc_mb.network.SoundShardPacket;
import io.github.c20c01.cc_mb.util.InstrumentBlocksHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class CCMain implements ModInitializer {
    public static final String ID = "cc_mb";

    // text
    public static final String TEXT_EMPTY = "text." + ID + ".empty";
    public static final String TEXT_PUNCH = "text." + ID + ".punch";
    public static final String TEXT_CONNECT = "text." + ID + ".connect";
    public static final String TEXT_CHECK = "text." + ID + ".check";
    public static final String TEXT_SET_TICK_PER_BEAT = "text." + ID + ".set_tick_per_beat";
    public static final String TEXT_CHANGE_TICK_PER_BEAT = "text." + ID + ".change_tick_per_beat";
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

    // item
    public static final Item NOTE_GRID_ITEM;
    public static final Item AWL_ITEM;
    public static final Item SOUND_SHARD_ITEM;

    // block
    /**
     * Don't forget to add these blocks to the {@link io.github.c20c01.cc_mb.datagen.CCLootTableProvider loot table}.
     */
    public static final Block MUSIC_BOX_BLOCK;
    public static final BlockItem MUSIC_BOX_BLOCK_ITEM;
    public static final BlockEntityType<MusicBoxBlockEntity> MUSIC_BOX_BLOCK_ENTITY;

    public static final Block PERFORATION_TABLE_BLOCK;
    public static final BlockItem PERFORATION_TABLE_BLOCK_ITEM;

    public static final Block SOUND_BOX_BLOCK;
    public static final BlockItem SOUND_BOX_BLOCK_ITEM;
    public static final BlockEntityType<SoundBoxBlockEntity> SOUND_BOX_BLOCK_ENTITY;

    // menu
    public static final MenuType<PerforationTableMenu> PERFORATION_TABLE_MENU;

    static {
        NOTE_GRID_ITEM = new NoteGrid(new Item.Properties().stacksTo(1));
        AWL_ITEM = new Awl(new Item.Properties().durability(1024));
        SOUND_SHARD_ITEM = new SoundShard(new Item.Properties().stacksTo(1));

        MUSIC_BOX_BLOCK = new MusicBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        MUSIC_BOX_BLOCK_ITEM = new BlockItem(MUSIC_BOX_BLOCK, new Item.Properties());
        MUSIC_BOX_BLOCK_ENTITY = BlockEntityType.Builder.of(MusicBoxBlockEntity::new, MUSIC_BOX_BLOCK).build(DSL.remainderType());

        PERFORATION_TABLE_BLOCK = new PerforationTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
        PERFORATION_TABLE_BLOCK_ITEM = new BlockItem(PERFORATION_TABLE_BLOCK, new Item.Properties());

        SOUND_BOX_BLOCK = new SoundBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.CUSTOM_HEAD).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        SOUND_BOX_BLOCK_ITEM = new BlockItem(SOUND_BOX_BLOCK, new Item.Properties());
        SOUND_BOX_BLOCK_ENTITY = BlockEntityType.Builder.of(SoundBoxBlockEntity::new, SOUND_BOX_BLOCK).build(DSL.remainderType());

        PERFORATION_TABLE_MENU = new MenuType<>(PerforationTableMenu::new, FeatureFlags.VANILLA_SET);
    }

    public static ResourceLocation getKey(String id) {
        return new ResourceLocation(ID, id);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, getKey("note_grid"), NOTE_GRID_ITEM);
        Registry.register(BuiltInRegistries.ITEM, getKey("awl"), AWL_ITEM);
        Registry.register(BuiltInRegistries.ITEM, getKey("sound_shard"), SOUND_SHARD_ITEM);

        Registry.register(BuiltInRegistries.BLOCK, getKey("music_box_block"), MUSIC_BOX_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, getKey("music_box_block"), MUSIC_BOX_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, getKey("music_box_block"), MUSIC_BOX_BLOCK_ENTITY);

        Registry.register(BuiltInRegistries.BLOCK, getKey("perforation_table_block"), PERFORATION_TABLE_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, getKey("perforation_table_block"), PERFORATION_TABLE_BLOCK_ITEM);

        Registry.register(BuiltInRegistries.BLOCK, getKey("sound_box_block"), SOUND_BOX_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, getKey("sound_box_block"), SOUND_BOX_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, getKey("sound_box_block"), SOUND_BOX_BLOCK_ENTITY);

        Registry.register(BuiltInRegistries.MENU, getKey("perforation_table_menu"), PERFORATION_TABLE_MENU);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, getKey("tab"), FabricItemGroup.builder()
                .icon(MUSIC_BOX_BLOCK_ITEM::getDefaultInstance)
                .displayItems((parameters, output) -> {
                    output.accept(MUSIC_BOX_BLOCK_ITEM);
                    output.accept(PERFORATION_TABLE_BLOCK_ITEM);
                    output.accept(SOUND_BOX_BLOCK_ITEM);
                    output.accept(AWL_ITEM);
                    output.accept(SOUND_SHARD_ITEM);
                    output.accept(Items.SLIME_BALL);
                    output.accept(Items.WRITABLE_BOOK);
                    output.accept(NOTE_GRID_ITEM);
                    output.acceptAll(new PresetNoteGridData().getItems());
                    output.acceptAll(InstrumentBlocksHelper.getItems());
                })
                .title(Component.translatable(MUSIC_BOX_BLOCK.getDescriptionId()))
                .build()
        );

        ServerPlayNetworking.registerGlobalReceiver(NoteGridDataPacket.Request.KEY, (server, player, handler, buf, responseSender) -> NoteGridDataPacket.Request.handle(player, buf, responseSender));
        ServerPlayNetworking.registerGlobalReceiver(SoundShardPacket.KEY, (server, player, handler, buf, responseSender) -> SoundShardPacket.handle(player, buf));
    }
}
