package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.NoteGridBoxBlock;
import io.github.c20c01.cc_mb.block.SoundBoxBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

@EventBusSubscriber(modid = MusicBox.ID, value = Dist.CLIENT)
public class MyModelProvider extends ModelProvider {
    public MyModelProvider(PackOutput output) {
        super(output, MusicBox.ID);
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client event) {
        event.createProvider(MyModelProvider::new);
    }

    private static void createMusicBoxBlock(BlockModelGenerators blockModels, Block block) {
        final String POWERED = "_powered";
        final String LOADED = "_loaded";

        MultiVariant base = plainVariant(TexturedModel.ORIENTABLE.create(block, blockModels.modelOutput));
        MultiVariant powered = plainVariant(
                TexturedModel.ORIENTABLE
                        .updateTexture(m -> m
                                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front" + POWERED)))
                        .createWithSuffix(block, POWERED, blockModels.modelOutput));
        MultiVariant loaded = plainVariant(
                TexturedModel.ORIENTABLE
                        .updateTexture(m -> m
                                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front" + LOADED)))
                        .createWithSuffix(block, LOADED, blockModels.modelOutput));
        MultiVariant poweredLoaded = plainVariant(
                TexturedModel.ORIENTABLE
                        .updateTexture(m -> m
                                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front" + LOADED + POWERED)))
                        .createWithSuffix(block, LOADED + POWERED, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(MusicBoxBlock.POWERED, NoteGridBoxBlock.HAS_NOTE_GRID)
                                .select(false, false, base)
                                .select(true, false, powered)
                                .select(false, true, loaded)
                                .select(true, true, poweredLoaded))
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    private static void createSoundBoxBlock(BlockModelGenerators blockModels, Block block) {
        final String LOADED = "_loaded";

        MultiVariant base = plainVariant(
                TexturedModel.CUBE_TOP
                        .updateTexture(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(MusicBox.MUSIC_BOX_BLOCK.get(), "_side")))
                        .create(block, blockModels.modelOutput));
        MultiVariant loaded = plainVariant(
                TexturedModel.CUBE_TOP
                        .updateTexture(m -> m
                                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(MusicBox.MUSIC_BOX_BLOCK.get(), "_side"))
                                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top" + LOADED)))
                        .createWithSuffix(block, LOADED, blockModels.modelOutput));
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(SoundBoxBlock.HAS_SOUND_SHARD)
                                .select(false, base)
                                .select(true, loaded)));
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        createMusicBoxBlock(blockModels, MusicBox.MUSIC_BOX_BLOCK.get());
        createSoundBoxBlock(blockModels, MusicBox.SOUND_BOX_BLOCK.get());
        blockModels.createTrivialBlock(MusicBox.PERFORATION_TABLE_BLOCK.get(), TexturedModel.CUBE_TOP);

        itemModels.generateFlatItem(MusicBox.NOTE_GRID_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(MusicBox.AWL_ITEM.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(MusicBox.SOUND_SHARD_ITEM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(MusicBox.PAPER_PASTE_ITEM.get(), ModelTemplates.FLAT_ITEM);
    }
}
