package io.github.kormcrete.orderdesk.model;

/** A line that can be priced: a cart line or an order line. */
public interface PricedLine {

    String name();

    Money price();

    int quantity();
}
