package yuki135.blacksouls.story;

import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalNpcPicturesPacket;
import yuki135.blacksouls.util.OriginalNpcPictures;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public final class OriginalNpcPictureService {
    private OriginalNpcPictureService() {
    }

    public static int apply(ServerPlayer player, int code, List<JsonElement> parameters) {
        return apply(player, "", code, parameters);
    }

    public static int apply(ServerPlayer player, String sourceNpc, int code, List<JsonElement> parameters) {
        if (!sourceNpc.isEmpty() && !sourceNpc.matches("map[0-9]{3}_ev[0-9]{3}")) {
            throw new IllegalArgumentException("Invalid original picture source");
        }
        if (battle(player)) throw new UnsupportedOperationException("picture_battle_scope");
        if (!player.isAlive()) throw new UnsupportedOperationException("picture_owner_dead");
        if (OriginalNpcService.isPictureClockSuspended(player)) throw new UnsupportedOperationException("picture_waiter_suspended");
        Command command = parse(player, code, parameters);
        OriginalNpcPictureData data = OriginalNpcPictureData.get(player.server);
        OriginalNpcPictureData.Screen previous = data.screens.get(player.getUUID());
        if (previous != null) discardOmittedPictures(player, previous);
        OriginalNpcPictureState current = previous == null ? null : previous.pictures.get(command.number());
        if (code == 232 && current != null) validatePicture(current.name());
        OriginalNpcPictureState next = current == null ? new OriginalNpcPictureState(command.number())
                : OriginalNpcPictureState.restore(current.snapshot());
        if (code == 231) {
            next.show(command.name(), command.origin(), command.x(), command.y(), command.zoomX(), command.zoomY(),
                    command.opacity(), command.blendType());
        } else if (code == 232) {
            next.move(command.origin(), command.x(), command.y(), command.zoomX(), command.zoomY(), command.opacity(),
                    command.blendType(), command.duration());
        } else {
            next.erase();
        }
        OriginalNpcPictureData.Screen screen = data.screens.computeIfAbsent(player.getUUID(), ignored -> new OriginalNpcPictureData.Screen());
        screen.pictures.put(next.number(), next);
        if (code == 235 || code == 231 && sourceNpc.isEmpty()) screen.sources.remove(next.number());
        else if (!sourceNpc.isEmpty()) screen.sources.put(next.number(), sourceNpc);
        screen.paused = false;
        screen.visible = true;
        publish(player, data, screen);
        return code == 232 && command.waitForCompletion() ? (command.duration() + 2) / 3 : 0;
    }

    public static void tick(MinecraftServer server) {
        OriginalNpcPictureData data = OriginalNpcPictureData.get(server);
        if (data.lastServerTick == server.getTickCount()) return;
        data.lastServerTick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            OriginalNpcPictureData.Screen screen = data.screens.get(player.getUUID());
            if (screen == null) continue;
            boolean visible = visible(player);
            boolean paused = !visible || OriginalNpcService.isPictureClockSuspended(player);
            boolean changed = screen.paused != paused || screen.visible != visible;
            screen.paused = paused;
            screen.visible = visible;
            if (!paused) {
                for (OriginalNpcPictureState state : screen.pictures.values()) {
                    if (state.duration() > 0) {
                        state.advanceFrames(3);
                        changed = true;
                    }
                }
            }
            boolean reconnecting = screen.connection != player.connection
                    || !player.level().dimension().location().equals(screen.dimension);
            if (changed || reconnecting) {
                publish(player, data, screen);
            }
        }
    }

    public static void sync(ServerPlayer player) {
        OriginalNpcPictureData data = OriginalNpcPictureData.get(player.server);
        OriginalNpcPictureData.Screen screen = data.screens.computeIfAbsent(player.getUUID(), ignored -> new OriginalNpcPictureData.Screen());
        screen.visible = visible(player);
        screen.paused = !screen.visible || OriginalNpcService.isPictureClockSuspended(player);
        discardOmittedPictures(player, screen);
        publish(player, data, screen);
    }

    public static void clear(ServerPlayer player) {
        OriginalNpcPictureData data = OriginalNpcPictureData.get(player.server);
        OriginalNpcPictureData.Screen screen = data.screens.get(player.getUUID());
        if (screen == null) return;
        screen.pictures.clear();
        screen.sources.clear();
        screen.visible = visible(player);
        screen.paused = !screen.visible || OriginalNpcService.isPictureClockSuspended(player);
        publish(player, data, screen);
    }

    private static void discardOmittedPictures(ServerPlayer player, OriginalNpcPictureData.Screen screen) {
        if (screen.pictures.isEmpty()) return;
        var omitted = OriginalNpcService.omittedCheckpointIds(player);
        if (omitted.isEmpty()) return;
        var iterator = screen.pictures.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String source = screen.sources.get(entry.getKey());
            if (source == null || omitted.contains(source)) {
                iterator.remove();
                screen.sources.remove(entry.getKey());
            }
        }
    }

    public static void forgetObserver(MinecraftServer server, UUID player) {
        OriginalNpcPictureData.Screen screen = OriginalNpcPictureData.get(server).screens.get(player);
        if (screen != null) {
            screen.connection = null;
            screen.dimension = null;
        }
    }

    private static boolean battle(ServerPlayer player) {
        return TurnBattleManager.isInBattle(player) || OriginalNpcService.isBattlePresentationPending(player);
    }

    private static boolean visible(ServerPlayer player) {
        return player.isAlive() && !battle(player);
    }

    private static void publish(ServerPlayer player, OriginalNpcPictureData data, OriginalNpcPictureData.Screen screen) {
        discardOmittedPictures(player, screen);
        screen.revision = Math.addExact(screen.revision, 1L);
        data.setDirty();
        if (player.connection == null) return;
        NetworkHandler.sendToPlayer(new ClientboundOriginalNpcPicturesPacket(player.level().dimension().location(),
                screen.revision, screen.paused, screen.visible,
                screen.pictures.values().stream().map(OriginalNpcPictureState::snapshot).toList()), player);
        screen.connection = player.connection;
        screen.dimension = player.level().dimension().location();
    }

    private static Command parse(ServerPlayer player, int code, List<JsonElement> parameters) {
        try {
            int size = switch (code) {
                case 231 -> 10;
                case 232 -> 12;
                case 235 -> 1;
                default -> throw new UnsupportedOperationException("picture_command:" + code);
            };
            require(parameters.size() == size, "picture_parameters");
            int number = integer(parameters, 0, 1, 100);
            if (code == 235) return new Command(number, "", 0, 0, 0, 100, 100, 255, 0, 0, false);
            String name = "";
            if (code == 231) {
                require(parameters.get(1).isJsonPrimitive() && parameters.get(1).getAsJsonPrimitive().isString(), "picture_name");
                name = parameters.get(1).getAsString();
                validatePicture(name);
            }
            int origin = integer(parameters, 2, 0, 1);
            int coordinateMode = integer(parameters, 3, 0, 1);
            double x = coordinateMode == 0 ? number(parameters, 4)
                    : OriginalNpcProgress.readVariable(player, integer(parameters, 4, 1, Integer.MAX_VALUE));
            double y = coordinateMode == 0 ? number(parameters, 5)
                    : OriginalNpcProgress.readVariable(player, integer(parameters, 5, 1, Integer.MAX_VALUE));
            double zoomX = number(parameters, 6);
            double zoomY = number(parameters, 7);
            double opacity = number(parameters, 8);
            require(zoomX >= 0.0D && zoomX <= 2000.0D && zoomY >= 0.0D && zoomY <= 2000.0D, "picture_zoom");
            require(opacity >= 0.0D && opacity <= 255.0D, "picture_opacity");
            int blend = integer(parameters, 9, 0, 2);
            require(blend == 0, "picture_blend:" + blend);
            int duration = code == 232 ? integer(parameters, 10, 0, 360000) : 0;
            boolean wait = code == 232 && bool(parameters, 11);
            return new Command(number, name, origin, x, y, zoomX, zoomY, opacity, blend, duration, wait);
        } catch (RuntimeException exception) {
            if (exception instanceof UnsupportedOperationException unsupported) throw unsupported;
            throw new UnsupportedOperationException("picture_parameters:" + code + ":" + exception.getMessage(), exception);
        }
    }

    private static void validatePicture(String name) {
        if (name.isEmpty()) return;
        OriginalNpcPictures.Entry entry = OriginalNpcPictures.get(name);
        if (entry == null) throw new UnsupportedOperationException("picture_asset:" + name);
        ResourceLocation texture = ResourceLocation.tryParse(entry.texture());
        if (texture == null) throw new UnsupportedOperationException("picture_texture:" + name);
        String path = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
        try (InputStream stream = OriginalNpcPictureService.class.getResourceAsStream(path)) {
            if (stream == null) throw new UnsupportedOperationException("picture_texture:" + name);
        } catch (IOException exception) {
            throw new UnsupportedOperationException("picture_texture:" + name, exception);
        }
    }

    private static int integer(List<JsonElement> parameters, int index, int minimum, int maximum) {
        JsonElement element = parameters.get(index);
        require(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber(), "picture_integer:" + index);
        int value = element.getAsBigDecimal().intValueExact();
        require(value >= minimum && value <= maximum, "picture_integer_range:" + index);
        return value;
    }

    private static double number(List<JsonElement> parameters, int index) {
        JsonElement element = parameters.get(index);
        require(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber(), "picture_number:" + index);
        double value = element.getAsDouble();
        require(Double.isFinite(value) && Math.abs(value) <= 1.0E12D, "picture_number_range:" + index);
        return value;
    }

    private static boolean bool(List<JsonElement> parameters, int index) {
        JsonElement element = parameters.get(index);
        require(element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean(), "picture_wait");
        return element.getAsBoolean();
    }

    private static void require(boolean value, String reason) {
        if (!value) throw new UnsupportedOperationException(reason);
    }

    private record Command(int number, String name, int origin, double x, double y, double zoomX, double zoomY,
                           double opacity, int blendType, int duration, boolean waitForCompletion) {
    }
}
