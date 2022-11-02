package com.cleo.labs.cidr;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RangePattern {
    /**
     * Returns a compiled {@code Pattern} matching numbers in the range {@code min}-{@code max}.
     * The compiled pattern matches numbers in the range {@code min}-{@code max} without
     * accounting for leading zeroes or leading or trailing whitespace.
     * @param min the lower end of the range.
     * @param max the upper end of the range.
     * @return the compiled pattern.
     */
    public static Pattern compile(int min, int max) {
        String pattern = pattern(min, max);
        return Pattern.compile(pattern);
    }

    /**
     * Returns a compiled {@code Pattern} matching numbers in the range {@code min}-{@code max}.
     * The compiled pattern matches numbers in the range {@code min}-{@code max} without
     * accounting for leading zeroes or leading or trailing whitespace.
     * @param min   the lower end of the range.
     * @param max   the upper end of the range.
     * @param flags match flags as in {@code Pattern.compile}.
     * @return the compiled pattern.
     */
    public static Pattern compile(int min, int max, int flags) {
        String pattern = pattern(min, max);
        return Pattern.compile(pattern, flags);
    }

    /**
     * Returns a string representing a regex matching numbers in the range {@code min}-{@code max}.
     * @param min the lower end of the range.
     * @param max the upper end of the range.
     * @return the pattern string.
     */
    public static String pattern(int min, int max) {
        return join(patterns(min, max));
    }
    
    /**
     * Returns a regex string matching single digits in the range {@code min}-{@code max}.
     * The string will be "n" for a single digit, "mn" for adjacent digits,
     * "m-n" for a range of digits, or "\\d" for 0-9.  If the range is empty,
     * i.e. {@code min>max}, returns "".
     * @param min the lower end of the range (rounded up to 0 if negative).
     * @param max the upper end of the range (rounded down to 9 if larger).
     * @return a regex string.
     */
    private static String digits(int min, int max) {
        min = Math.max(min, 0);
        max = Math.min(max, 9);
        String result;
        if (min > max) {
            // nuthin'
            result = "";
        } else if (min==max) {
            // single digit
            result = String.valueOf(min);
        } else if (min==0 && max==9) {
            // any digit: \d
            result = "\\d";
        } else if (max-min == 1) {
            // adjacent: [xy]
            result = "["+min+max+"]";
        } else {
            // range: [x-y]
            result = "["+min+"-"+max+"]";
        }
        return result;
    }

    /**
     * Returns a list of regex patterns that together match {@code min}-{@code max}.
     * This method is used internally and creates fixed-width matching patterns {@code digits}
     * wide, including leading zeroes.  {@code modulus} and {@code mask} bound the range of
     * values with {@code digits} digits: {@code modulus} is the smallest value with {@code digits}
     * digits (i.e. {@code 10^(digits-1)}), and {@code mask} is the largest such value +1
     * (i.e. {@code 10^digits}).  Note that the following constraints on the input values
     * are <i>not checked</i> as this is for internal use only:
     * <ul><li>min is >= modulus and < max</li>
     *     <li>max is >= min and < mask</li>
     *     <li>modulus is 10^(digits-1)</li>
     *     <li>mask is 10^digits</li>
     * </ul>
     * @param min      the lower end of the range.
     * @param max      the upper end of the range.
     * @param digits   the number of digits to use in the matching pattern.
     * @param modulus  ten-to-the digits-1
     * @param mask     ten-to-the digits
     * @return a list of regex patterns.
     */
    private static List<String> rr(int min, int max, int digits, int modulus, int mask) {
        ArrayList<String> result = new ArrayList<String>();
        if (modulus==1) {
            result.add(digits(min, max));
        } else {
            int minDigit = min / modulus;
            int maxDigit = max / modulus;
            if (minDigit == maxDigit) {
                result.add(minDigit+join(rr(min%modulus, max%modulus, digits-1, modulus/10, modulus)));
            } else {
                if (min != minDigit*modulus) {
                    result.add(minDigit+join(rr(min%modulus, 9, digits-1, modulus/10, modulus)));
                    minDigit++;
                }
                String maxSlop = null;
                if (max+1 != (maxDigit+1)*modulus) {
                    maxSlop = maxDigit+join(rr(0, max%modulus, digits-1, modulus/10, modulus));
                    maxDigit--;
                }
                if (minDigit <= maxDigit) {
                    if (digits <= 2) {
                        result.add(digits(minDigit, maxDigit)+"\\d");
                    } else {
                        result.add(digits(minDigit, maxDigit)+"\\d{"+(digits-1)+"}");
                    }
                }
                if (maxSlop != null) {
                    result.add(maxSlop);
                }
            }
        }
        return result;
    }

    /**
     * Returns a list of regex patterns that together match {@code min}-{@code max}.
     * This method returns a list of regex patterns for arbitrary ranges, including
     * empty ({@code min}>{@code max}) ranges and values < 0.
     * @param min      the lower end of the range.
     * @param max      the upper end of the range.
     * @return a list of regex patterns.
     */
    private static List<String> patterns(int min, int max) {
        ArrayList<String> result = new ArrayList<String>();
        if (min <= max) {
            // deal with negatives, splitting off min through -1 (or max, whichever is less)
            if (min < 0) {
                result.add("-"+pattern(Math.max(1, -max), -min));
                min = 0;
            }
            // deal with 0-9, splitting off single digit patterns
            if (min < 10 && min <= max) {
                result.add(digits(min, max));
                min = 10;
            }
            // now deal with multi-digit cases
            int modulus = 10;
            int digits = 2;
            while (min <= max) {
                int mask  = modulus*10;
                if (min < mask) {
                    int max9 = Math.min(max, mask-1);
                    result.addAll(rr(min, max9, digits, modulus, mask));
                    min = mask;
                }
                modulus = mask;
                digits++;
            }
        }
        return result;
    }

    /**
     * Returns a string or'ing the patterns in a list of patterns.
     * @param patterns the list of regular expression strings.
     * @return a composite pattern (?:x|y|...|z)
     */
    private static String join(List<String> patterns) {
        StringBuilder result = new StringBuilder();
        if (patterns.size()==0) {
            // empty is ok
        } else if (patterns.size()==1) {
            result.append(patterns.get(0));
        } else {
            result.append("(?:");
            for (String pattern : patterns) {
                result.append(pattern).append("|");
            }
            result.setCharAt(result.length()-1, ')');
        }
        return result.toString();
    }

    /**
     * Private constructor.
     */
    private RangePattern() {}
}
