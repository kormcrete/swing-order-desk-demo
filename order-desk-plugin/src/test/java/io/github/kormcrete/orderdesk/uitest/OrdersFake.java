package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The main window on its Orders tab, as {@link FakeApp} answers reads: four orders (Ada Shop Ltd has two, Café Lumen and
 * Birch & Co one each; Placed, Placed, Shipped, Cancelled), the Search field, the Status filter, the status bar ("Ready",
 * "2 open orders"), one tab (Orders, selected) and the menu bar's items. {@link #sortable} makes a click on a header
 * sort the rows as the app does (a new column ascending, the same one reversed).
 */
class OrdersFake extends FakeApp {

    static final List<String> COLUMNS = Arrays.asList("Order", "Date", "Customer", "Items", "Total", "Status");
    static final List<List<String>> ORDERS = Collections.unmodifiableList(Arrays.asList(
            Arrays.asList("ORD-1639", "9 Oct 2026", "Ada Shop Ltd", "55", "€95.52", "Placed"),
            Arrays.asList("ORD-1638", "7 Oct 2026", "Café Lumen", "8", "€181.44", "Placed"),
            Arrays.asList("ORD-1636", "3 Oct 2026", "Ada Shop Ltd", "12", "€40.56", "Shipped"),
            Arrays.asList("ORD-1634", "1 Oct 2026", "Birch & Co", "3", "€542.58", "Cancelled")));

    private List<List<String>> rows = new ArrayList<List<String>>(ORDERS);
    private List<Integer> selected = new ArrayList<Integer>();
    private boolean sortable;
    private String sortedBy;
    private boolean ascending;

    OrdersFake() {
        answer("text 'Search'", field("Search", ""));
        Node status = node("combo", "Status");
        status.value = "All";
        answer("combo 'Status'", status);
        answer("element #statusMessage", label("statusMessage", "Ready"));
        answer("label 'open order'", label("2 open orders"));
        tabs("Orders", "Orders");
        menu("File", "New order", "Close tab", "Exit");
        menu("Order", "Checkout…", "Remove from cart");
        menu("Help", "About Order Desk");
        show();
    }

    /** The open tabs, left to right, {@code selected} the selected one. */
    OrdersFake tabs(String selected, String... titles) {
        Node[] all = new Node[titles.length];
        for (int i = 0; i < titles.length; i++) all[i] = node("tab", titles[i]);
        answer("tab", all);
        Node orders = node("tab", "Orders");
        orders.value = selected;   // a tab's remember(selected) reads its pane's selected title
        answer("tab 'Orders'", orders);
        for (String t : titles) if (!"Orders".equals(t)) answer("tab '" + t + "'", node("tab", t));
        return this;
    }

    /** The orders listed: only these (by number), in this order; none: the empty text {@code empty} instead. */
    OrdersFake listing(String empty, String... numbers) {
        List<List<String>> now = new ArrayList<List<String>>();
        for (String n : numbers) for (List<String> r : ORDERS) if (r.get(0).equals(n)) now.add(r);
        rows = now;
        if (now.isEmpty()) answer("element #ordersEmpty", label("ordersEmpty", empty));
        show();
        return this;
    }

    /** Rows {@code at} (indexes in the list as it shows) are selected. */
    OrdersFake selecting(Integer... at) {
        selected = new ArrayList<Integer>(Arrays.asList(at));
        show();
        return this;
    }

    OrdersFake searchHolds(String text) {
        answer("text 'Search'", field("Search", text));
        return this;
    }

    OrdersFake statusFilterOn(String status) {
        Node combo = node("combo", "Status");
        combo.value = status;
        answer("combo 'Status'", combo);
        return this;
    }

    /** A click on a header sorts the rows: a new column ascending, the same column again reversed. */
    OrdersFake sortable() {
        sortable = true;
        sortedBy = "Order";
        ascending = false;
        return this;
    }

    private void show() {
        if (rows.isEmpty()) {
            forget("table");
            return;
        }
        forget("element #ordersEmpty");
        Node table = table(COLUMNS, rows.size(), rows);
        table.selected = selected;
        answer("table", table);
        for (List<String> r : rows) {
            for (int c = 1; c < COLUMNS.size(); c++) {
                answer("table row=" + r.get(0) + " col=" + COLUMNS.get(c), cell(COLUMNS.get(c), r.get(c)));
            }
        }
    }

    @Override
    public VerbOutcome click(Locator target, String button, Integer count, List<String> modifiers, Long timeout) {
        if (sortable && "table".equals(target.role) && target.row == null && target.column != null) {
            ascending = !target.column.equals(sortedBy) || !ascending;
            sortedBy = target.column;
            final int c = COLUMNS.indexOf(sortedBy);
            final Comparator<String> order = OrderDeskOrdersSteps.comparator(sortedBy);
            Comparator<List<String>> byColumn = (a, b) -> order.compare(a.get(c), b.get(c));
            rows.sort(ascending ? byColumn : byColumn.reversed());
            show();
        }
        return super.click(target, button, count, modifiers, timeout);
    }

    static Node field(String caption, String value) {
        Node n = node("text", caption);
        n.value = value;
        return n;
    }
}
