package dev.wsldocker.service;

import dev.wsldocker.core.AppException;

import java.nio.file.Path;
import java.util.Locale;

public final class WindowsToWslPath {
    private WindowsToWslPath() {
    }

    public static String convert(Path windowsPath) {
        if (windowsPath == null) {
            throw new AppException("パスが指定されていません。");
        }
        Path absolute = windowsPath.toAbsolutePath().normalize();
        String raw = absolute.toString();
        if (raw.startsWith("\\\\") || raw.startsWith("//")) {
            throw new AppException("UNC パスは WSL に渡せません。ローカルディスク上のパスを指定してください。");
        }
        String slash = raw.replace('\\', '/');
        if (slash.length() >= 2 && slash.charAt(1) == ':') {
            char drive = Character.toLowerCase(slash.charAt(0));
            if (drive < 'a' || drive > 'z') {
                throw new AppException("ドライブ文字を解釈できません: " + raw);
            }
            String rest = slash.length() > 2 ? slash.substring(2) : "";
            if (!rest.startsWith("/")) {
                rest = "/" + rest;
            }
            return "/mnt/" + drive + rest;
        }
        if (slash.toLowerCase(Locale.ROOT).startsWith("/mnt/")) {
            return slash;
        }
        throw new AppException("WSL パスへ変換できません: " + raw);
    }
}
