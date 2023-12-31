package mekanism.common.network.to_client.qio;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import mekanism.common.lib.inventory.HashedItem.UUIDAwareHashedItem;
import mekanism.common.network.PacketUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.NotNull;

public class PacketBatchItemViewerSync extends PacketQIOItemViewerGuiSync {

    public static final ResourceLocation ID = Mekanism.rl("batch_qio_sync");

    public PacketBatchItemViewerSync(FriendlyByteBuf buffer) {
        super(buffer);
    }

    public PacketBatchItemViewerSync(long countCapacity, int typeCapacity, Object2LongMap<UUIDAwareHashedItem> itemMap) {
        super(countCapacity, typeCapacity, itemMap);
    }

    @NotNull
    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void handle(PlayPayloadContext context) {
        PacketUtils.container(context, QIOItemViewerContainer.class)
              .ifPresent(container -> container.handleBatchUpdate(itemMap, countCapacity, typeCapacity));
    }
}