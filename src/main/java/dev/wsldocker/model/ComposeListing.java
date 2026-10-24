package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

import java.util.Arrays;
import java.util.List;

public record ComposeListing(String name, String status, String configFiles) {
    public static ComposeListing from(JsonObject object) {
        return new ComposeListing(
                DockerJson.str(object, "Name"),
                DockerJson.str(object, "Status"),
                DockerJson.str(object, "ConfigFiles")
        );
    }

    public List<String> files() {
        if (configFiles == null || configFiles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configFiles.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
