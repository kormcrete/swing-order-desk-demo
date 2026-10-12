package io.github.kormcrete.orderdesk.model;

/** A product in the catalog; only its stock changes. */
public final class Product {

    private final String sku;
    private final String name;
    private final Category category;
    private final Money price;
    private int stock;

    public Product(String sku, String name, Category category, Money price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public Category category() {
        return category;
    }

    public Money price() {
        return price;
    }

    public int stock() {
        return stock;
    }

    void setStock(int stock) {
        this.stock = stock;
    }

    public boolean inStock() {
        return stock > 0;
    }

    /** Five or fewer left: the products table shows a warning. */
    public boolean lowStock() {
        return stock > 0 && stock <= 5;
    }

    /**
     * "Only 4 Staplers in stock"; a name with a comma reads "Only 35 × Bin bags, roll of 50 in stock".
     * One left adds no plural "s" ("Only 1 Stapler in stock", "Only 1 × Bin bags, roll of 50 in stock").
     * None left reads "No Staplers in stock" ("No Bin bags, roll of 50 in stock" for a name with a comma).
     */
    public String onlyInStockMessage() {
        boolean comma = name.indexOf(',') >= 0;
        String plural = comma || stock == 1 || name.endsWith("s") ? name : name + "s";
        if (stock == 0) return "No " + plural + " in stock";
        String what = comma ? stock + " × " + name : stock + " " + plural;
        return "Only " + what + " in stock";
    }

    @Override
    public String toString() {
        return name;
    }
}
