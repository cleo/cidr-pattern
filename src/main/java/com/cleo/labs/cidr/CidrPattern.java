package com.cleo.labs.cidr;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class CidrPattern {
    private static final String MATCH_255     = "(?:\\d|[1-9]\\d|1\\d{2}|2(?:[0-4]\\d|5[0-5]))";
    private static final String CAPTURE_255   = "(\\d|[1-9]\\d|1\\d{2}|2(?:[0-4]\\d|5[0-5])|\\*)";
    private static final String CAPTURE_32    = "([1-9]|[12]\\d|3[0-2])";
    private static final Pattern CIDR_PATTERN = Pattern.compile(CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                ")?)?)?(?:/"+CAPTURE_32+")?");

    /**
     * Returns a compiled {@code Pattern} matching CIDR pattern list {@code cidrs}.
     * @param cidrs a comma-separated list of CIDR expressions like 192.168/16,10/8.
     * @return the compiled pattern.
     * @throws PatternSyntaxException if {@code cidrs} is not a list of CIDR patterns.
     */
    public static Pattern compile(String cidrs) throws PatternSyntaxException {
        return Pattern.compile(patterns(cidrs));
    }

    /**
     * Returns a compiled {@code Pattern} matching CIDR pattern list {@code cidrs}.
     * @param cidrs a comma-separated list of CIDR expressions like 192.168/16,10/8.
     * @param flags match flags as in {@code Pattern.compile}.
     * @return the compiled pattern.
     * @throws PatternSyntaxException if {@code cidrs} is not a list of CIDR patterns.
     */
    public static Pattern compile(String cidrs, int flags) throws PatternSyntaxException {
        return Pattern.compile(patterns(cidrs), flags);
    }

    /**
     * Convenience method that compiles a CIDR pattern list and matches it against an input.
     * @param cidrs a comma-separated list of CIDR expressions like 192.168/16,10/8.
     * @param input the input to match against the CIDR pattern list
     * @return {@code true} if the input matches the pattern list
     * @throws PatternSyntaxException if {@code cidrs} is not a list of CIDR patterns.
     */
    public static boolean matches(String cidrs, CharSequence input) throws PatternSyntaxException {
        return compile(cidrs).matcher(input).matches();
    }

    /**
     * Returns a string representing a regex matching CIDR pattern {@code cidr}.
     * @param cidrs a comma-separated list of CIDR expressions like 192.168/16,10/8.
     * @return the pattern string.
     * @throws PatternSyntaxException if {@code cidrs} is not a list of CIDR patterns.
     */
    private static String patterns(String cidrs) throws PatternSyntaxException {
        List<String> results = new ArrayList<>();
        for (String cidr : cidrs.split("\\s*,\\s*")) {
            results.add(pattern(cidr));
        }
        if (results.size()==0) {
            throw new PatternSyntaxException("Empty CIDR Pattern", cidrs, 0);
        } else if (results.size()==1) {
            return results.get(0);
        } else {
            return results.stream().collect(Collectors.joining("|", "(?:", ")"));
        }
    }

    /**
     * Returns a string representing a regex matching CIDR pattern {@code cidr}.
     * @param cidr a CIDR expression like 192.168/16.
     * @return the pattern string.
     * @throws PatternSyntaxException if {@code cidr} is not a CIDR pattern.
     */
    private static String pattern(String cidr) throws PatternSyntaxException {
        Matcher m = CIDR_PATTERN.matcher(cidr);
        if (!m.matches()) {
            throw new PatternSyntaxException("Not a CIDR Pattern", cidr, 0);
        }
        StringBuilder result = new StringBuilder();
        String bitsString = m.group(5);
        if (bitsString==null) {
            if (m.group(4)==null) {
                throw new PatternSyntaxException("/bits required unless full address provided", cidr, cidr.length()-1);
            }
            bitsString = "32";
        }
        int bits  = Integer.valueOf(bitsString);
        for (int i = 0; i<4; i++) {
            if (i>0) {
                result.append("\\.");
            }
            String capture = m.group(i+1)==null ? "0" : m.group(i+1);
            if (bits==0 || capture.equals("*")) {
                result.append(MATCH_255);
            } else if (bits >= 8) {
                result.append(capture);
            } else if (bits > 0) {
                int mask = (1 << (8-bits))-1;
                int base = Integer.valueOf(capture) & ~mask;
                result.append(RangePattern.pattern(base, base+mask));
            }
            bits = Math.max(0, bits-8);
        }
        return result.toString();
    }

    /**
     * Private constructor.
     */
    private CidrPattern() {}
}
