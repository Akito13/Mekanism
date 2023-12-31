package mekanism.common.network.to_server.frequency;

import mekanism.common.Mekanism;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.frequency.IColorableFrequency;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.NotNull;

public class PacketSetFrequencyColor<FREQ extends Frequency & IColorableFrequency> extends PacketSetFrequency<FREQ> {

    public static final ResourceLocation ID = Mekanism.rl("set_frequency_color");

    public PacketSetFrequencyColor(FriendlyByteBuf buf) {
        super(buf);
    }

    public PacketSetFrequencyColor(FREQ freq, boolean next) {
        super(next, (FrequencyType<FREQ>) freq.getType(), freq.getIdentity());
    }

    @NotNull
    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void handle(PlayPayloadContext context) {
        context.player()
              .map(Entity::getUUID)
              .ifPresent(player -> {
                  FREQ freq = type.getFrequency(data, player);
                  if (freq != null && freq.ownerMatches(player)) {
                      freq.setColor(set ? freq.getColor().getNext() : freq.getColor().getPrevious());
                  }
              });
    }
}