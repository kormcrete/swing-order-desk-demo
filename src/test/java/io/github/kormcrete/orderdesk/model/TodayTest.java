package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodayTest {

    @Test
    void fixedDate() {
        assertThat(Today.from(" 2026-10-12 ").get()).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void systemClockWhenNotSet() {
        assertThat(Today.from(null).get()).isEqualTo(LocalDate.now());
        assertThat(Today.from("").get()).isEqualTo(LocalDate.now());
    }

    @Test
    void badTodayNamesTheProperty() {
        assertThatThrownBy(() -> Today.from("12.10.2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("-Dorderdesk.today must be a date like 2026-10-12, was '12.10.2026'");
    }
}
