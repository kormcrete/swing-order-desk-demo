package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.OrderStatus;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Totals;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

/** The small building blocks every screen uses, so they look and behave the same. */
final class Ui {

    private Ui() {
    }

    /** A small bold caption above a block: "Categories", "Cart". */
    static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: -1 bold; foreground: $OrderDesk.muted");
        return label;
    }

    /** Grey text: why something is disabled, an empty state, a hint. */
    static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground: $OrderDesk.muted");
        return label;
    }

    /** Red text under a field; a single space keeps the row's height when there is no error. */
    static JLabel fieldError() {
        JLabel label = new JLabel(" ");
        label.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Actions.Red");   // readable in both themes
        return label;
    }

    /** Red outline and message, or neither when {@code message} is null. */
    static void showError(JComponent field, JLabel error, String message) {
        field.putClientProperty(FlatClientProperties.OUTLINE, message == null ? null : FlatClientProperties.OUTLINE_ERROR);
        error.setText(message == null ? " " : message);
    }

    /**
     * The one accent-filled button of a screen: Checkout…, Next, Place order. The focused* keys keep it filled while it
     * has the focus, and the default.* keys while it is a dialog's default button (Next): FlatLaf paints both states
     * with its own light colours otherwise, under white text.
     */
    static void primary(JButton button) {
        String accent = "$Component.accentColor";
        button.putClientProperty(FlatClientProperties.STYLE, "background: " + accent + "; foreground: #ffffff;"
                + " borderColor: " + accent + "; hoverBackground: darken(" + accent + ",6%);"
                + " pressedBackground: darken(" + accent + ",12%); font: bold;"
                + " focusedBackground: " + accent + "; focusedForeground: #ffffff;"
                + " default.background: " + accent + "; default.foreground: #ffffff;"
                + " default.focusedBackground: " + accent + "; default.focusedForeground: #ffffff;"
                + " default.hoverBackground: darken(" + accent + ",6%);"
                + " default.pressedBackground: darken(" + accent + ",12%);"
                + " default.borderColor: " + accent + "; default.focusedBorderColor: " + accent);
    }

    /** A search box: placeholder, magnifier, clear button. */
    static JTextField searchField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.get(Icons.SEARCH));
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        return field;
    }

    /** "Standard · 3–5 business days · €4.90", or "· Free". */
    static String shippingText(Shipping shipping, Totals totals) {
        return shipping.describe() + " · " + (totals.shipping().isZero() ? "Free" : totals.shipping().format());
    }

    static JTable table(TableModel model) {
        return configure(new JTable(model));
    }

    /** Fixed columns, number/money/date/status renderers, headers aligned like their cells, fills its viewport. */
    static <T extends JTable> T configure(T table) {
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        alignHeaders(table);
        table.setDefaultRenderer(Integer.class, new IntegerRenderer());
        table.setDefaultRenderer(Money.class, new MoneyRenderer());
        table.setDefaultRenderer(LocalDate.class, new DateRenderer());
        table.setDefaultRenderer(OrderStatus.class, new StatusRenderer());
        return table;
    }

    /**
     * Column widths in unscaled pixels; 0 marks the one column that takes the rest of the width (a name), the others
     * keep theirs when the table grows.
     */
    static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            if (widths[i] > 0) {
                column.setPreferredWidth(UIScale.scale(widths[i]));
                column.setMaxWidth(UIScale.scale(widths[i]));
            } else {
                column.setPreferredWidth(UIScale.scale(200));
            }
        }
    }

    /**
     * Numbers and money right-aligned in the header as in the cells, text left-aligned. It wraps the header's own
     * renderer (FlatLaf keeps painting its column lines for a renderer that delegates to it).
     */
    static void alignHeaders(JTable table) {
        JTableHeader header = table.getTableHeader();
        TableCellRenderer original = header.getDefaultRenderer();
        header.setDefaultRenderer((t, value, selected, focused, row, column) -> {
            Component c = original.getTableCellRendererComponent(t, value, selected, focused, row, column);
            if (c instanceof JLabel && t != null && column >= 0) ((JLabel) c).setHorizontalAlignment(headerAlignment(t, column));
            return c;
        });
    }

    static int headerAlignment(JTable table, int viewColumn) {
        Class<?> type = table.getColumnClass(viewColumn);
        return Number.class.isAssignableFrom(type) || Money.class == type ? SwingConstants.RIGHT : SwingConstants.LEADING;
    }

    static JScrollPane scroll(Component view) {
        return new JScrollPane(view);
    }

    /** Swaps a table for its empty-state label and back. */
    static void showInViewport(JScrollPane scroll, Component view) {
        if (scroll.getViewport().getView() != view) scroll.setViewportView(view);
    }

    static void onTextChange(JTextComponent field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    static Action action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
    }

    static JLabel labelFor(String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        return label;
    }

    /**
     * A popup menu for a table's rows. The look and feel shows a component's popup before any mouse listener of the
     * component runs, so for a right-click the row under the pointer is selected here, in {@code show}, before the
     * menu's listeners (and so its enabled state) see the selection. From the keyboard (Shift+F10, the Menu key) the
     * selection stays as it is and the menu opens at the lead selected row.
     */
    static JPopupMenu rowPopupMenu(JTable table) {
        return new JPopupMenu() {
            @Override
            public void show(Component invoker, int x, int y) {
                if (invoker == table) {
                    Point at = popupPoint(table, x, y, EventQueue.getCurrentEvent());
                    x = at.x;
                    y = at.y;
                }
                super.show(invoker, x, y);
            }
        };
    }

    /**
     * Where the row menu opens. A mouse event selects the row under the point and opens there; anything else (a key)
     * keeps the selection and opens below the lead selected row's first cell when that row is in view.
     */
    static Point popupPoint(JTable table, int x, int y, AWTEvent cause) {
        if (cause instanceof MouseEvent) {
            selectRowForPopup(table, x, y);
            return new Point(x, y);
        }
        int row = table.getSelectionModel().getLeadSelectionIndex();
        if (row < 0 || row >= table.getRowCount() || !table.isRowSelected(row)) row = table.getSelectedRow();
        if (row < 0) return new Point(x, y);
        Rectangle cell = table.getCellRect(row, 0, false);
        if (!table.getVisibleRect().intersects(cell)) return new Point(x, y);
        return new Point(cell.x + cell.height / 2, cell.y + cell.height);
    }

    /** Selects the row at the point unless it is already selected (keeps a multi-row selection). */
    static void selectRowForPopup(JTable table, int x, int y) {
        int row = table.rowAtPoint(new Point(x, y));
        if (row >= 0 && !table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
    }

    static void onDoubleClick(JTable table, Runnable action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    action.run();
                }
            }
        });
    }

    /** Lines of text stacked, e.g. an address. */
    static JPanel textColumn(List<String> lines) {
        JPanel column = new JPanel(new MigLayout("insets 0, gap 0, wrap 1"));
        column.setOpaque(false);   // on whatever background its parent has
        setLines(column, lines);
        return column;
    }

    static void setLines(JPanel column, List<String> lines) {
        column.removeAll();
        for (String line : lines) column.add(new JLabel(line));
        column.revalidate();
        column.repaint();
    }
}
