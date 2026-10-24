package dev.wsldocker.core;

import dev.wsldocker.model.ComposeProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsComposeProjects() {
        Path file = tempDir.resolve("settings.properties");
        SettingsStore store = new SettingsStore(file);
        store.setComposeProjects(List.of(
                new ComposeProject("oracle", "/home/n-sug/oracle/docker-compose.yml", ""),
                new ComposeProject("oracle2", "/home/n-sug/oracle2/docker-compose.yml", "")
        ));

        SettingsStore loaded = new SettingsStore(file);
        List<ComposeProject> projects = loaded.composeProjects();
        assertEquals(2, projects.size());
        assertEquals("oracle", projects.getFirst().name());
        assertEquals("/home/n-sug/oracle/docker-compose.yml", projects.getFirst().filePath());
        assertEquals("oracle2", projects.get(1).name());
    }
}
