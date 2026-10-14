package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/** Draws the Qty cell as a spinner, red-outlined when the line asks for more than is in stock. */
final class QuantityRenderer extends JSpinner implements TableCellRenderer {

    QuantityRenderer() {
        super(new SpinnerNumberModel(1, 0, 99999, 1));
        setEditor(new NumberEditor(this, "#"));   // digits only, whatever the machine's locale
        putClientProperty(FlatClientProperties.STYLE, QuantityEditor.COMPACT);   // fits a 26 px row
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        // a screen reader's call (JTable's accessibility) passes null: the spinner refuses anything but a number
        if (value instanceof Number) setValue(value);
        boolean overStock = ((CartTableModel) table.getModel()).lineAt(table.convertRowIndexToModel(row)).overStock();
        putClientProperty(FlatClientProperties.OUTLINE, overStock ? FlatClientProperties.OUTLINE_ERROR : null);
        return this;
    }
}
