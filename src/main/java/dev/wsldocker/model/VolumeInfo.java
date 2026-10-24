package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

public record VolumeInfo(String name, String driver, String mountpoint) {
    public static VolumeInfo from(JsonObject object) {
        return new VolumeInfo(
                DockerJson.str(object, "Name"),
                DockerJson.str(object, "Driver"),
                DockerJson.str(object, "Mountpoint")
        );
    }
}
