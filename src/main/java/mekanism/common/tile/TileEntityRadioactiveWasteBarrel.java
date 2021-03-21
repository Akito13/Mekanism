package mekanism.common.tile;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import mekanism.api.Action;
import mekanism.api.IConfigurable;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.chemical.StackedWasteBarrel;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.resolver.BasicCapabilityResolver;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tags.MekanismTags;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class TileEntityRadioactiveWasteBarrel extends TileEntityMekanism implements IConfigurable {

    private static final float TOLERANCE = 0.05F;

    private long lastProcessTick;
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getStored", "getCapacity", "getNeeded", "getFilledPercentage"})
    private StackedWasteBarrel gasTank;
    private float prevScale;
    private int processTicks;

    public TileEntityRadioactiveWasteBarrel() {
        super(MekanismBlocks.RADIOACTIVE_WASTE_BARREL);
        addCapabilityResolver(BasicCapabilityResolver.constant(Capabilities.CONFIGURABLE_CAPABILITY, this));
    }

    @Nonnull
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks() {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder = ChemicalTankHelper.forSide(this::getDirection);
        builder.addTank(gasTank = StackedWasteBarrel.create(this), RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        if (level.getGameTime() > lastProcessTick) {
            //If we are not on the same tick do stuff, otherwise ignore it (anti tick accelerator protection)
            lastProcessTick = level.getGameTime();
            if (MekanismConfig.general.radioactiveWasteBarrelDecayAmount.get() > 0 && !gasTank.isEmpty() &&
                !gasTank.getType().isIn(MekanismTags.Gases.WASTE_BARREL_DECAY_BLACKLIST) &&
                ++processTicks >= MekanismConfig.general.radioactiveWasteBarrelProcessTicks.get()) {
                processTicks = 0;
                gasTank.shrinkStack(MekanismConfig.general.radioactiveWasteBarrelDecayAmount.get(), Action.EXECUTE);
            }
            if (getActive()) {
                ChemicalUtil.emit(EnumSet.of(Direction.DOWN), gasTank, this);
            }

            float scale = getGasScale();
            if (Math.abs(scale - prevScale) > TOLERANCE) {
                sendUpdatePacket();
                prevScale = scale;
            }
        }
    }

    public StackedWasteBarrel getGasTank() {
        return gasTank;
    }

    public float getGasScale() {
        return (float) (gasTank.getStored() / (double) gasTank.getCapacity());
    }

    public GasStack getGas() {
        return gasTank.getStack();
    }

    @Override
    public ActionResultType onSneakRightClick(PlayerEntity player, Direction side) {
        if (!isRemote()) {
            setActive(!getActive());
            World world = getLevel();
            if (world != null) {
                world.playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.3F, 1);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResultType onRightClick(PlayerEntity player, Direction side) {
        return ActionResultType.PASS;
    }

    @Nonnull
    @Override
    public CompoundNBT getReducedUpdateTag() {
        CompoundNBT updateTag = super.getReducedUpdateTag();
        updateTag.put(NBTConstants.GAS_STORED, gasTank.serializeNBT());
        updateTag.putInt(NBTConstants.PROGRESS, processTicks);
        return updateTag;
    }

    @Override
    public void handleUpdateTag(BlockState state, @Nonnull CompoundNBT tag) {
        super.handleUpdateTag(state, tag);
        NBTUtils.setCompoundIfPresent(tag, NBTConstants.GAS_STORED, nbt -> gasTank.deserializeNBT(nbt));
        NBTUtils.setIntIfPresent(tag, NBTConstants.PROGRESS, val -> processTicks = val);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(gasTank.getStored(), gasTank.getCapacity());
    }
}