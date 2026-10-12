package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Money;

import javax.swing.table.DefaultTableCellRenderer;

/** Money, right-aligned: {@code €1,234.50}. */
final class MoneyRenderer extends DefaultTableCellRenderer {

    MoneyRenderer() {
        setHorizontalAlignment(RIGHT);
    }

    @Override
    protected void setValue(Object value) {
        setText(value instanceof Money ? ((Money) value).format() : "");
    }
}
