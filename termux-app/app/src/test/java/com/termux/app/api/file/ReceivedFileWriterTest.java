package com.termux.app.api.file;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;

@RunWith(RobolectricTestRunner.class)
public class ReceivedFileWriterTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void rejectsPathNamesFromSharedMetadata() throws Exception {
        File downloads = temporaryFolder.newFolder("downloads");
        for (String name : new String[]{"../.termux/termux.properties", "/tmp/termux.properties",
            "..\\termux.properties", ".", "..", "", " ", "bad\nname", "bad" + (char) 0 + "name"}) {
            assertFalse(name, ReceivedFileWriter.isSafeFileName(name));
            try {
                ReceivedFileWriter.save(downloads, name, text("unsafe"));
                fail("Accepted unsafe file name: " + name);
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("File name"));
            }
        }
        assertTrue(ReceivedFileWriter.isSafeFileName("résumé 📁.txt"));
    }

    @Test public void savesUnicodeNameWithoutReplacingExistingFile() throws Exception {
        File downloads = temporaryFolder.newFolder("downloads");
        File existing = new File(downloads, "résumé.txt");
        Files.write(existing.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        File saved = ReceivedFileWriter.save(downloads, "résumé.txt", text("incoming"));

        assertEquals(downloads.getCanonicalFile(), saved.getCanonicalFile().getParentFile());
        assertEquals("résumé (1).txt", saved.getName());
        assertArrayEquals("original".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(existing.toPath()));
        assertArrayEquals("incoming".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(saved.toPath()));
    }

    @Test public void rejectsExistingSymlink() throws Exception {
        File downloads = temporaryFolder.newFolder("downloads");
        File outside = temporaryFolder.newFile("outside.txt");
        Files.write(outside.toPath(), "original".getBytes(StandardCharsets.UTF_8));
        File link = new File(downloads, "incoming.txt");
        try {
            Files.createSymbolicLink(link.toPath(), outside.toPath());
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            assumeNoException(unavailable);
        }

        try {
            ReceivedFileWriter.save(downloads, link.getName(), text("overwrite"));
            fail("Followed a symlink in downloads");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("outside") || expected.getMessage().contains("symbolic link"));
        }
        assertArrayEquals("original".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(outside.toPath()));
    }

    @Test public void removesIncompleteFileAfterReadFailure() throws Exception {
        File downloads = temporaryFolder.newFolder("downloads");
        InputStream broken = new InputStream() {
            private boolean first = true;
            @Override public int read() throws IOException {
                if (first) { first = false; return 'x'; }
                throw new IOException("source failed");
            }
        };

        try {
            ReceivedFileWriter.save(downloads, "incomplete.txt", broken);
            fail("Saved incomplete content");
        } catch (IOException expected) {
            assertEquals("source failed", expected.getMessage());
        }
        assertFalse(new File(downloads, "incomplete.txt").exists());
    }

    private static InputStream text(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
