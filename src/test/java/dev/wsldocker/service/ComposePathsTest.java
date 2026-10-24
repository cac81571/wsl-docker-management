package dev.wsldocker.service;

import dev.wsldocker.model.ComposeProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposePathsTest {
    @Test
    void keepsWslPaths() {
        ComposePaths.Resolved resolved = ComposePaths.resolve(
                new ComposeProject("oracle", "/home/n-sug/oracle/docker-compose.yml", ""));
        assertEquals("/home/n-sug/oracle/docker-compose.yml", resolved.wslFile());
        assertEquals("/home/n-sug/oracle", resolved.wslDir());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void convertsWindowsPaths() {
        assertEquals("/mnt/c/work/app/docker-compose.yml",
                ComposePaths.toWsl("C:\\work\\app\\docker-compose.yml"));
    }

    @Test
    void detectsWindowsPath() {
        assertTrue(ComposePaths.isWindowsPath("D:/data/compose.yml"));
        assertFalse(ComposePaths.isWindowsPath("/home/user/compose.yml"));
    }
}
