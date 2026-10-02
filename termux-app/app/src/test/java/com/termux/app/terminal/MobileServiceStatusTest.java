package com.termux.app.terminal;
import org.junit.Test;
import static org.junit.Assert.*;
public class MobileServiceStatusTest {
    @Test public void keepsRuntimeAndStartupIndependent() {
        MobileServiceStatus running = MobileServiceStatus.parse("run: /usr/var/service/sshd: (pid 20082) 95s; run: log: (pid 7) 96s", false);
        assertEquals("running", running.state);
        assertEquals(20082, running.pid);
        assertFalse(running.startupEnabled);
        MobileServiceStatus stopped = MobileServiceStatus.parse("down: /usr/var/service/sshd: 12s; run: log: (pid 7) 96s", true);
        assertEquals("stopped", stopped.state);
        assertEquals(-1, stopped.pid);
        assertTrue(stopped.startupEnabled);
    }
    @Test public void unavailableSupervisorIsNotReportedAsStopped() {
        assertEquals("unavailable", MobileServiceStatus.parse("warning: unable to open supervise/ok", true).state);
        assertEquals(-1, MobileServiceStatus.parse("run: /x: (pid 99999999999999999999)", true).pid);
    }
    @Test public void serviceNamesCannotBecomeOptionsOrPaths() {
        assertTrue(MobileServiceStatus.isValidName("layla-relay"));
        assertTrue(MobileServiceStatus.isValidName("tx11-xfce4"));
        for (String name : new String[]{"-up", "../sshd", ".", "a/b", "sshd;id", "$(id)", "", "a\n"})
            assertFalse(name, MobileServiceStatus.isValidName(name));
        assertFalse(MobileServiceStatus.isValidName(null));
    }
}
