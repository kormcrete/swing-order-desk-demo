package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dates as the app shows them, whatever the machine's locale. */
public final class Dates {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private Dates() {
    }

    /** {@code 9 Oct 2026}. */
    public static String format(LocalDate date) {
        return FORMAT.format(date);
    }
}
