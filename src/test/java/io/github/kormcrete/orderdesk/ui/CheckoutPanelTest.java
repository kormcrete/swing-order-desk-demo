package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.AddressField;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final List<String> asked = new ArrayList<>();
    private final Deque<Integer> answers = new ArrayDeque<>();
    private final CountDownLatch placed = new CountDownLatch(1);
    private final AtomicReference<Order> order = new AtomicReference<>();
    private boolean cancelled;
    private Cart lastCart;
    private final List<Throwable> uncaught = new CopyOnWriteArrayList<>();
    private Thread.UncaughtExceptionHandler previousHandler;

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private CheckoutPanel checkout(String customerName, Object... skuQuantity) {
        Customer customer = store.findCustomers(customerName).get(0);
        Cart cart = new Cart();
        lastCart = cart;
        for (int i = 0; i < skuQuantity.length; i += 2) {
            cart.add(store.catalog().find((String) skuQuantity[i]).get(), (Integer) skuQuantity[i + 1]);
        }
        CheckoutPanel.Asker asker = (title, message, detail, type, options) -> {
            asked.add(message);
            return answers.isEmpty() ? -1 : answers.poll();
        };
        return Edt.call(() -> new CheckoutPanel(store, customer, cart, asker, o -> {
            order.set(o);
            placed.countDown();
        }, () -> cancelled = true, 0));
    }

    @Test
    void placesAnInvoiceOrderEndToEnd() throws Exception {
        CheckoutPanel panel = checkout("Ada", "PAP-A4-500", 3, "PEN-GEL-BLK", 10);
        Edt.run(() -> {
            assertThat(panel.step()).isZero();
            assertThat(panel.reason()).isEmpty();
            panel.next();
            assertThat(panel.step()).isEqualTo(1);
            assertThat(panel.shippingStep().standardPrice()).isEqualTo("€4.90");
            panel.next();
            assertThat(panel.step()).isEqualTo(2);
            assertThat(panel.reason()).isEqualTo("Enter the PO number");
            assertThat(panel.nextButton().isEnabled()).isFalse();
            panel.paymentStep().poField().setText("PO-2026-140");
            assertThat(panel.nextButton().isEnabled()).isTrue();
            panel.next();
            assertThat(panel.step()).isEqualTo(3);
            assertThat(panel.nextButton().getText()).isEqualTo("Place order");
            assertThat(panel.reviewStep().totals().valueOf("Total")).isEqualTo("€37.20");
            answers.add(0);
            panel.next();
            assertThat(asked).containsExactly("Place order for €37.20?");
            assertThat(panel.isPlacing()).isTrue();
            assertThat(panel.placingText()).isEqualTo("Order for €37.20 · Ada Shop Ltd");
            assertThat(panel.nextButton().isEnabled()).isFalse();
        });
        assertThat(placed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(order.get().number()).isEqualTo("ORD-1640");
        assertThat(order.get().payment().poNumber()).isEqualTo("PO-2026-140");
        assertThat(store.findCustomers("Ada").get(0).owes().format()).isEqualTo("€487.20");
    }

    @Test
    void notConfirmingPlaceOrderStaysOnReview() {
        CheckoutPanel panel = checkout("Fjord", "CLN-WIPES", 1);
        Edt.run(() -> {
            panel.goTo(3);
            answers.add(1);
            panel.next();
            assertThat(panel.isPlacing()).isFalse();
            assertThat(panel.step()).isEqualTo(3);
            assertThat(store.orders().nextNumber()).isEqualTo("ORD-1640");
        });
    }

    @Test
    void creditWarningCanSwitchToCard() {
        CheckoutPanel panel = checkout("Birch", "COF-ESPRESSO", 1);
        Edt.run(() -> {
            panel.goTo(2);
            answers.add(0);   // Pay by card instead
            panel.next();
            assertThat(asked).containsExactly("This order takes Birch & Co to €1,238.80 of its €1,000.00 credit limit.");
            assertThat(panel.step()).isEqualTo(2);
            assertThat(panel.paymentStep().cardButton().isSelected()).isTrue();
            assertThat(panel.reason()).isEqualTo("Complete the card details");
        });
    }

    @Test
    void creditWarningCanBeAccepted() {
        CheckoutPanel panel = checkout("Birch", "COF-ESPRESSO", 1);
        Edt.run(() -> {
            panel.goTo(2);
            answers.add(1);   // Continue with invoice
            panel.next();
            assertThat(panel.step()).isEqualTo(3);
        });
    }

    @Test
    void anIncompleteAddressBlocksNext() {
        CheckoutPanel panel = checkout("Elm", "CLN-WIPES", 1);
        Edt.run(() -> {
            assertThat(panel.reason()).isEqualTo("Fix the fields marked in red");
            assertThat(panel.nextButton().isEnabled()).isFalse();
            panel.next();
            assertThat(panel.step()).isZero();
            panel.addressStep().field(AddressField.POSTAL_CODE).setText("04109");
            assertThat(panel.nextButton().isEnabled()).isTrue();
        });
    }

    @Test
    void cardOnlyCustomerAbroad() {
        CheckoutPanel panel = checkout("Lumen", "COF-BEANS-1K", 2);
        Edt.run(() -> {
            panel.next();
            assertThat(panel.shippingStep().expressButton().isEnabled()).isFalse();
            panel.next();
            assertThat(panel.paymentStep().invoiceButton().isEnabled()).isFalse();
            assertThat(panel.reason()).isEqualTo("Complete the card details");
            panel.backButton().doClick();
            assertThat(panel.step()).isEqualTo(1);
        });
    }

    @Test
    void cancelClosesWhenNotPlacing() {
        CheckoutPanel panel = checkout("Ada", "PAP-A4-500", 1);
        Edt.run(panel::cancel);
        assertThat(cancelled).isTrue();
    }

    @Test
    void aRefusedPlaceLeavesThePanelUsableOnReviewAndPlacesNothing() throws Exception {
        CheckoutPanel panel = checkout("Fjord", "CLN-WIPES", 1);
        answers.add(0);
        answers.add(0);
        Edt.run(() -> {
            panel.goTo(3);
            lastCart.setQuantity(store.catalog().find("CLN-WIPES").get(), 500);   // more than the stock
            panel.next();
            assertThat(panel.isPlacing()).isTrue();
        });
        for (int i = 0; i < 100 && asked.size() < 2; i++) {
            Thread.sleep(50);
        }
        Edt.run(() -> {
            assertThat(asked).hasSize(2);
            assertThat(asked.get(1)).isEqualTo("The order could not be placed.");
            assertThat(panel.isPlacing()).isFalse();
            assertThat(panel.step()).isEqualTo(3);
            assertThat(panel.nextButton().isEnabled()).isTrue();
            assertThat(store.orders().nextNumber()).isEqualTo("ORD-1640");
            panel.cancel();
            assertThat(cancelled).isTrue();
        });
    }

    @Test
    void aThrowingStoreListenerDoesNotMakeAPlacedOrderLookFailed() throws Exception {
        CheckoutPanel panel = checkout("Fjord", "CLN-WIPES", 1);
        store.addListener(() -> {
            throw new IllegalArgumentException("listener broke");
        });
        answers.add(0);
        try {
            Edt.run(() -> {
                recordUncaughtOnTheEdt();
                panel.goTo(3);
                panel.next();
            });
            assertThat(placed.await(5, TimeUnit.SECONDS)).isTrue();
            Edt.run(() -> { });
            assertThat(asked).containsExactly("Place order for €9.96?");
            assertThat(store.orders().nextNumber()).isEqualTo("ORD-1641");
            assertThat(uncaught).extracting(Throwable::getMessage).containsExactly("listener broke");
        } finally {
            restoreTheEdtHandler();
        }
    }

    @Test
    void anExceptionFromOnPlacedIsNotReportedAsAFailedPlace() throws Exception {
        Customer customer = store.findCustomers("Fjord").get(0);
        Cart cart = new Cart();
        cart.add(store.catalog().find("CLN-WIPES").get(), 1);
        CheckoutPanel.Asker asker = (title, message, detail, type, options) -> {
            asked.add(message);
            return 0;
        };
        CheckoutPanel panel = Edt.call(() -> new CheckoutPanel(store, customer, cart, asker, o -> {
            order.set(o);
            placed.countDown();
            throw new IllegalStateException("after placing");
        }, () -> cancelled = true, 0));
        try {
            Edt.run(() -> {
                recordUncaughtOnTheEdt();
                panel.goTo(3);
                panel.next();
            });
            assertThat(placed.await(5, TimeUnit.SECONDS)).isTrue();
            Edt.run(() -> { });
            assertThat(asked).containsExactly("Place order for €9.96?");
            assertThat(store.orders().nextNumber()).isEqualTo("ORD-1641");
            assertThat(uncaught).extracting(Throwable::getMessage).containsExactly("after placing");
        } finally {
            restoreTheEdtHandler();
        }
    }

    /** Keeps the exceptions these tests expect out of the build output; call on the EDT. */
    private void recordUncaughtOnTheEdt() {
        Thread edt = Thread.currentThread();
        previousHandler = edt.getUncaughtExceptionHandler();
        edt.setUncaughtExceptionHandler((thread, e) -> uncaught.add(e));
    }

    private void restoreTheEdtHandler() {
        if (previousHandler != null) Edt.run(() -> Thread.currentThread().setUncaughtExceptionHandler(previousHandler));
    }
}
