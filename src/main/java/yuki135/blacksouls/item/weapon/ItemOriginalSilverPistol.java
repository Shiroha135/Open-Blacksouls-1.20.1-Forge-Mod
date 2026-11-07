package yuki135.blacksouls.item.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ItemOriginalSilverPistol extends Item {
    public ItemOriginalSilverPistol(Properties properties) { super(properties.stacksTo(1)); }

    public int sourceArmorId() { return 65; }

    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId()+".desc").withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
        tooltip.add(Component.empty());tooltip.add(Component.translatable(getDescriptionId()+".effect").withStyle(ChatFormatting.AQUA));
    }
}
