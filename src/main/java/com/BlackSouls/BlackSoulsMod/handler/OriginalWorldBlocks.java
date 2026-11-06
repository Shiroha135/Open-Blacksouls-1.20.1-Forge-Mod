package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.block.OriginalAltarGateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class OriginalWorldBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BlackSouls.MODID);
    public static final RegistryObject<OriginalAltarGateBlock> ALTAR_GATE = BLOCKS.register("original_altar_gate", OriginalAltarGateBlock::new);

    private OriginalWorldBlocks() {
    }
}
