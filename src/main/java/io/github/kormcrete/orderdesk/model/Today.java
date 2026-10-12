package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/** "Today" for new orders and the card expiry check: the system clock, or a fixed date for tests and screenshots. */
public final class Today {

    public static final String PROPERTY = "orderdesk.today";

    private Today() {
    }

    /** {@code -Dorderdesk.today=2026-10-12} fixes the date; a malformed value stops the app with a clear message. */
    public static Supplier<LocalDate> fromSystemProperty() {
        return from(System.getProperty(PROPERTY));
    }

    static Supplier<LocalDate> from(String value) {
        if (value == null || value.trim().isEmpty()) return LocalDate::now;
        try {
            LocalDate fixed = LocalDate.parse(value.trim());
            return () -> fixed;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("-D" + PROPERTY + " must be a date like 2026-10-12, was '" + value + "'");
        }
    }
}
