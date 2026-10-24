package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

public record NetworkInfo(String id, String name, String driver, String scope) {
    public static NetworkInfo from(JsonObject object) {
        return new NetworkInfo(
                DockerJson.str(object, "ID", "Id"),
                DockerJson.str(object, "Name"),
                DockerJson.str(object, "Driver"),
                DockerJson.str(object, "Scope")
        );
    }

    public boolean builtIn() {
        return "bridge".equals(name) || "host".equals(name) || "none".equals(name);
    }
}
