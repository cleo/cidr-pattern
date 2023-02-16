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
    private static final String CAPTURE_IP    = "("+MATCH_255+"\\."+MATCH_255+"\\."+MATCH_255+"\\."+MATCH_255+")";
    private static final Pattern CIDR_PATTERN = Pattern.compile(CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                "(?:\\."+CAPTURE_255+
                                                                ")?)?)?(?:/"+CAPTURE_32+"|-"+CAPTURE_IP+")?");

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
     * Converts a string of the form number{.number} into an
     * array of ints. Does no error checking.
     * @param strings number.number...
     * @return int.int...
     */
    private static int[] ints(String[] strings) {
        int[] ints = new int[strings.length];
        for (int i=0; i<strings.length; i++) {
            ints[i] = Integer.valueOf(strings[i]);
        }
        return ints;
    }

    private static int[] ZEROS = new int[] {0, 0, 0, 0};
    private static int[] STARS = new int[] {255, 255, 255, 255};

    /**
     * Returns a regex {@code String} that matches the range described
     * by {@code from} and {@code to} starting at {@code index}.
     * If {@code index} is at the end of the arrays, this is just
     * a simple {@link RangePattern}. Otherwise a pattern (or set
     * of patterns) describing the range is assembled, including
     * recursive subrange patterns.
     * @param from starting point of the range
     * @param to ending point of the range
     * @param index where in the from/to arrays to start
     * @return a regex {@code String}
     */
    private static String subrange(int[] from, int[] to, int index) {
        if (index==from.length-1) {
            return RangePattern.pattern(from[index], to[index]);
        } else if (from[index] > to[index]) {
            throw new IllegalArgumentException();
        } else if (from[index] == to[index]) {
            return from[index]+"\\."+subrange(from, to, index+1);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("(?:").append(from[index]).append("\\.").append(subrange(from, STARS, index+1));
            if (to[index]-from[index] > 1) {
                sb.append('|').append(RangePattern.pattern(from[index]+1, to[index]-1));
                for (int i=index; i<from.length-1; i++) {
                    sb.append("\\.").append(MATCH_255);
                }
            }
            sb.append('|').append(to[index]).append("\\.").append(subrange(ZEROS, to, index+1)).append(')');
            return sb.toString();
        }
    }

    private static String range(String cidr, String[] from, String[] to) throws PatternSyntaxException {
        try {
            return subrange(ints(from), ints(to), 0);
        } catch (IllegalArgumentException e) {
            throw new PatternSyntaxException("Range must be in order", cidr, cidr.indexOf("-"));
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
        String rangeString = m.group(6);
        if (rangeString!=null) {
            if (m.group(4)==null) {
                throw new PatternSyntaxException("full start address required for address range", cidr, cidr.indexOf("-"));
            }
            return range(cidr, new String[]{m.group(1), m.group(2), m.group(3), m.group(4)},
                    rangeString.split("\\."));
        } else {
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
        }
        return result.toString();
    }

    /**
     * Private constructor.
     */
    private CidrPattern() {}
}
