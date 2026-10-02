package com.termux.app.terminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

/** An opt-in debug interval, checked both in-process and after a process restart/reboot. */
public final class MobileDebugLogging {
    public static final long DURATION_MS = 15 * 60 * 1000;
    private static final String PREFS = "mobile_debug_logging";
    private static final Handler TIMER = new Handler(Looper.getMainLooper());
    private static Runnable expiry;
    private MobileDebugLogging() {}

    public static void start(Context context) {
        Context app = context.getApplicationContext();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(app);
        if (preferences == null) return;
        cancel(app);
        preferences.setTerminalViewKeyLoggingEnabled(false);
        preferences.setLogLevel(null, Logger.LOG_LEVEL_DEBUG);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("start_wall", System.currentTimeMillis()).putLong("start_elapsed", SystemClock.elapsedRealtime())
            .putBoolean("active", true).apply();
        check(app);
    }
    public static void normal(Context context) {
        cancel(context);
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        if (preferences != null) {
            preferences.setLogLevel(null, Logger.LOG_LEVEL_NORMAL);
            preferences.setTerminalViewKeyLoggingEnabled(false);
        }
    }
    public static void cancel(Context context) {
        if (expiry != null) TIMER.removeCallbacks(expiry);
        expiry = null;
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
    public static boolean isActive(Context context) {
        check(context);
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("active", false);
    }
    public static void check(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences timer = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!timer.getBoolean("active", false)) return;
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(app);
        if (preferences == null) return;
        if (preferences.getLogLevel() != Logger.LOG_LEVEL_DEBUG) { cancel(app); return; }
        long wall = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        long startWall = timer.getLong("start_wall", 0), startElapsed = timer.getLong("start_elapsed", 0);
        if (MobileDebugPolicy.hasExpired(wall, elapsed, startWall, startElapsed, DURATION_MS)) { normal(app); return; }
        if (expiry != null) TIMER.removeCallbacks(expiry);
        long remaining = Math.min(DURATION_MS - (elapsed - startElapsed), DURATION_MS - (wall - startWall));
        expiry = () -> check(app);
        TIMER.postDelayed(expiry, remaining);
    }
}
