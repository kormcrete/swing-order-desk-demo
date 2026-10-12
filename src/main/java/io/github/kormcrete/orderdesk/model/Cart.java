package io.github.kormcrete.orderdesk.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** The lines of a new order, in the order they were added. */
public final class Cart {

    private final List<CartLine> lines = new ArrayList<>();

    /** Adds to the product's line if there is one. */
    public void add(Product product, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1, was " + quantity);
        if (!product.inStock()) throw new IllegalArgumentException(product.name() + " is out of stock");
        CartLine line = lineOf(product);
        if (line == null) lines.add(new CartLine(product, quantity));
        else line.setQuantity(line.quantity() + quantity);
    }

    public void setQuantity(Product product, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1, was " + quantity);
        CartLine line = lineOf(product);
        if (line == null) throw new IllegalArgumentException(product.name() + " is not in the cart");
        line.setQuantity(quantity);
    }

    public void remove(Collection<Product> products) {
        lines.removeIf(l -> products.contains(l.product()));
    }

    public List<CartLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int units() {
        return lines.stream().mapToInt(CartLine::quantity).sum();
    }

    /** "Only 4 Staplers in stock", one per line that asks for more than the shop has. */
    public List<String> stockProblems() {
        return lines.stream().filter(CartLine::overStock)
                .map(l -> l.product().onlyInStockMessage()).collect(Collectors.toList());
    }

    /** Before checkout: no shipping yet, VAT on the goods. */
    public Totals totals() {
        return Pricing.of(lines, null);
    }

    private CartLine lineOf(Product product) {
        for (CartLine line : lines) {
            if (line.product() == product) return line;
        }
        return null;
    }
}
