package com.termux.app.terminal;

import org.junit.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class MobileTranscriptSearchTest {
    @Test public void searchesNewestFirstIgnoringCaseWithLiteralCharacters() {
        List<MobileTranscriptSearch.Match> found = MobileTranscriptSearch.find(
            row -> row == -1 ? "Error [a.*]" : "older error [a.*]", -2, -1, "ERROR [a.*]");
        assertEquals(2, found.size());
        assertEquals(-1, found.get(0).row);
        assertEquals(-2, found.get(1).row);
    }

    @Test public void rejectsEmptyAndMultilineQueriesWithoutReadingBuffer() {
        MobileTranscriptSearch.Rows rows = row -> { fail("must not read"); return null; };
        assertTrue(MobileTranscriptSearch.find(rows, -3, 0, "").isEmpty());
        assertTrue(MobileTranscriptSearch.find(rows, -3, 0, null).isEmpty());
        assertTrue(MobileTranscriptSearch.find(rows, -3, 0, "a\nb").isEmpty());
        assertTrue(MobileTranscriptSearch.find(rows, -3, 0, new String(new char[257])).isEmpty());
    }

    @Test public void boundsScannedRows() {
        AtomicInteger count = new AtomicInteger();
        assertTrue(MobileTranscriptSearch.find(row -> { count.incrementAndGet(); return "no match"; },
            -100000, 0, "missing").isEmpty());
        assertEquals(MobileTranscriptSearch.MAX_ROWS, count.get());
    }

    @Test public void boundsCharactersAndResults() {
        String longRow = new String(new char[10000]).replace('\0', 'x');
        AtomicInteger scanned = new AtomicInteger();
        assertTrue(MobileTranscriptSearch.find(row -> { scanned.incrementAndGet(); return longRow; },
            -100, 0, "missing").isEmpty());
        assertEquals(MobileTranscriptSearch.MAX_CHARACTERS / longRow.length() + 1, scanned.get());
        assertEquals(MobileTranscriptSearch.MAX_MATCHES,
            MobileTranscriptSearch.find(row -> "match", -1000, 0, "match").size());
    }

    @Test public void handlesUnicodeAndMissingRows() {
        assertEquals(1, MobileTranscriptSearch.find(row -> row == 0 ? "終端 error" : null,
            -1, 0, "終端").size());
        assertTrue(MobileTranscriptSearch.find(row -> "a", 1, 0, "a").isEmpty());
    }
    private static MobileTranscriptSearch.Rows wrapped(String[] text, boolean[] wraps) {
        return new MobileTranscriptSearch.Rows() {
            public String textAt(int row) { return text[row]; }
            public boolean wrapsAt(int row) { return wraps[row]; }
        };
    }

    @Test public void findsTextAcrossSoftWrapsWithVisualPositions() {
        MobileTranscriptSearch.Rows rows = wrapped(new String[]{"prefix ER", "ROR tail"}, new boolean[]{true, false});
        MobileTranscriptSearch.Match match = MobileTranscriptSearch.find(rows, 0, 1, "error").get(0);
        assertEquals(0, match.row);
        assertEquals(1, match.endRow);
        assertEquals(7, match.startIndex);
        assertEquals(3, match.endIndex);
        assertTrue(match.isCurrent(rows, 0, 1));
    }

    @Test public void doesNotJoinHardNewlinesOrLoseWrappedSpaces() {
        assertTrue(MobileTranscriptSearch.find(wrapped(new String[]{"ER", "ROR"},
            new boolean[]{false, false}), 0, 1, "error").isEmpty());
        assertEquals(1, MobileTranscriptSearch.find(wrapped(new String[]{"hello ", "world"},
            new boolean[]{true, false}), 0, 1, "hello world").size());
    }

    @Test public void returnsMultipleOccurrencesNewestFirst() {
        List<MobileTranscriptSearch.Match> matches = MobileTranscriptSearch.find(row -> "error error", 0, 0, "error");
        assertEquals(2, matches.size());
        assertEquals(6, matches.get(0).startIndex);
        assertEquals(0, matches.get(1).startIndex);
    }

    @Test public void rejectsEvictedEditedAndReflowedMatches() {
        String[] text = {"ER", "ROR"};
        boolean[] wraps = {true, false};
        MobileTranscriptSearch.Rows rows = wrapped(text, wraps);
        MobileTranscriptSearch.Match match = MobileTranscriptSearch.find(rows, 0, 1, "error").get(0);
        assertFalse(match.isCurrent(rows, 1, 1));
        text[1] = "RXX";
        assertFalse(match.isCurrent(rows, 0, 1));
        text[1] = "ROR";
        wraps[0] = false;
        assertFalse(match.isCurrent(rows, 0, 1));
    }

    @Test public void boundsOneVeryLongWrappedLineAndResultCount() {
        String[] rows = new String[2100];
        boolean[] wraps = new boolean[rows.length];
        java.util.Arrays.fill(rows, "aaaa");
        java.util.Arrays.fill(wraps, true);
        List<MobileTranscriptSearch.Match> matches = MobileTranscriptSearch.find(wrapped(rows, wraps), 0, 2099, "aa");
        assertEquals(MobileTranscriptSearch.MAX_MATCHES, matches.size());
        assertEquals(2099, matches.get(0).row);
        assertTrue(matches.get(0).isCurrent(wrapped(rows, wraps), 0, 2099));
    }
}
