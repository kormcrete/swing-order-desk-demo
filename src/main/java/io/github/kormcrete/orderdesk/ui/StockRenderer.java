package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Product;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/**
 * The products table's Stock column: a warning icon when stock runs low, with a tooltip that says so ("Low stock: 4
 * left"), "Out of stock" at 0.
 */
final class StockRenderer extends DefaultTableCellRenderer {

    StockRenderer() {
        setHorizontalAlignment(RIGHT);
        setHorizontalTextPosition(TRAILING);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Product product = ((ProductsTableModel) table.getModel()).productAt(table.convertRowIndexToModel(row));
        setText(product.inStock() ? String.valueOf(product.stock()) : "Out of stock");
        boolean low = product.lowStock();
        setIcon(low ? Icons.get(Icons.WARNING) : null);
        setToolTipText(low ? "Low stock: " + product.stock() + " left" : null);   // the renderer is shared: reset it
        return this;
    }
}
