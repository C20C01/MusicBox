package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CCMain.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLanguageProvider extends LanguageProvider {
    private static final String EN_US = "en_us";
    private static final String ZH_CN = "zh_cn";

    private final String locale;

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(Boolean.TRUE, new CCLanguageProvider(generator, EN_US));
        generator.addProvider(Boolean.TRUE, new CCLanguageProvider(generator, ZH_CN));
    }

    private CCLanguageProvider(DataGenerator gen, String locale) {
        super(gen.getPackOutput(), CCMain.ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        this.add(CCMain.NOTE_GRID_ITEM.get(), switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Note grid";
            case ZH_CN -> "纸带";
        });
        this.add(CCMain.MUSIC_BOX_BLOCK.get(), switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Music box";
            case ZH_CN -> "八音盒";
        });
        this.add(CCMain.PERFORATION_TABLE_BLOCK.get(), switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Perforation table";
            case ZH_CN -> "打孔台";
        });
        this.add(CCMain.AWL_ITEM.get(), switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Awl";
            case ZH_CN -> "锥子";
        });


        this.add(CCMain.TEXT_PUNCH, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Punch the note grid with awl";
            case ZH_CN -> "使用锥子为纸带打孔";
        });
        this.add(CCMain.TEXT_CONNECT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Connect the two note grid";
            case ZH_CN -> "将两纸带连接";
        });
        this.add(CCMain.TEXT_CLONE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Cover the note grid with the previous one\n(Requires consistent number of pages)";
            case ZH_CN -> "将前一张纸带覆盖到后一张纸带\n（需要页数一致）";
        });
        this.add(CCMain.TEXT_SET_TICK_PER_BEAT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Ticks per beat: ";
            case ZH_CN -> "每拍所用刻数: ";
        });
        this.add(CCMain.TEXT_CHANGE_TICK_PER_BEAT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Ticks per beat has been set to:";
            case ZH_CN -> "八音盒每拍所用刻数已设为: ";
        });
    }
}