package io.github.c20c01.cc_mb.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CCDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(CCLanguageProvider.factory(CCLanguageProvider.EN_US));
        pack.addProvider(CCLanguageProvider.factory(CCLanguageProvider.ZH_CN));
        pack.addProvider(CCLootTableProvider::new);
        pack.addProvider(CCRecipeProvider::new);
    }
}
