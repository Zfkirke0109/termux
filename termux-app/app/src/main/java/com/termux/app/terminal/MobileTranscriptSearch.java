package com.termux.app.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded literal search over logical lines, with visual positions for highlighting. */
public final class MobileTranscriptSearch {
    public static final int MAX_ROWS = 2000;
    public static final int MAX_CHARACTERS = 250000;
    public static final int MAX_MATCHES = 100;

    public interface Rows {
        String textAt(int row);
        default boolean wrapsAt(int row) { return false; }
    }

    private static final class Row {
        final int number;
        final String text;
        final boolean wraps;
        Row(int number, String text, boolean wraps) {
            this.number = number;
            this.text = text;
            this.wraps = wraps;
        }
    }

    public static final class Match {
        public final int row, endRow, startIndex, endIndex;
        public final String text;
        private final List<Row> snapshot;

        private Match(List<Row> line, int start, int end, String text) {
            int base = 0, startRow = 0, endRowIndex = 0, first = 0, last = 0;
            for (int i = 0; i < line.size(); i++) {
                Row r = line.get(i);
                if (start >= base && start < base + r.text.length()) {
                    startRow = i;
                    first = start - base;
                }
                if (end > base && end <= base + r.text.length()) {
                    endRowIndex = i;
                    last = end - base;
                    break;
                }
                base += r.text.length();
            }
            row = line.get(startRow).number;
            endRow = line.get(endRowIndex).number;
            startIndex = first;
            endIndex = last;
            this.text = text;
            snapshot = new ArrayList<>(line.subList(startRow, endRowIndex + 1));
        }

        /** Never jump using positions from a changed or evicted transcript. */
        public boolean isCurrent(Rows rows, int oldest, int newest) {
            if (row < oldest || endRow > newest) return false;
            for (Row r : snapshot) {
                if (!r.text.equals(rows.textAt(r.number)) || r.wraps != rows.wrapsAt(r.number)) return false;
            }
            return true;
        }
    }

    private MobileTranscriptSearch() {}

    /** Newest occurrences first. Soft wraps join; hard newlines never join. */
    public static List<Match> find(Rows rows, int oldestRow, int newestRow, String query) {
        List<Match> matches = new ArrayList<>();
        if (query == null || query.isEmpty() || query.length() > 256
            || query.indexOf('\n') >= 0 || query.indexOf('\r') >= 0) return matches;
        List<Row> recent = new ArrayList<>();
        int characters = 0;
        for (int row = newestRow, count = 0; row >= oldestRow && count < MAX_ROWS; row--, count++) {
            String text = rows.textAt(row);
            if (text == null) text = "";
            if (text.length() > MAX_CHARACTERS - characters) break;
            characters += text.length();
            recent.add(new Row(row, text, rows.wrapsAt(row)));
            if (row == Integer.MIN_VALUE) break;
        }
        Collections.reverse(recent);
        int end = recent.size();
        while (end > 0 && matches.size() < MAX_MATCHES) {
            int start = end - 1;
            while (start > 0 && recent.get(start - 1).wraps) start--;
            List<Row> line = recent.subList(start, end);
            StringBuilder builder = new StringBuilder();
            for (Row r : line) builder.append(r.text);
            String text = builder.toString();
            for (int offset = text.length() - query.length(); offset >= 0; offset--) {
                if (text.regionMatches(true, offset, query, 0, query.length())) {
                    matches.add(new Match(line, offset, offset + query.length(),
                        text.substring(Math.max(0, offset - 40), Math.min(text.length(), offset + query.length() + 80))));
                    if (matches.size() == MAX_MATCHES) break;
                }
            }
            end = start;
        }
        return matches;
    }
}
