package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiActionFailed;
import io.github.kormcrete.swingui.junit.UiAssertionError;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;

/**
 * What every Order Desk step does the same way: a short look for the screen it needs, a name a tester gives found by
 * its start, a list of names in one param, and a check that fails at once, in words, with what was expected and what
 * the app shows. Client calls only, like the steps.
 */
final class OrderDeskLooks {

    /** How long a step looks for the screen it needs, and a check for its value, before it says why not. */
    static final Duration LOOK = Duration.ofSeconds(1);
    /** The word a tester gives for "not shown": no reason, no warning, no suggestions, an empty cart. */
    static final String NONE = "none";
    /** Several names in one param: product names hold commas, so a semicolon parts them. */
    static final String SEPARATOR = ";";
    private static final long POLL_MS = 100;

    private OrderDeskLooks() {
    }

    static boolean isNone(String value) {
        return value != null && value.trim().equalsIgnoreCase(NONE);
    }

    /** The names in {@code values}, parted by {@link #SEPARATOR}, trimmed, the empty ones left out. */
    static List<String> names(String values) {
        List<String> out = new ArrayList<String>();
        for (String v : values.split(SEPARATOR)) {
            if (!v.trim().isEmpty()) out.add(v.trim());
        }
        return out;
    }

    static String join(List<String> names) {
        return String.join(SEPARATOR + " ", names);
    }

    static String quoted(String text) {
        return "“" + text + "”";
    }

    /**
     * Asks {@code read} again until it answers, or {@link #LOOK} has passed: then null. A read that finds nothing
     * (NOT_FOUND, AMBIGUOUS while a screen changes) counts as no answer yet.
     */
    static <T> T look(Supplier<T> read) {
        long end = System.currentTimeMillis() + LOOK.toMillis();
        while (true) {
            try {
                T answer = read.get();
                if (answer != null) return answer;
            } catch (UiActionFailed e) {
                if (e.reason() != Reason.NOT_FOUND && e.reason() != Reason.TIMEOUT && e.reason() != Reason.EXPECTATION) throw e;
            } catch (DriverException e) {
                if (e.reason() != Reason.NOT_FOUND && e.reason() != Reason.TIMEOUT && e.reason() != Reason.EXPECTATION) throw e;
            }
            if (System.currentTimeMillis() >= end) return null;
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    /** True once {@code target} shows, within {@link #LOOK}. */
    static boolean showing(final Target target) {
        return look(() -> target.presence() != Target.Presence.NONE ? Boolean.TRUE : null) != null;
    }

    /** The text a person sees in {@code target} now (a label, a cell, a field, a tab), or null when it shows none. */
    static String shown(Target target) {
        try {
            return target.timeout(Duration.ofMillis(1)).shownText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    /**
     * The one of {@code choices} a tester means by {@code wanted}: the one equal to it, any case, else the only one that
     * starts with it. None or several fail naming the choices that fit (all of them when none does), e.g.
     * "“gel pen” fits 2 products: Gel pen, black; Gel pen, blue. Give more of the name". {@code kind} and
     * {@code kinds} name a choice ("product", "products"); {@code where} says where they were read, for a miss.
     */
    static String pick(String wanted, List<String> choices, String kind, String kinds, String where) {
        String w = wanted.trim().toLowerCase(Locale.ROOT);
        List<String> starting = new ArrayList<String>();
        for (String c : choices) {
            String lc = c.toLowerCase(Locale.ROOT);
            if (lc.equals(w)) return c;
            if (lc.startsWith(w)) starting.add(c);
        }
        if (starting.size() == 1) return starting.get(0);
        if (starting.size() > 1) {
            throw new DriverException(Reason.AMBIGUOUS, quoted(wanted) + " fits " + starting.size() + " " + kinds + ": "
                    + join(starting) + ". Give more of the name");
        }
        throw new DriverException(Reason.NOT_FOUND, "No " + kind + " fits " + quoted(wanted) + ". " + where + ": "
                + (choices.isEmpty() ? "none" : join(choices)));
    }

    /** The values of {@code column} in every row of {@code table}'s read, in order. */
    static List<String> column(List<Map<String, String>> rows, String column) {
        List<String> out = new ArrayList<String>();
        for (Map<String, String> row : rows) {
            String v = row.get(column);
            if (v != null) out.add(v);
        }
        return out;
    }

    /**
     * Checks that {@code target} shows {@code expected}: an {@code expect} that waits at most {@link #LOOK}, then, when it
     * fails, one read of what it shows, so the failure says both: "The cart total reads “€12.00”, expected “€31.32”".
     */
    static void shows(Target target, String what, String expected) {
        try {
            assertThat(target).within(LOOK).hasText(expected);
        } catch (UiAssertionError e) {
            String actual = shown(target);
            throw new AssertionError(actual == null ? what + " is not shown, expected " + quoted(expected)
                    : what + " reads " + quoted(actual) + ", expected " + quoted(expected));
        }
    }

    /** Checks that {@code target} is not shown; when it is, the failure says what it shows. */
    static void notShown(Target target, String what) {
        try {
            assertThat(target).within(LOOK).isAbsent();
        } catch (UiAssertionError e) {
            String actual = shown(target);
            throw new AssertionError(what + (actual == null ? " is shown" : " reads " + quoted(actual)) + ", expected none");
        }
    }

    /** Checks a list of names: "The cart holds A; B, expected B; A". */
    static void namesAre(List<String> actual, List<String> expected, String what) {
        if (!actual.equals(expected)) {
            throw new AssertionError(what + " " + (actual.isEmpty() ? "nothing" : join(actual)) + ", expected " + join(expected));
        }
    }
}
