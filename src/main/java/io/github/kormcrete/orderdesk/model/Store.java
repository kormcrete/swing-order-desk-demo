package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * What the UI reads and changes. Every change tells the listeners, synchronously; the UI changes the model on the
 * event dispatch thread only, so listeners run there too.
 */
public final class Store {

    private final Catalog catalog;
    private final List<Customer> customers;
    private final OrderBook orders;
    private final Supplier<LocalDate> today;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public Store(Catalog catalog, List<Customer> customers, OrderBook orders, Supplier<LocalDate> today) {
        this.catalog = catalog;
        this.customers = Collections.unmodifiableList(new ArrayList<>(customers));
        this.orders = orders;
        this.today = today;
    }

    public Catalog catalog() {
        return catalog;
    }

    public List<Customer> customers() {
        return customers;
    }

    public OrderBook orders() {
        return orders;
    }

    public LocalDate today() {
        return today.get();
    }

    /** By name or city, any case, sorted by name: the Customer field's suggestions. */
    public List<Customer> findCustomers(String query) {
        return customers.stream().filter(c -> c.matches(query))
                .sorted(Comparator.comparing(Customer::name)).collect(Collectors.toList());
    }

    public Order place(Customer customer, Cart cart, Address address, Shipping shipping, String note, Payment payment) {
        Order order = orders.place(customer, cart, address, shipping, note, payment, today.get());
        changed();
        return order;
    }

    /** Cancels all of them or, if one cannot be cancelled, none. */
    public void cancel(Collection<Order> toCancel) {
        for (Order order : toCancel) {
            if (!order.cancellable()) {
                throw new IllegalStateException("Only placed orders can be cancelled: " + order.number());
            }
        }
        for (Order order : toCancel) orders.cancel(order);
        changed();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /**
     * Tells every listener. A listener that throws does not stop the others and does not undo the change that was
     * made: its exception goes to the thread's uncaught-exception handler.
     */
    private void changed() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
            }
        }
    }
}
