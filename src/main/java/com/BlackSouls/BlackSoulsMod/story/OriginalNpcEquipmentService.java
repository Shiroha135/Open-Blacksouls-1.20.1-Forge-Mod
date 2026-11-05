package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.util.OriginalEquipmentData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OriginalNpcEquipmentService {
    private static final Map<ResourceLocation, Integer> ARMORS = OriginalNpcItems.values().stream()
            .filter(entry -> entry.kind() == 2 && entry.registry() != null)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(OriginalNpcItems.Entry::registry, OriginalNpcItems.Entry::id));
    private OriginalNpcEquipmentService() {
    }

    public static ClientboundOriginalNpcPacket.EquipmentView view(ServerPlayer player, int kind, int id) {
        var output = OriginalEquipmentData.get(kind, id);
        if (output == null) return null;
        List<OriginalEquipmentData.Equipment> equipped = equipment(player);
        int parameter = kind == 1 ? 2 : 3;
        OriginalEquipmentData.Equipment current = null;
        boolean found = false;
        var slots = OriginalEquipmentData.playerSlots();
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index) != output.equipmentType()) continue;
            var candidate = equipped.get(index);
            if (!found || value(candidate, parameter) < value(current, parameter)) {
                current = candidate;
                found = true;
            }
        }
        var comparison = new ClientboundOriginalNpcPacket.SynthesisComparison(2, StoryNameData.get(player),
                OriginalEquipmentData.playerEquippable(output, equipped), current == null ? "" : current.name(),
                output.parameters().get(parameter) - value(current, parameter));
        return new ClientboundOriginalNpcPacket.EquipmentView(parameter, output.description(), List.of(comparison));
    }

    private static int value(OriginalEquipmentData.Equipment item, int parameter) {
        return item == null ? 0 : item.parameters().get(parameter);
    }

    private static List<OriginalEquipmentData.Equipment> equipment(ServerPlayer player) {
        List<OriginalEquipmentData.Equipment> result = new ArrayList<>();
        result.add(fitted(1, 0, player.getMainHandItem()));
        result.add(fitted(2, 1, player.getOffhandItem()));
        result.add(fitted(2, 2, curio(player, "head", 0, player.getItemBySlot(EquipmentSlot.HEAD))));
        result.add(fitted(2, 3, curio(player, "body", 0, player.getItemBySlot(EquipmentSlot.CHEST))));
        for (int index = 0; index < 4; index++) result.add(fitted(2, 4, curio(player, "ring", index, ItemStack.EMPTY)));
        return result;
    }

    private static ItemStack curio(ServerPlayer player, String slot, int index, ItemStack fallback) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.getStacksHandler(slot).map(stacks -> {
            if (index >= stacks.getStacks().getSlots()) return fallback;
            ItemStack item = stacks.getStacks().getStackInSlot(index);
            return item.isEmpty() ? fallback : item;
        }).orElse(fallback)).orElse(fallback);
    }

    private static OriginalEquipmentData.Equipment identify(int kind, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (kind == 1) return OriginalEquipmentData.get(1, OriginalNpcItems.sourceId(stack));
        return OriginalEquipmentData.get(2, ARMORS.getOrDefault(ForgeRegistries.ITEMS.getKey(stack.getItem()), -1));
    }

    private static OriginalEquipmentData.Equipment fitted(int kind, int slot, ItemStack stack) {
        var item = identify(kind, stack);
        return item != null && item.equipmentType() == slot ? item : null;
    }
}
