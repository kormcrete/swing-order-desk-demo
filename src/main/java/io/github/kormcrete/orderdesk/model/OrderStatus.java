package io.github.kormcrete.orderdesk.model;

/** Where an order is. Only placed orders can be cancelled. */
public enum OrderStatus {
    PLACED("Placed"), SHIPPED("Shipped"), CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
