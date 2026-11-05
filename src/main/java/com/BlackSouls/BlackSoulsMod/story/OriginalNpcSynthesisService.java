package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcItems;
import com.BlackSouls.BlackSoulsMod.util.OriginalSynthesisRecipes;
import com.BlackSouls.BlackSoulsMod.util.StoryNameData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class OriginalNpcSynthesisService {
    private OriginalNpcSynthesisService() {
    }

    public static ClientboundOriginalNpcPacket.SynthesisView view(ServerPlayer player) {
        return view(player, unlockedRecipes(player));
    }

    public static List<Integer> unlockedRecipes(ServerPlayer player) {
        OriginalNpcProgress progress = OriginalNpcProgress.get(player.server);
        return OriginalSynthesisRecipes.values().stream().filter(recipe -> progress.recipeUnlocked(recipe.kind(), recipe.id()))
                .map(OriginalSynthesisRecipes.Recipe::id).toList();
    }

    public static ClientboundOriginalNpcPacket.SynthesisView view(ServerPlayer player, List<Integer> recipeIds) {
        List<ClientboundOriginalNpcPacket.SynthesisOffer> offers = new ArrayList<>();
        for (OriginalSynthesisRecipes.Recipe recipe : OriginalSynthesisRecipes.values()) {
            if (!recipeIds.contains(recipe.id())) continue;
            var entry = OriginalNpcItems.get(recipe.kind(), recipe.id());
            var weapon = OriginalSynthesisRecipes.weapon(recipe.id());
            if (OriginalNpcItems.item(recipe.kind(), recipe.id()) == null || weapon == null) {
                throw new UnsupportedOperationException("unmapped_synthesis_output:" + recipe.id());
            }
            List<ClientboundOriginalNpcPacket.SynthesisMaterial> materials = new ArrayList<>();
            for (var material : recipe.materials()) {
                var item = OriginalNpcItems.get(material.kind(), material.id());
                if (OriginalNpcItems.item(material.kind(), material.id()) == null) {
                    throw new UnsupportedOperationException("unmapped_synthesis_material:" + material.id());
                }
                materials.add(new ClientboundOriginalNpcPacket.SynthesisMaterial(material.kind(), material.id(),
                        item.name(), item.registry().toString(), material.quantity(),
                        OriginalNpcInventoryService.count(player, material.kind(), material.id())));
            }
            int maximum = maximum(player, recipe);
            offers.add(new ClientboundOriginalNpcPacket.SynthesisOffer(recipe.kind(), recipe.id(), 1, entry.name(),
                    entry.registry().toString(), weapon.description(), OriginalNpcInventoryService.count(player, 1, recipe.id()),
                    maximum, maximum > 0, recipe.cost(), weapon.parameters(), materials, comparisons(player, weapon)));
        }
        return new ClientboundOriginalNpcPacket.SynthesisView(Math.max(0L, stats(player).souls), false, offers);
    }

    public static boolean synthesize(ServerPlayer player, int kind, int id, int quantity) {
        var recipe = OriginalSynthesisRecipes.get(kind, id);
        if (player == null || recipe == null || quantity < 1 || quantity > 99
                || !OriginalNpcProgress.get(player.server).recipeUnlocked(kind, id)
                || quantity > sourceMaximum(player, recipe)) return false;
        List<ItemStack> plan = plan(player, recipe, quantity);
        if (plan == null) return false;
        long cost = Math.multiplyExact(recipe.cost(), quantity);
        for (int slot = 0; slot < plan.size(); slot++) player.getInventory().items.set(slot, plan.get(slot));
        stats(player).souls -= cost;
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    private static int maximum(ServerPlayer player, OriginalSynthesisRecipes.Recipe recipe) {
        for (int quantity = sourceMaximum(player, recipe); quantity > 0; quantity--) {
            if (plan(player, recipe, quantity) != null) return quantity;
        }
        return 0;
    }

    private static int sourceMaximum(ServerPlayer player, OriginalSynthesisRecipes.Recipe recipe) {
        int maximum = Math.max(0, 99 - OriginalNpcInventoryService.count(player, recipe.kind(), recipe.id()));
        for (var material : recipe.materials()) {
            maximum = Math.min(maximum, OriginalNpcInventoryService.count(player, material.kind(), material.id()) / material.quantity());
        }
        if (recipe.cost() > 0) maximum = (int) Math.min(maximum, Math.max(0L, stats(player).souls) / recipe.cost());
        return maximum;
    }

    private static List<ItemStack> plan(ServerPlayer player, OriginalSynthesisRecipes.Recipe recipe, int quantity) {
        List<ItemStack> inventory = player.getInventory().items.stream().map(ItemStack::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<ItemStack> inputs = new ArrayList<>();
        for (var material : recipe.materials()) {
            int remaining = Math.multiplyExact(quantity, material.quantity());
            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (material.kind() == 1 && slot == player.getInventory().selected) continue;
                ItemStack stack = inventory.get(slot);
                if (!OriginalNpcItems.matches(material.kind(), material.id(), stack)) continue;
                int amount = Math.min(remaining, stack.getCount());
                if (material.kind() == 1) {
                    for (int index = 0; index < amount; index++) inputs.add(stack.copyWithCount(1));
                }
                stack.shrink(amount);
                remaining -= amount;
            }
            if (remaining != 0) return null;
        }
        if (inputs.size() != quantity) throw new IllegalStateException("Invalid original synthesis weapon sequence");
        for (ItemStack input : inputs) {
            ItemStack output = OriginalNpcItems.createStack(recipe.kind(), recipe.id(), 1);
            CompoundTag saved = input.save(new CompoundTag());
            CompoundTag canonical = output.save(new CompoundTag());
            saved.putString("id", canonical.getString("id"));
            saved.putByte("Count", (byte) 1);
            CompoundTag tag = saved.getCompound("tag");
            tag.remove("bs2_upgrade_level");
            if (output.hasTag() && output.getTag().contains("bs2_upgrade_level")) {
                tag.putInt("bs2_upgrade_level", output.getTag().getInt("bs2_upgrade_level"));
            }
            saved.put("tag", tag);
            ItemStack moving = ItemStack.of(saved);
            if (!OriginalNpcItems.matches(recipe.kind(), recipe.id(), moving) || !insert(player, inventory, moving)) return null;
        }
        return inventory;
    }

    private static boolean insert(ServerPlayer player, List<ItemStack> inventory, ItemStack moving) {
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (slot == player.getInventory().selected) continue;
                ItemStack stack = inventory.get(slot);
                if (pass == 0 ? stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, moving) : !stack.isEmpty()) continue;
                if (stack.getCount() >= Math.min(moving.getMaxStackSize(), player.getInventory().getMaxStackSize())) continue;
                if (stack.isEmpty()) inventory.set(slot, moving.copy());
                else stack.grow(1);
                return true;
            }
        }
        return false;
    }

    private static List<ClientboundOriginalNpcPacket.SynthesisComparison> comparisons(ServerPlayer player, OriginalSynthesisRecipes.Weapon output) {
        OriginalSynthesisRecipes.Weapon equipped = OriginalSynthesisRecipes.weapon(OriginalNpcItems.sourceId(player.getMainHandItem()));
        if (equipped != null && equipped.equipmentType() != output.equipmentType()) equipped = null;
        boolean equippable = output.equipmentType() == 0 && output.weaponType() >= 1 && output.weaponType() <= 10;
        return List.of(new ClientboundOriginalNpcPacket.SynthesisComparison(2, StoryNameData.get(player), equippable,
                equipped == null ? "" : equipped.name(), output.parameters().get(2) - (equipped == null ? 0 : equipped.parameters().get(2))));
    }

    private static BSPlayerStats stats(ServerPlayer player) {
        return player.getCapability(BSPlayerStats.CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing original synthesis player stats"));
    }
}
