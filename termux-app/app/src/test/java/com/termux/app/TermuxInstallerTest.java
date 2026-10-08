package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TermuxInstallerTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File createScript(String relativePath) throws Exception {
        File script = new File(temporaryFolder.getRoot(), relativePath);
        Files.createDirectories(script.getParentFile().toPath());
        Files.createFile(script.toPath());
        return script;
    }

    @Test public void selectsCurrentSecondStageBeforeLegacyScript() throws Exception {
        File legacy = createScript("etc/termux/bootstrap/termux-bootstrap-second-stage.sh");
        assertEquals(legacy, TermuxInstaller.findBootstrapSecondStageFile(temporaryFolder.getRoot()));

        File current = createScript("etc/termux/termux-bootstrap/second-stage/termux-bootstrap-second-stage.sh");
        assertEquals(current, TermuxInstaller.findBootstrapSecondStageFile(temporaryFolder.getRoot()));
    }

    @Test public void doesNotSelectMissingOrDirectoryPath() throws Exception {
        assertNull(TermuxInstaller.findBootstrapSecondStageFile(temporaryFolder.getRoot()));
        Files.createDirectories(new File(temporaryFolder.getRoot(),
            "etc/termux/termux-bootstrap/second-stage/termux-bootstrap-second-stage.sh").toPath());
        assertNull(TermuxInstaller.findBootstrapSecondStageFile(temporaryFolder.getRoot()));
    }
}
