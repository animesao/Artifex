package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkConfigReaderTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());

    @TempDir
    Path tempDir;

    @Test
    void readsValidSyntheticLink() throws Exception {
        File file = tempDir.resolve("links.yml").toFile();
        Files.writeString(file.toPath(), "100000000000000001: " + SYNTH + "\n");

        Map<String, UUID> links = LinkConfigReader.read(file);

        assertEquals(SYNTH, links.get("100000000000000001"));
    }

    @Test
    void skipsInvalidEntries() throws Exception {
        File file = tempDir.resolve("links.yml").toFile();
        Files.writeString(file.toPath(),
                "100000000000000001: " + SYNTH + "\n"
                        + "100000000000000002: not-a-uuid\n"
                        + "bad-id: " + SYNTH + "\n"
                        + "short: " + SYNTH + "\n");

        Map<String, UUID> links = LinkConfigReader.read(file);

        assertEquals(Map.of("100000000000000001", SYNTH), links);
    }

    @Test
    void missingFileReturnsEmptyMap() {
        assertTrue(LinkConfigReader.read(tempDir.resolve("absent.yml").toFile()).isEmpty());
    }
}
