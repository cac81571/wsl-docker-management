package dev.wsldocker.ui.image;

import dev.wsldocker.model.ImageInfo;

import java.util.List;
import java.util.Locale;

public final class ImageArchiveNames {
    private ImageArchiveNames() {
    }

    public static String defaultFileName(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) {
            return "docker-images.tar";
        }
        if (images.size() == 1) {
            return ensureTar(sanitize(images.getFirst().reference()));
        }
        return "docker-images.tar";
    }

    public static String ensureTar(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        if (name.isEmpty()) {
            return "docker-images.tar";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".tar") || lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            return name;
        }
        return name + ".tar";
    }

    public static String sanitize(String reference) {
        if (reference == null || reference.isBlank()) {
            return "image";
        }
        return reference.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
