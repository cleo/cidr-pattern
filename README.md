# README #


## Overview ##

This simple package creates a Java [Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
from a list of IP addresses, including support for [CIDR](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing)
block notation and wildcards. Only IPv4 is supported, not IPv6.

## Usage ##

The `CidrPattern` class exposes `compile` and `matches` methods similar to Java's built-in `Pattern` class:

```java
public static Pattern compile(String cidrs) throws PatternSyntaxException;
public static Pattern compile(String cidrs, int flags) throws PatternSyntaxException;
public static boolean matches(String cidrs, CharSequence input) throws PatternSyntaxException;
```

The `cidrs` input is a comma-separated list of CIDR, wildcard or address range IPv4 expressions of the form:

* _IP-Address_`/`_bits_
* _IP-Address_`-`_IP-Address_
* _IP-Address_

where

* _IP-Address_ is betwen one and four integers 0-255 (or `*`) separated by `.` and
* _bits_ is a number between 1 and 32 (defaulting to 32 if `/`_bits_ is omitted)

The following are valid CIDR patterns:

* `10/8,172.16/12,192.168/16` describes the [RFC 1918](https://datatracker.ietf.org/doc/html/rfc1918) private address space.
* `10/24` and `10.0.0.*` describe the same set of addresses starting with `10.0.0.`.
* `10.0.0.0-10.0.0.255` also describes the same set of addresses starting with `10.0.0`.
* `142.250.72.196` matches a single IP Address.
    

```java
import com.cleo.labs.cidr.CidrPattern;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// ...

try {
    Pattern cidr = CidrPattern.compile("192.168.0/24, 10/8");
    if (cidr.matcher("192.168.0.33").matches()) {
        // success!
    }
} catch (PatternSyntaxException e) {
    System.err.println(e.getDescription());
}
```

### RangePattern ###

A helper class `RangePattern` may also be used directly if needed. It builds a regex pattern
designed to match a range of integers:

```java
public static Pattern compile(int min, int max);
public static Pattern compile(int min, int max, int flags);
```

The compiled pattern checks that the input:

* is an integer (optional leading `-`, followed by 1 or more digits), and
* has a value >= `min` and <= `max`.

Note that `min` and `max` maybe negative, 0, or positive. If `min` is larger than `max`, the
returned Pattern matches no inputs. But no `PatternSyntaxException` is ever thrown&mdash;all
inputs produce a valid `Pattern`.

## Interactive Explorer Tool ##

`CidrPattern` is packaged as an executable `jar` whose main class is an interactive explorer
tool that supports interactive testing of patterns against inputs.

To launch the tool:

```
java -jar cidr-pattern-1.0.0.jar
```

The tool supports the following commands:

* `cidr` _pattern_ &mdash; compile _pattern_ as a list of CIDR patterns
* `range` _min_ _max_ &mdash; compile a `RangePattern` from _min_ to _max_
* `pattern` _pattern_ &mdash; compile _pattern_ as a `java.util.regex.Pattern` directly
* `test` _input_ &mdash; test _input_ against the last `cidr`, `range` or `pattern` entered
* `.` &mdash; exit the tool


## Maven

The `cidr-pattern` library is packaged as a single `jar` file with no additional dependencies.
It is stored in the GitHub package repository and can be included in your project's `pom.xml` as follows:

```
<dependency>
  <groupId>com.cleo.labs</groupId>
  <artifactId>cidr-pattern</artifactId>
  <version>1.0.0</version>
</dependency>
```

## License

The `cidr-pattern` library is released under the MIT License.
