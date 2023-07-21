package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCRecipeProvider extends RecipeProvider {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(Boolean.TRUE, new CCRecipeProvider(generator));
    }

    public CCRecipeProvider(DataGenerator generator) {
        super(generator.getPackOutput());
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CCMain.NOTE_GRID_ITEM.get(), 1).define('P', Items.PAPER).pattern("PPP").pattern("PPP").pattern("PPP").unlockedBy("has_paper", has(Items.PAPER)).save(consumer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, CCMain.AWL_ITEM.get(), 1).requires(Items.IRON_INGOT).requires(Items.IRON_NUGGET).unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get())).save(consumer);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CCMain.MUSIC_BOX_BLOCK_ITEM.get(), 1).define('N', Items.NOTE_BLOCK).define('P', ItemTags.PLANKS).pattern("PPP").pattern("PNP").pattern("PPP").unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get())).save(consumer);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CCMain.PERFORATION_TABLE_BLOCK_ITEM.get(), 1).define('N', CCMain.NOTE_GRID_ITEM.get()).define('P', ItemTags.PLANKS).pattern("NN").pattern("PP").pattern("PP").unlockedBy("has_note_grid", has(CCMain.NOTE_GRID_ITEM.get())).save(consumer);
    }
}