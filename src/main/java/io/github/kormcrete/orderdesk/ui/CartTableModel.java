package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.CartLine;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Pricing;

import javax.swing.table.AbstractTableModel;

/** Product, Qty (editable, at least 1), Price, Line total. */
final class CartTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Product", "Qty", "Price", "Line total"};
    private static final Class<?>[] TYPES = {String.class, Integer.class, Money.class, Money.class};

    private final Cart cart;
    private final Runnable onChange;

    CartTableModel(Cart cart, Runnable onChange) {
        this.cart = cart;
        this.onChange = onChange;
    }

    CartLine lineAt(int row) {
        return cart.lines().get(row);
    }

    @Override
    public int getRowCount() {
        return cart.lines().size();
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
    public boolean isCellEditable(int row, int column) {
        return column == 1;
    }

    @Override
    public Object getValueAt(int row, int column) {
        CartLine line = cart.lines().get(row);
        switch (column) {
            case 0:
                return line.name();
            case 1:
                return line.quantity();
            case 2:
                return line.price();
            default:
                return Pricing.lineTotal(line);
        }
    }

    /** Anything but a whole number of at least 1 is ignored; the cart never sees it. */
    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column != 1 || !(value instanceof Integer) || (Integer) value < 1) return;
        CartLine line = cart.lines().get(row);
        if (line.quantity() == (Integer) value) return;
        cart.setQuantity(line.product(), (Integer) value);
        fireTableRowsUpdated(row, row);
        onChange.run();
    }
}
