package io.github.kormcrete.orderdesk.model;

/** A product and how many of it, in a cart; the price is the product's current price. */
public final class CartLine implements PricedLine {

    private final Product product;
    private int quantity;

    CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product product() {
        return product;
    }

    @Override
    public String name() {
        return product.name();
    }

    @Override
    public Money price() {
        return product.price();
    }

    @Override
    public int quantity() {
        return quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /** More than the shop has: the cart shows an error and checkout waits. */
    public boolean overStock() {
        return quantity > product.stock();
    }
}
