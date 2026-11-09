package yuki135.blacksouls.item.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import yuki135.blacksouls.util.OriginalShieldData;

public class ItemOriginalShield extends ShieldItem {
    private final int sourceArmorId;
    public ItemOriginalShield(Properties properties,int sourceArmorId) {
        super(properties.stacksTo(1));
        if(OriginalShieldData.get(sourceArmorId)==null)throw new IllegalArgumentException("Unknown original shield");
        this.sourceArmorId=sourceArmorId;
    }
    public int sourceArmorId() {return sourceArmorId;}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag) {
        String[] lines=OriginalShieldData.get(sourceArmorId).description().split("\n",-1);
        for(int i=0;i<lines.length;i++)if(!lines[i].isEmpty())tooltip.add(Component.literal(lines[i]).withStyle(i==0?ChatFormatting.AQUA:ChatFormatting.GRAY));
    }
}
