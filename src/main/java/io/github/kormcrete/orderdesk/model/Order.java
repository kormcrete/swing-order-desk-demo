package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A placed order. Everything but its status is fixed when it is placed. */
public final class Order {

    private final String number;
    private final LocalDate date;
    private final Customer customer;
    private final List<OrderLine> lines;
    private final Address address;
    private final Shipping shipping;
    private final String note;
    private final Payment payment;
    private final Totals totals;
    private OrderStatus status;

    Order(String number, LocalDate date, Customer customer, List<OrderLine> lines, Address address,
          Shipping shipping, String note, Payment payment, OrderStatus status) {
        this.number = number;
        this.date = date;
        this.customer = customer;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        this.address = address;
        this.shipping = shipping;
        this.note = note == null ? "" : note;
        this.payment = payment;
        this.status = status;
        this.totals = Pricing.of(this.lines, shipping);
    }

    /** "ORD-1640". */
    public String number() {
        return number;
    }

    public LocalDate date() {
        return date;
    }

    public Customer customer() {
        return customer;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    /** Where it is delivered. */
    public Address address() {
        return address;
    }

    public Shipping shipping() {
        return shipping;
    }

    public String note() {
        return note;
    }

    public Payment payment() {
        return payment;
    }

    public Totals totals() {
        return totals;
    }

    public OrderStatus status() {
        return status;
    }

    void setStatus(OrderStatus status) {
        this.status = status;
    }

    /** The number of units, as the Items column shows it. */
    public int units() {
        return lines.stream().mapToInt(OrderLine::quantity).sum();
    }

    public boolean cancellable() {
        return status == OrderStatus.PLACED;
    }
}
