package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.util.NoteGridData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class NoteGrid extends Item {
    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    public record Tooltip(NoteGridData.Page page, Byte numberOfPages) implements TooltipComponent {
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        NoteGridData.Page[] pages = NoteGridData.readFromTag(itemStack);
        return Optional.of(new Tooltip(pages[0], (byte) pages.length));
    }
}
