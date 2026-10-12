package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Product;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** The cart: editable quantities, stock problems, Remove, totals and Checkout… with its reason when disabled. */
final class CartPanel extends JPanel {

    private final Cart cart;
    private final Runnable onChange;
    private final CartTableModel model;
    private final JTable table;
    private final QuantityEditor editor = new QuantityEditor();
    private final JScrollPane scroll;
    private final JLabel empty = Ui.hint("Select products and press Add to cart.");
    private final JLabel stockMessage = Ui.fieldError();
    private final JButton remove = new JButton("Remove", Icons.get(Icons.X));
    private final JLabel removeHint = Ui.hint("");
    private final TotalsPanel totals = new TotalsPanel();
    private final JButton checkout = new JButton("Checkout…");
    private final JLabel checkoutHint = Ui.hint("");

    CartPanel(Cart cart, Runnable onChange, Runnable onCheckout) {
        super(new MigLayout("insets 0, fill, hidemode 3", "[grow,fill]", "[]4[grow,fill]2[]8[]8[]"));
        this.cart = cart;
        this.onChange = onChange;
        model = new CartTableModel(cart, this::changed);
        table = Ui.table(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        QuantityRenderer renderer = new QuantityRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellEditor(editor);
        table.setRowHeight(Math.max(table.getRowHeight(), renderer.getPreferredSize().height + 4));
        Ui.widths(table, 0, 104, 84, 96);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        scroll = Ui.scroll(table);

        Ui.primary(checkout);
        remove.addActionListener(e -> removeSelected());
        checkout.addActionListener(e -> onCheckout.run());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            updateRemove();
            onChange.run();
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove-lines");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "remove-lines");
        table.getActionMap().put("remove-lines", Ui.action(this::removeSelected));
        JPopupMenu menu = Ui.rowPopupMenu(table);
        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> removeSelected());
        menu.add(removeItem);
        table.setComponentPopupMenu(menu);

        add(Ui.heading("Cart"), "wrap");
        add(scroll, "grow, wrap");
        add(stockMessage, "wrap");
        add(remove, "split 3, top, growx 0");
        add(removeHint, "top, gaptop 4");
        add(totals, "gapleft push, wrap");
        add(checkoutHint, "split 2, gapleft push");
        add(checkout);
        refresh();
    }

    /** The cart changed from outside the table (added products, stock placed elsewhere). */
    void refresh() {
        model.fireTableDataChanged();
        updateView();
    }

    private void changed() {
        updateView();
        onChange.run();
    }

    private void updateView() {
        List<String> problems = cart.stockProblems();
        stockMessage.setText(problems.isEmpty() ? " " : String.join("; ", problems));
        totals.show(cart.totals(), "chosen at checkout");
        Ui.showInViewport(scroll, cart.isEmpty() ? empty : table);
        updateRemove();
        table.repaint();
    }

    /** Remove needs a selected line; with lines in the cart and none selected, the hint says so. */
    private void updateRemove() {
        remove.setEnabled(hasSelection());
        removeHint.setText(hasSelection() || cart.isEmpty() ? "" : "Select a cart line first");
    }

    void setCheckoutBlockedReason(String reason) {
        checkout.setEnabled(reason == null);
        checkoutHint.setText(reason == null ? "" : reason);
    }

    boolean hasSelection() {
        return table.getSelectedRowCount() > 0;
    }

    /** More than one line asks first. */
    void removeSelected() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) return;
        if (rows.length > 1 && !Dialogs.confirm(this, "Remove lines", "Remove " + rows.length + " lines from the cart?",
                null, "Remove lines", "Keep")) {
            return;
        }
        List<Product> products = new ArrayList<>();
        for (int row : rows) products.add(model.lineAt(table.convertRowIndexToModel(row)).product());
        cart.remove(products);
        model.fireTableDataChanged();
        changed();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (editor != null) SwingUtilities.updateComponentTreeUI(editor.component());
    }

    // for tests

    JTable table() {
        return table;
    }

    String stockMessage() {
        return stockMessage.getText().trim();
    }

    TotalsPanel totals() {
        return totals;
    }

    String emptyMessage() {
        return scroll.getViewport().getView() == empty ? empty.getText() : null;
    }

    JButton checkoutButton() {
        return checkout;
    }

    String checkoutHint() {
        return checkoutHint.getText();
    }

    JButton removeButton() {
        return remove;
    }

    String removeHint() {
        return removeHint.getText();
    }
}
