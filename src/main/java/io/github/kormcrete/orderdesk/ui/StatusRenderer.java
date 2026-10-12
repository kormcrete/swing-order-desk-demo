package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.OrderStatus;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;

/** The Status column: a pill on the row's background. */
final class StatusRenderer extends StatusPill implements TableCellRenderer {

    StatusRenderer() {
        super(16, 8);   // the table's 8 px cell padding
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setFont(pillFont(table.getFont()));
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setStatus((OrderStatus) value, isSelected);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
