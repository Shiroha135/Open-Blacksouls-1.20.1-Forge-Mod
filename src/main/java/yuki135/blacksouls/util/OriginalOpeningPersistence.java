package yuki135.blacksouls.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public final class OriginalOpeningPersistence {
    private OriginalOpeningPersistence() {
    }

    public static boolean canSave(ServerPlayer player) {
        try {
            context(player);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean saveAndVerify(ServerPlayer player) {
        try {
            Context context = context(player);
            CompoundTag expected = player.saveWithoutId(new CompoundTag());
            if (!expected.getCompound("ForgeCaps").contains("blacksouls:bs_stats", Tag.TAG_COMPOUND)) return false;
            context.save.invoke(player.getServer().getPlayerList(), player);
            if (!Files.isRegularFile(context.file, LinkOption.NOFOLLOW_LINKS)
                    || !context.file.toRealPath().equals(context.file)) return false;
            CompoundTag actual = NbtIo.readCompressed(context.file.toFile());
            for (String key : List.of("UUID", "Inventory", "SelectedItemSlot", "ForgeCaps", "ForgeData")) {
                if (!Objects.equals(expected.get(key), actual.get(key))) return false;
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static Context context(ServerPlayer player) throws IOException {
        if (player == null || player.getServer() == null || !player.getServer().isSameThread() || player.connection == null
                || !player.getUUID().equals(player.getGameProfile().getId())
                || player.getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
            throw new IOException("A current online server player is required");
        }
        Path directory = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toAbsolutePath().normalize();
        if (!directory.toRealPath().equals(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(directory) || !Files.isWritable(directory)) throw new IOException("Unverified player directory");
        for (String suffix : List.of(".dat", ".dat_old")) {
            Path file = directory.resolve(player.getUUID() + suffix);
            if (!Files.notExists(file, LinkOption.NOFOLLOW_LINKS)
                    && (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !file.toRealPath().equals(file))) {
                throw new IOException("Unverified player file");
            }
        }
        Method save = ObfuscationReflectionHelper.findMethod(PlayerList.class, "m_6765_", ServerPlayer.class);
        return new Context(save, directory.resolve(player.getUUID() + ".dat"));
    }

    private record Context(Method save, Path file) { }
}
