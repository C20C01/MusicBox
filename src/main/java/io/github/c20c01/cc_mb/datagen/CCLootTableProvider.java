package io.github.c20c01.cc_mb.datagen;

import com.google.common.collect.Iterables;
import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = CCMain.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLootTableProvider extends LootTableProvider {
    private CCLootTableProvider(DataGenerator gen) {
        super(gen.getPackOutput(), Set.of(), List.of(new SubProviderEntry(CCBlockLoot::new, LootContextParamSets.BLOCK)));
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new CCLootTableProvider(generator));
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationcontext) {
        map.forEach((key, value) -> value.validate(validationcontext));
    }

    private static class CCBlockLoot extends BlockLootSubProvider {
        protected CCBlockLoot() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            getKnownBlocks().forEach((this::dropSelf));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Iterables.transform(CCMain.BLOCKS.getEntries(), RegistryObject::get);
        }
    }
}