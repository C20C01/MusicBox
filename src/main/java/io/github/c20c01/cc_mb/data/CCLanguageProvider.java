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
            case EN_US -> "Connect the right note grid to the end of the left one";
            case ZH_CN -> "将右面的纸带连接到左面的纸带的末尾";
        });
        this.add(CCMain.TEXT_SUPERPOSE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Superpose the right note grid to the left one";
            case ZH_CN -> "将右面的纸带叠加到左面的纸带";
        });
        this.add(CCMain.TEXT_BOOK, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Read from book and superpose to the left note grid";
            case ZH_CN -> "从书中读取并叠加到左面的纸带";
        });

        this.add(CCMain.TEXT_SET_TICK_PER_BEAT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Ticks per beat: ";
            case ZH_CN -> "每拍所用刻数: ";
        });
        this.add(CCMain.TEXT_CHANGE_TICK_PER_BEAT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Ticks per beat has been set to: ";
            case ZH_CN -> "八音盒每拍所用刻数已设为: ";
        });
        this.add(CCMain.TEXT_SHIFT_TO_PREVIEW, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "[Shift to preview]";
            case ZH_CN -> "[按住Shift键预览]";
        });
        this.add(CCMain.TEXT_NUMBER_OF_PAGES, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Page count: ";
            case ZH_CN -> "总页数: ";
        });

        this.add(CCMain.TEXT_SOUND_BASS, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Bass";
            case ZH_CN -> "贝斯";
        });
        this.add(CCMain.TEXT_SOUND_SNARE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Snare";
            case ZH_CN -> "小军鼓";
        });
        this.add(CCMain.TEXT_SOUND_HAT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Hat";
            case ZH_CN -> "击鼓沿";
        });
        this.add(CCMain.TEXT_SOUND_BASS_DRUM, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Bass drum";
            case ZH_CN -> "底鼓";
        });
        this.add(CCMain.TEXT_SOUND_BELL, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Bell";
            case ZH_CN -> "铃铛（钟琴）";
        });
        this.add(CCMain.TEXT_SOUND_FLUTE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Flute";
            case ZH_CN -> "长笛";
        });
        this.add(CCMain.TEXT_SOUND_CHIME, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Chime";
            case ZH_CN -> "管钟";
        });
        this.add(CCMain.TEXT_SOUND_GUITAR, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Guitar";
            case ZH_CN -> "吉他";
        });
        this.add(CCMain.TEXT_SOUND_XYLOPHONE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Xylophone";
            case ZH_CN -> "木琴";
        });
        this.add(CCMain.TEXT_SOUND_IRON_XYLOPHONE, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Iron xylophone";
            case ZH_CN -> "铁木琴（颤音琴）";
        });
        this.add(CCMain.TEXT_SOUND_COW_BELL, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Cow bell";
            case ZH_CN -> "牛铃";
        });
        this.add(CCMain.TEXT_SOUND_DIDGERIDOO, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Didgeridoo";
            case ZH_CN -> "迪吉里杜管";
        });
        this.add(CCMain.TEXT_SOUND_BIT, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Bit";
            case ZH_CN -> "芯片（方波）";
        });
        this.add(CCMain.TEXT_SOUND_BANJO, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Banjo";
            case ZH_CN -> "班卓琴";
        });
        this.add(CCMain.TEXT_SOUND_PLING, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Pling";
            case ZH_CN -> "扣弦（电钢琴）";
        });
        this.add(CCMain.TEXT_SOUND_HARP, switch (this.locale) {
            default -> throw new IllegalStateException();
            case EN_US -> "Harp";
            case ZH_CN -> "竖琴";
        });
    }
}