package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DatesTest {

    @Test
    void formatsDayShortMonthYearWhateverTheLocale() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertThat(Dates.format(LocalDate.of(2026, 10, 9))).isEqualTo("9 Oct 2026");
            assertThat(Dates.format(LocalDate.of(2026, 9, 1))).isEqualTo("1 Sep 2026");
        } finally {
            Locale.setDefault(before);
        }
    }
}
