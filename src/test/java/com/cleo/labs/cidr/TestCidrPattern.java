package com.cleo.labs.cidr;

import static org.junit.Assert.*;

import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.junit.Test;

public class TestCidrPattern {

    private void testError(String input, String description) {
        try {
            CidrPattern.compile(input);
            fail("exception expected for "+input);
        } catch (PatternSyntaxException e) {
            assertEquals(description, e.getDescription());
        }
    }

    private void testMatch(String cidr, String...matches) {
        boolean expected = true;
        try {
            Pattern pattern = CidrPattern.compile(cidr);
            for (String match : matches) {
                if (match==null) {
                    expected = !expected;
                } else {
                    assertEquals(cidr+" matching "+match, expected, pattern.matcher(match).matches());
                }
            }
        } catch (PatternSyntaxException e) {
            fail(cidr+" pattern syntax error: "+e.getMessage());
        }
    }

    @Test
    public void testMatching () {
        testMatch("10/8", "10.1.2.3", "10.255.255.255", null,
                  "10.1.2", "10.256.255.255");
        testMatch("10.1.2.3", "10.1.2.3", null,
                  "10.1.2.0", "10.256.255.255");
        testMatch("10.1.2.3,192.168.9.10", "10.1.2.3", "192.168.9.10", null,
                  "10.1.2.0", "10.256.255.255", "192.168.9.11");
        testMatch("10.1.2.*", "10.1.2.0", "10.1.2.255", null,
                  "10.1.3.0", "10.256.255.255", "192.168.9.11");
        testMatch("*.18.23/24", "1.18.23.44", "220.18.23.102", "0.18.23.0", null,
                  "1.19.23.0");
    }

    @Test
    public void testErrors() {
        testError("not even numbers", "Not a CIDR Pattern");
        testError("10", "/bits required unless full address provided");
        testError("256/8", "Not a CIDR Pattern");     // 0-255 only
        testError("1.2.3.4.5", "Not a CIDR Pattern"); // up to four only
        testError("1.2.3-1.2.3.4", "full start address required for address range");
        testError("1.2.3.4-1.1.3.4", "Range must be in order");
    }

    @Test
    public void testSimpleRange() {
        String ip = "10.35.88.123";
        assertEquals(ip.replaceAll("\\.", "\\\\."), CidrPattern.compile(ip+"-"+ip).pattern());
    }

    private boolean iterate(String from, String to, Function<String, Boolean> test) {
        long start = Stream.of(from.split("\\.")).map(Long::valueOf).reduce((result, n)->(256L * result + n)).get();
        long stop = Stream.of(to.split("\\.")).map(Long::valueOf).reduce((result, n)->(256L * result + n)).get();
        for (long i=start; i<=stop; i++) {
            String ip = String.format("%d.%d.%d.%d", (int)(i>>24), (int)(i>>16 & 0xFF), (int)(i>>8 & 0xFF), (int)(i&0xFF));
            if (!test.apply(ip)) {
                System.err.println("failure: "+ip);
                return false;
            }
        }
        return true;
    }

    @Test
    public void testRanges() {
        assertEquals("1\\.2\\.3\\.[45]", CidrPattern.compile("1.2.3.4-1.2.3.5").pattern());
        assertEquals("1\\.2\\.(?:3\\.(?:[4-9]|[1-9]\\d|1\\d{2}|2(?:[0-4]\\d|5[0-5]))|4\\.[0-5])", CidrPattern.compile("1.2.3.4-1.2.4.5").pattern());
    }

    @Test
    public void testRange() {
        Pattern p1 = CidrPattern.compile("192.168.50.100-192.168.50.105");
        assertTrue(iterate("192.168.50.100", "192.168.50.105", (s)->p1.matcher(s).matches()));
        assertTrue(iterate("192.168.50.254", "192.168.51.3", (s)->!p1.matcher(s).matches()));
        Pattern p2 = CidrPattern.compile("1.2.3.4-1.2.5.14");
        assertTrue(iterate("1.2.3.4", "1.2.5.14", (s)->p2.matcher(s).matches()));
        assertTrue(iterate("1.2.5.15", "1.2.5.18", (s)->!p2.matcher(s).matches()));
        assertTrue(iterate("1.2.0.0", "1.2.3.3", (s)->!p2.matcher(s).matches()));
    }

}
