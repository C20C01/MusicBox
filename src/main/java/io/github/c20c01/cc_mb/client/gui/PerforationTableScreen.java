package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
public class PerforationTableScreen extends AbstractContainerScreen<PerforationTableMenu> {
    protected static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(CCMain.ID, "textures/gui/perforation_table_screen.png");
    protected static final int BLACK = -16777216;
    protected NoteGrid.Page[] pages;
    protected byte page = 0;

    private PageButton backButton;
    private PageButton forwardButton;
    private NoteGridOnTableWidget gridOnTableWidget;
    private NoteGridEditWidget editWidget;
    private Button editDoneButton;
    private boolean editMode = false;

    public PerforationTableScreen(PerforationTableMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        int top = this.topPos + 57;
        this.backButton = this.addRenderableWidget(new PageButton(this.leftPos + 57, top, Boolean.FALSE, (button) -> pageBack(), Boolean.TRUE));
        this.forwardButton = this.addRenderableWidget(new PageButton(this.leftPos + 145, top, Boolean.TRUE, (button) -> pageForward(), Boolean.TRUE));
        this.gridOnTableWidget = this.addRenderableWidget(new NoteGridOnTableWidget(this.leftPos + 79, this.topPos + 15, this));
        this.editWidget = this.addRenderableWidget(new NoteGridEditWidget(this.width, this.height, this));
        this.editDoneButton = this.addRenderableWidget(new Button.Builder(Component.translatable("gui.back"), (button) -> changeEditMode(Boolean.FALSE)).pos(this.width / 2 - 80, this.height / 2 + 70).width(160).build());
        changeEditMode(editMode);
        updatePageButtonVisibility();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        if (editMode) {
            editWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
            editDoneButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void containerTick() {
        if (menu.shouldUpdate()) {
            NoteGrid.Page[] update = menu.getPages();
            if (update == null || page >= update.length) {
                page = 0;
                changeEditMode(Boolean.FALSE);
            }
            if (editMode && menu.mode != PerforationTableMenu.Mode.PUNCH) {
                changeEditMode(Boolean.FALSE);
            }
            pages = update;
            updatePageButtonVisibility();
            gridOnTableWidget.setTip(menu.mode);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        int left = (this.width - this.imageWidth) / 2;
        int up = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(GUI_BACKGROUND, left, up, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CCMain.LOGGER.info("mouseClicked: " + mouseX + ", " + mouseY + ", " + button);
        if (editMode) {
            editWidget.mouseClicked(mouseX, mouseY, button);
            editDoneButton.mouseClicked(mouseX, mouseY, button);
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
        if (editMode) editWidget.setMousePosOnGird(x, y);
    }

    private void pageBack() {
        if (page > 0) {
            --page;
        }
        updatePageButtonVisibility();
    }

    private void pageForward() {
        if (page < getPageSize() - 1) {
            ++this.page;
        }
        updatePageButtonVisibility();
    }

    private int getPageSize() {
        return pages == null ? 0 : pages.length;
    }

    private void updatePageButtonVisibility() {
        backButton.visible = page > 0;
        forwardButton.visible = page < getPageSize() - 1;
    }

    protected void changeEditMode(boolean isEditMode) {
        editMode = isEditMode;
        editWidget.visible = isEditMode;
        editDoneButton.visible = isEditMode;
        gridOnTableWidget.visible = !isEditMode;
    }

    protected boolean isEditMode() {
        return editMode;
    }
}
