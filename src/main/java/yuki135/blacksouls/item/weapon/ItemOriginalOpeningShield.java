package yuki135.blacksouls.item.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ItemOriginalOpeningShield extends ShieldItem {
    private final int sourceArmorId;

    public ItemOriginalOpeningShield(Properties properties,int sourceArmorId) {
        super(properties.stacksTo(1));
        if (sourceArmorId!=39 && sourceArmorId!=42 && sourceArmorId!=43 && sourceArmorId!=45 && sourceArmorId!=47) throw new IllegalArgumentException("Invalid original opening shield");
        this.sourceArmorId=sourceArmorId;
    }

    public int sourceArmorId() { return sourceArmorId; }

    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId()+".desc").withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
        tooltip.add(Component.empty());tooltip.add(Component.translatable(getDescriptionId()+".effect").withStyle(ChatFormatting.AQUA));
    }
}
