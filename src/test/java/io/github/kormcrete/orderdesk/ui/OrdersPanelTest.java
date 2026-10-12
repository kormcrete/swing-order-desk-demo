package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.OrderStatus;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OrdersPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final RecordingWorkspace workspace = new RecordingWorkspace();
    private final OrdersPanel panel = Edt.call(() -> new OrdersPanel(store, workspace));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private List<Object> numbers() {
        JTable table = panel.table();
        List<Object> out = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) out.add(table.getValueAt(row, 0));
        return out;
    }

    private void select(String... orderNumbers) {
        JTable table = panel.table();
        table.clearSelection();
        for (String number : orderNumbers) {
            for (int row = 0; row < table.getRowCount(); row++) {
                if (table.getValueAt(row, 0).equals(number)) table.addRowSelectionInterval(row, row);
            }
        }
    }

    @Test
    void showsAllOrdersNewestFirst() {
        Edt.run(() -> {
            assertThat(numbers()).hasSize(12);
            assertThat(numbers().get(0)).isEqualTo("ORD-1639");
            assertThat(panel.table().getColumnName(5)).isEqualTo("Status");
        });
    }

    @Test
    void searchAndStatusFilter() {
        Edt.run(() -> {
            panel.searchField().setText("birch");
            assertThat(numbers()).containsExactly("ORD-1637", "ORD-1631");
            panel.statusFilter().setSelectedItem("Placed");
            assertThat(numbers()).containsExactly("ORD-1637");
            panel.searchField().setText("ORD-163");
            assertThat(numbers()).containsExactly("ORD-1639", "ORD-1638", "ORD-1637", "ORD-1635");
        });
    }

    @Test
    void emptyState() {
        Edt.run(() -> {
            panel.searchField().setText("xyz");
            assertThat(panel.emptyMessage()).isEqualTo("No orders match ‘xyz’");
            panel.searchField().setText("");
            assertThat(panel.emptyMessage()).isNull();
        });
    }

    @Test
    void cancelItemDisabledForMixedSelection() {
        Edt.run(() -> {
            select("ORD-1639", "ORD-1636");
            panel.updateMenu();
            assertThat(panel.cancelItem().isEnabled()).isFalse();
            assertThat(panel.cancelHint().isVisible()).isTrue();
            assertThat(panel.cancelHint().getText()).isEqualTo("Only placed orders can be cancelled");
            select("ORD-1639", "ORD-1638");
            panel.updateMenu();
            assertThat(panel.cancelItem().isEnabled()).isTrue();
            assertThat(panel.cancelHint().isVisible()).isFalse();
            select();
            panel.updateMenu();
            assertThat(panel.cancelItem().isEnabled()).isFalse();
            assertThat(panel.cancelHint().isVisible()).isFalse();
        });
    }

    @Test
    void searchAndStatusHaveVisibleLabels() {
        Edt.run(() -> {
            Map<String, Component> labelled = new HashMap<>();
            for (Component c : panel.getComponents()) {
                if (c instanceof JLabel && ((JLabel) c).getLabelFor() != null) {
                    labelled.put(((JLabel) c).getText(), ((JLabel) c).getLabelFor());
                }
            }
            assertThat(labelled.get("Search")).isSameAs(panel.searchField());
            assertThat(labelled.get("Status")).isSameAs(panel.statusFilter());
        });
    }

    @Test
    void openingTheSelectedOrders() {
        Edt.run(() -> {
            select("ORD-1638");
            panel.table().getActionMap().get("open-order").actionPerformed(null);
            assertThat(workspace.opened).extracting(Order::number).containsExactly("ORD-1638");
        });
    }

    @Test
    void refreshesWhenTheStoreChanges() {
        Edt.run(() -> {
            select("ORD-1639");
            store.cancel(Collections.singletonList(store.orders().find("ORD-1639").get()));
            assertThat(panel.table().getValueAt(0, 5)).isEqualTo(OrderStatus.CANCELLED);
            assertThat(panel.table().getSelectedRowCount()).isEqualTo(1);
        });
    }

    @Test
    void emptyStateWordingForStatusFilters() {
        Edt.run(() -> {
            store.cancel(new ArrayList<>(store.orders().orders()).stream().filter(Order::cancellable).collect(Collectors.toList()));
            panel.statusFilter().setSelectedItem("Placed");
            assertThat(panel.emptyMessage()).isEqualTo("No placed orders");
        });
    }

    @Test
    void rightClickSelectsTheRowBeforeTheMenuIsComputed() {
        Edt.run(() -> {
            JTable table = panel.table();
            select("ORD-1639", "ORD-1636");
            int placedRow = rowOf("ORD-1638");
            Rectangle r = table.getCellRect(placedRow, 0, true);
            // a real popup cannot show headless; this is the entry point JPopupMenu.show uses
            Ui.selectRowForPopup(table, r.x + 2, r.y + 2);
            assertThat(table.getSelectedRowCount()).isEqualTo(1);
            assertThat(table.getValueAt(table.getSelectedRow(), 0)).isEqualTo("ORD-1638");
            panel.updateMenu();
            assertThat(panel.cancelItem().isEnabled()).isTrue();

            select("ORD-1639", "ORD-1638");
            Rectangle inSelection = table.getCellRect(rowOf("ORD-1638"), 0, true);
            Ui.selectRowForPopup(table, inSelection.x + 2, inSelection.y + 2);
            assertThat(table.getSelectedRowCount()).isEqualTo(2);
        });
    }

    @Test
    void aKeyboardMenuKeepsTheSelection() {
        Edt.run(() -> {
            JTable table = panel.table();
            select("ORD-1638");
            Rectangle other = table.getCellRect(rowOf("ORD-1639"), 0, true);
            // Shift+F10 shows the menu at the middle of the visible area, over whichever row is there
            KeyEvent shiftF10 = new KeyEvent(table, KeyEvent.KEY_PRESSED, 0, KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_F10,
                    KeyEvent.CHAR_UNDEFINED);
            Ui.popupPoint(table, other.x + 2, other.y + 2, shiftF10);
            Ui.popupPoint(table, other.x + 2, other.y + 2, null);
            assertThat(table.getSelectedRowCount()).isEqualTo(1);
            assertThat(table.getValueAt(table.getSelectedRow(), 0)).isEqualTo("ORD-1638");

            MouseEvent rightClick = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 0, 0, other.x + 2, other.y + 2, 1,
                    true, MouseEvent.BUTTON3);
            Ui.popupPoint(table, other.x + 2, other.y + 2, rightClick);
            assertThat(table.getValueAt(table.getSelectedRow(), 0)).isEqualTo("ORD-1639");
        });
    }

    private int rowOf(String number) {
        for (int row = 0; row < panel.table().getRowCount(); row++) {
            if (panel.table().getValueAt(row, 0).equals(number)) return row;
        }
        throw new AssertionError(number);
    }

    @Test
    void cancelWithAMixedSelectionChangesNothing() {
        Edt.run(() -> {
            select("ORD-1639", "ORD-1636");
            panel.updateMenu();
            List<OrderStatus> before = new ArrayList<>();
            for (Order o : store.orders().orders()) before.add(o.status());
            panel.cancelSelected(); // returns at the guard: a dialog here would block the test
            List<OrderStatus> after = new ArrayList<>();
            for (Order o : store.orders().orders()) after.add(o.status());
            assertThat(after).isEqualTo(before);
            assertThat(workspace.statuses).isEmpty();
            assertThat(panel.cancelHint().isVisible()).isTrue();
        });
    }
}
