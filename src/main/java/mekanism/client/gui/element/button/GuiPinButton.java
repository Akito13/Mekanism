package mekanism.client.gui.element.button;

import mekanism.api.text.ILangEntry;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.MekanismLang;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.resources.ResourceLocation;

public class GuiPinButton extends ToggleButton {

    private static final ResourceLocation PINNED = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "pinned.png");
    private static final ResourceLocation UNPINNED = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "unpinned.png");
    public static final int WIDTH = 16;

    public GuiPinButton(IGuiWrapper gui, int x, int y, GuiWindow window) {
        super(gui, x, y, WIDTH, 8, 12, 24, PINNED, UNPINNED, window::isPinned, window::togglePinned, (onHover, guiGraphics, mouseX, mouseY) -> {
            ILangEntry langEntry = window.isPinned() ? MekanismLang.UNPIN : MekanismLang.PIN;
            gui.displayTooltips(guiGraphics, mouseX, mouseY, langEntry.translate());
        });
    }

    @Override
    public boolean resetColorBeforeRender() {
        return false;
    }
}
