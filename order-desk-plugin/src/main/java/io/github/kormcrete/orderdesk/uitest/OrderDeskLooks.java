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

    /**
     * The choice a tester means by {@code wanted} among names that have other words for them too: {@code words} maps
     * each word (a choice's name, the label the app shows, an id) to its choice. Compared by letters and digits only,
     * any case ("postalCode" is "Postal code", "Express · next business day" is "Express"): a word equal to it, else the
     * words that start with it, else those with a word that starts with it ("note": Delivery note), else the word it
     * starts with (the whole label given). Each level counts choices, not words; several fail naming them, none names
     * them all, as {@link #pick} does.
     */
    static String resolve(String wanted, Map<String, String> words, String kind, String kinds, String where) {
        String w = letters(wanted);
        List<String> choices = new ArrayList<String>(new java.util.LinkedHashSet<String>(words.values()));
        if (!w.isEmpty()) {
            for (int level = 0; level < 4; level++) {
                java.util.Set<String> fit = new java.util.LinkedHashSet<String>();
                for (Map.Entry<String, String> e : words.entrySet()) {
                    String word = letters(e.getKey());
                    boolean fits = level == 0 ? word.equals(w)
                            : level == 1 ? word.startsWith(w)
                            : level == 2 ? anyWordStarts(e.getKey(), w)
                            : w.startsWith(word);
                    if (fits) fit.add(e.getValue());
                }
                if (fit.size() == 1) return fit.iterator().next();
                if (fit.size() > 1) {
                    throw new DriverException(Reason.AMBIGUOUS, quoted(wanted) + " fits " + fit.size() + " " + kinds + ": "
                            + join(new ArrayList<String>(fit)) + ". Give more of the name");
                }
            }
        }
        throw new DriverException(Reason.NOT_FOUND, "No " + kind + " fits " + quoted(wanted) + ". " + where + ": "
                + join(choices));
    }

    /** {@code text} by its letters and digits only, lower case: "Postal code" and "postalCode" read "postalcode". */
    static String letters(String text) {
        StringBuilder b = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) if (Character.isLetterOrDigit(c)) b.append(c);
        return b.toString();
    }

    private static boolean anyWordStarts(String text, String start) {
        for (String word : text.split("[^\\p{L}\\p{N}]+")) {
            if (!word.isEmpty() && letters(word).startsWith(start)) return true;
        }
        return false;
    }

    /** Text with its runs of white space as one space, trimmed: a message's lines compare as one sentence. */
    static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
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

    /** Runs a step that puts the screen back after a failure; its own failure must not hide the first one. */
    static void quietly(Runnable undo) {
        try {
            undo.run();
        } catch (RuntimeException ignored) {
            // the step's own failure is the one to report
        }
    }

    /** True when {@code target} is there now: one read, no wait. */
    static boolean presentNow(Target target) {
        try {
            return target.timeout(Duration.ofMillis(1)).presence() != Target.Presence.NONE;
        } catch (UiActionFailed e) {
            return false;
        }
    }

    /**
     * A message's lines, read within {@link #LOOK}; null when it does not show. A line is the label's text: its value
     * when it has one (a message pane names every label "OptionPane.label"), else its name.
     */
    static List<String> messageLines(final Target labels) {
        return look(() -> {
            List<String> names = labels.texts();
            List<String> values = labels.values();
            List<String> lines = new ArrayList<String>();
            for (int i = 0; i < names.size(); i++) {
                String v = i < values.size() ? values.get(i) : null;
                lines.add(v != null && !v.isEmpty() ? v : names.get(i));
            }
            return lines.isEmpty() ? null : lines;
        });
    }

    /** The message is {@code expected}: its whole text, or one of its lines, white space aside. */
    static void messageIs(List<String> lines, String expected, String what) {
        String want = oneLine(expected);
        String all = oneLine(String.join(" ", lines));
        if (want.equals(all)) return;
        for (String line : lines) if (want.equals(oneLine(line))) return;
        throw new AssertionError(what + " reads " + quoted(all) + ", expected " + quoted(want));
    }

    /**
     * The text value of {@code key} in the JSON object {@code json} (its top level only); null when it has none. For an
     * outcome's text ({@code Outcome.toString()}): its JSON objects are the agent's own shaded classes, which a plugin
     * built against the plain library does not link against.
     */
    static String topLevelText(String json, String key) {
        int depth = 0;
        int i = 0;
        boolean keyNext = false;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{' || c == '[') {
                depth++;
                keyNext = c == '{' && depth == 1;
                i++;
            } else if (c == '}' || c == ']') {
                depth--;
                i++;
            } else if (c == ',') {
                keyNext = depth == 1;
                i++;
            } else if (c == '"') {
                StringBuilder text = new StringBuilder();
                i = string(json, i, text);
                if (keyNext && depth == 1) {
                    keyNext = false;
                    if (key.equals(text.toString())) {
                        while (i < json.length() && (json.charAt(i) == ':' || Character.isWhitespace(json.charAt(i)))) i++;
                        if (i >= json.length() || json.charAt(i) != '"') return null;
                        StringBuilder value = new StringBuilder();
                        string(json, i, value);
                        return value.toString();
                    }
                }
            } else {
                i++;
            }
        }
        return null;
    }

    /** Reads the JSON string starting at {@code at} (its opening quote) into {@code out}; returns the index after it. */
    private static int string(String json, int at, StringBuilder out) {
        int i = at + 1;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') return i;
            if (c != '\\' || i >= json.length()) {
                out.append(c);
                continue;
            }
            char e = json.charAt(i++);
            switch (e) {
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'r': out.append('\r'); break;
                case 'b': out.append('\b'); break;
                case 'f': out.append('\f'); break;
                case 'u':
                    if (i + 4 <= json.length()) {
                        out.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
                        i += 4;
                    }
                    break;
                default: out.append(e);
            }
        }
        return i;
    }
}
