package io.github.kormcrete.orderdesk.ui;

import javax.swing.table.DefaultTableCellRenderer;

/** Whole numbers (Items, Qty), right-aligned, digits only: {@code 1200} on every machine. */
final class IntegerRenderer extends DefaultTableCellRenderer {

    IntegerRenderer() {
        setHorizontalAlignment(RIGHT);
    }

    @Override
    protected void setValue(Object value) {
        setText(value instanceof Integer ? Integer.toString((Integer) value) : "");
    }
}
