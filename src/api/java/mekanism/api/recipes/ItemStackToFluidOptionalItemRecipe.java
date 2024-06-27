package mekanism.api.recipes;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for defining ItemStack to fluid recipes with an optional item output.
 * <br>
 * Input: ItemStack
 * <br>
 * Output: FluidStack, Optional ItemStack
 *
 * @apiNote There is currently only one type of ItemStack to FluidStack recipe type:
 * <ul>
 *     <li>Nutritional Liquification: These cannot currently be created, but are processed in the Nutritional Liquifier.</li>
 * </ul>
 *
 * @since 10.6.3
 */
@NothingNullByDefault
public abstract class ItemStackToFluidOptionalItemRecipe extends MekanismRecipe<SingleRecipeInput> implements Predicate<@NotNull ItemStack> {

    @Override
    public abstract boolean test(ItemStack itemStack);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        //Don't match incomplete recipes or ones that don't match
        return !isIncomplete() && test(input.item());
    }

    /**
     * Gets the input ingredient.
     */
    public abstract ItemStackIngredient getInput();

    /**
     * Gets a new output based on the given input.
     *
     * @param input Specific input.
     *
     * @return New output.
     *
     * @apiNote While Mekanism does not currently make use of the input, it is important to support it and pass the proper value in case any addons define input based
     * outputs where things like NBT may be different
     * @implNote The passed in input should <strong>NOT</strong> be modified.
     */
    @Contract(value = "_ -> new", pure = true)
    public abstract FluidOptionalItemOutput getOutput(ItemStack input);

    /**
     * For JEI, gets the output representations to display.
     *
     * @return Representation of the output, <strong>MUST NOT</strong> be modified.
     */
    public abstract List<FluidOptionalItemOutput> getOutputDefinition();

    @Override
    public boolean isIncomplete() {
        return getInput().hasNoMatchingInstances();
    }

    /**
     * @apiNote Fluid must be present, but the item may be empty.
     */
    public record FluidOptionalItemOutput(FluidStack fluid, ItemStack optionalItem) {

        public FluidOptionalItemOutput {
            Objects.requireNonNull(fluid, "Fluid output cannot be null.");
            Objects.requireNonNull(optionalItem, "Item output cannot be null.");
            if (fluid.isEmpty()) {
                throw new IllegalArgumentException("Fluid output cannot be empty.");
            }
        }

        /**
         * Copies the backing objects of this output object.
         */
        public FluidOptionalItemOutput copy() {
            return new FluidOptionalItemOutput(fluid.copy(), optionalItem.copy());
        }
    }
}
