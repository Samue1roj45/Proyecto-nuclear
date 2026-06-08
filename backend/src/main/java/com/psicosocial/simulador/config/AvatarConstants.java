package com.psicosocial.simulador.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AvatarConstants {

    private static final String INITIALS_BASE =
            "https://api.dicebear.com/7.x/initials/svg?backgroundColor=c5cae9&seed=";

    private AvatarConstants() {}

    public static String initialsUrl(String fullName) {
        String seed = fullName == null || fullName.isBlank() ? "Usuario" : fullName.trim();
        return INITIALS_BASE + URLEncoder.encode(seed, StandardCharsets.UTF_8);
    }

    public static boolean hasCustomAvatar(String avatarUrl) {
        return avatarUrl != null && !avatarUrl.isBlank();
    }
}
