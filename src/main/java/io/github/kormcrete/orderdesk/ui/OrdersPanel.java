package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Store;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableRowSorter;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** The pinned Orders tab: search, status filter, sortable multi-select table, right-click actions. */
final class OrdersPanel extends JPanel {

    private static final String[] STATUSES = {"All", "Placed", "Shipped", "Cancelled"};

    private final Store store;
    private final Workspace workspace;
    private final OrdersTableModel model;
    private final JTable table;
    private final TableRowSorter<OrdersTableModel> sorter;
    private final JTextField search = Ui.searchField("Search orders or customers");
    private final JComboBox<String> status = new JComboBox<>(STATUSES);
    private final JLabel empty = Ui.hint("");
    private final JLabel cancelHint = Ui.hint(CancelOrders.HINT);
    private final JScrollPane scroll;
    private final JMenuItem openItem = new JMenuItem("Open");
    private final JMenuItem cancelItem = new JMenuItem("Cancel orders…");
    private final JMenuItem copyItem = new JMenuItem("Copy order number");

    OrdersPanel(Store store, Workspace workspace) {
        super(new MigLayout("insets 10 12 12 12, fill, gap 10", "[][320!]10[][150!]10[grow]", "[]10[grow,fill]"));
        this.store = store;
        this.workspace = workspace;
        model = new OrdersTableModel(store.orders());
        table = Ui.table(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sorter = new TableRowSorter<>(model);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        table.setRowSorter(sorter);
        Ui.widths(table, 110, 110, 0, 80, 110, 110);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        scroll = Ui.scroll(table);

        Ui.onTextChange(search, this::applyFilter);
        status.addActionListener(e -> applyFilter());
        Ui.onDoubleClick(table, this::openSelected);
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-order");
        table.getActionMap().put("open-order", Ui.action(this::openSelected));
        table.setComponentPopupMenu(popup());
        table.getSelectionModel().addListSelectionListener(e -> updateMenu());

        add(Ui.labelFor("Search", search));
        add(search, "growx");
        add(Ui.labelFor("Status", status));
        add(status, "growx");
        add(cancelHint, "wrap");
        add(scroll, "span, grow");

        store.addListener(this::refresh);
        updateMenu();
    }

    private JPopupMenu popup() {
        JPopupMenu menu = Ui.rowPopupMenu(table);
        openItem.addActionListener(e -> openSelected());
        cancelItem.addActionListener(e -> cancelSelected());
        copyItem.addActionListener(e -> copySelected());
        menu.add(openItem);
        menu.add(cancelItem);
        menu.addSeparator();
        menu.add(copyItem);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateMenu();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        return menu;
    }

    void updateMenu() {
        List<Order> selected = selectedOrders();
        openItem.setEnabled(!selected.isEmpty());
        copyItem.setEnabled(!selected.isEmpty());
        boolean allCancellable = selected.stream().allMatch(Order::cancellable);
        cancelItem.setEnabled(!selected.isEmpty() && allCancellable);
        cancelHint.setVisible(!selected.isEmpty() && !allCancellable);
    }

    private List<Order> selectedOrders() {
        List<Order> out = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) out.add(model.orderAt(table.convertRowIndexToModel(viewRow)));
        return out;
    }

    void openSelected() {
        for (Order order : selectedOrders()) workspace.openOrder(order);
    }

    void cancelSelected() {
        CancelOrders.run(this, store, workspace, selectedOrders());
    }

    private void copySelected() {
        String numbers = selectedOrders().stream().map(Order::number).collect(Collectors.joining(", "));
        if (numbers.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(numbers), null);
        workspace.setStatus("Copied " + numbers);
    }

    private void applyFilter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        String wanted = (String) status.getSelectedItem();
        sorter.setRowFilter(new RowFilter<OrdersTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends OrdersTableModel, ? extends Integer> entry) {
                Order order = model.orderAt(entry.getIdentifier());
                boolean text = query.isEmpty()
                        || order.number().toLowerCase(Locale.ROOT).contains(query)
                        || order.customer().name().toLowerCase(Locale.ROOT).contains(query);
                return text && ("All".equals(wanted) || order.status().label().equals(wanted));
            }
        });
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean none = table.getRowCount() == 0;
        if (none) {
            String query = search.getText().trim();
            String wanted = (String) status.getSelectedItem();
            empty.setText(!query.isEmpty() ? "No orders match ‘" + query + "’"
                    : "All".equals(wanted) ? "No orders" : "No " + wanted.toLowerCase(Locale.ROOT) + " orders");
        }
        Ui.showInViewport(scroll, none ? empty : table);
    }

    private void refresh() {
        List<Order> selected = selectedOrders();
        model.fireTableDataChanged();
        for (Order order : selected) {
            int viewRow = table.convertRowIndexToView(store.orders().orders().indexOf(order));
            if (viewRow >= 0) table.addRowSelectionInterval(viewRow, viewRow);
        }
        updateEmptyState();
        updateMenu();
    }

    // for tests

    JTable table() {
        return table;
    }

    JTextField searchField() {
        return search;
    }

    JComboBox<String> statusFilter() {
        return status;
    }

    String emptyMessage() {
        return scroll.getViewport().getView() == empty ? empty.getText() : null;
    }

    JLabel cancelHint() {
        return cancelHint;
    }

    JMenuItem cancelItem() {
        return cancelItem;
    }
}
