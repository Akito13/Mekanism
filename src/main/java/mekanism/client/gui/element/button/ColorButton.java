package mekanism.client.gui.element.button;

import java.util.function.Supplier;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ColorButton extends MekanismButton {

    private final Supplier<EnumColor> colorSupplier;

    public ColorButton(IGuiWrapper gui, int x, int y, int width, int height, Supplier<EnumColor> colorSupplier, @NotNull Runnable onPress, @NotNull Runnable onRightClick) {
        super(gui, x, y, width, height, Component.empty(), onPress, onRightClick, (onHover, guiGraphics, mouseX, mouseY) -> {
            EnumColor color = colorSupplier.get();
            if (color != null) {
                gui.displayTooltips(guiGraphics, mouseX, mouseY, color.getColoredName());
            } else {
                gui.displayTooltips(guiGraphics, mouseX, mouseY, MekanismLang.NONE.translate());
            }
        });
        this.colorSupplier = colorSupplier;
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //Ensure the color gets reset. The default GuiButtonImage doesn't so other GuiButton's can have the color leak out of them
        EnumColor color = colorSupplier.get();
        if (color != null) {
            guiGraphics.fill(getButtonX(), getButtonY(), getButtonX() + getButtonWidth(), getButtonY() + getButtonHeight(), MekanismRenderer.getColorARGB(color, 1));
            MekanismRenderer.resetColor(guiGraphics);
        }
    }
}