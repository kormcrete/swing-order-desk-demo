package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Dates;

import javax.swing.table.DefaultTableCellRenderer;
import java.time.LocalDate;

/** {@code 9 Oct 2026}. */
final class DateRenderer extends DefaultTableCellRenderer {

    @Override
    protected void setValue(Object value) {
        setText(value instanceof LocalDate ? Dates.format((LocalDate) value) : "");
    }
}
