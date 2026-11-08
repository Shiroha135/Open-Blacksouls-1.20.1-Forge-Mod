package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.block.OriginalBlockDefinition;
import yuki135.blacksouls.block.OriginalTileBlock;
import yuki135.blacksouls.item.OriginalTileBlockItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OriginalTileBlocks {
    public static final List<OriginalBlockDefinition> DEFINITIONS = OriginalBlockDefinition.load();
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlackSouls.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BlackSouls.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlackSouls.MODID);
    public static final Map<String, RegistryObject<Item>> ENTRIES = new LinkedHashMap<>();

    static {
        for (var definition : DEFINITIONS) {
            for (int index = 0; index < definition.parts().size(); index++) {
                int part = index;
                String id = definition.id() + (index == 0 ? "" : "_part_" + index);
                definition.blocks().add(BLOCKS.register(id, () -> new OriginalTileBlock(definition, part)));
            }
            ENTRIES.put(definition.id(), ITEMS.register(definition.id(), () -> new OriginalTileBlockItem(definition, new Item.Properties())));
        }
        TABS.register("original_building_blocks", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.blacksouls.original_building_blocks"))
                .icon(() -> ENTRIES.get("original_library_bookcase").get().getDefaultInstance())
                .displayItems((parameters, output) -> ENTRIES.values().forEach(entry -> output.accept(entry.get())))
                .build());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private OriginalTileBlocks() {
    }
}
