package com.termux.app.terminal;
import org.junit.Test;
import static org.junit.Assert.*;
public class MobileDebugPolicyTest {
    private static final long WALL = 10000000, ELAPSED = 2000000, DURATION = 900000;
    @Test public void expiresAtTheDeadline() {
        assertFalse(MobileDebugPolicy.hasExpired(WALL + DURATION - 1, ELAPSED + DURATION - 1, WALL, ELAPSED, DURATION));
        assertTrue(MobileDebugPolicy.hasExpired(WALL + DURATION, ELAPSED + DURATION, WALL, ELAPSED, DURATION));
    }
    @Test public void rebootAndClockChangesCannotExtendDebugging() {
        assertTrue(MobileDebugPolicy.hasExpired(WALL + 100000, 1000, WALL, ELAPSED, DURATION));
        assertTrue(MobileDebugPolicy.hasExpired(WALL - 100, ELAPSED + 100, WALL, ELAPSED, DURATION));
        assertTrue(MobileDebugPolicy.hasExpired(WALL + 100000, ELAPSED + 100, WALL, ELAPSED, DURATION));
        assertTrue(MobileDebugPolicy.hasExpired(WALL, ELAPSED, WALL, ELAPSED, 0));
    }
    @Test public void smallClockCorrectionsKeepTheOriginalElapsedDeadline() {
        assertFalse(MobileDebugPolicy.hasExpired(WALL + 1001, ELAPSED + 1000, WALL, ELAPSED, DURATION));
    }
}
