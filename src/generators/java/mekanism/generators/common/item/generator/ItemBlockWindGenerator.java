package mekanism.generators.common.item.generator;

import java.util.function.Consumer;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.generators.client.render.GeneratorsRenderPropertiesProvider;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

public class ItemBlockWindGenerator extends ItemBlockTooltip<BlockTile<?, ?>> {

    public ItemBlockWindGenerator(BlockTile<?, ?> block) {
        super(block);
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(GeneratorsRenderPropertiesProvider.wind());
    }
}