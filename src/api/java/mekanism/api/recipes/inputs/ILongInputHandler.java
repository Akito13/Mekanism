package mekanism.api.recipes.inputs;

/**
 * Interface describing handling of an input that can handle long values.
 *
 * @param <INPUT> Type of input handled by this handler.
 */
public interface ILongInputHandler<INPUT> extends IInputHandler<INPUT> {

    @Override
    default void use(INPUT recipeInput, int operations) {
        //Wrap to the long implementation
        use(recipeInput, (long) operations);
    }

    /**
     * Adds {@code operations} operations worth of {@code recipeInput} from the input.
     *
     * @param recipeInput Recipe input result.
     * @param operations  Operations to perform.
     */
    void use(INPUT recipeInput, long operations);

    @Override
    default int operationsCanSupport(InputIngredient<INPUT> recipeIngredient, int currentMax, int usageMultiplier) {
        //Wrap to the long implementation
        return operationsCanSupport(recipeIngredient, currentMax, (long) usageMultiplier);
    }

    /**
     * Calculates how many operations the input can sustain.
     *
     * @param recipeIngredient Recipe ingredient.
     * @param currentMax       The current maximum number of operations that can happen.
     * @param usageMultiplier  Usage multiplier to multiply the recipeIngredient's amount by per operation.
     *
     * @return The number of operations the input can sustain.
     */
    int operationsCanSupport(InputIngredient<INPUT> recipeIngredient, int currentMax, long usageMultiplier);
}