package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final AtomicInteger changes = new AtomicInteger();

    {
        store.addListener(changes::incrementAndGet);
    }

    private Customer customer(String name) {
        return store.findCustomers(name).get(0);
    }

    private Product product(String sku) {
        return store.catalog().find(sku).get();
    }

    @Test
    void findsCustomersByNameOrCitySortedByName() {
        assertThat(store.findCustomers("a")).extracting(Customer::name).containsExactly(
                "Ada Shop Ltd", "Birch & Co", "Café Lumen", "Durand Bureau", "Fjord Supplies");
        assertThat(store.findCustomers("  BERLIN ")).extracting(Customer::name).containsExactly("Ada Shop Ltd");
        assertThat(store.findCustomers("zzz")).isEmpty();
    }

    @Test
    void placingUsesTodayAndTellsListeners() {
        Customer ada = customer("Ada");
        Cart cart = new Cart();
        cart.add(product("PAP-A4-500"), 3);
        cart.add(product("PEN-GEL-BLK"), 10);
        Order order = store.place(ada, cart, ada.address(), Shipping.STANDARD, "", Payment.invoice("PO-2026-140"));
        assertThat(order.number()).isEqualTo("ORD-1640");
        assertThat(order.date()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(order.totals().total()).isEqualTo(Money.of("37.20"));
        assertThat(ada.owes()).isEqualTo(Money.of("487.20"));
        assertThat(store.orders().openCount()).isEqualTo(5);
        assertThat(changes.get()).isEqualTo(1);
    }

    @Test
    void cancelIsAllOrNothing() {
        Order placed = store.orders().find("ORD-1639").get();
        Order shipped = store.orders().find("ORD-1636").get();
        int clips = product("DSK-CLIPS").stock();
        assertThatThrownBy(() -> store.cancel(Arrays.asList(placed, shipped)))
                .hasMessage("Only placed orders can be cancelled: ORD-1636");
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(product("DSK-CLIPS").stock()).isEqualTo(clips);
        assertThat(changes.get()).isZero();

        store.cancel(Arrays.asList(placed));
        assertThat(placed.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product("DSK-CLIPS").stock()).isEqualTo(clips + 2);
        assertThat(customer("Ada").owes()).isEqualTo(Money.of("354.48"));   // 450.00 - 95.52
        assertThat(changes.get()).isEqualTo(1);
    }

    @Test
    void listenersCanBeRemoved() {
        Runnable extra = changes::incrementAndGet;
        store.addListener(extra);
        store.removeListener(extra);
        store.cancel(Arrays.asList(store.orders().find("ORD-1638").get()));
        assertThat(changes.get()).isEqualTo(1);
    }

    @Test
    void aThrowingListenerDoesNotStopTheOthersNorUndoThePlace() {
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler before = thread.getUncaughtExceptionHandler();
        java.util.List<Throwable> reported = new java.util.ArrayList<>();
        thread.setUncaughtExceptionHandler((t, e) -> reported.add(e));
        try {
            store.addListener(() -> {
                throw new IllegalArgumentException("listener broke");
            });
            AtomicInteger later = new AtomicInteger();
            store.addListener(later::incrementAndGet);
            Customer fjord = customer("Fjord");
            Cart cart = new Cart();
            cart.add(product("CLN-WIPES"), 1);
            Order order = store.place(fjord, cart, fjord.address(), Shipping.STANDARD, "", Payment.card("4242424242424242"));
            assertThat(order.number()).isEqualTo("ORD-1640");
            assertThat(store.orders().nextNumber()).isEqualTo("ORD-1641");
            assertThat(changes.get()).isEqualTo(1);
            assertThat(later.get()).isEqualTo(1);
            assertThat(reported).hasSize(1);
            assertThat(reported.get(0)).hasMessage("listener broke");
        } finally {
            thread.setUncaughtExceptionHandler(before);
        }
    }
}
