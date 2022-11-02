package com.cleo.labs.cidr;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Explore {
    private static Pattern pattern = null;
    
    private static void usage(PrintStream out) {
        out.println("usage: range number number - print regex for range\n"+
                "       cidr pattern        - parse pattern and print regex\n"+
                "       pattern pattern     - parse pattern as a Java Pattern\n"+
                "       match test          - after cidr, range or pattern command, test against an input\n"+
                "       .                   - exit"); 
    }

    private static void test (String command) {
        String[] argv       = command.split("\\s+", 2);
        String verb         = argv[0];
        String arg          = argv.length>1 ? argv[1] : "";
        if (verb.equalsIgnoreCase("range")) {
            try {
                String[] args = arg.split("[-\\.\\s]+", 2);
                int min = Integer.parseInt(args[0]);
                int max = Integer.parseInt(args[1]);
                pattern = RangePattern.compile(min, max);
                System.out.println("range ["+min+" "+max+"] = "+pattern);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("usage: range min max");
            }
        } else if (verb.equalsIgnoreCase("cidr")) {
            try {
                pattern = CidrPattern.compile(arg);
                System.out.println("cidr ["+arg+"] = "+pattern);
            } catch (PatternSyntaxException e) {
                System.out.println("error: ["+arg+"] is not a cidr pattern");
            }
        } else if (verb.equalsIgnoreCase("match")) {
            if (pattern==null) {
                System.out.println("set a pattern first");
            } else if (pattern.matcher(arg).matches()) {
                System.out.println("match ["+arg+"] success");
            } else {
                System.out.println("match ["+arg+"] failed");
            }
        } else if (verb.equalsIgnoreCase("pattern")) {
            try {
                pattern = Pattern.compile(arg);
            } catch (PatternSyntaxException e) {
                System.out.println("error: ["+arg+"] is not a valid pattern: "+e.getDescription());
            }
        } else if (verb.length()>0 && pattern!=null) {
            if (pattern.matcher(command).matches()) {
                System.out.println("match ["+command+"] success");
            } else {
                System.out.println("match ["+command+"] failed");
            }
        } else {
            usage(System.out);
        }
    }

    /**
     * Test driver.
     */
    public static void main(String[] argv) throws IOException {
        if (argv.length > 0) {
            for (String arg : argv) {
                test(arg);
            }
        } else {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            String line;
            usage(System.out);
            try {
                while ((line = input.readLine()) != null && !line.equals(".")) {
                    try {
                        test(line);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace(System.out);
                    }
                }
            } catch (Exception e) {
                // done
            }
        }
    }
}
