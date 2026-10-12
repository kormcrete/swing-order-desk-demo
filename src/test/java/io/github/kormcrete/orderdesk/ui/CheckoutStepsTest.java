package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.AddressField;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Country;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Payment;
import io.github.kormcrete.orderdesk.model.Pricing;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutStepsTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final AtomicInteger changes = new AtomicInteger();

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private Customer customer(String name) {
        return store.findCustomers(name).get(0);
    }

    @Test
    void addressStepStartsFromTheCustomerAndShowsErrorsAtOnce() {
        Edt.run(() -> {
            AddressStep step = new AddressStep(customer("Elm").address(), changes::incrementAndGet);
            assertThat(step.field(AddressField.NAME).getText()).isEqualTo("Mia Hoffmann");
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEqualTo("Postal code is required");
            assertThat(step.problem()).isEqualTo("Fix the fields marked in red");
            assertThat(changes.get()).isZero();

            step.field(AddressField.POSTAL_CODE).setText("04109");
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEmpty();
            assertThat(step.problem()).isNull();
            assertThat(changes.get()).isPositive();

            step.countryBox().setSelectedItem(Country.AUSTRIA);
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEqualTo("Austria uses a 4-digit postal code");
            assertThat(step.address().country()).isEqualTo(Country.AUSTRIA);
        });
    }

    @Test
    void validPrefilledFieldsShowTheirErrorOnlyAfterTheyWereLeft() {
        Edt.run(() -> {
            AddressStep step = new AddressStep(customer("Ada").address(), changes::incrementAndGet);
            assertThat(step.problem()).isNull();

            step.field(AddressField.POSTAL_CODE).setText("123");
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEmpty();
            // nothing is red yet, so the reason names the problem itself
            assertThat(step.problem()).isEqualTo("Germany uses a 5-digit postal code");

            int before = changes.get();
            step.leave(AddressField.POSTAL_CODE);
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isNotEmpty();
            assertThat(step.problem()).isEqualTo("Fix the fields marked in red");
            assertThat(changes.get()).isGreaterThan(before);   // Next's reason is refreshed

            step.field(AddressField.POSTAL_CODE).setText("");
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEqualTo("Postal code is required");
            step.field(AddressField.POSTAL_CODE).setText("10119");
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEmpty();
            assertThat(step.problem()).isNull();
        });
    }

    @Test
    void changingTheCountryShowsAPostalCodeErrorAtOnce() {
        Edt.run(() -> {
            AddressStep step = new AddressStep(customer("Ada").address(), changes::incrementAndGet);
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEmpty();
            step.countryBox().setSelectedItem(Country.AUSTRIA);
            assertThat(step.errorText(AddressField.POSTAL_CODE)).isEqualTo("Austria uses a 4-digit postal code");
        });
    }

    @Test
    void shippingStepPricesAndExpressRule() {
        Edt.run(() -> {
            ShippingStep step = new ShippingStep(changes::incrementAndGet);
            step.enter(Country.GERMANY, Money.of("26.10"));
            assertThat(step.standardPrice()).isEqualTo("€4.90");
            assertThat(step.expressButton().isEnabled()).isTrue();
            step.expressButton().doClick();
            assertThat(step.method()).isEqualTo(Shipping.EXPRESS);

            step.enter(Country.NETHERLANDS, Money.of("120.00"));
            assertThat(step.standardPrice()).isEqualTo("Free");
            assertThat(step.expressButton().isEnabled()).isFalse();
            assertThat(step.expressReason()).isEqualTo("Express delivers to Germany and Austria only");
            assertThat(step.method()).isEqualTo(Shipping.STANDARD);

            step.noteArea().setText("  Ring twice. ");
            assertThat(step.note()).isEqualTo("Ring twice.");
        });
    }

    @Test
    void theDeliveryNoteIsOneLineAndTabLeavesIt() {
        Edt.run(() -> {
            ShippingStep step = new ShippingStep(changes::incrementAndGet);
            step.noteArea().setText("Ring twice.\nLeave at the\r\n\n  back door. ");
            assertThat(step.note()).isEqualTo("Ring twice. Leave at the back door.");
            assertThat(step.noteArea().getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS))
                    .contains(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
            assertThat(step.noteArea().getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS))
                    .contains(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
        });
    }

    @Test
    void anUnshownAddressErrorNamesItself() {
        Edt.run(() -> {
            AddressStep step = new AddressStep(customer("Ada").address(), changes::incrementAndGet);
            step.countryBox().setSelectedItem(Country.AUSTRIA);   // shown at once: red
            assertThat(step.problem()).isEqualTo("Fix the fields marked in red");
            step.field(AddressField.POSTAL_CODE).setText("1010");
            step.field(AddressField.CITY).setText("");             // not left yet: not red
            assertThat(step.errorText(AddressField.CITY)).isEmpty();
            assertThat(step.problem()).isEqualTo("City is required");
        });
    }

    @Test
    void paymentStepForAnAccountCustomerWithPoNumbers() {
        assertThat(Edt.call(() -> new PaymentStep(customer("Ada"), () -> LocalDate.of(2026, 10, 12), () -> { })
                .poField().isVisible())).isTrue();
        Edt.run(() -> {
            PaymentStep step = new PaymentStep(customer("Ada"), () -> LocalDate.of(2026, 10, 12), changes::incrementAndGet);
            assertThat(step.invoiceButton().isSelected()).isTrue();
            assertThat(step.invoiceNote()).isEqualTo("Credit limit €2,000.00 · owes €450.00");
            assertThat(step.cardNumberField().isEnabled()).isFalse();
            assertThat(step.problem()).isEqualTo("Enter the PO number");
            step.poField().setText("PO-2026-140");
            assertThat(step.problem()).isNull();
            assertThat(step.payment().describe()).isEqualTo("Invoice (30 days) · PO PO-2026-140");
        });
    }

    @Test
    void paymentStepForACardOnlyCustomer() {
        PaymentStep step = Edt.call(() -> new PaymentStep(customer("Lumen"), () -> LocalDate.of(2026, 10, 12), changes::incrementAndGet));
        Edt.run(() -> {
            assertThat(step.invoiceButton().isEnabled()).isFalse();
            assertThat(step.cardButton().isSelected()).isTrue();
            assertThat(step.invoiceNote()).isEqualTo("Café Lumen pays by card");
            assertThat(step.poField().isVisible()).isFalse();   // no PO number for a card-only customer
            assertThat(step.problem()).isEqualTo("Complete the card details");
            step.cardNumberField().setText("4242424242424242");
            step.expiryField().setText("1230");
            step.nameOnCardField().setText("Noor de Vries");
        });
        Edt.run(() -> {   // formatting runs after the typing event
            assertThat(step.cardNumberField().getText()).isEqualTo("4242 4242 4242 4242");
            assertThat(step.expiryField().getText()).isEqualTo("12/30");
            assertThat(step.problem()).isNull();
            assertThat(step.payment().describe()).isEqualTo("Card ending 4242");
        });
    }

    @Test
    void cardErrorsShowAfterTheFieldWasLeft() {
        Edt.run(() -> {
            PaymentStep step = new PaymentStep(customer("Fjord"), () -> LocalDate.of(2026, 10, 12), changes::incrementAndGet);
            step.cardNumberField().setText("4242");
            assertThat(step.errorText(step.cardNumberField())).isEmpty();
            step.leave(step.cardNumberField());
            assertThat(step.errorText(step.cardNumberField())).isEqualTo("Enter the 16 digits of the card");
        });
    }

    @Test
    void reviewStepSummarises() {
        Edt.run(() -> {
            Customer ada = customer("Ada");
            Cart cart = new Cart();
            cart.add(store.catalog().find("PAP-A4-500").get(), 3);
            cart.add(store.catalog().find("PEN-GEL-BLK").get(), 10);
            ReviewStep step = new ReviewStep(i -> { });
            step.show(ada.address(), Shipping.STANDARD, "Ring twice.", Payment.invoice("PO-1"), cart.lines(),
                    Pricing.of(cart.lines(), Shipping.STANDARD));
            assertThat(step.sectionLines("Delivery address")).containsExactly(
                    "Ada Lindqvist", "Torstraße 118", "10119 Berlin", "Germany", "+49 30 5550 1188");
            assertThat(step.sectionLines("Shipping")).containsExactly("Standard · 3–5 business days · €4.90", "Note: Ring twice.");
            assertThat(step.sectionLines("Payment")).containsExactly("Invoice (30 days) · PO PO-1");
            assertThat(step.totals().valueOf("Total")).isEqualTo("€37.20");
        });
    }
}
