package dev.wsldocker.model;

public record WslDistro(String name, String state, String version, boolean defaultDistro) {
    public boolean running() {
        return "Running".equalsIgnoreCase(state);
    }

    public String label() {
        String marker = defaultDistro ? " *" : "";
        return name + "  (" + state + ", v" + version + ")" + marker;
    }
}
