package dev.wsldocker.service;

import com.google.gson.JsonObject;
import dev.wsldocker.model.ContainerInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerJsonTest {
    @Test
    void parsesDockerPsLineWithObjectPlatform() {
        String json = """
                {"Command":"\\"nginx\\"","CreatedAt":"2026-10-24 17:24:45 +0900 JST","ID":"bbdbd26d949e","Image":"gvenzl/oracle-xe:21-slim","Names":"oracle-xe","Networks":"oracle_default","Platform":{"architecture":"amd64","os":"linux"},"Ports":"0.0.0.0:1521->1521/tcp","State":"running","Status":"Up 5 hours"}
                """;
        List<JsonObject> objects = DockerJson.parseLines(json);
        assertEquals(1, objects.size());
        ContainerInfo container = ContainerInfo.from(objects.getFirst());
        assertEquals("oracle-xe", container.name());
        assertEquals("bbdbd26d949e", container.id());
        assertEquals("running", container.state());
        assertTrue(container.running());
        assertEquals("0.0.0.0:1521->1521/tcp", container.ports());
    }

    @Test
    void skipsWarningsAndBlankLines() {
        String stdout = """
                WARNING: This output is designed for human readability
                {"ID":"abc","Names":"web","Image":"nginx","State":"exited","Status":"Exited","Ports":"","CreatedAt":"now","Command":"nginx","Networks":"bridge"}
                """;
        assertEquals(1, DockerJson.parseLines(stdout).size());
        assertEquals("web", ContainerInfo.from(DockerJson.parseLines(stdout).getFirst()).name());
    }

    @Test
    void parsesComposeLsArray() {
        String json = """
                [{"Name":"oracle","Status":"running(1)","ConfigFiles":"/home/n-sug/oracle/docker-compose.yml"},{"Name":"oracle2","Status":"created(1)","ConfigFiles":"/home/n-sug/oracle2/docker-compose.yml"}]
                """;
        assertEquals(2, DockerJson.parseArrayOrLines(json).size());
        assertEquals("oracle", DockerJson.str(DockerJson.parseArrayOrLines(json).getFirst(), "Name"));
    }

    @Test
    void readsNestedOrMissingFields() {
        JsonObject object = DockerJson.parseObject("prefix {\"Name\":\"host\",\"NCPU\":8,\"MemTotal\":17179869184} suffix");
        assertEquals("host", DockerJson.str(object, "Name"));
        assertEquals(8, DockerJson.asInt(object, "NCPU"));
        assertEquals(17179869184L, DockerJson.asLong(object, "MemTotal"));
        assertEquals("", DockerJson.str(object, "Missing"));
    }
}
