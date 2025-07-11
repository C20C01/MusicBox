package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = CCMain.ID)
public class CCRecipeProvider extends RecipeProvider {
    public CCRecipeProvider(PackOutput gen, CompletableFuture<HolderLookup.Provider> lookup) {
        super(gen, lookup);
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(true, new CCRecipeProvider(generator.getPackOutput(), event.getLookupProvider()));
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.MISC, CCMain.NOTE_GRID_ITEM.get(), 1)
                .requires(Items.PAPER, 2).unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.TOOLS, CCMain.AWL_ITEM.get(), 1)
                .requires(Items.IRON_INGOT).requires(Items.IRON_NUGGET)
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.MUSIC_BOX_BLOCK_ITEM.get(), 1)
                .define('N', Items.NOTE_BLOCK)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.PERFORATION_TABLE_BLOCK_ITEM.get(), 1)
                .define('N', CCMain.NOTE_GRID_ITEM.get())
                .define('P', ItemTags.PLANKS)
                .pattern("NN").pattern("PP").pattern("PP")
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get()))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.TOOLS, CCMain.SOUND_SHARD_ITEM.get(), 1)
                .requires(Items.GOLD_INGOT).requires(Items.ECHO_SHARD)
                .unlockedBy("has_music_box", has(CCMain.MUSIC_BOX_BLOCK_ITEM.get()))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.SOUND_BOX_BLOCK_ITEM.get(), 1)
                .define('H', ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PHP").pattern("PPP")
                .unlockedBy("has_music_box", has(CCMain.MUSIC_BOX_BLOCK_ITEM.get()))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.MISC, CCMain.PAPER_PASTE_ITEM.get(), 16)
                .requires(Items.PAPER).requires(Items.WATER_BUCKET)
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get()))
                .save(recipeOutput);
    }
}