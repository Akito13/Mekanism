package mekanism.common.content.gear.shared;

import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismModules;
import net.minecraft.world.item.ItemStack;

@ParametersAreNotNullByDefault
public class ModuleEnergyUnit implements ICustomModule<ModuleEnergyUnit> {

    public static FloatingLong getEnergyCapacity(ItemStack stack, FloatingLongSupplier base) {
        return getEnergyValue(stack, base);
    }

    public static FloatingLong getChargeRate(ItemStack stack, FloatingLongSupplier base) {
        return getEnergyValue(stack, base);
    }

    private static FloatingLong getEnergyValue(ItemStack stack, FloatingLongSupplier base) {
        IModule<ModuleEnergyUnit> module = IModuleHelper.INSTANCE.getModule(stack, MekanismModules.ENERGY_UNIT);
        if (module == null) {
            return base.get();
        }
        return base.get().multiply(Math.pow(2, module.getInstalledCount()));
    }

    @Override
    public void onRemoved(IModule<ModuleEnergyUnit> module, IModuleContainer moduleContainer, ItemStack stack, boolean wasLast) {
        //Note: While technically we could use IModule#getEnergyContainer as it is just a helper,
        // we choose not to so that the behavior is clearer when the module was the last module
        // and technically is no longer installed in the module container
        IStrictEnergyHandler energyHandlerItem = Capabilities.STRICT_ENERGY.getCapability(stack);
        if (energyHandlerItem instanceof IMekanismStrictEnergyHandler energyHandler) {
            for (IEnergyContainer energyContainer : energyHandler.getEnergyContainers(null)) {
                energyContainer.setEnergy(energyContainer.getEnergy().min(energyContainer.getMaxEnergy()));
            }
        }
    }
}