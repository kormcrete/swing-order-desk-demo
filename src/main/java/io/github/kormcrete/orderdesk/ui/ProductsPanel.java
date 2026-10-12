package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Catalog;
import io.github.kormcrete.orderdesk.model.Category;
import io.github.kormcrete.orderdesk.model.Product;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Categories tree and the products table, with Add to cart. */
final class ProductsPanel extends JPanel {

    private final Consumer<List<Product>> onAdd;
    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
    private final JTree tree;
    private final ProductsTableModel model;
    private final JTable table;
    private final TableRowSorter<ProductsTableModel> sorter;
    private final JTextField search = Ui.searchField("Search products");
    private final JButton add = new JButton("Add to cart", Icons.get(Icons.CART));
    private final JLabel addHint = Ui.hint("");
    private final JLabel count = Ui.hint("");
    private final JLabel empty = Ui.hint("");
    private final JScrollPane tableScroll;
    private Category category;

    ProductsPanel(Catalog catalog, Consumer<List<Product>> onAdd) {
        super(new MigLayout("insets 0, fill", "[200!]10[grow,fill]", "[]6[]8[grow,fill]8[]"));
        this.onAdd = onAdd;
        this.category = catalog.root();

        treeRoot.add(new DefaultMutableTreeNode(catalog.root()));   // "All products", shown as a leaf
        for (Category child : catalog.root().children()) treeRoot.add(node(child));
        tree = new JTree(treeRoot);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        tree.setSelectionRow(0);
        tree.addTreeSelectionListener(e -> categoryChosen());

        model = new ProductsTableModel(catalog.products());
        table = Ui.configure(new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    // set it either way: the renderers are shared, so a grey row must not leave the next one grey
                    boolean inStock = model.productAt(convertRowIndexToModel(row)).inStock();
                    c.setForeground(inStock ? getForeground() : UIManager.getColor("Label.disabledForeground"));
                }
                return c;
            }
        });
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(3).setCellRenderer(new StockRenderer());
        Ui.widths(table, 118, 0, 84, 112);
        table.getSelectionModel().addListSelectionListener(e -> {
            updateCount();
            updateAdd();
        });
        Ui.onDoubleClick(table, this::addSelected);
        table.setComponentPopupMenu(popup());
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        tableScroll = Ui.scroll(table);

        Ui.onTextChange(search, this::applyFilter);
        add.addActionListener(e -> addSelected());

        add(Ui.heading("Categories"));
        add(Ui.heading("Products"), "wrap");
        add(Ui.scroll(tree), "spany 2, grow");
        JPanel searchRow = new JPanel(new MigLayout("insets 0", "[]8[grow,fill]"));
        searchRow.add(Ui.labelFor("Search", search));
        searchRow.add(search);
        add(searchRow, "growx, wrap");
        add(tableScroll, "grow, wrap");
        add(count, "skip 1, split 3");
        add(addHint, "gapleft push");
        add(add);
        applyFilter();
    }

    private static DefaultMutableTreeNode node(Category category) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(category);
        for (Category child : category.children()) node.add(node(child));
        return node;
    }

    private JPopupMenu popup() {
        JPopupMenu menu = Ui.rowPopupMenu(table);
        JMenuItem addItem = new JMenuItem("Add to cart");
        addItem.addActionListener(e -> addSelected());
        JMenuItem showItem = new JMenuItem("Show in category");
        showItem.addActionListener(e -> showInCategory());
        menu.add(addItem);
        menu.add(showItem);
        return menu;
    }

    private void categoryChosen() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        category = (Category) node.getUserObject();
        applyFilter();
    }

    private void applyFilter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        Category current = category;
        sorter.setRowFilter(new RowFilter<ProductsTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends ProductsTableModel, ? extends Integer> entry) {
                Product p = model.productAt(entry.getIdentifier());
                return current.contains(p.category()) && (query.isEmpty()
                        || p.sku().toLowerCase(Locale.ROOT).contains(query)
                        || p.name().toLowerCase(Locale.ROOT).contains(query));
            }
        });
        int rows = table.getRowCount();
        updateCount();
        empty.setText("No products match ‘" + search.getText().trim() + "’");
        Ui.showInViewport(tableScroll, rows == 0 ? empty : table);
        updateAdd();
    }

    /** "22 products", and how many of them are selected: "22 products · 2 selected". */
    private void updateCount() {
        int rows = table.getRowCount();
        int selected = table.getSelectedRowCount();
        count.setText((rows == 1 ? "1 product" : rows + " products") + (selected > 0 ? " · " + selected + " selected" : ""));
    }

    private List<Product> selectedProducts() {
        List<Product> out = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) out.add(model.productAt(table.convertRowIndexToModel(viewRow)));
        return out;
    }

    private void updateAdd() {
        List<Product> selected = selectedProducts();
        boolean any = selected.stream().anyMatch(Product::inStock);
        add.setEnabled(any);
        addHint.setText(selected.isEmpty() ? "Select a product first" : any ? "" : "The selected products are out of stock");
    }

    private void addSelected() {
        List<Product> addable = selectedProducts().stream().filter(Product::inStock).collect(Collectors.toList());
        if (!addable.isEmpty()) onAdd.accept(addable);
    }

    /** Selects the first selected product's category in the tree and keeps the product selected. */
    void showInCategory() {
        List<Product> selected = selectedProducts();
        if (selected.isEmpty()) return;
        Product product = selected.get(0);
        selectNodeWhere(category -> category == product.category());
        int viewRow = table.convertRowIndexToView(model.indexOf(product));
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        }
    }

    /** Selects the first category node (breadth first) that matches. */
    private void selectNodeWhere(Predicate<Category> matches) {
        Enumeration<?> nodes = treeRoot.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof Category && matches.test((Category) node.getUserObject())) {
                tree.setSelectionPath(new TreePath(node.getPath()));
                return;
            }
        }
    }

    /** Stock changed elsewhere. */
    void refresh() {
        int rows = model.getRowCount();
        if (rows > 0) model.fireTableRowsUpdated(0, rows - 1);
        updateAdd();
    }

    // for tests

    JTree tree() {
        return tree;
    }

    JTable table() {
        return table;
    }

    JTextField searchField() {
        return search;
    }

    JButton addButton() {
        return add;
    }

    String addHint() {
        return addHint.getText();
    }

    String countText() {
        return count.getText();
    }

    String emptyMessage() {
        return tableScroll.getViewport().getView() == empty ? empty.getText() : null;
    }

    void selectCategory(String id) {
        selectNodeWhere(category -> category.id().equals(id));
    }
}
