package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.block.OriginalOpeningVisualBlock;
import yuki135.blacksouls.block.OriginalOpeningVisualDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OriginalOpeningVisualBlocks {
    private static final OriginalOpeningVisualDefinition.Catalog CATALOG=OriginalOpeningVisualDefinition.load();
    public static final String CATALOG_HASH=CATALOG.sha256();
    public static final List<OriginalOpeningVisualDefinition> DEFINITIONS=CATALOG.definitions();
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,BlackSouls.MODID);
    private static final Map<String,OriginalOpeningVisualDefinition> MODULES;
    private static final Map<String,List<RegistryObject<OriginalOpeningVisualBlock>>> PARTS;

    static {
        var modules=new LinkedHashMap<String,OriginalOpeningVisualDefinition>();
        var parts=new LinkedHashMap<String,List<RegistryObject<OriginalOpeningVisualBlock>>>();
        for (var definition:DEFINITIONS) {
            modules.put(definition.id(),definition);
            var registered=new ArrayList<RegistryObject<OriginalOpeningVisualBlock>>();
            for (int index=0;index<definition.parts().size();index++) {
                int part=index;
                registered.add(BLOCKS.register(definition.partId(part),()->new OriginalOpeningVisualBlock(definition,part)));
            }
            parts.put(definition.id(),List.copyOf(registered));
        }
        MODULES=Collections.unmodifiableMap(modules);
        PARTS=Collections.unmodifiableMap(parts);
    }

    private OriginalOpeningVisualBlocks() {
    }

    public static void register(IEventBus bus) { BLOCKS.register(Objects.requireNonNull(bus)); }
    public static OriginalOpeningVisualDefinition definition(String module) {
        if (module==null || !module.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid opening visual module");
        String id=module.startsWith("original_opening_")?module:"original_opening_"+module;
        var result=MODULES.get(id);
        if (result==null) throw new IllegalArgumentException("Unknown opening visual module: "+module);
        return result;
    }
    public static RegistryObject<OriginalOpeningVisualBlock> entry(String module, int part) {
        var definition=definition(module);
        definition.partId(part);
        return PARTS.get(definition.id()).get(part);
    }
    public static OriginalOpeningVisualBlock block(String module, int part) { return entry(module,part).get(); }
    public static BlockState state(String module, int part, Direction facing) { return block(module,part).state(facing); }
    public static boolean hasCollision(String module, int part) {
        var definition=definition(module);
        definition.partId(part);
        return definition.parts().get(part).hasCollision();
    }
    public static int registeredPartCount() { return PARTS.values().stream().mapToInt(List::size).sum(); }
}
