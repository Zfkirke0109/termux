package com.termux.app.terminal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parse only the primary service status, not its logger's PID. */
public final class MobileServiceStatus {
    private static final Pattern PID = Pattern.compile("^run: .*?: \\(pid ([0-9]+)\\)");
    public final String state;
    public final int pid;
    public final boolean startupEnabled;
    private MobileServiceStatus(String state, int pid, boolean startupEnabled) {
        this.state = state; this.pid = pid; this.startupEnabled = startupEnabled;
    }
    public static boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,63}");
    }
    public static MobileServiceStatus parse(String output, boolean startupEnabled) {
        Matcher matcher = PID.matcher(output);
        if (matcher.find()) {
            try { return new MobileServiceStatus("running", Integer.parseInt(matcher.group(1)), startupEnabled); }
            catch (NumberFormatException ignored) {}
        }
        String state = output.startsWith("down:") ? "stopped" : "unavailable";
        return new MobileServiceStatus(state, -1, startupEnabled);
    }
}
