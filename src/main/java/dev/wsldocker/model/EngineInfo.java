package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

public record EngineInfo(
        String name,
        String serverVersion,
        String operatingSystem,
        String osType,
        String architecture,
        String driver,
        int cpus,
        long memoryBytes,
        int containers,
        int containersRunning,
        int images,
        String rawJson
) {
    public static EngineInfo from(JsonObject object, String rawJson) {
        return new EngineInfo(
                DockerJson.str(object, "Name"),
                DockerJson.str(object, "ServerVersion"),
                DockerJson.str(object, "OperatingSystem"),
                DockerJson.str(object, "OSType"),
                DockerJson.str(object, "Architecture"),
                DockerJson.str(object, "Driver"),
                DockerJson.asInt(object, "NCPU"),
                DockerJson.asLong(object, "MemTotal"),
                DockerJson.asInt(object, "Containers"),
                DockerJson.asInt(object, "ContainersRunning"),
                DockerJson.asInt(object, "Images"),
                rawJson
        );
    }

    public String summary() {
        return "Docker " + serverVersion + "  /  " + operatingSystem + "  /  "
                + containersRunning + " running  /  " + images + " images";
    }

    public String memoryLabel() {
        if (memoryBytes <= 0) {
            return "-";
        }
        double gib = memoryBytes / 1024.0 / 1024.0 / 1024.0;
        return String.format("%.1f GiB", gib);
    }
}
