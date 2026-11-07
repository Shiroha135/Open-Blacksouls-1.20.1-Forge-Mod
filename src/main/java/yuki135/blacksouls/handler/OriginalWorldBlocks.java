package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.block.OriginalAltarGateBlock;
import yuki135.blacksouls.block.OriginalPoolTile65Block;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OriginalWorldBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlackSouls.MODID);
    public static final RegistryObject<OriginalAltarGateBlock> ALTAR_GATE = BLOCKS.register("original_altar_gate", OriginalAltarGateBlock::new);

    public static final RegistryObject<OriginalPoolTile65Block> POOL_TILE65 = BLOCKS.register("original_pool_tile65", OriginalPoolTile65Block::new);

    private OriginalWorldBlocks() {
    }
}
