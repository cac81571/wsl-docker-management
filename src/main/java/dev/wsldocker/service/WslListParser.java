package dev.wsldocker.service;

import dev.wsldocker.model.WslDistro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WslListParser {
    private static final Set<String> STATES = Set.of(
            "running", "stopped", "starting", "installing", "converting", "uninstalling"
    );

    private WslListParser() {
    }

    public static List<WslDistro> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\u0000", "").replace("\uFEFF", "");
        List<WslDistro> distros = new ArrayList<>();
        for (String raw : normalized.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || isHeader(line)) {
                continue;
            }
            boolean defaultDistro = line.startsWith("*");
            if (defaultDistro) {
                line = line.substring(1).trim();
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                continue;
            }

            String version = "";
            String state;
            String name;
            if (parts[parts.length - 1].matches("\\d+")) {
                version = parts[parts.length - 1];
                if (parts.length < 3) {
                    continue;
                }
                state = parts[parts.length - 2];
                name = String.join(" ", Arrays.copyOf(parts, parts.length - 2));
            } else {
                state = parts[parts.length - 1];
                name = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            }
            if (name.isBlank() || !STATES.contains(state.toLowerCase(Locale.ROOT))) {
                continue;
            }
            distros.add(new WslDistro(name, capitalize(state), version, defaultDistro));
        }
        return List.copyOf(distros);
    }

    private static boolean isHeader(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.contains("NAME") && (upper.contains("STATE") || upper.contains("VERSION"));
    }

    private static String capitalize(String state) {
        if (state.isEmpty()) {
            return state;
        }
        return Character.toUpperCase(state.charAt(0)) + state.substring(1).toLowerCase(Locale.ROOT);
    }
}
