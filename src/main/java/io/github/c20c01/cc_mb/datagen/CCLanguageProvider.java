package io.github.c20c01.cc_mb.datagen;

import io.github.c20c01.cc_mb.CCMain;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class CCLanguageProvider extends FabricLanguageProvider {
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";

    private final String locale;

    private CCLanguageProvider(FabricDataOutput output, String locale) {
        super(output, locale);
        this.locale = locale;
    }

    public static FabricDataGenerator.Pack.Factory<FabricLanguageProvider> factory(String locale) {
        return (dataOutput) -> new CCLanguageProvider(dataOutput, locale);
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        builder.add(CCMain.NOTE_GRID_ITEM, switch (locale) {
            case EN_US -> "Note Grid";
            case ZH_CN -> "纸带";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.MUSIC_BOX_BLOCK, switch (locale) {
            case EN_US -> "Music Box";
            case ZH_CN -> "八音盒";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.PERFORATION_TABLE_BLOCK, switch (locale) {
            case EN_US -> "Perforation Table";
            case ZH_CN -> "打孔台";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.AWL_ITEM, switch (locale) {
            case EN_US -> "Awl";
            case ZH_CN -> "锥子";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.SOUND_BOX_BLOCK, switch (locale) {
            case EN_US -> "Sound Box";
            case ZH_CN -> "声响盒";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.SOUND_SHARD_ITEM, switch (locale) {
            case EN_US -> "Sound Shard";
            case ZH_CN -> "声响碎片";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.PAPER_PASTE_ITEM, switch (locale) {
            case EN_US -> "Paper Paste";
            case ZH_CN -> "纸糊";
            default -> throw new IllegalStateException();
        });

        builder.add(CCMain.TEXT_PUNCH, switch (locale) {
            case EN_US -> "Punch the Note Grid with Awl";
            case ZH_CN -> "使用锥子为纸带打孔";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CONNECT, switch (locale) {
            case EN_US -> "Connect the right Note Grid to the end of the left one";
            case ZH_CN -> "将右面的纸带连接到左面的纸带的末尾";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_EMPTY, switch (locale) {
            case EN_US -> "Put the Note Grid in to operate";
            case ZH_CN -> "放入纸带以进行操作";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CHECK, switch (locale) {
            case EN_US -> "Check the Note Grid";
            case ZH_CN -> "查看当前纸带";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_FIX, switch (locale) {
            case EN_US -> "Fix the Note Grid";
            case ZH_CN -> "修补纸带";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CUT, switch (this.locale) {
            case EN_US -> "Cut the Note Grid from the end of the current page";
            case ZH_CN -> "将纸带从本页末尾剪断";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CANNOT_CUT, switch (this.locale) {
            case EN_US -> "Cannot cut the last page";
            case ZH_CN -> "无法剪断最后一页";
            default -> throw new IllegalStateException();
        });

        builder.add(CCMain.TEXT_TICK_PER_BEAT, switch (locale) {
            case EN_US -> "Ticks per beat: ";
            case ZH_CN -> "每拍所用刻数: ";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CHANGE_TICK_PER_BEAT, switch (locale) {
            case EN_US -> "Ticks per beat has been set to: ";
            case ZH_CN -> "每拍所用刻数已设为: ";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_CHANGE_OCTAVE, switch (this.locale) {
            case EN_US -> "Octave offset has been set to: ";
            case ZH_CN -> "八度偏移已设为: ";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SHARD_WITHOUT_SOUND, switch (locale) {
            case EN_US -> "The shard doesn't have a sound";
            case ZH_CN -> "碎片还没有记录声音";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_PAGE_SIZE, switch (locale) {
            case EN_US -> "Page size: %1$s";
            case ZH_CN -> "页数: %1$s";
            default -> throw new IllegalStateException();
        });


        builder.add(CCMain.TEXT_SOUND_BASS, switch (locale) {
            case EN_US -> "Bass";
            case ZH_CN -> "贝斯";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_SNARE, switch (locale) {
            case EN_US -> "Snare";
            case ZH_CN -> "小军鼓";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_HAT, switch (locale) {
            case EN_US -> "Hat";
            case ZH_CN -> "击鼓沿";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_BASS_DRUM, switch (locale) {
            case EN_US -> "Bass Drum";
            case ZH_CN -> "底鼓";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_BELL, switch (locale) {
            case EN_US -> "Bell";
            case ZH_CN -> "铃铛（钟琴）";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_FLUTE, switch (locale) {
            case EN_US -> "Flute";
            case ZH_CN -> "长笛";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_CHIME, switch (locale) {
            case EN_US -> "Chime";
            case ZH_CN -> "管钟";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_GUITAR, switch (locale) {
            case EN_US -> "Guitar";
            case ZH_CN -> "吉他";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_XYLOPHONE, switch (locale) {
            case EN_US -> "Xylophone";
            case ZH_CN -> "木琴";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_IRON_XYLOPHONE, switch (locale) {
            case EN_US -> "Iron Xylophone";
            case ZH_CN -> "铁木琴（颤音琴）";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_COW_BELL, switch (locale) {
            case EN_US -> "Cow Bell";
            case ZH_CN -> "牛铃";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_DIDGERIDOO, switch (locale) {
            case EN_US -> "Didgeridoo";
            case ZH_CN -> "迪吉里杜管";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_BIT, switch (locale) {
            case EN_US -> "Bit";
            case ZH_CN -> "芯片（方波）";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_BANJO, switch (locale) {
            case EN_US -> "Banjo";
            case ZH_CN -> "班卓琴";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_PLING, switch (locale) {
            case EN_US -> "Pling";
            case ZH_CN -> "扣弦（电钢琴）";
            default -> throw new IllegalStateException();
        });
        builder.add(CCMain.TEXT_SOUND_HARP, switch (locale) {
            case EN_US -> "Harp";
            case ZH_CN -> "竖琴";
            default -> throw new IllegalStateException();
        });
    }
}