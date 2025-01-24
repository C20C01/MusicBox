package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.CCMain;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class CCRecipeProvider extends FabricRecipeProvider {
    public CCRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> recipeOutput) {
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.MISC, CCMain.NOTE_GRID_ITEM, 1)
                .requires(Items.PAPER, 2).unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.TOOLS, CCMain.AWL_ITEM, 1)
                .requires(Items.IRON_INGOT).requires(Items.IRON_NUGGET)
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.MUSIC_BOX_BLOCK_ITEM, 1)
                .define('N', Items.NOTE_BLOCK)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PNP").pattern("PPP")
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.PERFORATION_TABLE_BLOCK_ITEM, 1)
                .define('N', CCMain.NOTE_GRID_ITEM)
                .define('P', ItemTags.PLANKS)
                .pattern("NN").pattern("PP").pattern("PP")
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.TOOLS, CCMain.SOUND_SHARD_ITEM, 1)
                .requires(Items.GOLD_INGOT).requires(Items.ECHO_SHARD)
                .unlockedBy("has_music_box", has(CCMain.MUSIC_BOX_BLOCK_ITEM))
                .save(recipeOutput);
        ShapedRecipeBuilder
                .shaped(RecipeCategory.MISC, CCMain.SOUND_BOX_BLOCK_ITEM, 1)
                .define('H', ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS)
                .define('P', ItemTags.PLANKS)
                .pattern("PPP").pattern("PHP").pattern("PPP")
                .unlockedBy("has_music_box", has(CCMain.MUSIC_BOX_BLOCK_ITEM))
                .save(recipeOutput);
        ShapelessRecipeBuilder
                .shapeless(RecipeCategory.MISC, CCMain.PAPER_PASTE_ITEM, 16)
                .requires(Items.PAPER).requires(Items.WATER_BUCKET)
                .unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM))
                .save(recipeOutput);
    }
}