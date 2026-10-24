package dev.wsldocker.ui.image;

import dev.wsldocker.model.ImageInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageArchiveNamesTest {
    @Test
    void usesSanitizedReferenceForSingleImage() {
        ImageInfo image = new ImageInfo("abc", "gvenzl/oracle-xe", "21-slim", "2.58GB", "", "");
        assertEquals("gvenzl_oracle-xe_21-slim.tar", ImageArchiveNames.defaultFileName(List.of(image)));
    }

    @Test
    void usesGenericNameForMultipleImages() {
        ImageInfo first = new ImageInfo("a", "nginx", "latest", "10MB", "", "");
        ImageInfo second = new ImageInfo("b", "alpine", "latest", "8MB", "", "");
        assertEquals("docker-images.tar", ImageArchiveNames.defaultFileName(List.of(first, second)));
    }

    @Test
    void keepsArchiveExtension() {
        assertEquals("app.tar", ImageArchiveNames.ensureTar("app"));
        assertEquals("app.tar.gz", ImageArchiveNames.ensureTar("app.tar.gz"));
    }
}
