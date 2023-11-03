package mekanism.generators.common.config;

import mekanism.api.math.FloatingLong;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedFloatValue;
import mekanism.common.config.value.CachedFloatingLongValue;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public class GeneratorsGearConfig extends BaseMekanismConfig {

    private static final String MEKASUIT_CATEGORY = "mekasuit";
    private static final String MEKASUIT_DAMAGE_CATEGORY = "damage_absorption";

    private final ModConfigSpec configSpec;

    //MekaSuit
    public final CachedFloatingLongValue mekaSuitGeothermalChargingRate;
    public final CachedFloatValue mekaSuitHeatDamageReductionRatio;

    GeneratorsGearConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Mekanism Generators Gear Config. This config is synced from server to client.").push("generators-gear");

        builder.comment("MekaSuit Settings").push(MEKASUIT_CATEGORY);
        mekaSuitGeothermalChargingRate = CachedFloatingLongValue.define(this, builder, "Geothermal charging rate (Joules) of pants per tick, per degree above ambient, per upgrade installed. This value scales down based on how much of the MekaSuit Pants is submerged. Fire is treated as having a temperature of ~200K above ambient, lava has a temperature of 1,000K above ambient.",
              "geothermalChargingRate", FloatingLong.createConst(10.5));
        builder.push(MEKASUIT_DAMAGE_CATEGORY);
        mekaSuitHeatDamageReductionRatio = CachedFloatValue.wrap(this, builder.comment("Percent of heat damage negated by MekaSuit Pants with maximum geothermal generator units installed. This number scales down linearly based on how many units are actually installed.")
              .defineInRange("heatDamageReductionRatio", 0.8, 0, 1));
        builder.pop(2);

        builder.pop();
        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "generators-gear";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public Type getConfigType() {
        return Type.SERVER;
    }

    @Override
    public boolean addToContainer() {
        return false;
    }
}