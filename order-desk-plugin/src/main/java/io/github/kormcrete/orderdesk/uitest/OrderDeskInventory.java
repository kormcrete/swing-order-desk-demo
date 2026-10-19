package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.Edt;
import io.github.kormcrete.swingui.spi.UiInventory;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.RootPaneContainer;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What {@code describe_ui} reports for Order Desk, read off its main window on the event dispatch thread (a snapshot
 * would reset the agent's refs): the selected tab, the toolbar's buttons, and the open order's own tabs (Items,
 * Delivery, Invoice). The menu bar and sections are the library's defaults. Order Desk has no entry points and no
 * nav bar: those stay empty.
 */
final class OrderDeskInventory implements UiInventory {

    private final Supplier<Component> main;

    OrderDeskInventory(Supplier<Component> main) {
        this.main = main;
    }

    /** The selected tab of the main window: "Orders", "New order · Ada Shop Ltd", "ORD-1640". */
    @Override
    public String activeTabTitle() {
        return Edt.call(() -> {
            JTabbedPane strip = strip(main.get());
            int i = strip == null ? -1 : strip.getSelectedIndex();
            return i < 0 ? null : strip.getTitleAt(i);
        });
    }

    @Override
    public List<String> entryPointIds() {
        return Collections.emptyList();
    }

    /**
     * The buttons above the tabs, each with its label (its text, else its tooltip) and whether it is enabled. They have
     * no ids: the label is what a click target names.
     */
    @Override
    public List<Map<String, Object>> toolbarActions() {
        return Edt.call(() -> {
            List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
            Component window = main.get();
            // the content pane: the menu bar's menus are buttons too
            if (!(window instanceof RootPaneContainer)) return out;
            Container content = ((RootPaneContainer) window).getContentPane();
            List<AbstractButton> buttons = new ArrayList<AbstractButton>();
            collectButtons(content, strip(content), buttons);
            for (AbstractButton b : buttons) {
                String label = label(b);
                if (label == null) continue;
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("label", label);
                row.put("enabled", b.isEnabled());
                out.add(row);
            }
            return out;
        });
    }

    @Override
    public Map<String, List<String>> navEntryPoints() {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        out.put("roots", Collections.<String>emptyList());
        out.put("children", Collections.<String>emptyList());
        return out;
    }

    /** The open order's own tabs, keyed by the order's tab title; none while Orders or a new order is selected. */
    @Override
    public Map<String, List<String>> viewTabs() {
        return Edt.call(() -> {
            Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
            JTabbedPane strip = strip(main.get());
            int i = strip == null ? -1 : strip.getSelectedIndex();
            if (i < 0) return out;
            Component view = strip.getComponentAt(i);
            JTabbedPane inner = view instanceof Container ? firstTabs((Container) view) : null;
            if (inner == null) return out;
            List<String> titles = new ArrayList<String>();
            for (int t = 0; t < inner.getTabCount(); t++) titles.add(inner.getTitleAt(t));
            out.put(strip.getTitleAt(i), titles);
            return out;
        });
    }

    /** The tab pane that holds the Orders tab, or null. EDT. */
    static JTabbedPane strip(Component c) {
        if (c instanceof JTabbedPane && ((JTabbedPane) c).indexOfTab(OrderDeskSteps.ORDERS_TAB) >= 0) return (JTabbedPane) c;
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                JTabbedPane found = strip(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTabbedPane firstTabs(Container c) {
        for (Component child : c.getComponents()) {
            if (!child.isVisible()) continue;
            if (child instanceof JTabbedPane) return (JTabbedPane) child;
            if (child instanceof Container) {
                JTabbedPane found = firstTabs((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** The visible buttons under {@code c}, leaving out the tab strip (an order's own buttons). */
    private static void collectButtons(Component c, Component skip, List<AbstractButton> out) {
        if (c == skip || !c.isVisible()) return;
        if (c instanceof AbstractButton) {
            out.add((AbstractButton) c);
            return;
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) collectButtons(child, skip, out);
        }
    }

    private static String label(AbstractButton b) {
        String text = b.getText();
        if (text != null && !text.trim().isEmpty()) return text.trim();
        String tip = b.getToolTipText();
        return tip == null || tip.trim().isEmpty() ? null : tip.trim();
    }
}
