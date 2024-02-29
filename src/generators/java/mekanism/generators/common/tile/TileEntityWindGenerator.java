package mekanism.generators.common.tile;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableFloatingLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TileEntityWindGenerator extends TileEntityGenerator implements IBoundingBlock {

    private static final float SPEED = 32F;

    private double angle;
    private FloatingLong currentMultiplier = FloatingLong.ZERO;
    private boolean isBlacklistDimension;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy item slot")
    EnergyInventorySlot energySlot;

    public TileEntityWindGenerator(BlockPos pos, BlockState state) {
        super(GeneratorsBlocks.WIND_GENERATOR, pos, state, MekanismGeneratorsConfig.generators.windGenerationMax);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(energySlot = EnergyInventorySlot.drain(getEnergyContainer(), listener, 143, 35));
        return builder.build();
    }

    @Override
    protected RelativeSide[] getEnergySides() {
        return new RelativeSide[]{RelativeSide.FRONT, RelativeSide.BOTTOM};
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        energySlot.drainContainer();
        // If we're in a blacklisted dimension, there's nothing more to do
        if (isBlacklistDimension) {
            return;
        }
        if (ticker % SharedConstants.TICKS_PER_SECOND == 0) {
            // Recalculate the current multiplier once a second
            currentMultiplier = getMultiplier();
            setActive(MekanismUtils.canFunction(this) && !currentMultiplier.isZero());
        }
        if (!currentMultiplier.isZero() && MekanismUtils.canFunction(this) && !getEnergyContainer().getNeeded().isZero()) {
            getEnergyContainer().insert(MekanismGeneratorsConfig.generators.windGenerationMin.get().multiply(currentMultiplier), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();
        if (getActive()) {
            angle = (angle + getHeightSpeedRatio()) % 360;
        }
    }

    public float getHeightSpeedRatio() {
        int height = getBlockPos().getY() + 4;
        if (level == null) {
            //Fallback to default values, but in general this is not going to happen
            return SPEED * height / 384F;
        }
        //Shift so that a wind generator at the min build height acts as if it was at a height of zero
        int minBuildHeight = level.getMinBuildHeight();
        height -= minBuildHeight;
        return SPEED * height / (level.getMaxBuildHeight() - minBuildHeight);
    }

    /**
     * Determines the current output multiplier, taking sky visibility and height into account.
     **/
    private FloatingLong getMultiplier() {
        if (level != null) {
            BlockPos top = getBlockPos().above(4);
            if (level.getFluidState(top).isEmpty() && level.canSeeSky(top)) {
                //Validate it isn't fluid logged to help try and prevent https://github.com/mekanism/Mekanism/issues/7344
                //Clamp the height limits as the logical bounds of the world
                int minY = Math.max(MekanismGeneratorsConfig.generators.windGenerationMinY.get(), level.getMinBuildHeight());
                int maxY = Math.min(MekanismGeneratorsConfig.generators.windGenerationMaxY.get(), level.dimensionType().logicalHeight());
                float clampedY = Math.min(maxY, Math.max(minY, top.getY()));
                FloatingLong minG = MekanismGeneratorsConfig.generators.windGenerationMin.get();
                FloatingLong maxG = MekanismGeneratorsConfig.generators.windGenerationMax.get();
                FloatingLong slope = maxG.subtract(minG).divide(maxY - minY);
                FloatingLong toGen = minG.add(slope.multiply(clampedY - minY));
                return toGen.divide(minG);
            }
        }
        return FloatingLong.ZERO;
    }

    @Override
    public void setLevel(@NotNull Level world) {
        super.setLevel(world);
        // Check the blacklist and force an update if we're in the blacklist. Otherwise, we'll never send
        // an initial activity status and the client (in MP) will show the windmills turning while not
        // generating any power
        isBlacklistDimension = MekanismGeneratorsConfig.generators.windGenerationDimBlacklist.get().contains(world.dimension().location());
        if (isBlacklistDimension) {
            setActive(false);
        }
    }

    public FloatingLong getCurrentMultiplier() {
        return currentMultiplier;
    }

    public double getAngle() {
        return angle;
    }

    @ComputerMethod(nameOverride = "isBlacklistedDimension")
    public boolean isBlacklistDimension() {
        return isBlacklistDimension;
    }

    @Override
    public SoundSource getSoundCategory() {
        return SoundSource.WEATHER;
    }

    @Override
    public BlockPos getSoundPos() {
        return super.getSoundPos().above(4);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableFloatingLong.create(this::getCurrentMultiplier, value -> currentMultiplier = value));
        container.track(SyncableBoolean.create(this::isBlacklistDimension, value -> isBlacklistDimension = value));
    }

    //Methods relating to IComputerTile
    @Override
    FloatingLong getProductionRate() {
        return getActive() ? MekanismGeneratorsConfig.generators.windGenerationMin.get().multiply(getCurrentMultiplier()) : FloatingLong.ZERO;
    }
    //End methods IComputerTile
}