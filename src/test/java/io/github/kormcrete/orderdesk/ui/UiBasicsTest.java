package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.OrderStatus;
import io.github.kormcrete.orderdesk.model.Pricing;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Point;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UiBasicsTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    @Test
    void everyIconIsThere() {
        for (String name : Icons.ALL) {
            assertThat(Icons.class.getClassLoader().getResource("io/github/kormcrete/orderdesk/icons/" + name + ".svg"))
                    .as(name).isNotNull();
        }
    }

    @Test
    void renderersFormatMoneyDatesAndStatus() {
        Edt.run(() -> {
            JTable table = Ui.table(new DefaultTableModel(1, 1));
            JLabel money = (JLabel) new MoneyRenderer().getTableCellRendererComponent(table, Money.of("1234.5"), false, false, 0, 0);
            assertThat(money.getText()).isEqualTo("€1,234.50");
            assertThat(money.getHorizontalAlignment()).isEqualTo(SwingConstants.RIGHT);
            JLabel date = (JLabel) new DateRenderer().getTableCellRendererComponent(table, LocalDate.of(2026, 10, 9), false, false, 0, 0);
            assertThat(date.getText()).isEqualTo("9 Oct 2026");
            StatusRenderer status = (StatusRenderer) new StatusRenderer().getTableCellRendererComponent(table, OrderStatus.SHIPPED, true, false, 0, 0);
            assertThat(status.getText()).isEqualTo("Shipped");
            assertThat(status.status()).isEqualTo(OrderStatus.SHIPPED);
        });
    }

    @Test
    void wholeNumbersHaveNoGroupingInAnyLocale() {
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            Edt.run(() -> {
                JTable table = Ui.table(new LinesTableModel());
                TableCellRenderer renderer = table.getDefaultRenderer(table.getColumnClass(1));   // Qty
                assertThat(renderer).isInstanceOf(IntegerRenderer.class);
                JLabel qty = (JLabel) renderer.getTableCellRendererComponent(table, 1200, false, false, 0, 1);
                assertThat(qty.getText()).isEqualTo("1200");
                assertThat(qty.getHorizontalAlignment()).isEqualTo(SwingConstants.RIGHT);
            });
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void aKeyboardMenuOpensAtTheSelectedRow() {
        Edt.run(() -> {
            DefaultTableModel model = new DefaultTableModel(20, 2);
            JTable table = Ui.table(model);
            table.setSize(300, table.getRowHeight() * 20);
            table.setRowSelectionInterval(5, 5);
            Rectangle cell = table.getCellRect(5, 0, false);
            Point at = Ui.popupPoint(table, 150, 40, null);
            assertThat(at.y).isEqualTo(cell.y + cell.height);
            assertThat(at.x).isBetween(cell.x, cell.x + cell.width);
            assertThat(table.getSelectedRow()).isEqualTo(5);

            table.clearSelection();
            assertThat(Ui.popupPoint(table, 150, 40, null)).isEqualTo(new Point(150, 40));
        });
    }

    @Test
    void totalsHideAZeroDiscountAndSayFree() {
        Edt.run(() -> {
            Cart cart = new Cart();
            cart.add(store.catalog().find("COF-ESPRESSO").get(), 1);
            TotalsPanel totals = new TotalsPanel();
            totals.show(Pricing.of(cart.lines(), Shipping.STANDARD), null);
            assertThat(totals.valueOf("Subtotal")).isEqualTo("€249.00");
            assertThat(totals.valueOf("Volume discount")).isNull();
            assertThat(totals.valueOf("Shipping")).isEqualTo("Free");
            assertThat(totals.valueOf("VAT (20%)")).isEqualTo("€49.80");
            assertThat(totals.valueOf("Total")).isEqualTo("€298.80");

            cart.add(store.catalog().find("PEN-GEL-BLK").get(), 10);
            totals.show(cart.totals(), "chosen at checkout");
            assertThat(totals.valueOf("Volume discount")).isEqualTo("−€0.60");
            assertThat(totals.valueOf("Shipping")).isEqualTo("chosen at checkout");
        });
    }

    @Test
    void linesTable() {
        Edt.run(() -> {
            LinesTableModel lines = new LinesTableModel();
            lines.setLines(store.orders().find("ORD-1639").get().lines());
            assertThat(lines.getRowCount()).isEqualTo(3);
            assertThat(lines.getValueAt(0, 0)).isEqualTo("Gel pen, black");
            assertThat(lines.getValueAt(0, 1)).isEqualTo(50);
            assertThat(lines.getValueAt(0, 3)).isEqualTo(Money.of("60.00"));
        });
    }

    @Test
    void fieldErrorsOutlineTheField() {
        Edt.run(() -> {
            JTextField field = new JTextField();
            JLabel error = Ui.fieldError();
            Ui.showError(field, error, "Name is required");
            assertThat(field.getClientProperty(FlatClientProperties.OUTLINE)).isEqualTo(FlatClientProperties.OUTLINE_ERROR);
            assertThat(error.getText()).isEqualTo("Name is required");
            Ui.showError(field, error, null);
            assertThat(field.getClientProperty(FlatClientProperties.OUTLINE)).isNull();
            assertThat(error.getText()).isEqualTo(" ");
        });
    }

    @Test
    void themeSwitches() {
        Edt.run(Theme::dark);
        assertThat(FlatLaf.isLafDark()).isTrue();
        Edt.run(Theme::light);
        assertThat(FlatLaf.isLafDark()).isFalse();
    }

    @Test
    void headersAlignLikeTheirCells() {
        Edt.run(() -> {
            JTable table = Ui.table(new LinesTableModel());
            assertThat(Ui.headerAlignment(table, 0)).isEqualTo(SwingConstants.LEADING);   // Product
            assertThat(Ui.headerAlignment(table, 1)).isEqualTo(SwingConstants.RIGHT);     // Qty
            assertThat(Ui.headerAlignment(table, 3)).isEqualTo(SwingConstants.RIGHT);     // Line total
            JLabel header = (JLabel) table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, "Line total", false, false, -1, 3);
            assertThat(header.getHorizontalAlignment()).isEqualTo(SwingConstants.RIGHT);
        });
    }

    @Test
    void onlyTheNameColumnGrows() {
        Edt.run(() -> {
            JTable table = Ui.table(new LinesTableModel());
            Ui.widths(table, 0, 60, 90, 100);
            assertThat(table.getColumnModel().getColumn(0).getMaxWidth()).isEqualTo(Integer.MAX_VALUE);
            assertThat(table.getColumnModel().getColumn(1).getMaxWidth())
                    .isEqualTo(table.getColumnModel().getColumn(1).getPreferredWidth());
        });
    }

    @Test
    void orderLinesWithSku() {
        Edt.run(() -> {
            LinesTableModel lines = LinesTableModel.withSku(store.orders().find("ORD-1639").get().lines());
            assertThat(lines.getColumnName(0)).isEqualTo("SKU");
            assertThat(lines.getValueAt(0, 0)).isEqualTo("PEN-GEL-BLK");
            assertThat(lines.getValueAt(0, 1)).isEqualTo("Gel pen, black");
        });
    }

    @Test
    void primaryStaysFilledAsTheDefaultButton() {
        Edt.run(() -> {
            javax.swing.JButton next = new javax.swing.JButton("Next");
            Ui.primary(next);
            // FlatLaf only logs a style key it does not know, so check the colours the button UI resolved
            com.formdev.flatlaf.ui.FlatButtonUI ui = (com.formdev.flatlaf.ui.FlatButtonUI) next.getUI();
            java.awt.Color accent = javax.swing.UIManager.getColor("Component.accentColor");
            assertThat(next.getBackground()).isEqualTo(accent);
            assertThat(ui.getStyleableValue(next, "default.background")).isEqualTo(accent);
            assertThat(ui.getStyleableValue(next, "focusedBackground")).isEqualTo(accent);
            assertThat(ui.getStyleableValue(next, "default.foreground")).isEqualTo(java.awt.Color.WHITE);
        });
    }
}
