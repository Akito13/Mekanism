package mekanism.client.gui.element.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.slot.GuiSequencedSlotDisplay;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.MekanismLang;
import mekanism.common.content.filter.FilterManager;
import mekanism.common.content.filter.IFilter;
import mekanism.common.content.filter.IItemStackFilter;
import mekanism.common.content.filter.IMaterialFilter;
import mekanism.common.content.filter.IModIDFilter;
import mekanism.common.content.filter.ITagFilter;
import mekanism.common.content.oredictionificator.OredictionificatorFilter;
import mekanism.common.content.transporter.SorterFilter;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterButton extends MekanismButton {

    private static final ResourceLocation TEXTURE = MekanismUtils.getResource(ResourceType.GUI_BUTTON, "filter_holder.png");
    protected static final int TEXTURE_WIDTH = 96;
    protected static final int TEXTURE_HEIGHT = 58;

    protected final FilterManager<?> filterManager;
    private final GuiSequencedSlotDisplay slotDisplay;
    private final IntSupplier filterIndex;
    private final RadioButton toggleButton;
    private final GuiSlot slot;
    private final int index;
    private IFilter<?> prevFilter;

    @Nullable
    private static IFilter<?> getFilter(FilterManager<?> filterManager, int index) {
        if (index >= 0 && index < filterManager.count()) {
            return filterManager.getFilters().get(index);
        }
        return null;
    }

    public FilterButton(IGuiWrapper gui, int x, int y, int width, int height, int index, IntSupplier filterIndex, FilterManager<?> filterManager,
          ObjIntConsumer<IFilter<?>> onPress, IntConsumer toggleButtonPress, Function<IFilter<?>, List<ItemStack>> renderStackSupplier) {
        super(gui, x, y, width, height, Component.empty(), () -> {
            int actualIndex = filterIndex.getAsInt() + index;
            onPress.accept(getFilter(filterManager, actualIndex), actualIndex);
        }, null);
        this.index = index;
        this.filterIndex = filterIndex;
        this.filterManager = filterManager;
        slot = addChild(new GuiSlot(SlotType.NORMAL, gui, relativeX + 2, relativeY + 2));
        slotDisplay = addChild(new GuiSequencedSlotDisplay(gui, relativeX + 3, relativeY + 3, () -> renderStackSupplier.apply(getFilter())));
        BooleanSupplier enabledCheck = () -> {
            IFilter<?> filter = getFilter();
            return filter != null && filter.isEnabled();
        };
        toggleButton = addChild(new RadioButton(gui, relativeX + this.width - RadioButton.RADIO_SIZE - getToggleXShift(), relativeY + this.height - RadioButton.RADIO_SIZE - getToggleYShift(),
              enabledCheck, () -> toggleButtonPress.accept(getActualIndex()), (element, matrix, mouseX, mouseY) -> {
            if (enabledCheck.getAsBoolean()) {
                displayTooltips(matrix, mouseX, mouseY, MekanismLang.FILTER_STATE.translate(EnumColor.BRIGHT_GREEN, MekanismLang.MODULE_ENABLED_LOWER));
            } else {
                displayTooltips(matrix, mouseX, mouseY, MekanismLang.FILTER_STATE.translate(EnumColor.RED, MekanismLang.MODULE_DISABLED_LOWER));
            }
        }));
        setButtonBackground(ButtonBackground.NONE);
    }

    protected int getToggleXShift() {
        return 2;
    }

    protected int getToggleYShift() {
        return 2;
    }

    protected int getActualIndex() {
        return filterIndex.getAsInt() + index;
    }

    @Nullable
    protected IFilter<?> getFilter() {
        return getFilter(filterManager, getActualIndex());
    }

    public FilterButton warning(@NotNull WarningType type, @NotNull Predicate<IFilter<?>> hasWarning) {
        //Proxy applying the warning to the slot
        slot.warning(type, () -> hasWarning.test(getFilter()));
        return this;
    }

    protected void setVisibility(boolean visible) {
        //TODO: Should we check visibility before passing things like tooltip to children? That way we don't have to manually hide the children as well
        this.visible = visible;
        this.slot.visible = visible;
        this.slotDisplay.visible = visible;
        this.toggleButton.visible = visible;
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        setVisibility(getFilter() != null);
        super.render(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawBackground(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(matrix, mouseX, mouseY, partialTicks);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(matrix, x, y, width, height, 0, isMouseOverCheckWindows(mouseX, mouseY) ? 0 : 29, TEXTURE_WIDTH, 29, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void renderForeground(PoseStack matrix, int mouseX, int mouseY) {
        super.renderForeground(matrix, mouseX, mouseY);
        IFilter<?> filter = getFilter();
        if (filter != prevFilter) {
            slotDisplay.updateStackList();
            prevFilter = filter;
        }
        int x = this.x - getGuiLeft();
        int y = this.y - getGuiTop();
        if (filter instanceof IItemStackFilter) {
            drawFilterType(matrix, x, y, MekanismLang.ITEM_FILTER);
        } else if (filter instanceof ITagFilter) {
            drawFilterType(matrix, x, y, MekanismLang.TAG_FILTER);
        } else if (filter instanceof IMaterialFilter) {
            drawFilterType(matrix, x, y, MekanismLang.MATERIAL_FILTER);
        } else if (filter instanceof IModIDFilter) {
            drawFilterType(matrix, x, y, MekanismLang.MODID_FILTER);
        } else if (filter instanceof OredictionificatorFilter<?, ?, ?> oredictionificatorFilter) {
            drawFilterType(matrix, x, y, MekanismLang.FILTER);
            drawTextScaledBound(matrix, oredictionificatorFilter.getFilterText(), x + 22, y + 11, titleTextColor(), getMaxLength());
        }
        if (filter instanceof SorterFilter<?> sorterFilter) {
            drawTextScaledBound(matrix, sorterFilter.color == null ? MekanismLang.NONE.translate() : sorterFilter.color.getColoredName(), x + 22, y + 11,
                  titleTextColor(), getMaxLength());
        }
    }

    protected int getMaxLength() {
        return width - 22 - 2;
    }

    private void drawFilterType(PoseStack matrix, int x, int y, ILangEntry langEntry) {
        drawTextScaledBound(matrix, langEntry.translate(), x + 22, y + 2, titleTextColor(), getMaxLength());
    }
}