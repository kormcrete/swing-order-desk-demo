package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Product;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/** SKU, Product, Price, Stock. */
final class ProductsTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"SKU", "Product", "Price", "Stock"};
    private static final Class<?>[] TYPES = {String.class, String.class, Money.class, Integer.class};

    private final List<Product> products;

    ProductsTableModel(List<Product> products) {
        this.products = products;
    }

    Product productAt(int modelRow) {
        return products.get(modelRow);
    }

    int indexOf(Product product) {
        return products.indexOf(product);
    }

    @Override
    public int getRowCount() {
        return products.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Product product = products.get(row);
        switch (column) {
            case 0:
                return product.sku();
            case 1:
                return product.name();
            case 2:
                return product.price();
            default:
                return product.stock();
        }
    }
}
