package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** All orders, the next order number, and what placing and cancelling do to stock and credit. */
public final class OrderBook {

    private final Catalog catalog;
    private final List<Order> orders = new ArrayList<>();
    private int nextNumber;

    OrderBook(Catalog catalog, int nextNumber) {
        this.catalog = catalog;
        this.nextNumber = nextNumber;
    }

    /** An order from before the app started (seed data): no effect on stock or credit. */
    void addExisting(Order order) {
        orders.add(order);
    }

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public Optional<Order> find(String number) {
        return orders.stream().filter(o -> o.number().equals(number)).findFirst();
    }

    /** The number the next placed order gets. */
    public String nextNumber() {
        return "ORD-" + nextNumber;
    }

    /** Checks the rules, takes the stock, adds an invoice total to what the customer owes. All or nothing. */
    public Order place(Customer customer, Cart cart, Address address, Shipping shipping, String note,
                       Payment payment, LocalDate date) {
        if (cart.isEmpty()) throw new IllegalStateException("The cart is empty");
        List<String> problems = cart.stockProblems();
        if (!problems.isEmpty()) throw new IllegalStateException(problems.get(0));
        if (!shipping.allowedFor(address.country())) {
            throw new IllegalStateException("Express delivers to Germany and Austria only");
        }
        if (payment.isInvoice() && !customer.terms().invoiceAllowed()) {
            throw new IllegalStateException(customer.name() + " pays by card");
        }
        List<OrderLine> lines = cart.lines().stream().map(OrderLine::of).collect(Collectors.toList());
        for (OrderLine line : lines) catalog.take(line.product(), line.quantity());
        Order order = new Order(nextNumber(), date, customer, lines, address, shipping, note, payment, OrderStatus.PLACED);
        nextNumber++;
        if (payment.isInvoice()) customer.setOwes(customer.owes().plus(order.totals().total()));
        orders.add(order);
        return order;
    }

    /** Gives the stock back and takes an invoice total off what the customer owes. */
    public void cancel(Order order) {
        if (!order.cancellable()) {
            throw new IllegalStateException("Only placed orders can be cancelled: " + order.number());
        }
        for (OrderLine line : order.lines()) catalog.giveBack(line.product(), line.quantity());
        if (order.payment().isInvoice()) {
            order.customer().setOwes(order.customer().owes().minus(order.totals().total()));
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    /** Placed orders: the status bar's "4 open orders". */
    public int openCount() {
        return (int) orders.stream().filter(o -> o.status() == OrderStatus.PLACED).count();
    }

    /** Customers by their latest order, newest first: the New order dropdown. */
    public List<Customer> recentCustomers(int max) {
        List<Order> byRecent = new ArrayList<>(orders);
        byRecent.sort(Comparator.comparing(Order::date).thenComparing(Order::number).reversed());
        List<Customer> out = new ArrayList<>();
        for (Order order : byRecent) {
            if (out.size() == max) break;
            if (!out.contains(order.customer())) out.add(order.customer());
        }
        return out;
    }
}
