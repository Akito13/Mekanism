package mekanism.common.network.to_server;

import java.util.function.Predicate;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.ModuleData;
import mekanism.api.gear.config.ModuleBooleanData;
import mekanism.api.gear.config.ModuleColorData;
import mekanism.api.gear.config.ModuleConfigData;
import mekanism.api.gear.config.ModuleEnumData;
import mekanism.api.gear.config.ModuleIntegerData;
import mekanism.api.math.MathUtils;
import mekanism.common.Mekanism;
import mekanism.common.content.gear.IModuleContainerItem;
import mekanism.common.content.gear.Module;
import mekanism.common.content.gear.ModuleConfigItem;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.NotNull;

//TODO: Eventually it would be nice to make this more generic in terms of how it can sync module data so that we can support custom types
// though given the module tweaker screen doesn't currently have a way to support custom types it isn't that big a deal to make this support it yet either
public class PacketUpdateModuleSettings implements IMekanismPacket<PlayPayloadContext> {

    public static final ResourceLocation ID = Mekanism.rl("update_module");

    public static PacketUpdateModuleSettings create(int slotId, ModuleData<?> moduleType, int dataIndex, ModuleConfigData<?> configData) {
        if (configData instanceof ModuleEnumData<?> enumData) {
            return new PacketUpdateModuleSettings(slotId, moduleType, dataIndex, ModuleDataType.ENUM, enumData.get().ordinal());
        }
        for (ModuleDataType type : ModuleDataType.VALUES) {
            if (type.typeMatches(configData)) {
                return new PacketUpdateModuleSettings(slotId, moduleType, dataIndex, type, configData.get());
            }
        }
        throw new IllegalArgumentException("Unknown config data type.");
    }

    private final ModuleData<?> moduleType;
    private final int slotId;
    private final int dataIndex;
    private final ModuleDataType dataType;
    private final Object value;

    private PacketUpdateModuleSettings(int slotId, ModuleData<?> moduleType, int dataIndex, ModuleDataType dataType, Object value) {
        this.slotId = slotId;
        this.moduleType = moduleType;
        this.dataIndex = dataIndex;
        this.dataType = dataType;
        this.value = value;
    }

    @NotNull
    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void handle(PlayPayloadContext context) {
        if (dataIndex >= 0 && value != null) {
            context.player()
                  .map(player -> player.getInventory().getItem(slotId))
                  .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof IModuleContainerItem)
                  .map(stack -> ModuleHelper.get().load(stack, moduleType))
                  .map(Module::getConfigItems)
                  .filter(configItems -> dataIndex < configItems.size())
                  .map(configItems -> configItems.get(dataIndex))
                  .ifPresent(this::setValue);
        }
    }

    private <TYPE> void setValue(ModuleConfigItem<TYPE> moduleConfigItem) {
        ModuleConfigData<TYPE> configData = moduleConfigItem.getData();
        if (configData instanceof ModuleEnumData && dataType == ModuleDataType.ENUM) {
            moduleConfigItem.set((TYPE) MathUtils.getByIndexMod(((ModuleEnumData<?>) configData).getEnums(), (int) value));
        } else if (dataType.typeMatches(configData)) {
            //noinspection unchecked
            moduleConfigItem.set((TYPE) value);
        }
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeEnum(dataType);
        buffer.writeVarInt(slotId);
        buffer.writeId(MekanismAPI.MODULE_REGISTRY, moduleType);
        buffer.writeVarInt(dataIndex);
        dataType.writer.accept(buffer, value);
    }

    public static PacketUpdateModuleSettings decode(FriendlyByteBuf buffer) {
        ModuleDataType dataType = buffer.readEnum(ModuleDataType.class);
        return new PacketUpdateModuleSettings(buffer.readVarInt(), buffer.readById(MekanismAPI.MODULE_REGISTRY), buffer.readVarInt(), dataType, dataType.reader.apply(buffer));
    }

    private enum ModuleDataType {
        BOOLEAN(data -> data instanceof ModuleBooleanData, (buf, obj) -> buf.writeBoolean((boolean) obj), FriendlyByteBuf::readBoolean),
        //Must be above integer, so it uses the color type as ModuleColorData extends ModuleIntegerData
        COLOR(data -> data instanceof ModuleColorData, (buf, obj) -> buf.writeInt((int) obj), FriendlyByteBuf::readInt),
        INTEGER(data -> data instanceof ModuleIntegerData, (buf, obj) -> buf.writeVarInt((int) obj), FriendlyByteBuf::readVarInt),
        ENUM(data -> data instanceof ModuleEnumData, (buf, obj) -> buf.writeVarInt((int) obj), FriendlyByteBuf::readVarInt);

        //DO NOT MODIFY
        private static final ModuleDataType[] VALUES = values();

        private final Predicate<ModuleConfigData<?>> configDataPredicate;
        private final FriendlyByteBuf.Writer<Object> writer;
        private final FriendlyByteBuf.Reader<Object> reader;

        ModuleDataType(Predicate<ModuleConfigData<?>> configDataPredicate, FriendlyByteBuf.Writer<Object> writer, FriendlyByteBuf.Reader<Object> reader) {
            this.configDataPredicate = configDataPredicate;
            this.writer = writer;
            this.reader = reader;
        }

        public boolean typeMatches(ModuleConfigData<?> data) {
            return configDataPredicate.test(data);
        }
    }
}