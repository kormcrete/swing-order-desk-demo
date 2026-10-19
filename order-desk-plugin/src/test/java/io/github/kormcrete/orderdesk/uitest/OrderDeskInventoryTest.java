package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.Edt;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * describe_ui's facts, on a root pane built like Order Desk's main window: an internal frame, since these tests run
 * headless.
 */
class OrderDeskInventoryTest {

    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton newOrder = new JButton("New order");
    private JInternalFrame frame;
    private OrderDeskInventory inventory;

    private void build() {
        Edt.run(() -> {
            frame = new JInternalFrame("Order Desk");
            JMenuBar bar = new JMenuBar();
            bar.add(new JMenu("File"));
            frame.setJMenuBar(bar);
            JButton recent = new JButton();
            recent.setToolTipText("New order for a recent customer");
            JPanel toolbar = new JPanel();
            toolbar.add(newOrder);
            toolbar.add(recent);
            tabs.addTab("Orders", new JPanel());
            JPanel order = new JPanel(new BorderLayout());
            order.add(new JButton("Print"), BorderLayout.NORTH);
            JTabbedPane details = new JTabbedPane();
            details.addTab("Items", new JPanel());
            details.addTab("Delivery", new JPanel());
            details.addTab("Invoice", new JPanel());
            order.add(details, BorderLayout.CENTER);
            tabs.addTab("ORD-1640", order);
            tabs.addTab("New order · Ada Shop Ltd", new JPanel());
            JPanel content = new JPanel(new BorderLayout());
            content.add(toolbar, BorderLayout.NORTH);
            content.add(tabs, BorderLayout.CENTER);
            frame.setContentPane(content);
        });
        inventory = new OrderDeskInventory(() -> frame);
    }

    @Test
    void theActiveTabIsTheSelectedTabOfTheMainWindow() {
        build();
        assertThat(inventory.activeTabTitle()).isEqualTo("Orders");
        Edt.run(() -> tabs.setSelectedIndex(2));
        assertThat(inventory.activeTabTitle()).isEqualTo("New order · Ada Shop Ltd");
    }

    @Test
    void theToolbarListsTheButtonsAboveTheTabsByWhatTheySay() {
        build();
        Edt.run(() -> newOrder.setEnabled(false));
        assertThat(inventory.toolbarActions()).containsExactly(
                row("New order", false), row("New order for a recent customer", true));
    }

    @Test
    void anOpenOrderReportsItsOwnTabs() {
        build();
        assertThat(inventory.viewTabs()).isEmpty();
        Edt.run(() -> tabs.setSelectedIndex(1));
        assertThat(inventory.viewTabs()).containsExactly(
                new AbstractMap.SimpleEntry<String, List<String>>("ORD-1640", Arrays.asList("Items", "Delivery", "Invoice")));
    }

    @Test
    void orderDeskHasNoEntryPointsAndNoNavBar() {
        build();
        assertThat(inventory.entryPointIds()).isEmpty();
        assertThat(inventory.navTargets()).isEmpty();
        assertThat(inventory.navEntryPoints()).containsOnlyKeys("roots", "children");
        assertThat(inventory.navEntryPoints().get("roots")).isEmpty();
    }

    @Test
    void noMainWindowReportsNothing() {
        inventory = new OrderDeskInventory(() -> null);
        assertThat(inventory.activeTabTitle()).isNull();
        assertThat(inventory.toolbarActions()).isEmpty();
        assertThat(inventory.viewTabs()).isEmpty();
    }

    private static Map<String, Object> row(String label, boolean enabled) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("label", label);
        row.put("enabled", enabled);
        return row;
    }
}
