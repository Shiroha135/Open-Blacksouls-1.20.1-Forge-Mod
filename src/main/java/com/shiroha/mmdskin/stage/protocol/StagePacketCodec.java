package com.shiroha.mmdskin.stage.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class StagePacketCodec {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final String PREFIX = "S3:";

    private StagePacketCodec() {
    }

    public static String encode(StagePacket packet) {
        String json = GSON.toJson(packet);
        return PREFIX + ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static StagePacket decode(String raw) {
        if (!isStagePacket(raw)) {
            return null;
        }
        try {
            byte[] jsonBytes = DECODER.decode(raw.substring(PREFIX.length()));
            StagePacket packet = GSON.fromJson(new String(jsonBytes, StandardCharsets.UTF_8), StagePacket.class);
            if (packet == null || packet.version != 3 || packet.type == null) {
                return null;
            }
            if (packet.members == null) {
                packet.members = java.util.Collections.emptyList();
            }
            if (packet.motionFiles == null) {
                packet.motionFiles = java.util.Collections.emptyList();
            }
            return packet;
        } catch (Exception e) {
            LOGGER.warn("[舞台协议] 数据包解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isStagePacket(String raw) {
        return raw != null && raw.startsWith(PREFIX);
    }
}
