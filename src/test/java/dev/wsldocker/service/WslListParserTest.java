package dev.wsldocker.service;

import dev.wsldocker.model.WslDistro;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WslListParserTest {
    @Test
    void parsesDefaultUbuntu() {
        String text = """
                  NAME      STATE           VERSION
                * Ubuntu    Running         2
                """;
        List<WslDistro> distros = WslListParser.parse(text);
        assertEquals(1, distros.size());
        assertEquals("Ubuntu", distros.getFirst().name());
        assertEquals("Running", distros.getFirst().state());
        assertEquals("2", distros.getFirst().version());
        assertTrue(distros.getFirst().defaultDistro());
        assertTrue(distros.getFirst().running());
    }

    @Test
    void parsesMultipleDistrosAndIgnoresNulls() {
        String text = "\uFEFF  NAME                   STATE           VERSION\u0000\n"
                + "* Ubuntu                 Running         2\n"
                + "  docker-desktop         Running         2\n"
                + "  docker-desktop-data    Stopped         2\n";
        List<WslDistro> distros = WslListParser.parse(text);
        assertEquals(3, distros.size());
        assertEquals("Ubuntu", distros.get(0).name());
        assertTrue(distros.get(0).defaultDistro());
        assertEquals("docker-desktop", distros.get(1).name());
        assertEquals("Stopped", distros.get(2).state());
    }

    @Test
    void returnsEmptyForBlank() {
        assertTrue(WslListParser.parse("").isEmpty());
        assertTrue(WslListParser.parse("NAME STATE VERSION\n").isEmpty());
    }
}
