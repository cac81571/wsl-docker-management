package dev.wsldocker.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRunnerDecodeTest {
    @Test
    void decodesUtf16LeWslOutput() {
        String original = "  NAME      STATE           VERSION\r\n* Ubuntu    Running         2\r\n";
        byte[] utf16 = original.getBytes(StandardCharsets.UTF_16LE);
        String decoded = ProcessRunner.decode(utf16);
        assertTrue(decoded.contains("Ubuntu"));
        assertTrue(decoded.contains("Running"));
    }
}
