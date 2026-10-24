package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

public record ContainerInfo(
        String id,
        String name,
        String image,
        String state,
        String status,
        String ports,
        String createdAt,
        String command,
        String networks
) {
    public static ContainerInfo from(JsonObject object) {
        return new ContainerInfo(
                DockerJson.str(object, "ID", "Id"),
                DockerJson.str(object, "Names", "Name"),
                DockerJson.str(object, "Image"),
                DockerJson.str(object, "State").toLowerCase(),
                DockerJson.str(object, "Status"),
                DockerJson.str(object, "Ports"),
                DockerJson.str(object, "CreatedAt", "Created"),
                DockerJson.str(object, "Command"),
                DockerJson.str(object, "Networks")
        );
    }

    public boolean running() {
        return "running".equals(state);
    }

    public boolean startable() {
        return !running() && !"restarting".equals(state);
    }

    public boolean stoppable() {
        return running() || "paused".equals(state);
    }
}
