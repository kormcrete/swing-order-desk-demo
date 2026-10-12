package io.github.kormcrete.orderdesk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** The products and their category tree. Stock changes only through placing and cancelling orders. */
public final class Catalog {

    private final Category root;
    private final List<Product> products;

    public Catalog(Category root, List<Product> products) {
        this.root = root;
        this.products = Collections.unmodifiableList(new ArrayList<>(products));
    }

    /** "All products": every category is below it. */
    public Category root() {
        return root;
    }

    public List<Product> products() {
        return products;
    }

    public Optional<Product> find(String sku) {
        return products.stream().filter(p -> p.sku().equals(sku)).findFirst();
    }

    void take(Product product, int quantity) {
        if (quantity > product.stock()) throw new IllegalStateException(product.onlyInStockMessage());
        product.setStock(product.stock() - quantity);
    }

    void giveBack(Product product, int quantity) {
        product.setStock(product.stock() + quantity);
    }
}
