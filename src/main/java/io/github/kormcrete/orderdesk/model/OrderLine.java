package io.github.kormcrete.orderdesk.model;

/** A line of a placed order, with the price it was placed at. */
public final class OrderLine implements PricedLine {

    private final Product product;
    private final Money price;
    private final int quantity;

    OrderLine(Product product, Money price, int quantity) {
        this.product = product;
        this.price = price;
        this.quantity = quantity;
    }

    static OrderLine of(CartLine line) {
        return new OrderLine(line.product(), line.price(), line.quantity());
    }

    public Product product() {
        return product;
    }

    public String sku() {
        return product.sku();
    }

    @Override
    public String name() {
        return product.name();
    }

    @Override
    public Money price() {
        return price;
    }

    @Override
    public int quantity() {
        return quantity;
    }
}
