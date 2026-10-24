package dev.wsldocker.model;

public record ComposeProject(String name, String filePath, String projectDir) {
    public ComposeProject {
        name = name == null ? "" : name.trim();
        filePath = filePath == null ? "" : filePath.trim();
        projectDir = projectDir == null ? "" : projectDir.trim();
    }

    public static ComposeProject fromFile(String filePath) {
        String path = filePath == null ? "" : filePath.trim();
        return new ComposeProject(defaultName(path), path, "");
    }

    public ComposeProject withName(String newName) {
        return new ComposeProject(newName, filePath, projectDir);
    }

    public static String defaultName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "compose";
        }
        String slash = filePath.replace('\\', '/');
        while (slash.endsWith("/")) {
            slash = slash.substring(0, slash.length() - 1);
        }
        int last = slash.lastIndexOf('/');
        String leaf = last >= 0 ? slash.substring(last + 1) : slash;
        if (isComposeFileName(leaf)) {
            String parent = last > 0 ? slash.substring(0, last) : "";
            int parentSlash = parent.lastIndexOf('/');
            String folder = parentSlash >= 0 ? parent.substring(parentSlash + 1) : parent;
            return folder.isBlank() ? leaf : folder;
        }
        return leaf.isBlank() ? "compose" : leaf;
    }

    private static boolean isComposeFileName(String leaf) {
        String lower = leaf.toLowerCase();
        return lower.equals("docker-compose.yml")
                || lower.equals("docker-compose.yaml")
                || lower.equals("compose.yml")
                || lower.equals("compose.yaml");
    }
}
