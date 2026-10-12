package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCheckTest {

    private final Customer birch = new Customer("Birch & Co",
            new Address("Jonas Birch", "Mariahilfer Straße 54", "1070", "Vienna", Country.AUSTRIA, ""),
            PaymentTerms.invoice(Money.of("1000.00"), false), Money.of("940.00"));
    private final Customer fjord = new Customer("Fjord Supplies",
            new Address("Lars Jensen", "Hafenstraße 21", "20359", "Hamburg", Country.GERMANY, ""),
            PaymentTerms.cardOnly(), Money.ZERO);

    @Test
    void warnsAboveTheLimitOnly() {
        assertThat(CreditCheck.exceeds(birch, Money.of("60.00"))).isFalse();
        assertThat(CreditCheck.exceeds(birch, Money.of("60.01"))).isTrue();
        assertThat(CreditCheck.exceeds(fjord, Money.of("99999.00"))).isFalse();
    }

    @Test
    void wording() {
        assertThat(CreditCheck.message(birch, Money.of("128.40")))
                .isEqualTo("This order takes Birch & Co to €1,068.40 of its €1,000.00 credit limit.");
        assertThat(CreditCheck.detail(birch, Money.of("128.40")))
                .isEqualTo("Owes €940.00 now, this order adds €128.40.");
    }
}
