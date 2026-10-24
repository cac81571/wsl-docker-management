package dev.wsldocker.model;

import com.google.gson.JsonObject;
import dev.wsldocker.service.DockerJson;

public record ImageInfo(
        String id,
        String repository,
        String tag,
        String size,
        String createdSince,
        String createdAt
) {
    public static ImageInfo from(JsonObject object) {
        return new ImageInfo(
                DockerJson.str(object, "ID", "Id"),
                DockerJson.str(object, "Repository"),
                DockerJson.str(object, "Tag"),
                DockerJson.str(object, "Size"),
                DockerJson.str(object, "CreatedSince"),
                DockerJson.str(object, "CreatedAt")
        );
    }

    public String reference() {
        if (repository.isBlank() || "<none>".equals(repository)) {
            return id;
        }
        if (tag.isBlank() || "<none>".equals(tag)) {
            return repository;
        }
        return repository + ":" + tag;
    }
}
