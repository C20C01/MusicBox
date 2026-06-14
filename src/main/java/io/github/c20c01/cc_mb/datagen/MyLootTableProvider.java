package io.github.c20c01.cc_mb.datagen;

import com.google.common.collect.Iterables;
import io.github.c20c01.cc_mb.MusicBox;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = MusicBox.ID)
public class MyLootTableProvider extends LootTableProvider {
    private MyLootTableProvider(PackOutput gen, CompletableFuture<HolderLookup.Provider> lookup) {
        super(gen, Set.of(), List.of(new SubProviderEntry(MyBlockLoot::new, LootContextParamSets.BLOCK)), lookup);
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        generator.addProvider(true, new MyLootTableProvider(generator.getPackOutput(), event.getLookupProvider()));
    }

    private static class MyBlockLoot extends BlockLootSubProvider {
        protected MyBlockLoot(HolderLookup.Provider lookupProvider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
        }

        @Override
        protected void generate() {
            getKnownBlocks().forEach((this::dropSelf));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Iterables.transform(MusicBox.BLOCKS.getEntries(), DeferredHolder::get);
        }
    }
}