package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.story.OriginalOpeningProfile;
import yuki135.blacksouls.util.skill.SkillCrush;
import yuki135.blacksouls.util.skill.SkillDodge;
import yuki135.blacksouls.util.skill.SkillRegistry;
import yuki135.blacksouls.util.skill.SkillSeekAdvice;
import yuki135.blacksouls.util.skill.SkillSoulArrow;
import yuki135.blacksouls.util.skill.SkillSoulLight;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class OriginalOpeningStartingKit {
    private static final String BLACK_ASH = "bs2_first_join_black_ash";
    private static final String RECOVERY_REQUIRED = "bs2_opening_reward_recovery_required";

    private OriginalOpeningStartingKit() {
    }

    public enum Status {
        READY, GRANTED, ALREADY_COMMITTED, WRONG_STAGE, PREVIEW, LEGACY,
        MISSING_MAPPING, OCCUPIED, INVENTORY_FULL, UNAVAILABLE, SAVE_FAILED
    }

    public record Result(Status status, String detail) {
        public boolean ready() { return status == Status.READY; }
        public boolean committed() { return status == Status.GRANTED || status == Status.ALREADY_COMMITTED; }
    }

    public static Result preflight(ServerPlayer player, int originId) {
        try {
            var stats = eligible(player);
            var profile = stats.openingProfile();
            if (originId < 1 || originId > 9) throw failure(Status.WRONG_STAGE, "invalid_origin");
            if (profile.realKitGrantCommitted()) {
                return profile.realOriginId() == originId ? result(Status.ALREADY_COMMITTED, "kit")
                        : result(Status.WRONG_STAGE, "origin_already_committed");
            }
            var stage = profile.realStage().orElse(null);
            if (stage != OriginalOpeningProfile.Stage.ORIGIN_SELECTION && stage != OriginalOpeningProfile.Stage.ORIGIN_CONFIRMED
                    || stage == OriginalOpeningProfile.Stage.ORIGIN_CONFIRMED && profile.realOriginId() != originId) {
                throw failure(Status.WRONG_STAGE, "origin_not_selectable");
            }
            build(player, stats, originId, false);
            return result(Status.READY, "kit_and_coda_preflight");
        } catch (Failure failure) {
            return failure.result;
        } catch (RuntimeException exception) {
            return result(Status.UNAVAILABLE, exception.getClass().getSimpleName());
        }
    }

    public static Result grant(ServerPlayer player) {
        return commit(player, Operation.KIT);
    }

    public static Result opCoda(ServerPlayer player) {
        return commit(player, Operation.CODA);
    }

    public static Result arrivalCleanup(ServerPlayer player) {
        return commit(player, Operation.ARRIVAL);
    }

    public static boolean learnCareerSkills(BSPlayerStats stats, int level) {
        if (stats == null) return false;
        var profile = stats.openingProfile();
        if (profile.readState() != OriginalOpeningProfile.ReadState.REAL || profile.previewActive() || profile.realOriginId() == 0 || !profile.realKitGrantCommitted()) return false;
        List<String> learning = learning(profile.realOriginId(), level);
        boolean changed = false;
        for (String id : learning) if (!stats.unlockedSkills.contains(id)) {
            stats.unlockedSkills.add(id);
            changed = true;
        }
        return changed;
    }

    private static Result commit(ServerPlayer player, Operation operation) {
        Plan plan = null;
        boolean mutated = false;
        try {
            BSPlayerStats stats = eligible(player);
            var profile = stats.openingProfile();
            boolean already = switch (operation) {
                case KIT -> profile.realKitGrantCommitted();
                case CODA -> profile.realOpCodaGrantCommitted();
                case ARRIVAL -> profile.realArrivalRingCleanupCommitted();
            };
            if (already) return result(Status.ALREADY_COMMITTED, operation.name().toLowerCase(java.util.Locale.ROOT));
            var required = operation == Operation.ARRIVAL ? OriginalOpeningProfile.Stage.ARRIVAL_PENDING : OriginalOpeningProfile.Stage.ORIGIN_CONFIRMED;
            if (profile.realStage().orElse(null) != required || profile.realOriginId() == 0
                    || operation == Operation.CODA && !profile.realKitGrantCommitted()) {
                throw failure(Status.WRONG_STAGE, "reward_stage:" + operation.name());
            }
            plan = operation == Operation.ARRIVAL ? cleanup(player, stats) : build(player, stats, profile.realOriginId(), operation == Operation.CODA);
            if (!unchanged(player, plan)) throw failure(Status.OCCUPIED, "preflight_state_changed");
            mutated = true;
            writeInventory(player, plan.afterItems);
            player.setItemSlot(EquipmentSlot.OFFHAND, plan.afterOffhand.copy());
            for (ArmorWrite write : plan.armor) if (!ItemStack.matches(write.before, write.after)) player.setItemSlot(write.slot, write.after.copy());
            for (CurioWrite write : plan.curios) write.handler.setStackInSlot(write.slot, write.after.copy());
            if (!matchesInventory(player, plan.afterItems) || !ItemStack.matches(player.getOffhandItem(), plan.afterOffhand)
                    || plan.curios.stream().anyMatch(write -> !ItemStack.matches(write.handler.getStackInSlot(write.slot), write.after))
                    || plan.armor.stream().anyMatch(write -> !ItemStack.matches(player.getItemBySlot(write.slot), write.after))) {
                throw failure(Status.OCCUPIED, "equipment_write_rejected");
            }
            if (operation == Operation.CODA) {
                persisted(player, true).putBoolean(BLACK_ASH, true);
            } else if (operation == Operation.KIT) {
                stats.souls = plan.afterSouls;
                for (String id : plan.skills) if (!stats.unlockedSkills.contains(id)) stats.unlockedSkills.add(id);
            }
            StatEventHandler.applyStats(player);
            boolean marked = switch (operation) {
                case KIT -> profile.markRealKitGrantCommitted();
                case CODA -> profile.markRealOpCodaGrantCommitted();
                case ARRIVAL -> profile.markRealArrivalRingCleanupCommitted();
            };
            if (!marked) throw failure(Status.WRONG_STAGE, "marker_changed_during_commit");
            player.getInventory().setChanged();
            if (!OriginalOpeningPersistence.saveAndVerify(player)) throw failure(Status.SAVE_FAILED, "player_save_readback");
            try {
                player.inventoryMenu.broadcastChanges();
                if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
                StatEventHandler.syncToClient(player);
            } catch (RuntimeException exception) {
                return result(Status.GRANTED, "committed_sync_pending");
            }
            return result(Status.GRANTED, plan.detail);
        } catch (Failure failure) {
            if (mutated && !rollback(player, plan)) return result(Status.SAVE_FAILED, "rollback_requires_recovery");
            return failure.result;
        } catch (Exception exception) {
            if (mutated && !rollback(player, plan)) return result(Status.SAVE_FAILED, "rollback_requires_recovery");
            return result(mutated ? Status.SAVE_FAILED : Status.UNAVAILABLE, exception.getClass().getSimpleName());
        }
    }

    private static BSPlayerStats eligible(ServerPlayer player) {
        if (player == null || player.getServer() == null || !player.getServer().isSameThread()
                || player.getServer().getPlayerList().getPlayer(player.getUUID()) != player
                || !player.getUUID().equals(player.getGameProfile().getId()) || player.connection == null) {
            throw failure(Status.UNAVAILABLE, "server_player_required");
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) throw failure(Status.UNAVAILABLE, "player_stats_missing");
        var profile = stats.openingProfile();
        if (profile.previewActive()) throw failure(Status.PREVIEW, "preview_has_no_real_rewards");
        if (profile.readState() != OriginalOpeningProfile.ReadState.REAL) throw failure(Status.LEGACY, "verified_real_opening_required");
        if (player.getPersistentData().contains(RECOVERY_REQUIRED)) throw failure(Status.SAVE_FAILED, "recovery_required");
        return stats;
    }

    private static Plan build(ServerPlayer player, BSPlayerStats stats, int originId, boolean coda) {
        var profession = OriginalOpeningData.get(originId);
        if (profession == null) throw failure(Status.MISSING_MAPPING, "source_profession:" + originId);
        var items = player.getInventory().items;
        int selected = player.getInventory().selected;
        if (items.size() != 36 || selected < 0 || selected > 8) throw failure(Status.UNAVAILABLE, "inventory_layout");
        List<ItemStack> before = copies(items);
        List<ItemStack> after = copies(items);
        ItemStack oldOffhand = player.getOffhandItem().copy();
        ItemStack nextOffhand = oldOffhand.copy();
        List<CurioWrite> curios = new ArrayList<>();
        List<String> skills = coda ? List.of() : learning(originId, stats.level);
        long souls = stats.souls;
        boolean ashClaimed = blackAshClaimed(player);
        var shared = OriginalOpeningData.catalog().openingCoda().sharedItem();
        if (shared.kind() != 0 || shared.id() != 67 || shared.amount() != 1) throw failure(Status.MISSING_MAPPING, "source_op_coda");
        ItemStack ash = mapped(shared.kind(), shared.id(), shared.amount());
        if (!coda) {
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                if (!player.getItemBySlot(slot).isEmpty()) throw failure(Status.OCCUPIED, "vanilla_armor:" + slot.getName());
            }
            for (OriginalOpeningData.EquipmentSlot slot : profession.initialEquipment()) {
                ItemStack stack = slot.id() == 0 ? ItemStack.EMPTY : mapped(slot.kind(), slot.id(), 1);
                if (slot.slot() == 0) {
                    if (!before.get(selected).isEmpty()) throw failure(Status.OCCUPIED, "source_slot:0");
                    after.set(selected, stack.copy());
                } else if (slot.slot() == 1) {
                    if (!oldOffhand.isEmpty()) throw failure(Status.OCCUPIED, "source_slot:1");
                    nextOffhand = stack.copy();
                } else {
                    String type = slot.slot() == 2 ? "head" : slot.slot() == 3 ? "body" : "ring";
                    int index = slot.slot() < 4 ? 0 : slot.slot() - 4;
                    var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
                    var group = inventory == null ? null : inventory.getStacksHandler(type).orElse(null);
                    IDynamicStackHandler handler = group == null ? null : group.getStacks();
                    if (handler == null || index >= handler.getSlots()) {
                        if (!stack.isEmpty()) throw failure(Status.UNAVAILABLE, "curio_slot:" + type + ":" + index);
                        continue;
                    }
                    ItemStack old = handler.getStackInSlot(index);
                    if (!old.isEmpty()) throw failure(Status.OCCUPIED, "source_slot:" + slot.slot());
                    if (!stack.isEmpty() && !handler.insertItem(index, stack.copy(), true).isEmpty()) {
                        throw failure(Status.UNAVAILABLE, "curio_rejected:" + type + ":" + index);
                    }
                    if (!stack.isEmpty()) curios.add(new CurioWrite(handler, index, old.copy(), stack.copy()));
                }
            }
            try {
                souls = Math.addExact(stats.souls, profession.extraSouls());
            } catch (ArithmeticException exception) {
                throw failure(Status.UNAVAILABLE, "souls_overflow");
            }
            for (var reward : profession.extraRewards()) if (reward.kind() == OriginalOpeningData.RewardKind.ITEM) {
                ItemStack stack = mapped(0, reward.id(), reward.amount());
                if (!insert(after, selected, stack, player.getInventory().getMaxStackSize())) throw failure(Status.INVENTORY_FULL, "profession_reward:" + reward.id());
            }
            List<ItemStack> complete = copies(after);
            if (!ashClaimed && !insert(complete, selected, ash, player.getInventory().getMaxStackSize())) {
                throw failure(Status.INVENTORY_FULL, "op_coda_capacity");
            }
        } else if (!ashClaimed && !insert(after, selected, ash, player.getInventory().getMaxStackSize())) {
            throw failure(Status.INVENTORY_FULL, "op_coda");
        }
        if (!OriginalOpeningPersistence.canSave(player)) throw failure(Status.UNAVAILABLE, "player_save_unavailable");
        return new Plan(stats, stats.openingProfile(), stats.serializeNBT(), player.getPersistentData().copy(),
                before, after, oldOffhand, nextOffhand, armor(player), List.copyOf(curios), skills, souls,
                player.getHealth(), stats.mp, coda && ashClaimed ? "op_coda_reconciled_existing_flag" : coda ? "op_coda" : "kit");
    }

    private static Plan cleanup(ServerPlayer player, BSPlayerStats stats) {
        var removal = OriginalOpeningData.catalog().openingCoda().ringRemoval();
        if (removal.kind() != 2 || removal.id() != 131 || removal.amount() != 1 || !removal.includeEquipped()) {
            throw failure(Status.MISSING_MAPPING, "source_arrival_cleanup");
        }
        List<ItemStack> before = copies(player.getInventory().items);
        List<ItemStack> after = copies(before);
        ItemStack oldOffhand = player.getOffhandItem().copy();
        ItemStack nextOffhand = oldOffhand.copy();
        var armor = new ArrayList<>(armor(player));
        List<CurioWrite> curios = new ArrayList<>();
        String detail;
        if (OriginalNpcItems.item(removal.kind(), removal.id()) == null) {
            boolean initialContains = OriginalOpeningData.catalog().professions().stream().flatMap(profession -> profession.initialEquipment().stream())
                    .anyMatch(slot -> slot.kind() == removal.kind() && slot.id() == removal.id());
            if (initialContains) throw failure(Status.MISSING_MAPPING, "unmapped_initial_cleanup_item");
            detail = "unmapped_ring_absent_from_all_source_initial_kits";
        } else {
            mapped(removal.kind(), removal.id(), 1);
            int remaining = 1;
            for (ItemStack stack : after) if (remaining > 0 && OriginalNpcItems.matches(removal.kind(), removal.id(), stack)) {
                stack.shrink(1);
                remaining--;
            }
            if (remaining > 0 && OriginalNpcItems.matches(removal.kind(), removal.id(), nextOffhand)) {
                nextOffhand.shrink(1);
                remaining--;
            }
            for (int index = 0; index < armor.size() && remaining > 0; index++) {
                var slot = armor.get(index);
                if (OriginalNpcItems.matches(removal.kind(), removal.id(), slot.after)) {
                    ItemStack next = slot.after.copy(); next.shrink(1);
                    armor.set(index, new ArmorWrite(slot.slot, slot.before, next));
                    remaining--;
                }
            }
            var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
            if (inventory == null) throw failure(Status.UNAVAILABLE, "curios_cleanup_inventory_missing");
            var groups = new ArrayList<>(inventory.getCurios().entrySet());
            groups.sort(java.util.Map.Entry.comparingByKey());
            for (var group : groups) {
                var handler = group.getValue().getStacks();
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    ItemStack old = handler.getStackInSlot(slot);
                    if (OriginalNpcItems.matches(removal.kind(), removal.id(), old)) {
                        ItemStack next = old.copy(); next.shrink(1);
                        curios.add(new CurioWrite(handler, slot, old.copy(), next));
                        remaining--;
                    }
                }
            }
            detail = remaining == 0 ? "source_ring_removed_once" : "source_ring_already_absent";
        }
        if (!OriginalOpeningPersistence.canSave(player)) throw failure(Status.UNAVAILABLE, "player_save_unavailable");
        return new Plan(stats, stats.openingProfile(), stats.serializeNBT(), player.getPersistentData().copy(),
                before, after, oldOffhand, nextOffhand, List.copyOf(armor), List.copyOf(curios), List.of(), stats.souls,
                player.getHealth(), stats.mp, detail);
    }

    private static List<ArmorWrite> armor(ServerPlayer player) {
        return List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).stream()
                .map(slot -> new ArmorWrite(slot, player.getItemBySlot(slot).copy(), player.getItemBySlot(slot).copy())).toList();
    }

    private static List<String> learning(int originId, int level) {
        if (level < 1 || level > 999) throw failure(Status.WRONG_STAGE, "career_level");
        var profession = OriginalOpeningData.get(originId);
        if (profession == null) throw failure(Status.MISSING_MAPPING, "source_profession:" + originId);
        var result = new LinkedHashSet<String>();
        for (var item : profession.professionClass().learnings()) if (item.level() <= level) result.add(skill(item.skillId()));
        return List.copyOf(result);
    }

    private static String skill(int source) {
        String id = switch (source) {
            case 12 -> "bs2_skill_dodge";
            case 13 -> "bs2_skill_crush";
            case 14 -> "bs2_skill_soul_arrow";
            case 15 -> "bs2_skill_soul_light";
            case 341 -> "bs2_skill_seek_advice";
            default -> throw failure(Status.MISSING_MAPPING, "source_skill:" + source);
        };
        var mapped = SkillRegistry.SKILLS.get(id);
        boolean valid = switch (source) {
            case 12 -> mapped instanceof SkillDodge;
            case 13 -> mapped instanceof SkillCrush;
            case 14 -> mapped instanceof SkillSoulArrow;
            case 15 -> mapped instanceof SkillSoulLight;
            case 341 -> mapped instanceof SkillSeekAdvice;
            default -> false;
        };
        if (!valid) throw failure(Status.MISSING_MAPPING, "source_skill:" + source);
        return id;
    }

    private static ItemStack mapped(int kind, int id, int count) {
        if (OriginalNpcItems.get(kind, id) == null || OriginalNpcItems.item(kind, id) == null) {
            throw failure(Status.MISSING_MAPPING, "source_item:" + kind + ":" + id);
        }
        ItemStack stack = OriginalNpcItems.createStack(kind, id, count);
        if (!OriginalNpcItems.matches(kind, id, stack)) throw failure(Status.MISSING_MAPPING, "source_item_identity:" + kind + ":" + id);
        return stack;
    }

    static boolean insert(List<ItemStack> inventory, int equipped, ItemStack reward, int inventoryLimit) {
        if (reward.isEmpty()) return true;
        long remaining = reward.getCount();
        int limit = Math.min(reward.getMaxStackSize(), inventoryLimit);
        if (remaining < 1 || limit < 1) return false;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (slot == equipped) continue;
                ItemStack stack = inventory.get(slot);
                if (pass == 0 && (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, reward))
                        || pass == 1 && !stack.isEmpty()) continue;
                int amount = (int) Math.min(remaining, Math.max(0, limit - stack.getCount()));
                if (amount == 0) continue;
                if (stack.isEmpty()) inventory.set(slot, reward.copyWithCount(amount));
                else stack.grow(amount);
                remaining -= amount;
            }
        }
        return remaining == 0;
    }

    private static boolean blackAshClaimed(ServerPlayer player) {
        CompoundTag data = persisted(player, false);
        if (data.contains(BLACK_ASH) && (!data.contains(BLACK_ASH, Tag.TAG_BYTE)
                || data.getByte(BLACK_ASH) != 0 && data.getByte(BLACK_ASH) != 1)) {
            throw failure(Status.UNAVAILABLE, "invalid_black_ash_receipt");
        }
        return data.getBoolean(BLACK_ASH);
    }

    private static CompoundTag persisted(ServerPlayer player, boolean create) {
        CompoundTag root = player.getPersistentData();
        if (root.contains(Player.PERSISTED_NBT_TAG) && !root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            throw failure(Status.UNAVAILABLE, "invalid_persisted_player_data");
        }
        if (create && !root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static boolean unchanged(ServerPlayer player, Plan plan) {
        return player.getCapability(BSPlayerStats.CAPABILITY).orElse(null) == plan.stats
                && plan.stats.openingProfile() == plan.profile && plan.stats.serializeNBT().equals(plan.beforeStats)
                && player.getPersistentData().equals(plan.beforePersistent) && matchesInventory(player, plan.beforeItems)
                && ItemStack.matches(player.getOffhandItem(), plan.beforeOffhand)
                && plan.armor.stream().allMatch(write -> ItemStack.matches(player.getItemBySlot(write.slot), write.before))
                && plan.curios.stream().allMatch(write -> ItemStack.matches(write.handler.getStackInSlot(write.slot), write.before));
    }

    private static boolean rollback(ServerPlayer player, Plan plan) {
        if (plan == null) return false;
        try {
            writeInventory(player, plan.beforeItems);
            player.setItemSlot(EquipmentSlot.OFFHAND, plan.beforeOffhand.copy());
            for (ArmorWrite write : plan.armor) player.setItemSlot(write.slot, write.before.copy());
            for (CurioWrite write : plan.curios) write.handler.setStackInSlot(write.slot, write.before.copy());
            plan.stats.deserializeNBT(plan.beforeStats.copy());
            CompoundTag persistent = player.getPersistentData();
            for (String key : new HashSet<>(persistent.getAllKeys())) persistent.remove(key);
            for (String key : plan.beforePersistent.getAllKeys()) persistent.put(key, plan.beforePersistent.get(key).copy());
            StatEventHandler.applyStats(player);
            player.setHealth(Math.min(plan.beforeHealth, player.getMaxHealth()));
            plan.stats.mp = plan.beforeMana;
            player.getInventory().setChanged();
            boolean restored = matchesInventory(player, plan.beforeItems) && ItemStack.matches(player.getOffhandItem(), plan.beforeOffhand)
                    && plan.curios.stream().allMatch(write -> ItemStack.matches(write.handler.getStackInSlot(write.slot), write.before))
                    && plan.armor.stream().allMatch(write -> ItemStack.matches(player.getItemBySlot(write.slot), write.before))
                    && java.util.Objects.equals(plan.stats.serializeNBT().get(OriginalOpeningProfile.TAG_KEY), plan.beforeStats.get(OriginalOpeningProfile.TAG_KEY))
                    && player.getPersistentData().equals(plan.beforePersistent) && plan.stats.souls == plan.beforeStats.getLong("Souls");
            restored = restored && OriginalOpeningPersistence.saveAndVerify(player);
            if (!restored) player.getPersistentData().putBoolean(RECOVERY_REQUIRED, true);
            return restored;
        } catch (RuntimeException exception) {
            player.getPersistentData().putBoolean(RECOVERY_REQUIRED, true);
            return false;
        }
    }

    private static boolean matchesInventory(ServerPlayer player, List<ItemStack> expected) {
        var actual = player.getInventory().items;
        if (actual.size() != expected.size()) return false;
        for (int slot = 0; slot < actual.size(); slot++) if (!ItemStack.matches(actual.get(slot), expected.get(slot))) return false;
        return true;
    }

    private static List<ItemStack> copies(List<ItemStack> source) {
        return source.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void writeInventory(ServerPlayer player, List<ItemStack> items) {
        for (int slot = 0; slot < items.size(); slot++) player.getInventory().items.set(slot, items.get(slot).copy());
    }

    private static Result result(Status status, String detail) { return new Result(status, detail); }
    private static Failure failure(Status status, String detail) { return new Failure(result(status, detail)); }

    private static final class Failure extends RuntimeException {
        private final Result result;
        private Failure(Result result) { super(result.status + ":" + result.detail); this.result = result; }
    }

    private record CurioWrite(IDynamicStackHandler handler, int slot, ItemStack before, ItemStack after) { }
    private record ArmorWrite(EquipmentSlot slot, ItemStack before, ItemStack after) { }
    private enum Operation { KIT, CODA, ARRIVAL }
    private record Plan(BSPlayerStats stats, OriginalOpeningProfile profile, CompoundTag beforeStats, CompoundTag beforePersistent,
                        List<ItemStack> beforeItems, List<ItemStack> afterItems, ItemStack beforeOffhand, ItemStack afterOffhand,
                        List<ArmorWrite> armor, List<CurioWrite> curios, List<String> skills, long afterSouls, float beforeHealth, double beforeMana,
                        String detail) { }
}
