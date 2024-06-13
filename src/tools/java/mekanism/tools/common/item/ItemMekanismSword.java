package mekanism.tools.common.item;

import java.util.List;
import mekanism.tools.common.material.MaterialCreator;
import mekanism.tools.common.util.ToolsUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public class ItemMekanismSword extends SwordItem {

    public ItemMekanismSword(MaterialCreator material, Item.Properties properties) {
        //TODO - 1.21: Ensure the patch to neo that adds a float variant gets added back in
        super(material, properties.attributes(createAttributes(material, (int) material.getSwordDamage(), material.getSwordAtkSpeed())));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ToolsUtils.addDurability(tooltip, stack);
    }
}