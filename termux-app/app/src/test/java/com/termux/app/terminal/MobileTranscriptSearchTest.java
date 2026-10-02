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
}
