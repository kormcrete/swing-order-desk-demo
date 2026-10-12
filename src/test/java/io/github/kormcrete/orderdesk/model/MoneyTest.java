package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void parsesEuros() {
        assertThat(Money.of("4.90").cents()).isEqualTo(490);
        assertThat(Money.of("1000").cents()).isEqualTo(100000);
    }

    @Test
    void refusesFractionsOfACent() {
        assertThatThrownBy(() -> Money.of("4.905")).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void formatsWithGroupingAndTwoDecimals() {
        assertThat(Money.of("1234.5").format()).isEqualTo("€1,234.50");
        assertThat(Money.of("1000000").format()).isEqualTo("€1,000,000.00");
        assertThat(Money.ZERO.format()).isEqualTo("€0.00");
        assertThat(Money.of("0.60").negate().format()).isEqualTo("−€0.60");
    }

    @Test
    void percentRoundsHalfUp() {
        assertThat(Money.of("12.00").percent(5)).isEqualTo(Money.of("0.60"));
        assertThat(Money.ofCents(1).percent(50)).isEqualTo(Money.ofCents(1));   // 0.5 cent -> 1
        assertThat(Money.ofCents(3).percent(50)).isEqualTo(Money.ofCents(2));   // 1.5 cents -> 2
        assertThat(Money.ofCents(31).percent(20)).isEqualTo(Money.ofCents(6));  // 6.2 cents -> 6
    }

    @Test
    void arithmeticAndComparison() {
        Money sum = Money.of("4.90").times(3).plus(Money.of("12.00")).minus(Money.of("0.60"));
        assertThat(sum).isEqualTo(Money.of("26.10"));
        assertThat(sum.isGreaterThan(Money.of("26.09"))).isTrue();
        assertThat(sum.isLessThan(Money.of("26.11"))).isTrue();
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(sum.isPositive()).isTrue();
        assertThat(sum.compareTo(Money.of("30"))).isNegative();
    }
}
