package com.termux.app.terminal;

import java.util.ArrayList;
import java.util.List;

/** Value parsing only; no historical command is ever replayed. */
public final class MobileRecovery {
    private MobileRecovery() {}
    public static int classifyExit(int reason, String description) {
        // API 37 reports OTHER + description; newer SDK extensions have reason 17.
        if (reason == 17 || (reason == 13 && description != null && description.contains("MemoryLimiter"))) return 17;
        return reason >= 0 && reason <= 17 ? reason : 0;
    }
    public static boolean validTmuxId(String id) { return id != null && id.matches("\\$[0-9]{1,10}"); }
    public static final class TmuxSession {
        public final String id, name;
        TmuxSession(String id, String name) { this.id = id; this.name = name; }
    }
    public static List<TmuxSession> parseTmuxSessions(String output) {
        List<TmuxSession> sessions = new ArrayList<>();
        for (String line : output.split("\\n")) {
            int tab = line.indexOf('\t');
            if (tab < 0 || !validTmuxId(line.substring(0, tab))) continue;
            sessions.add(new TmuxSession(line.substring(0, tab), line.substring(tab + 1,
                Math.min(line.length(), tab + 81)).replaceAll("[\\p{Cntrl}]", " ")));
            if (sessions.size() == 32) break;
        }
        return sessions;
    }
    public static String boundedCheckpoint(String text) {
        if (text == null) return "";
        int end = Math.min(text.length(), 25000);
        if (end > 0 && end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end);
    }
}
