package mekanism.client.gui.element.scroll;

import java.util.Set;
import java.util.function.ObjIntConsumer;
import mekanism.api.Upgrade;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuiUpgradeScrollList extends GuiScrollList {

    private static final ResourceLocation UPGRADE_SELECTION = MekanismUtils.getResource(ResourceType.GUI, "upgrade_selection.png");
    private static final int TEXTURE_WIDTH = 58;
    private static final int TEXTURE_HEIGHT = 36;

    private final TileComponentUpgrade component;
    private final Runnable onSelectionChange;
    @Nullable
    private Upgrade selectedType;

    public GuiUpgradeScrollList(IGuiWrapper gui, int x, int y, int width, int height, TileComponentUpgrade component, Runnable onSelectionChange) {
        super(gui, x, y, width, height, TEXTURE_HEIGHT / 3, GuiElementHolder.HOLDER, GuiElementHolder.HOLDER_SIZE);
        this.component = component;
        this.onSelectionChange = onSelectionChange;
    }

    private Set<Upgrade> getCurrentUpgrades() {
        return component.getInstalledTypes();
    }

    @Override
    protected int getMaxElements() {
        return getCurrentUpgrades().size();
    }

    @Override
    public boolean hasSelection() {
        return selectedType != null;
    }

    @Override
    protected void setSelected(int index) {
        Set<Upgrade> currentUpgrades = getCurrentUpgrades();
        if (index >= 0 && index < currentUpgrades.size()) {
            Upgrade newSelection = currentUpgrades.toArray(new Upgrade[0])[index];
            if (selectedType != newSelection) {
                selectedType = newSelection;
                onSelectionChange.run();
            }
        }
    }

    @Nullable
    public Upgrade getSelection() {
        return selectedType;
    }

    @Override
    public void clearSelection() {
        if (selectedType != null) {
            selectedType = null;
            onSelectionChange.run();
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        forEachUpgrade((upgrade, multipliedElement) -> drawTextScaledBound(guiGraphics, TextComponentUtil.build(upgrade), relativeX + 13, relativeY + 3 + multipliedElement,
              titleTextColor(), 44));
    }

    @Override
    public void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        if (mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1) {
            forEachUpgrade((upgrade, multipliedElement) -> {
                if (mouseY >= getY() + 1 + multipliedElement && mouseY < getY() + 1 + multipliedElement + elementHeight) {
                    displayTooltips(guiGraphics, mouseX, mouseY, upgrade.getDescription());
                }
            });
        }
    }

    @Override
    public void renderElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //Draw elements
        if (hasSelection() && component.getUpgrades(getSelection()) == 0) {
            clearSelection();
        }
        forEachUpgrade((upgrade, multipliedElement) -> {
            int shiftedY = getY() + 1 + multipliedElement;
            int j = 1;
            if (upgrade == getSelection()) {
                j = 2;
            } else if (mouseX >= getX() + 1 && mouseX < getX() + barXShift - 1 && mouseY >= shiftedY && mouseY < shiftedY + elementHeight) {
                j = 0;
            }
            MekanismRenderer.color(upgrade.getColor());
            guiGraphics.blit(UPGRADE_SELECTION, relativeX + 1, relativeY + 1 + multipliedElement, 0, elementHeight * j, TEXTURE_WIDTH, elementHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            MekanismRenderer.resetColor();
        });
        //Note: This needs to be in its own loop as rendering the items is likely to cause the texture manager to be bound to a different texture
        // and thus would make the selection area background get all screwed up
        forEachUpgrade((upgrade, multipliedElement) -> gui().renderItem(guiGraphics, UpgradeUtils.getStack(upgrade), relativeX + 3, relativeY + 3 + multipliedElement, 0.5F));
    }

    private void forEachUpgrade(ObjIntConsumer<Upgrade> consumer) {
        Upgrade[] upgrades = getCurrentUpgrades().toArray(new Upgrade[0]);
        int currentSelection = getCurrentSelection();
        for (int i = 0; i < getFocusedElements(); i++) {
            int index = currentSelection + i;
            if (index > upgrades.length - 1) {
                break;
            }
            consumer.accept(upgrades[index], elementHeight * i);
        }
    }

    @Override
    public void syncFrom(GuiElement element) {
        super.syncFrom(element);
        GuiUpgradeScrollList old = (GuiUpgradeScrollList) element;
        selectedType = old.selectedType;
    }
}