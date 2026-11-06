package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.util.SkillUtils;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalPlayerMoveSpeed {
    public static final String STATE = "BSOriginalMoveSpeed";
    private static final UUID MODIFIER = UUID.fromString("c804d67f-733c-41c6-b83c-925097d2cb5b");
    private static final Set<String> FIELDS = Set.of("Version", "SourceSpeed", "Normal", "High");

    private OriginalPlayerMoveSpeed() {
    }

    public static void set(ServerPlayer player, int speed) {
        if (speed < 1 || speed > 6) throw new UnsupportedOperationException("original_player_move_speed:" + speed);
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) throw new UnsupportedOperationException("original_player_move_attribute");
        CompoundTag state = state(player, true);
        state.putInt("SourceSpeed", speed);
        var data = player.getPersistentData();
        if (!data.contains("BSOriginalRouteState", Tag.TAG_COMPOUND)) {
            CompoundTag route = new CompoundTag();
            route.putInt("MoveFrequency", 6);
            data.put("BSOriginalRouteState", route);
        }
        data.getCompound("BSOriginalRouteState").putInt("MoveSpeed", speed);
        attribute.removeModifier(MODIFIER);
        if (speed != 4) attribute.addTransientModifier(new AttributeModifier(MODIFIER, "Original player move speed",
                Math.pow(2.0D, speed - 4) - 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    public static int speed(ServerPlayer player) {
        CompoundTag state = state(player, false);
        return state.isEmpty() ? 4 : state.getInt("SourceSpeed");
    }

    public static boolean switchValue(ServerPlayer player, int id) {
        String field = field(id);
        return state(player, false).getBoolean(field);
    }

    public static void setSwitch(ServerPlayer player, int id, boolean enabled) {
        state(player, true).putBoolean(field(id), enabled);
    }

    private static String field(int id) {
        if (id != 11 && id != 12) throw new UnsupportedOperationException("original_player_speed_switch:" + id);
        return id == 11 ? "Normal" : "High";
    }

    private static CompoundTag state(ServerPlayer player, boolean create) {
        var data = SkillUtils.getPersistedData(player);
        if (!data.contains(STATE)) {
            if (!create) return new CompoundTag();
            CompoundTag state = new CompoundTag();
            state.putInt("Version", 1);
            state.putInt("SourceSpeed", 4);
            state.putBoolean("Normal", false);
            state.putBoolean("High", false);
            data.put(STATE, state);
        }
        if (!data.contains(STATE, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid original player speed state");
        var state = data.getCompound(STATE);
        if (!state.getAllKeys().equals(FIELDS) || !state.contains("Version", Tag.TAG_INT) || state.getInt("Version") != 1
                || !state.contains("SourceSpeed", Tag.TAG_INT) || state.getInt("SourceSpeed") < 1 || state.getInt("SourceSpeed") > 6
                || !state.contains("Normal", Tag.TAG_BYTE) || !state.contains("High", Tag.TAG_BYTE)
                || state.getByte("Normal") < 0 || state.getByte("Normal") > 1 || state.getByte("High") < 0 || state.getByte("High") > 1) {
            throw new IllegalArgumentException("Invalid original player speed state");
        }
        return state;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var previous = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (previous.contains(STATE)) SkillUtils.getPersistedData(event.getEntity()).put(STATE, previous.get(STATE).copy());
    }

    private static void restore(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !state(serverPlayer, false).isEmpty()) set(serverPlayer, speed(serverPlayer));
    }
}
