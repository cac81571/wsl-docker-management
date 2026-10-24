package dev.wsldocker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsToWslPathTest {
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void convertsDrivePath() {
        assertEquals("/mnt/c/Users/foo/docker-compose.yml",
                WindowsToWslPath.convert(Path.of("C:\\Users\\foo\\docker-compose.yml")));
        assertEquals("/mnt/d/work/app",
                WindowsToWslPath.convert(Path.of("D:\\work\\app")));
    }

    @Test
    void rejectsUnc() {
        assertThrows(RuntimeException.class, () -> WindowsToWslPath.convert(Path.of("\\\\server\\share\\file.yml")));
    }
}
