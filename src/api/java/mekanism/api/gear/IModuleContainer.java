package mekanism.api.gear;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.MekanismIMC.ModuleContainerTarget;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.gear.config.ModuleConfig;
import mekanism.api.providers.IModuleDataProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item that can contain modules. Do not implement this interface directly, register new containers via
 * {@link mekanism.api.MekanismIMC#addModuleContainer(ModuleContainerTarget)}. Module containers are immutable.
 *
 * @since 10.5.0
 */
@NothingNullByDefault
public interface IModuleContainer {

    /**
     * {@return all the modules currently installed on this container mapped by their type}
     */
    Map<ModuleData<?>, ? extends IModule<?>> typedModules();

    /**
     * {@return all the modules currently installed on this container}
     */
    Collection<? extends IModule<?>> modules();

    /**
     * {@return set of module types for the modules currently installed on this container}
     */
    default Set<ModuleData<?>> moduleTypes() {
        return typedModules().keySet();
    }

    /**
     * Helper to replace the given config for the installed module of the given type.
     *
     * @param provider Holder lookup provider so that we can lookup enchantments if applicable.
     * @param stack    The stack the container is stored on.
     * @param type     Module type to replace the config for.
     * @param config   Config to replace.
     *
     * @return New immutable module container with the config using the replaced value.
     *
     * @throws IllegalStateException If no module of the given type is installed, or there is no config with the same name is not found installed on the module of the
     *                               given type.
     */
    <MODULE extends ICustomModule<MODULE>> IModuleContainer replaceModuleConfig(HolderLookup.Provider provider, ItemStack stack, IModuleDataProvider<MODULE> type,
          ModuleConfig<?> config);

    /**
     * {@return all the enchantments provided by installed modules}
     */
    ItemEnchantments moduleBasedEnchantments();

    /**
     * {@return the level provided by modules for the given enchantment, or zero if the enchantment isn't provided by any modules}
     */
    default int getModuleEnchantmentLevel(Holder<Enchantment> enchantment) {
        return moduleBasedEnchantments().getLevel(enchantment);
    }

    /**
     * {@return the number of installed module types}
     */
    default int installedCount() {
        return typedModules().size();
    }

    /**
     * {@return the number of modules of a given type that are installed}
     *
     * @param typeProvider Module type.
     */
    default int installedCount(IModuleDataProvider<?> typeProvider) {
        IModule<?> module = get(typeProvider);
        return module == null ? 0 : module.getInstalledCount();
    }

    /**
     * {@return the module if it is installed in this container}
     *
     * @param typeProvider Module type.
     */
    @Nullable
    <MODULE extends ICustomModule<MODULE>> IModule<MODULE> get(IModuleDataProvider<MODULE> typeProvider);

    /**
     * {@return the module if it is installed in this container and is currently enabled}
     *
     * @param typeProvider Module type.
     */
    @Nullable
    default <MODULE extends ICustomModule<MODULE>> IModule<MODULE> getIfEnabled(IModuleDataProvider<MODULE> typeProvider) {
        IModule<MODULE> module = get(typeProvider);
        return module != null && module.isEnabled() ? module : null;
    }

    /**
     * {@return whether the given module is installed in this container}
     *
     * @param typeProvider Module type.
     */
    default boolean has(IModuleDataProvider<?> typeProvider) {
        return typedModules().containsKey(typeProvider.getModuleData());
    }

    /**
     * {@return whether the given module is installed in this container and is enabled}
     *
     * @param typeProvider Module type.
     */
    default boolean hasEnabled(IModuleDataProvider<?> typeProvider) {
        return getIfEnabled(typeProvider) != null;
    }

    /**
     * Gets all the HUD elements that should be displayed when the MekaSuit is rendering the HUD.
     *
     * @param player Player using or wearing the container. In general this will be the client player, but is passed to make sidedness safer and easier.
     * @param stack  The stack the container is stored on.
     */
    List<IHUDElement> getHUDElements(Player player, ItemStack stack);

    /**
     * Gets all the text that should be displayed on the HUD.
     *
     * @param player Player using or wearing the container. In general this will be the client player, but is passed to make sidedness safer and easier.
     * @param stack  The stack the container is stored on.
     *
     * @apiNote These strings will be rendered without requiring the MekaSuit to be worn unlike {@link #getHUDElements(Player, ItemStack)}.
     */
    List<Component> getHUDStrings(Player player, ItemStack stack);
}