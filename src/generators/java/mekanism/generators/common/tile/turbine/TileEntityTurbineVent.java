package mekanism.generators.common.tile.turbine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityTurbineVent extends TileEntityTurbineCasing {

    private final Map<Direction, BlockCapabilityCache<IFluidHandler, @Nullable Direction>> capabilityCaches = new EnumMap<>(Direction.class);

    public TileEntityTurbineVent(BlockPos pos, BlockState state) {
        super(GeneratorsBlocks.TURBINE_VENT, pos, state);
    }

    @NotNull
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        return side -> {
            TurbineMultiblockData multiblock = getMultiblock();
            return multiblock.isFormed() ? multiblock.ventTanks : Collections.emptyList();
        };
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        //Do not handle fluid when it comes to syncing it/saving this tile to disk
        if (type == ContainerType.FLUID) {
            return false;
        }
        return super.persists(type);
    }

    public void addFluidTargetCapability(List<BlockCapabilityCache<IFluidHandler, @Nullable Direction>> outputTargets, Direction side) {
        outputTargets.add(capabilityCaches.computeIfAbsent(side, s -> Capabilities.FLUID.createCache((ServerLevel) level, worldPosition.relative(s), s.getOpposite())));
    }
}