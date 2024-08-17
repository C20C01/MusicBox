package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.CCMain;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class CCLootTableProvider extends FabricBlockLootTableProvider {
    CCLootTableProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        dropSelf(CCMain.MUSIC_BOX_BLOCK);
        dropSelf(CCMain.PERFORATION_TABLE_BLOCK);
        dropSelf(CCMain.SOUND_BOX_BLOCK);
    }
}