package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Dates;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Store;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.util.Collections;

/** An order's tab: number, status, facts, Save receipt…, Cancel order, and Items / Delivery / Invoice. */
final class OrderPanel extends JPanel {

    private final Store store;
    private final Workspace workspace;
    private final Order order;
    private final StatusPill status = Ui.named(new StatusPill(), "orderStatus");
    private final JLabel facts;
    private final JButton saveReceipt = new JButton("Save receipt…", Icons.get(Icons.SAVE));
    private final JButton cancel = new JButton("Cancel order", Icons.get(Icons.BAN));
    private final JLabel cancelHint = Ui.hint("");
    private final JTabbedPane details = new JTabbedPane() {
        /** Under Lagoon no box: the tabbed pane draws the line around its pages itself. */
        @Override
        public void updateUI() {
            boolean boxed = UIManager.getBoolean(Theme.BOXED_TABS);
            putClientProperty(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER, !boxed);
            super.updateUI();
        }
    };
    private final JTable itemsTable;
    private final Runnable storeListener = this::refresh;

    OrderPanel(Store store, Workspace workspace, Order order) {
        super(new MigLayout("insets 12, fill", "[grow,fill]", "[]0[]12[grow,fill]"));
        this.store = store;
        this.workspace = workspace;
        this.order = order;

        JLabel number = new JLabel(order.number());
        number.putClientProperty(FlatClientProperties.STYLE, "font: +5 bold");
        facts = Ui.named(new JLabel(order.customer().name() + " · " + Dates.format(order.date()) + " · "
                + order.totals().total().format()), "orderFacts");
        itemsTable = Ui.table(LinesTableModel.withSku(order.lines()));
        Ui.widths(itemsTable, 130, 0, 80, 110, 120);
        details.addTab("Items", itemsTab());
        details.addTab("Delivery", deliveryTab());
        details.addTab("Invoice", invoiceTab());

        saveReceipt.addActionListener(e -> Receipt.save(this, order, workspace::setStatus));
        cancel.addActionListener(e -> cancelOrder());

        // one row as in the mock: number, pill, facts, then the actions at the right; the hint right under them
        add(number, "split 5");
        add(status, "gapleft 8");
        add(facts, "gapleft 14");
        add(saveReceipt, "gapleft push");
        add(cancel, "wrap");
        add(cancelHint, "growx 0, alignx right, wrap");
        details.putClientProperty(FlatClientProperties.STYLE, "tabHeight: 30");
        add(box(details), "grow");

        refresh();
    }

    /** The lines and "13 units in 2 lines"; the totals are on the Invoice tab. */
    private JComponent itemsTab() {
        int units = order.units();
        int count = order.lines().size();
        JPanel panel = sheet(new MigLayout("insets 0, fill", "[grow,fill]", "[grow,fill]10[]10"));
        JScrollPane scroll = new JScrollPane(itemsTable) {
            /** No box of its own inside the boxed tabs: only a line under the rows. */
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
            }
        };
        panel.add(scroll, "wrap");
        panel.add(Ui.named(Ui.hint(units + (units == 1 ? " unit" : " units") + " in " + count
                + (count == 1 ? " line" : " lines")), "itemsSummary"), "gapleft 16");
        return panel;
    }

    private JComponent deliveryTab() {
        JPanel panel = sheet(new MigLayout("insets 14 16 14 16, wrap 2", "[150!]16[left]", "[top]6[top]6[top]"));
        captioned(panel, "Delivery address", Ui.named(Ui.textColumn(order.address().lines()), "deliveryAddress"));
        captioned(panel, "Shipping method", new JLabel(Ui.shippingText(order.shipping(), order.totals())));
        captioned(panel, "Delivery note", new JLabel(order.note().isEmpty() ? "—" : order.note()));
        return panel;
    }

    private JComponent invoiceTab() {
        TotalsPanel totals = new TotalsPanel();
        totals.show(order.totals(), null);
        totals.setOpaque(false);
        JPanel panel = sheet(new MigLayout("insets 14 16 14 16, wrap 2", "[150!]16[left]", "[top]6[top]18[top]"));
        captioned(panel, "Payment", new JLabel(order.payment().describe()));
        captioned(panel, "Order date", new JLabel(Dates.format(order.date())));
        panel.add(totals, "span");
        return panel;
    }

    /** A grey caption and its value side by side; the caption is tied to the value, as a person reads them. */
    private static void captioned(JPanel panel, String caption, JComponent value) {
        JLabel label = Ui.hint(caption);
        label.setLabelFor(value);
        panel.add(label);
        panel.add(value);
    }

    /** The sub-tabs in one box, as in the mock: a line around the strip and the pages (Lagoon: see details). */
    private static JComponent box(JTabbedPane tabs) {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(UIManager.getBoolean(Theme.BOXED_TABS)
                        ? BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")) : null);
            }
        };
        box.add(tabs);
        return box;
    }

    /** A sub-tab page on the component background (white in the light theme). */
    private static JPanel sheet(MigLayout layout) {
        return new JPanel(layout) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBackground(UIManager.getColor("Table.background"));
            }
        };
    }

    private void refresh() {
        status.setStatus(order.status(), false);
        cancel.setEnabled(order.cancellable());
        cancelHint.setText(CancelOrders.hintFor(order));
    }

    private void cancelOrder() {
        CancelOrders.run(this, store, workspace, Collections.singletonList(order));
    }

    Order order() {
        return order;
    }

    /** Follows the store while the tab is shown, and catches up with what changed while it was not. */
    @Override
    public void addNotify() {
        super.addNotify();
        store.addListener(storeListener);
        refresh();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        store.removeListener(storeListener);
    }

    // for tests

    String statusText() {
        return status.getText();
    }

    String factsText() {
        return facts.getText();
    }

    JButton cancelButton() {
        return cancel;
    }

    String cancelHint() {
        return cancelHint.getText();
    }

    JTabbedPane details() {
        return details;
    }

    JTable itemsTable() {
        return itemsTable;
    }
}
