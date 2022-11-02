package com.cleo.labs.cidr;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class TestRangePattern {

    @Test
    public void testMaxMin() {
        String range = RangePattern.pattern(10,0);
        assertEquals("", range);
    }

    @Test
    public void testSingle() {
        for (int v = -10; v < 11; v++) {
            String range = RangePattern.pattern(v, v);
            assertEquals(String.valueOf(v), range);
        }
    }

    @Test
    public void testRanges() {
        assertEquals("\\d", RangePattern.pattern(0, 9));
        assertEquals("[1-9]", RangePattern.pattern(1, 9));
        assertEquals("(?:-(?:[1-9]|1[01])|\\d|1[0-7])", RangePattern.pattern(-11, 17));
    }

    @Test
    public void testMatches() {
        int min = -11;
        int max = 17;
        Pattern range = RangePattern.compile(min, max);
        assertFalse(range.matcher(String.valueOf(min-1)).matches());
        assertFalse(range.matcher(String.valueOf(max+1)).matches());
        for (int v = min; v < max; v++) {
            assertTrue(range.matcher(String.valueOf(v)).matches());
        }
    }

}
