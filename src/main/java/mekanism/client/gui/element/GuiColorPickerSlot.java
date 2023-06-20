package mekanism.client.gui.element;

import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiColorWindow;
import mekanism.common.MekanismLang;
import mekanism.common.lib.Color;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class GuiColorPickerSlot extends GuiElement {

    private final Supplier<Color> supplier;
    private final Consumer<Color> consumer;
    private final boolean handlesAlpha;

    public GuiColorPickerSlot(IGuiWrapper gui, int x, int y, boolean handlesAlpha, Supplier<Color> supplier, Consumer<Color> consumer) {
        super(gui, x, y, 18, 18);
        this.handlesAlpha = handlesAlpha;
        this.supplier = supplier;
        this.consumer = consumer;
        addChild(new GuiElementHolder(gui, relativeX, relativeY, 18, 18));
    }

    @Override
    public void renderToolTip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        Component hex = MekanismLang.GENERIC_HEX.translateColored(EnumColor.GRAY, TextUtils.hex(false, 3, supplier.get().rgb()));
        displayTooltips(guiGraphics, mouseX, mouseY, hex);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        GuiUtils.fill(guiGraphics, relativeX + 1, relativeY + 1, width - 2, height - 2, supplier.get().argb());
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        GuiColorWindow window = new GuiColorWindow(gui(), getGuiWidth() / 2 - 160 / 2, getGuiHeight() / 2 - 120 / 2, handlesAlpha, consumer);
        window.setColor(supplier.get());
        gui().addWindow(window);
    }
}
