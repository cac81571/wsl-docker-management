package dev.wsldocker.core;

import dev.wsldocker.model.ComposeProject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class SettingsStore {
    private static final String DISTRO = "distro";
    private static final String DARK_THEME = "theme.dark";
    private static final String COMPOSE_COUNT = "compose.count";

    private final Path file;
    private final Properties properties = new Properties();

    public SettingsStore() {
        this(Path.of(System.getProperty("user.home"), ".wsl-docker-management", "settings.properties"));
    }

    SettingsStore(Path file) {
        this.file = file;
        load();
    }

    public String distro() {
        return properties.getProperty(DISTRO, "");
    }

    public void setDistro(String distro) {
        properties.setProperty(DISTRO, distro == null ? "" : distro);
        save();
    }

    public boolean darkTheme() {
        return Boolean.parseBoolean(properties.getProperty(DARK_THEME, "true"));
    }

    public void setDarkTheme(boolean dark) {
        properties.setProperty(DARK_THEME, Boolean.toString(dark));
        save();
    }

    public List<ComposeProject> composeProjects() {
        int count;
        try {
            count = Integer.parseInt(properties.getProperty(COMPOSE_COUNT, "0"));
        } catch (NumberFormatException e) {
            return List.of();
        }
        List<ComposeProject> projects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String file = properties.getProperty("compose." + i + ".file", "");
            if (file.isBlank()) {
                continue;
            }
            String name = properties.getProperty("compose." + i + ".name", "");
            String dir = properties.getProperty("compose." + i + ".dir", "");
            projects.add(new ComposeProject(
                    name.isBlank() ? ComposeProject.defaultName(file) : name,
                    file,
                    dir
            ));
        }
        return List.copyOf(projects);
    }

    public void setComposeProjects(List<ComposeProject> projects) {
        List<String> stale = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("compose.")) {
                stale.add(key);
            }
        }
        stale.forEach(properties::remove);
        List<ComposeProject> safe = projects == null ? List.of() : projects;
        properties.setProperty(COMPOSE_COUNT, Integer.toString(safe.size()));
        for (int i = 0; i < safe.size(); i++) {
            ComposeProject project = safe.get(i);
            properties.setProperty("compose." + i + ".name", project.name());
            properties.setProperty("compose." + i + ".file", project.filePath());
            properties.setProperty("compose." + i + ".dir", project.projectDir());
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ignored) {
            // 設定が読めなくても起動は続ける
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "WSL Docker Management");
            }
        } catch (IOException ignored) {
            // 設定保存失敗は機能を止めない
        }
    }
}
