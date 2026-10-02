package com.termux.app.terminal;

import java.util.ArrayList;
import java.util.List;

/** Bounded literal search; does not copy the entire scrollback or execute shell commands. */
public final class MobileTranscriptSearch {
    public static final int MAX_ROWS = 2000;
    public static final int MAX_CHARACTERS = 250000;
    public static final int MAX_MATCHES = 100;

    public interface Rows {
        String textAt(int row);
    }

    public static final class Match {
        public final int row;
        public final String text;

        Match(int row, String text) {
            this.row = row;
            this.text = text;
        }
    }

    private MobileTranscriptSearch() {}

    /** Newest visual rows first. Wrapped lines are searched independently. */
    public static List<Match> find(Rows rows, int oldestRow, int newestRow, String query) {
        List<Match> matches = new ArrayList<>();
        if (query == null || query.isEmpty() || query.length() > 256 || query.indexOf('\n') >= 0)
            return matches;
        int characters = 0;
        int count = 0;
        for (int row = newestRow; row >= oldestRow && count < MAX_ROWS; row--, count++) {
            String text = rows.textAt(row);
            if (text == null) continue;
            if (text.length() > MAX_CHARACTERS - characters) break;
            characters += text.length();
            for (int offset = 0; offset <= text.length() - query.length(); offset++) {
                if (text.regionMatches(true, offset, query, 0, query.length())) {
                    matches.add(new Match(row, text));
                    break;
                }
            }
            if (matches.size() == MAX_MATCHES) break;
        }
        return matches;
    }
}
