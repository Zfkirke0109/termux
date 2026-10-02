package com.termux.app.terminal;
import org.junit.Test;
import static org.junit.Assert.*;
public class MobileRecoveryTest {
    @Test public void distinguishesMemoryLimitsFromCrashesAndUpdates() {
        assertEquals(17, MobileRecovery.classifyExit(13, "MemoryLimiter:AnonSwap limit exceeded"));
        assertEquals(17, MobileRecovery.classifyExit(17, null));
        assertEquals(16, MobileRecovery.classifyExit(16, "package updated"));
        assertEquals(3, MobileRecovery.classifyExit(3, "low memory"));
        assertEquals(4, MobileRecovery.classifyExit(4, "MemoryLimiter text in a Java exception"));
        assertEquals(13, MobileRecovery.classifyExit(13, "Chimera killed empty process"));
        assertEquals(0, MobileRecovery.classifyExit(99, null));
    }
    @Test public void reconnectTargetsAreOnlyTmuxSessionIds() {
        assertTrue(MobileRecovery.validTmuxId("$12"));
        for (String id : new String[]{"-x", "server:1", "$1;id", "$1\n", "$(id)", "12", "$", "$12345678901"})
            assertFalse(id, MobileRecovery.validTmuxId(id));
        assertEquals(1, MobileRecovery.parseTmuxSessions("bad\tname\n$3\trelay\n$4;id\tevil").size());
        assertEquals("relay", MobileRecovery.parseTmuxSessions("$3\trelay").get(0).name);
    }
    @Test public void checkpointIsBoundedWithoutBreakingASurrogatePair() {
        String prefix = new String(new char[24999]).replace('\0', 'x');
        assertEquals(24999, MobileRecovery.boundedCheckpoint(prefix + "😀extra").length());
        assertEquals("", MobileRecovery.boundedCheckpoint(null));
        assertEquals("終端", MobileRecovery.boundedCheckpoint("終端"));
    }
}
