package dev.wsldocker.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.wsldocker.core.AppException;

import java.util.ArrayList;
import java.util.List;

public final class DockerJson {
    private DockerJson() {
    }

    public static List<JsonObject> parseLines(String stdout) {
        List<JsonObject> objects = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return objects;
        }
        int lineNo = 0;
        for (String raw : stdout.split("\\R")) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || !line.startsWith("{")) {
                continue;
            }
            try {
                JsonElement element = JsonParser.parseString(line);
                if (element.isJsonObject()) {
                    objects.add(element.getAsJsonObject());
                }
            } catch (JsonSyntaxException e) {
                throw new AppException("Docker の JSON 行を解析できません (line " + lineNo + "):\n" + line, e);
            }
        }
        return objects;
    }

    public static List<JsonObject> parseArrayOrLines(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            return List.of();
        }
        String trimmed = stdout.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            try {
                JsonElement element = JsonParser.parseString(trimmed.substring(start, end + 1));
                if (element.isJsonArray()) {
                    List<JsonObject> objects = new ArrayList<>();
                    for (JsonElement item : element.getAsJsonArray()) {
                        if (item.isJsonObject()) {
                            objects.add(item.getAsJsonObject());
                        }
                    }
                    return objects;
                }
            } catch (JsonSyntaxException e) {
                throw new AppException("Docker の JSON 配列を解析できません。\n" + trimmed, e);
            }
        }
        return parseLines(stdout);
    }

    public static JsonObject parseObject(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            throw new AppException("Docker から空の応答が返りました。");
        }
        String trimmed = stdout.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AppException("Docker の JSON 応答を解析できません。\n" + trimmed);
        }
        try {
            return JsonParser.parseString(trimmed.substring(start, end + 1)).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new AppException("Docker の JSON 応答を解析できません。\n" + trimmed, e);
        }
    }

    public static String pretty(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(element);
        } catch (JsonSyntaxException e) {
            return json;
        }
    }

    public static String str(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                JsonElement element = object.get(key);
                if (element.isJsonPrimitive()) {
                    return element.getAsString();
                }
                return element.toString();
            }
        }
        return "";
    }

    public static int asInt(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (NumberFormatException | IllegalStateException e) {
            return 0;
        }
    }

    public static long asLong(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return 0L;
        }
        try {
            return object.get(key).getAsLong();
        } catch (NumberFormatException | IllegalStateException e) {
            return 0L;
        }
    }
}
