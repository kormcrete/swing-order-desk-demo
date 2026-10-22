package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPanelTest {

    private static final String FULL_BORDER = FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER;

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final RecordingWorkspace workspace = new RecordingWorkspace();

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private OrderPanel panel(String number) {
        Order order = store.orders().find(number).get();
        return Edt.call(() -> new OrderPanel(store, workspace, order));
    }

    @Test
    void aShippedOrder() {
        OrderPanel panel = panel("ORD-1636");
        Edt.run(() -> {
            assertThat(panel.statusText()).isEqualTo("Shipped");
            assertThat(panel.factsText()).isEqualTo("Fjord Supplies · 2 Oct 2026 · €164.84");
            assertThat(panel.cancelButton().isEnabled()).isFalse();
            assertThat(panel.cancelHint()).isEqualTo("Only placed orders can be cancelled \u2014 this one is shipped");
            assertThat(panel.details().getTitleAt(0)).isEqualTo("Items");
            assertThat(panel.details().getTitleAt(1)).isEqualTo("Delivery");
            assertThat(panel.details().getTitleAt(2)).isEqualTo("Invoice");
            assertThat(panel.itemsTable().getRowCount()).isEqualTo(2);
        });
    }

    @Test
    void theTabsGetALineAroundThemExceptUnderLagoonWhereTheTabbedPaneDrawsItsOwnFullBorder() {
        OrderPanel panel = panel("ORD-1636");
        Edt.run(() -> {
            JComponent box = (JComponent) panel.details().getParent();
            assertThat(box.getBorder()).isNotNull();
            assertThat(panel.details().getClientProperty(FULL_BORDER)).isEqualTo(false);
            try {
                Theme.lagoon();
                SwingUtilities.updateComponentTreeUI(panel);
                assertThat(box.getBorder()).isNull();
                assertThat(panel.details().getClientProperty(FULL_BORDER)).isEqualTo(true);
            } finally {
                Theme.light();
            }
            SwingUtilities.updateComponentTreeUI(panel);
            assertThat(box.getBorder()).isNotNull();
            assertThat(panel.details().getClientProperty(FULL_BORDER)).isEqualTo(false);
        });
    }

    @Test
    void followsTheStoreWhileShown() {
        OrderPanel panel = panel("ORD-1639");
        Edt.run(() -> {
            panel.addNotify();   // what adding the tab to the window does
            assertThat(panel.cancelButton().isEnabled()).isTrue();
            assertThat(panel.cancelHint()).isEmpty();
            store.cancel(Collections.singletonList(panel.order()));
            assertThat(panel.statusText()).isEqualTo("Cancelled");
            assertThat(panel.cancelButton().isEnabled()).isFalse();
            assertThat(panel.cancelHint()).isEqualTo("Only placed orders can be cancelled \u2014 this one is cancelled");
            panel.removeNotify();
        });
    }

    @Test
    void stopsFollowingWhenClosedAndCatchesUpWhenShownAgain() {
        OrderPanel panel = panel("ORD-1638");
        Edt.run(() -> {
            panel.addNotify();
            panel.removeNotify();
            store.cancel(Collections.singletonList(panel.order()));
            assertThat(panel.statusText()).isEqualTo("Placed");
            panel.addNotify();
            assertThat(panel.statusText()).isEqualTo("Cancelled");
            panel.removeNotify();
        });
    }

    @Test
    void aPluginReadsTheHeaderAndItemsSummaryByTheirNames() {
        OrderPanel panel = panel("ORD-1636");
        Edt.run(() -> {
            assertThat(Named.find(panel, "orderStatus", JLabel.class).getText()).isEqualTo("Shipped");
            assertThat(Named.find(panel, "orderFacts", JLabel.class).getText()).isEqualTo("Fjord Supplies · 2 Oct 2026 · €164.84");
            assertThat(Named.find(panel, "itemsSummary", JLabel.class).getText()).matches("\\d+ units? in 2 lines");
        });
    }

    /** Each Delivery and Invoice caption is tied to its value, as a person reads them side by side. */
    @Test
    void theDeliveryAndInvoiceCaptionsNameTheirValues() {
        OrderPanel panel = panel("ORD-1636");
        Order order = store.orders().find("ORD-1636").get();
        Edt.run(() -> {
            Component address = valueFor(panel, "Delivery address");
            assertThat(address).isSameAs(Named.find(panel, "deliveryAddress", JPanel.class));
            List<String> lines = new ArrayList<>();
            for (Component line : ((JPanel) address).getComponents()) lines.add(((JLabel) line).getText());
            assertThat(lines).isEqualTo(order.address().lines());
            assertThat(((JLabel) valueFor(panel, "Shipping method")).getText()).startsWith("Standard · 3–5 business days");
            assertThat(((JLabel) valueFor(panel, "Delivery note")).getText()).isEqualTo(order.note().isEmpty() ? "—" : order.note());
            assertThat(((JLabel) valueFor(panel, "Payment")).getText()).isEqualTo(order.payment().describe());
            assertThat(((JLabel) valueFor(panel, "Order date")).getText()).isEqualTo("2 Oct 2026");
        });
    }

    private static Component valueFor(Container root, String caption) {
        List<Component> found = new ArrayList<>();
        collectCaptions(root, caption, found);
        assertThat(found).as(caption).hasSize(1);
        return found.get(0);
    }

    private static void collectCaptions(Container parent, String caption, List<Component> out) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel && caption.equals(((JLabel) c).getText()) && ((JLabel) c).getLabelFor() != null) {
                out.add(((JLabel) c).getLabelFor());
            }
            if (c instanceof Container) collectCaptions((Container) c, caption, out);
        }
    }
}
