package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.MusicBox;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = MusicBox.ID)
public class MyRecipeProvider extends RecipeProvider {
    public MyRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        generator.addProvider(true, new Runner(generator.getPackOutput(), event.getLookupProvider()));
    }

    @Override
    protected void buildRecipes() {
        shapeless(RecipeCategory.MISC, MusicBox.NOTE_GRID_ITEM.get(), 1)
                .requires(Items.PAPER, 2).unlockedBy("has_paper", has(Items.PAPER))
                .save(output);
        shapeless(RecipeCategory.TOOLS, MusicBox.AWL_ITEM.get(), 1)
                .requires(Items.IRON_INGOT).requires(Items.IRON_NUGGET)
                .unlockedBy("has_note_grid", has(MusicBox.NOTE_GRID_ITEM.get()))
                .save(output);
        shaped(RecipeCategory.MISC, MusicBox.MUSIC_BOX_BLOCK_ITEM.get(), 1)
                .define('N', Items.NOTE_BLOCK)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .unlockedBy("has_note_grid", has(MusicBox.NOTE_GRID_ITEM.get()))
                .save(output);
        shaped(RecipeCategory.MISC, MusicBox.PERFORATION_TABLE_BLOCK_ITEM.get(), 1)
                .define('N', MusicBox.NOTE_GRID_ITEM.get())
                .define('P', ItemTags.PLANKS)
                .pattern("NN").pattern("PP").pattern("PP")
                .unlockedBy("has_note_grid", has(MusicBox.NOTE_GRID_ITEM.get()))
                .save(output);
        shapeless(RecipeCategory.TOOLS, MusicBox.SOUND_SHARD_ITEM.get(), 1)
                .requires(Items.GOLD_INGOT).requires(Items.ECHO_SHARD)
                .unlockedBy("has_music_box", has(MusicBox.MUSIC_BOX_BLOCK_ITEM.get()))
                .save(output);
        shaped(RecipeCategory.MISC, MusicBox.SOUND_BOX_BLOCK_ITEM.get(), 1)
                .define('H', ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PHP").pattern("PPP")
                .unlockedBy("has_music_box", has(MusicBox.MUSIC_BOX_BLOCK_ITEM.get()))
                .save(output);
        shapeless(RecipeCategory.MISC, MusicBox.PAPER_PASTE_ITEM.get(), 16)
                .requires(Items.PAPER).requires(Items.WATER_BUCKET)
                .unlockedBy("has_note_grid", has(MusicBox.NOTE_GRID_ITEM.get()))
                .save(output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new MyRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return MusicBox.ID + " Recipes";
        }
    }
}