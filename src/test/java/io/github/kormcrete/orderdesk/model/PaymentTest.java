package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.github.kormcrete.orderdesk.model.CardField.EXPIRY;
import static io.github.kormcrete.orderdesk.model.CardField.NAME;
import static io.github.kormcrete.orderdesk.model.CardField.NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PaymentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    void anySixteenDigitsAreACardNumber() {
        assertThat(CardRules.validate("4242 4242 4242 4242", "12/28", "Ada Lindqvist", TODAY)).isEmpty();
        assertThat(CardRules.validate("1234567812345678", "12/28", "Ada Lindqvist", TODAY)).isEmpty();
        assertThat(CardRules.validate("4242 4242 4242 424", "12/28", "A", TODAY)).containsOnly(entry(NUMBER, "Enter the 16 digits of the card"));
        assertThat(CardRules.validate("4242 4242 4242 42421", "12/28", "A", TODAY)).containsKey(NUMBER);
        assertThat(CardRules.validate("4242 4242 4242 424a", "12/28", "A", TODAY)).containsKey(NUMBER);
    }

    @Test
    void acceptsDashesAndSpaces() {
        assertThat(CardRules.validate("4242-4242-4242-4242", "12/28", "A", TODAY)).isEmpty();
        assertThat(CardRules.digits(" 4242 4242-4242 4242 ")).isEqualTo("4242424242424242");
    }

    @Test
    void expiry() {
        assertThat(CardRules.validate("4242424242424242", "10/26", "A", TODAY)).isEmpty();
        assertThat(CardRules.validate("4242424242424242", "09/26", "A", TODAY)).containsOnly(entry(EXPIRY, "This card has expired"));
        assertThat(CardRules.validate("4242424242424242", "13/27", "A", TODAY)).containsOnly(entry(EXPIRY, "Month must be 01–12"));
        assertThat(CardRules.validate("4242424242424242", "1227", "A", TODAY)).containsOnly(entry(EXPIRY, "Use MM/YY"));
    }

    @Test
    void nameOnCardIsRequired() {
        assertThat(CardRules.validate("4242424242424242", "12/28", " ", TODAY)).containsOnly(entry(NAME, "Name on card is required"));
    }

    @Test
    void formattingAsTyped() {
        assertThat(CardRules.format("4242424242424242")).isEqualTo("4242 4242 4242 4242");
        assertThat(CardRules.format("4242-42")).isEqualTo("4242 42");
        assertThat(CardRules.format("42424242424242429999")).isEqualTo("4242 4242 4242 4242");
        assertThat(CardRules.formatExpiry("12")).isEqualTo("12");
        assertThat(CardRules.formatExpiry("123")).isEqualTo("12/3");
        assertThat(CardRules.formatExpiry("12/30")).isEqualTo("12/30");
        assertThat(CardRules.formatExpiry("12/305")).isEqualTo("12/30");
    }

    @Test
    void paymentDescriptions() {
        assertThat(Payment.invoice("PO-2026-131").describe()).isEqualTo("Invoice (30 days) · PO PO-2026-131");
        assertThat(Payment.invoice(" ").describe()).isEqualTo("Invoice (30 days)");
        Payment card = Payment.card("4242 4242 4242 1881");
        assertThat(card.isInvoice()).isFalse();
        assertThat(card.cardLast4()).isEqualTo("1881");
        assertThat(card.describe()).isEqualTo("Card ending 1881");
    }
}
