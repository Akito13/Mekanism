package mekanism.common.integration.energy.fluxnetworks;

import mekanism.api.Action;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import org.jetbrains.annotations.NotNull;
import sonar.fluxnetworks.api.energy.IFNEnergyStorage;

//Note: When wrapping joules to a whole number based energy type we don't need to add any extra simulation steps
// for insert or extract when executing as we will always round down the number and just act upon a lower max requested amount
@NothingNullByDefault
public class FNStrictEnergyHandler implements IStrictEnergyHandler {

    private final IFNEnergyStorage storage;

    public FNStrictEnergyHandler(IFNEnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getEnergyContainerCount() {
        return 1;
    }

    @Override
    public long getEnergy(int container) {
        return container == 0 ? EnergyUnit.FORGE_ENERGY.convertFrom(storage.getEnergyStoredL()) : 0L;
    }

    @Override
    public void setEnergy(int container, long energy) {
        //Not implemented or directly needed
    }

    @Override
    public long getMaxEnergy(int container) {
        return container == 0 ? EnergyUnit.FORGE_ENERGY.convertFrom(storage.getMaxEnergyStoredL()) : 0L;
    }

    @Override
    public long getNeededEnergy(int container) {
        return container == 0 ? EnergyUnit.FORGE_ENERGY.convertFrom(Math.max(0, storage.getMaxEnergyStoredL() - storage.getEnergyStoredL())) : 0L;
    }

    @Override
    public long insertEnergy(int container, long amount, @NotNull Action action) {
        return container == 0 ? insertEnergy(amount, action) : amount;
    }

    @Override
    public long insertEnergy(long amount, Action action) {
        if (storage.canReceive() && amount > 0) {
            long toInsert = EnergyUnit.FORGE_ENERGY.convertTo(amount);
            if (toInsert == 0) {
                return amount;
            }
            if (action.execute() && !EnergyUnit.FORGE_ENERGY.isOneToOne()) {
                //Before we can actually execute it we need to simulate to calculate how much we can actually insert
                long simulatedInserted = storage.receiveEnergyL(toInsert, true);
                if (simulatedInserted == 0) {
                    //Nothing can be inserted at all, just exit quickly
                    return amount;
                }
                //Convert how much we could insert back to Joules so that it gets appropriately clamped so that for example 2 FE gets treated
                // as trying to insert 0 J for how much we actually will accept, and then convert that clamped value to go back to FE
                // so that we don't allow inserting a tiny bit of extra for "free" and end up creating power from nowhere
                toInsert = convertFromAndBack(simulatedInserted);
                if (toInsert == 0L) {
                    //If converting back and forth between Joules and FE causes us to be clamped at zero, that means we can't accept anything or could only
                    // accept a partial amount; we need to exit early returning that we couldn't insert anything
                    return amount;
                }
            }
            long inserted = storage.receiveEnergyL(toInsert, action.simulate());
            if (inserted > 0) {
                //Only bother converting back if any was inserted
                return amount - EnergyUnit.FORGE_ENERGY.convertFrom(inserted);
            }
        }
        return amount;
    }

    private long convertFromAndBack(long value) {
        return EnergyUnit.FORGE_ENERGY.convertTo(EnergyUnit.FORGE_ENERGY.convertFrom(value));
    }

    @Override
    public long extractEnergy(int container, long amount, @NotNull Action action) {
        return container == 0 ? extractEnergy(amount, action) : 0L;
    }

    @Override
    public long extractEnergy(long amount, Action action) {
        if (storage.canExtract() && amount > 0) {
            long toExtract = EnergyUnit.FORGE_ENERGY.convertTo(amount);
            if (toExtract == 0) {
                return 0L;
            }
            if (action.execute() && !EnergyUnit.FORGE_ENERGY.isOneToOne()) {
                //Before we can actually execute it we need to simulate to calculate how much we can actually extract in our other units
                long simulatedExtracted = storage.extractEnergyL(toExtract, true);
                //Convert how much we could extract back to Joules so that it gets appropriately clamped so that for example 1 Joule gets treated
                // as trying to extract 0 FE for how much we can actually provide, and then convert that clamped value to go back to Joules
                // so that we don't allow extracting a tiny bit into nowhere causing some power to be voided
                // This is important as otherwise if we can have 1.5 Joules extracted, we will reduce our amount by 1.5 Joules but the caller will only receive 1 Joule
                toExtract = convertFromAndBack(simulatedExtracted);
                if (toExtract == 0L) {
                    //If converting back and forth between Joules and FE causes us to be clamped at zero, that means we can't provide anything or could only
                    // provide a partial amount; we need to exit early returning that nothing could be extracted
                    return 0;
                }
            }
            long extracted = storage.extractEnergyL(toExtract, action.simulate());
            return EnergyUnit.FORGE_ENERGY.convertFrom(extracted);
        }
        return 0L;
    }
}