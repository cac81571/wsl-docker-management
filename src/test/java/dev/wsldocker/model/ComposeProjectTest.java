package dev.wsldocker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComposeProjectTest {
    @Test
    void usesParentFolderAsDefaultName() {
        assertEquals("oracle", ComposeProject.defaultName("/home/n-sug/oracle/docker-compose.yml"));
        assertEquals("app", ComposeProject.defaultName("C:\\work\\app\\compose.yaml"));
    }

    @Test
    void usesLeafWhenNotAComposeFileName() {
        assertEquals("stack.yml", ComposeProject.defaultName("/tmp/stack.yml"));
    }
}
