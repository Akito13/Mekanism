package mekanism.common.integration.lookingat.wthit;

import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mekanism.common.integration.lookingat.LookingAtUtils;
import net.minecraft.world.entity.Entity;

public class WTHITEntityDataProvider implements IDataProvider<Entity> {

    static final WTHITEntityDataProvider INSTANCE = new WTHITEntityDataProvider();

    @Override
    public void appendData(IDataWriter dataWriter, IServerAccessor<Entity> serverAccessor, IPluginConfig config) {
        //TODO - 1.20: Test this stuff relating to new data system
        WTHITLookingAtHelper helper = new WTHITLookingAtHelper();
        LookingAtUtils.addInfo(helper, serverAccessor.getTarget());
        dataWriter.add(WTHITLookingAtHelper.class, IDataWriter.Result::block);
    }
}