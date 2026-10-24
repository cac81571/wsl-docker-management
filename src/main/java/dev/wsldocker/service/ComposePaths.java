package dev.wsldocker.service;

import dev.wsldocker.core.AppException;
import dev.wsldocker.model.ComposeProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ComposePaths {
    public record Resolved(String wslFile, String wslDir) {
    }

    private ComposePaths() {
    }

    public static Resolved resolve(ComposeProject project) {
        return resolve(project.filePath(), project.projectDir());
    }

    public static Resolved resolve(String filePath, String projectDir) {
        if (filePath == null || filePath.isBlank()) {
            throw new AppException("Compose ファイルを指定してください。");
        }
        validateWindowsFile(filePath);
        String wslFile = toWsl(filePath);
        String wslDir;
        if (projectDir != null && !projectDir.isBlank()) {
            wslDir = toWsl(projectDir);
        } else {
            int slash = wslFile.lastIndexOf('/');
            wslDir = slash > 0 ? wslFile.substring(0, slash) : wslFile;
        }
        return new Resolved(wslFile, wslDir);
    }

    public static String toWsl(String path) {
        String trimmed = path.trim();
        if (isWindowsPath(trimmed)) {
            return WindowsToWslPath.convert(Path.of(trimmed));
        }
        return trimmed.replace('\\', '/');
    }

    public static String normalize(String path) {
        return toWsl(path).replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    public static boolean isWindowsPath(String path) {
        if (path == null) {
            return false;
        }
        String trimmed = path.trim();
        return trimmed.matches("^[A-Za-z]:[\\\\/].*") || trimmed.startsWith("\\\\") || trimmed.startsWith("//");
    }

    public static void validateWindowsFile(String filePath) {
        if (isWindowsPath(filePath) && !Files.isRegularFile(Path.of(filePath.trim()))) {
            throw new AppException("Compose ファイルが見つかりません: " + filePath);
        }
    }
}
