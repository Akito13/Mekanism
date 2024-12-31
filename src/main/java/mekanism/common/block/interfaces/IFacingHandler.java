package mekanism.common.block.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IFacingHandler {
    BlockState setFacing(BlockState state, Direction facing);
}
