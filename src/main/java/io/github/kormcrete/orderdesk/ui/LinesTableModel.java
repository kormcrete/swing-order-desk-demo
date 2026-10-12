package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.OrderLine;
import io.github.kormcrete.orderdesk.model.PricedLine;
import io.github.kormcrete.orderdesk.model.Pricing;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;

/** Read-only lines: the Review step (Product, Qty, Price, Line total) and an order's Items tab (SKU first). */
final class LinesTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"SKU", "Product", "Qty", "Price", "Line total"};
    private static final Class<?>[] TYPES = {String.class, String.class, Integer.class, Money.class, Money.class};

    private final int first;
    private List<? extends PricedLine> lines = Collections.emptyList();

    /** Product, Qty, Price, Line total. */
    LinesTableModel() {
        first = 1;
    }

    /** SKU, Product, Qty, Price, Line total. */
    static LinesTableModel withSku(List<OrderLine> lines) {
        LinesTableModel model = new LinesTableModel(0);
        model.setLines(lines);
        return model;
    }

    private LinesTableModel(int first) {
        this.first = first;
    }

    void setLines(List<? extends PricedLine> lines) {
        this.lines = lines;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return lines.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length - first;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column + first];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column + first];
    }

    @Override
    public Object getValueAt(int row, int column) {
        PricedLine line = lines.get(row);
        switch (column + first) {
            case 0:
                return ((OrderLine) line).sku();
            case 1:
                return line.name();
            case 2:
                return line.quantity();
            case 3:
                return line.price();
            default:
                return Pricing.lineTotal(line);
        }
    }
}
