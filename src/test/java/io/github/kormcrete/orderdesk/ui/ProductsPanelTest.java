package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Category;
import io.github.kormcrete.orderdesk.model.Product;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductsPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final List<List<Product>> added = new ArrayList<>();
    private final ProductsPanel panel = Edt.call(() -> new ProductsPanel(store.catalog(), added::add));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private void select(String... skus) {
        JTable table = panel.table();
        table.clearSelection();
        for (String sku : skus) {
            for (int row = 0; row < table.getRowCount(); row++) {
                if (table.getValueAt(row, 0).equals(sku)) table.addRowSelectionInterval(row, row);
            }
        }
    }

    @Test
    void categoryAndSearchFilter() {
        Edt.run(() -> {
            assertThat(panel.countText()).isEqualTo("22 products");
            panel.selectCategory("office/desk");
            assertThat(panel.table().getRowCount()).isEqualTo(5);
            assertThat(panel.countText()).isEqualTo("5 products");
            panel.searchField().setText("lamp");
            assertThat(panel.countText()).isEqualTo("1 product");
            panel.searchField().setText("zzz");
            assertThat(panel.emptyMessage()).isEqualTo("No products match ‘zzz’");
            panel.searchField().setText("");
            assertThat(panel.emptyMessage()).isNull();
        });
    }

    @Test
    void addButtonSaysWhyItIsDisabled() {
        Edt.run(() -> {
            assertThat(panel.addButton().isEnabled()).isFalse();
            assertThat(panel.addHint()).isEqualTo("Select a product first");
            select("DSK-LAMP");
            assertThat(panel.addButton().isEnabled()).isFalse();
            assertThat(panel.addHint()).isEqualTo("The selected products are out of stock");
            select("DSK-LAMP", "DSK-STAPLER");
            assertThat(panel.addButton().isEnabled()).isTrue();
            assertThat(panel.addHint()).isEmpty();
            panel.addButton().doClick();
            assertThat(added).hasSize(1);
            assertThat(added.get(0)).extracting(Product::sku).containsExactly("DSK-STAPLER");
        });
    }

    @Test
    void showInCategory() {
        Edt.run(() -> {
            select("COF-ESPRESSO");
            panel.showInCategory();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) panel.tree().getLastSelectedPathComponent();
            assertThat(((Category) node.getUserObject()).label()).isEqualTo("Machines");
            assertThat(panel.table().getSelectedRowCount()).isEqualTo(1);
            assertThat(panel.table().getValueAt(panel.table().getSelectedRow(), 0)).isEqualTo("COF-ESPRESSO");
        });
    }

    @Test
    void stockColumn() {
        Edt.run(() -> {
            JTable table = panel.table();
            StockRenderer renderer = new StockRenderer();
            int lamp = rowOf("DSK-LAMP");          // none left
            JLabel out = (JLabel) renderer.getTableCellRendererComponent(table, 0, false, false, lamp, 3);
            assertThat(out.getText()).isEqualTo("Out of stock");
            assertThat(out.getIcon()).isNull();
            int stapler = rowOf("DSK-STAPLER");    // 4 left: low
            out = (JLabel) renderer.getTableCellRendererComponent(table, 4, false, false, stapler, 3);
            assertThat(out.getText()).isEqualTo("4");
            assertThat(out.getIcon()).isNotNull();
            int paper = rowOf("PAP-A4-500");
            out = (JLabel) renderer.getTableCellRendererComponent(table, 120, false, false, paper, 3);
            assertThat(out.getIcon()).isNull();
        });
    }

    private int rowOf(String sku) {
        for (int row = 0; row < panel.table().getRowCount(); row++) {
            if (panel.table().getValueAt(row, 0).equals(sku)) return row;
        }
        throw new AssertionError(sku);
    }

    @Test
    void searchFieldHasAVisibleLabel() {
        Edt.run(() -> {
            JLabel label = null;
            for (java.awt.Component c : allComponents(panel)) {
                if (c instanceof JLabel && ((JLabel) c).getLabelFor() == panel.searchField()) label = (JLabel) c;
            }
            assertThat(label).isNotNull();
            assertThat(label.getText()).isEqualTo("Search");
        });
    }

    private static List<java.awt.Component> allComponents(java.awt.Container root) {
        List<java.awt.Component> out = new ArrayList<>();
        for (java.awt.Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof java.awt.Container) out.addAll(allComponents((java.awt.Container) c));
        }
        return out;
    }

    @Test
    void countSaysHowManyAreSelected() {
        Edt.run(() -> {
            select("PAP-A4-500", "PEN-GEL-BLK");
            assertThat(panel.countText()).isEqualTo("22 products · 2 selected");
            select();
            assertThat(panel.countText()).isEqualTo("22 products");
        });
    }

    @Test
    void onlyOutOfStockRowsAreGrey() {
        Edt.run(() -> {
            JTable table = panel.table();
            int lamp = -1;
            int paper = -1;
            for (int row = 0; row < table.getRowCount(); row++) {
                if (table.getValueAt(row, 0).equals("DSK-LAMP")) lamp = row;
                if (table.getValueAt(row, 0).equals("PAP-A4-500")) paper = row;
            }
            java.awt.Color grey = table.prepareRenderer(table.getCellRenderer(lamp, 1), lamp, 1).getForeground();
            // the renderer is shared: the row painted after the grey one must not stay grey
            java.awt.Color normal = table.prepareRenderer(table.getCellRenderer(paper, 1), paper, 1).getForeground();
            assertThat(grey).isEqualTo(javax.swing.UIManager.getColor("Label.disabledForeground"));
            assertThat(normal).isEqualTo(table.getForeground());
        });
    }
}
