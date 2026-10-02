package com.termux.app.terminal;

/** Pure clock policy; changing a device clock or rebooting cannot prolong debug logging. */
public final class MobileDebugPolicy {
    private MobileDebugPolicy() {}
    public static boolean hasExpired(long nowWall, long nowElapsed, long startWall, long startElapsed, long duration) {
        long bootDelta = (nowWall - nowElapsed) - (startWall - startElapsed);
        return duration <= 0 || nowElapsed < startElapsed || nowWall < startWall
            || Math.abs(bootDelta) > 5000
            || nowElapsed - startElapsed >= duration || nowWall - startWall >= duration;
    }
}
