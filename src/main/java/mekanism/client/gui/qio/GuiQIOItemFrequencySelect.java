package mekanism.client.gui.qio;

import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.custom.GuiFrequencySelector;
import mekanism.client.gui.element.custom.GuiFrequencySelector.IGuiColorFrequencySelector;
import mekanism.client.gui.element.custom.GuiFrequencySelector.IItemGuiFrequencySelector;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.inventory.container.item.QIOFrequencySelectItemContainer;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.network.to_server.PacketGuiButtonPress;
import mekanism.common.network.to_server.PacketGuiButtonPress.ClickedItemButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiQIOItemFrequencySelect extends GuiMekanism<QIOFrequencySelectItemContainer> implements IGuiColorFrequencySelector<QIOFrequency>,
      IItemGuiFrequencySelector<QIOFrequency, QIOFrequencySelectItemContainer> {

    public GuiQIOItemFrequencySelect(QIOFrequencySelectItemContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        imageHeight -= 11;
        titleLabelY = 5;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiFrequencySelector<>(this, 17));
        addRenderableWidget(new MekanismImageButton(this, 6, 6, 14, getButtonLocation("back"),
              () -> Mekanism.packetHandler().sendToServer(new PacketGuiButtonPress(ClickedItemButton.BACK_BUTTON, menu.getHand())), getOnHover(MekanismLang.BACK)));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    public FrequencyType<QIOFrequency> getFrequencyType() {
        return FrequencyType.QIO;
    }

    @Override
    public QIOFrequencySelectItemContainer getFrequencyContainer() {
        return menu;
    }

    @Override
    public void drawTitleText(GuiGraphics guiGraphics, Component text, float y) {
        //Adjust spacing for back button
        int leftShift = 15;
        int xSize = getXSize() - leftShift;
        int maxLength = xSize - 12;
        float textWidth = getStringWidth(text);
        float scale = Math.min(1, maxLength / textWidth);
        drawScaledCenteredText(guiGraphics, text, leftShift + xSize / 2F, y, titleTextColor(), scale);
    }
}