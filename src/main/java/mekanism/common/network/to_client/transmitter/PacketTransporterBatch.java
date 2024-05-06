package mekanism.common.network.to_client.transmitter;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import mekanism.common.Mekanism;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PacketTransporterBatch(BlockPos pos, IntSet deletes, Int2ObjectMap<TransporterStack> updates) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketTransporterBatch> TYPE = new CustomPacketPayload.Type<>(Mekanism.rl("transporter_batch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketTransporterBatch> STREAM_CODEC = StreamCodec.composite(
          BlockPos.STREAM_CODEC, PacketTransporterBatch::pos,
          ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.collection(IntOpenHashSet::new)), PacketTransporterBatch::deletes,
          ByteBufCodecs.map(Int2ObjectOpenHashMap::new, ByteBufCodecs.VAR_INT, TransporterStack.STREAM_CODEC), PacketTransporterBatch::updates,
          PacketTransporterBatch::new
    );

    public static PacketTransporterBatch create(BlockPos pos, IntSet deletes, Int2ObjectMap<TransporterStack> updates) {
        for (TransporterStack stack : updates.values()) {
            stack.updateForPos(pos);
        }
        return new PacketTransporterBatch(pos, deletes, updates);
    }

    @NotNull
    @Override
    public CustomPacketPayload.Type<PacketTransporterBatch> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, pos) instanceof TileEntityLogisticalTransporterBase tile) {
            LogisticalTransporterBase transporter = tile.getTransmitter();
            for (Int2ObjectMap.Entry<TransporterStack> entry : updates.int2ObjectEntrySet()) {
                transporter.addStack(entry.getIntKey(), entry.getValue());
            }
            for (int toDelete : deletes) {
                transporter.deleteStack(toDelete);
            }
        }
    }
}