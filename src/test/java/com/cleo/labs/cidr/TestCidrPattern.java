package com.cleo.labs.cidr;

import static org.junit.Assert.*;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;

public class TestCidrPattern {

    private void testError(String input) {
        try {
            CidrPattern.compile(input);
            fail("exception expected for "+input);
        } catch (PatternSyntaxException e) {
            // expected
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
        testError("not even numbers");
        testError("10");            // no / unless a.b.c.d
        testError("256/8");         // 0-255 only
        testError("1.2.3.4.5");     // up to four only
    }

}
