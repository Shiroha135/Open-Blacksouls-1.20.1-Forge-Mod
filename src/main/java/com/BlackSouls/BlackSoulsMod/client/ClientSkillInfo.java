package com.BlackSouls.BlackSoulsMod.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ClientSkillInfo {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DEFAULT_AVATAR = "knight_face";
    private static final String AVATAR_FILE_NAME = "blacksouls_avatar.txt";

    public static String currentSkill = "";

    private static String currentAvatar = DEFAULT_AVATAR;
    private static boolean isLoaded = false;
    private static int currentAvatarExpression = 0;

    private static final Set<String> UNLOCKED_DLC_AVATARS = new HashSet<>();

    private ClientSkillInfo() {
    }

    public static String getAvatar() {
        ensureLoaded();
        return currentAvatar;
    }

    public static void setAvatar(String avatar) {
        currentAvatar = sanitizeAvatar(avatar);
        isLoaded = true;
        saveAvatar();
    }

    public static void setUnlockedDlcAvatars(Collection<String> avatars) {
        UNLOCKED_DLC_AVATARS.clear();

        if (avatars != null) {
            UNLOCKED_DLC_AVATARS.addAll(avatars);
        }
    }

    public static boolean isDlcAvatarUnlocked(String avatarId) {
        return avatarId != null && UNLOCKED_DLC_AVATARS.contains(avatarId);
    }

    public static int getAvatarExpression() {
        return currentAvatarExpression;
    }

    public static void setAvatarExpression(int expression) {
        currentAvatarExpression = Mth.clamp(expression, 0, 7);
    }

    private static void ensureLoaded() {
        if (isLoaded) {
            return;
        }

        loadAvatar();
        isLoaded = true;
    }

    private static void saveAvatar() {
        try {
            Files.writeString(getAvatarFilePath(), currentAvatar + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Failed to save client avatar selection: {}", currentAvatar, exception);
        }
    }

    private static void loadAvatar() {
        Path path = getAvatarFilePath();

        if (!Files.isRegularFile(path)) {
            currentAvatar = DEFAULT_AVATAR;
            return;
        }

        try {
            String avatar = Files.readString(path, StandardCharsets.UTF_8).trim();
            currentAvatar = sanitizeAvatar(avatar);
        } catch (IOException exception) {
            currentAvatar = DEFAULT_AVATAR;
            LOGGER.warn("Failed to load client avatar selection. Using default avatar: {}", DEFAULT_AVATAR, exception);
        }
    }

    private static Path getAvatarFilePath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(AVATAR_FILE_NAME);
    }

    private static String sanitizeAvatar(String avatar) {
        if (avatar == null || avatar.trim().isEmpty()) {
            return DEFAULT_AVATAR;
        }

        return avatar.trim();
    }
}