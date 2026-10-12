package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    private final Customer ada = new Customer("Ada Shop Ltd",
            new Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, ""),
            PaymentTerms.invoice(Money.of("2000.00"), true), Money.of("450.00"));
    private final Customer lumen = new Customer("Café Lumen",
            new Address("Noor de Vries", "Prinsengracht 263", "1016 GV", "Amsterdam", Country.NETHERLANDS, ""),
            PaymentTerms.cardOnly(), Money.ZERO);

    @Test
    void termsSummary() {
        assertThat(ada.termsSummary())
                .isEqualTo("Account · Invoice 30 days · Credit limit €2,000.00 · Owes €450.00 · PO number required");
        Customer birch = new Customer("Birch & Co",
                new Address("Jonas Birch", "Mariahilfer Straße 54", "1070", "Vienna", Country.AUSTRIA, ""),
                PaymentTerms.invoice(Money.of("1000.00"), false), Money.of("940.00"));
        assertThat(birch.termsSummary()).isEqualTo("Account · Invoice 30 days · Credit limit €1,000.00 · Owes €940.00");
        assertThat(lumen.termsSummary()).isEqualTo("Card only");
        assertThat(ada.terms().poRequired()).isTrue();
        assertThat(lumen.terms().invoiceAllowed()).isFalse();
    }

    @Test
    void suggestionDetail() {
        assertThat(ada.suggestionDetail()).isEqualTo("Berlin · Account");
        assertThat(lumen.suggestionDetail()).isEqualTo("Amsterdam · Card only");
    }

    @Test
    void matchesNameOrCityIgnoringCaseAndSpaces() {
        assertThat(ada.matches("shop")).isTrue();
        assertThat(ada.matches("  BERLIN ")).isTrue();
        assertThat(lumen.matches("caf")).isTrue();
        assertThat(lumen.matches("berlin")).isFalse();
        assertThat(ada.matches("")).isTrue();
    }

    @Test
    void owesChanges() {
        ada.setOwes(Money.of("500.00"));
        assertThat(ada.owes()).isEqualTo(Money.of("500.00"));
        assertThat(ada.toString()).isEqualTo("Ada Shop Ltd");
    }
}
