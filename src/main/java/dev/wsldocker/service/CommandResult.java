package dev.wsldocker.service;

import java.util.List;

public record CommandResult(int exitCode, String stdout, String stderr, List<String> command) {
    public boolean ok() {
        return exitCode == 0;
    }

    public String combinedError() {
        if (stderr != null && !stderr.isBlank()) {
            return stderr.trim();
        }
        return stdout == null ? "" : stdout.trim();
    }

    public String commandLine() {
        return String.join(" ", command);
    }
}
