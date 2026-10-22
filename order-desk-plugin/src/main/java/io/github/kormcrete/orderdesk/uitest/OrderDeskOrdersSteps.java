package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiActionFailed;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.LOOK;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.column;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.isNone;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.join;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.look;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.names;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.notShown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.pick;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.presentNow;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quietly;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quoted;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.resolve;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.showing;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shows;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.*;

/**
 * The steps and checks of the Orders list, the pinned Orders tab, as Java: search, the status filter, sorting, the
 * orders listed and their values, the empty list's text, and the row menu (open, cancel, copy the numbers) on the
 * selected orders. {@link OrderDeskOrdersVocabulary} runs them for a recorded test's {@code do} steps; a Java UI test
 * calls them directly.
 *
 * <p>An act first looks, for about a second, for the main window with no dialog over it, then clicks the Orders tab
 * (it is always there), as a person goes there first. A check reads the Orders tab as it shows and fails fast, in words,
 * when another tab is selected. An order is given by its number ("ORD-1640"), its digits ("1640") or its customer's
 * name or the start of it when that customer has one order listed; several orders in one param are parted by
 * {@code ;}. A customer with several orders fails naming them. Client calls only, the names the app shows from
 * {@link OrderDeskSteps}' constants.
 */
public final class OrderDeskOrdersSteps {

    /** The columns, in the order the list shows them. */
    static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList(
            ORDER_COLUMN, DATE_COLUMN, CUSTOMER_COLUMN, ITEMS_COLUMN, TOTAL_COLUMN, STATUS_COLUMN));
    /** The columns a check reads (the Order column names the row). */
    static final List<String> CHECK_COLUMNS = Collections.unmodifiableList(COLUMNS.subList(1, COLUMNS.size()));
    /** What the status filter offers, in its order: also the order the Status column sorts in. */
    static final List<String> STATUSES = Collections.unmodifiableList(Arrays.asList(
            ALL_STATUSES, PLACED_STATUS, SHIPPED_STATUS, CANCELLED_STATUS));
    /** The row menu's items. */
    static final List<String> ROW_MENU_ITEMS = Collections.unmodifiableList(Arrays.asList(
            OPEN_ITEM, CANCEL_ORDERS_ITEM, COPY_ORDER_NUMBER_ITEM));
    static final String ASCENDING = "ascending";
    static final String DESCENDING = "descending";
    /** What a sort's outcome says when every order listed has the same value in the column: no direction shows. */
    static final String EITHER = "either";

    /** What a step says when the main window is not there. */
    static final String NO_MAIN = "Order Desk's main window is not showing (its Orders tab is not found)";
    /** What a check of the Orders list says while another tab is selected. */
    static final String NOT_ON_ORDERS = "The Orders tab is not selected. Switch to it first with \"Switch to the tab"
            + " Orders\" (every Orders list step, e.g. \"Search the orders for <text>\", goes there itself)";
    /** How the Date column writes a date: "9 Oct 2026". */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    /** How many clicks on a header a sort tries: the first sorts by the column, the next reverses it. */
    private static final int SORT_CLICKS = 3;

    private OrderDeskOrdersSteps() {
    }

    // ---- acts

    /**
     * Opens an order in a tab of its own from the Orders list: a double click on its row (the app selects the tab of an
     * order already open instead). Several, parted by ;, are selected and opened together through the row menu's Open.
     * When a tab does not open, the screen is left as found: the tabs this step opened close again, the orders
     * selected before are selected again, and the tab selected before shows again.
     */
    public static void openOrder(final Client ui, String order) {
        String before = goToOrders(ui);
        List<Map<String, String>> rows = orderRows(ui);
        final List<String> numbers = resolveOrders(ui, order, before, rows);
        List<String> selected = selectedNumbers(ui, rows);
        List<String> wereOpen = new ArrayList<String>();
        for (String n : numbers) if (presentNow(ui.tab(n))) wereOpen.add(n);
        if (numbers.size() == 1) {
            row(ui, numbers.get(0)).doubleClick();
        } else {
            select(ui, numbers);
            row(ui, numbers.get(0)).rightClick(OPEN_ITEM);
        }
        Boolean open = look(() -> {
            for (String n : numbers) if (!presentNow(ui.tab(n))) return null;
            return Boolean.TRUE;
        });
        if (open != null) return;
        List<String> shut = new ArrayList<String>();
        for (String n : numbers) {
            if (!presentNow(ui.tab(n))) shut.add(n);
            else if (!wereOpen.contains(n)) quietly(() -> closeOrderTab(ui, n));
        }
        quietly(() -> reselect(ui, selected, numbers));
        back(ui, before);
        throw new DriverException(Reason.NOT_ACTIONABLE, "The tab of " + join(shut) + " did not open");
    }

    /** Types {@code text} into the Orders tab's Search: the list shows the orders whose number or customer holds it. */
    public static void searchOrders(Client ui, String text) {
        goToOrders(ui);
        ordersTab(ui).field(SEARCH_FIELD).type(text);
    }

    /** Empties the Orders tab's Search with its X; nothing to do when it is empty. */
    public static void clearOrderSearch(Client ui) {
        goToOrders(ui);
        Target search = ordersTab(ui).field(SEARCH_FIELD);
        String text = shown(search);
        if (text != null && !text.isEmpty()) search.clearValue();
    }

    /** Shows the orders of one status: All, Placed, Shipped or Cancelled ("placed", "cancel"), in the Status filter. */
    public static void filterOrders(Client ui, String status) {
        requireMain(ui);
        String s = pick(status, STATUSES, "status", "statuses", "The Status filter offers");
        ui.tab(ORDERS_TAB).click();
        ordersTab(ui).combo(STATUS_COMBO).select(s);
    }

    /**
     * Sorts the orders by {@code column} in {@code direction} (ascending, descending; "newest first" and "oldest first"
     * too): clicks the column's header, which sorts by it, and again while the orders do not read in that direction (a
     * click reverses it; a sort is never undone). Returns the column and the direction the list is left in:
     * {@code {"column":"Total","direction":"descending"}}; "either" when every order listed has the same value there.
     */
    public static Map<String, Object> sortOrders(Client ui, String column, String direction) {
        requireMain(ui);
        String col = sortColumn(column);
        int wanted = direction(direction);
        String before = OrderDeskMainSteps.selectedTab(ui);
        ui.tab(ORDERS_TAB).click();
        List<Map<String, String>> start = orderRows(ui);
        Integer seen = null;
        for (int click = 0; click < SORT_CLICKS; click++) {
            ordersTable(ui).cell(col).click();
            seen = directionOf(column(orderRows(ui, col), col), comparator(col));
            if (seen != null && (seen == 0 || seen == wanted)) break;
        }
        if (seen == null || seen != 0 && seen != wanted) {
            String reads = join(column(orderRows(ui, col), col));
            boolean restored = restoreOrder(ui, start);
            back(ui, before);
            throw new DriverException(Reason.NOT_ACTIONABLE, "The orders do not read sorted by " + col + ", "
                    + word(wanted) + " after " + SORT_CLICKS + " clicks on its header: " + col + " reads " + reads
                    + (restored ? "" : ". The list could not be put back in the order it had before"));
        }
        Map<String, Object> left = new LinkedHashMap<String, Object>();
        left.put("column", col);
        left.put("direction", seen == 0 ? EITHER : word(seen));
        return left;
    }

    /** Selects orders: the first by a click on its row, the others with Cmd or Ctrl held (one named twice counts once). */
    public static void selectOrders(Client ui, String orders) {
        String before = goToOrders(ui);
        select(ui, resolveOrders(ui, orders, before));
    }

    /**
     * Cancels orders as a person does: selects them, the row menu's Cancel orders…, then confirms ("Cancel order
     * ORD-1635?" Cancel order, "Cancel 2 orders?" Cancel orders). An order that is not Placed fails before anything is
     * clicked, with the app's reason: only placed orders can be cancelled.
     */
    public static void cancelOrders(Client ui, String orders) {
        List<String> numbers = askToCancel(ui, orders);
        ui.answer(numbers.size() == 1 ? CANCEL_ORDER_ANSWER : CANCEL_ORDERS_ANSWER);
    }

    /** Asks to cancel orders (selects them, the row menu's Cancel orders…), then answers Keep: nothing changes. */
    public static void keepOrders(Client ui, String orders) {
        askToCancel(ui, orders);
        ui.answer(KEEP_ANSWER);
    }

    /** Copies the numbers of orders: selects them, then the row menu's Copy order number; the status line says so. */
    public static void copyOrderNumbers(Client ui, String orders) {
        String before = goToOrders(ui);
        List<String> numbers = resolveOrders(ui, orders, before);
        select(ui, numbers);
        row(ui, numbers.get(0)).rightClick(COPY_ORDER_NUMBER_ITEM);
    }

    // ---- checks

    /**
     * Checks one value of a listed order: its Date ("9 Oct 2026"), Customer, Items, Total ("€95.52") or Status (Placed,
     * Shipped, Cancelled, any case).
     */
    public static void orderIs(final Client ui, String order, String column, final String value) {
        requireOrdersList(ui);
        final String number = resolveOrder(ui, order, orderRows(ui, CUSTOMER_COLUMN));
        // the Order column names the row: the Check menu offers its check too
        final String col = ORDER_COLUMN.equalsIgnoreCase(column.trim()) ? ORDER_COLUMN
                : pick(column, CHECK_COLUMNS, "column", "columns", "The Orders list's columns");
        final Target cell = row(ui, number).cell(col);
        if (!STATUS_COLUMN.equals(col)) {
            shows(cell, number + "'s " + col, value);
            return;
        }
        Boolean same = look(() -> value.trim().equalsIgnoreCase(String.valueOf(shown(cell))) ? Boolean.TRUE : null);
        if (same == null) {
            String actual = shown(cell);
            throw new AssertionError(number + "'s Status " + (actual == null ? "is not shown" : "reads " + quoted(actual))
                    + ", expected " + quoted(value.trim()));
        }
    }

    /**
     * Checks which orders the list shows, in order, by number ({@code "ORD-1639; ORD-1638"}, digits do too);
     * {@code none}: no order is listed.
     */
    public static void ordersListedAre(final Client ui, String orders) {
        requireOrdersList(ui);
        final boolean none = isNone(orders);
        final List<String> wanted = none ? Collections.<String>emptyList() : names(orders);
        List<String> same = look(() -> {
            List<String> listed = numbers(orderRows(ui));
            return listed.equals(asListed(wanted, listed)) ? listed : null;
        });
        if (same != null) return;
        List<String> listed = numbers(orderRows(ui));
        throw new AssertionError("The Orders list shows " + (listed.isEmpty() ? "no orders" : listed.size()
                + (listed.size() == 1 ? " order: " : " orders: ") + join(listed)) + ", expected "
                + (none ? "none" : join(asListed(wanted, listed))));
    }

    /**
     * Checks the text that replaces the orders when none is listed: "No orders match ‘zzz’", "No orders", "No cancelled
     * orders"; {@code none}: orders are listed.
     */
    public static void ordersEmptyMessageIs(Client ui, String message) {
        requireOrdersList(ui);
        Target empty = ordersTab(ui).byId(ORDERS_EMPTY_ID);
        if (isNone(message)) notShown(empty, "The Orders list's empty text");
        else shows(empty, "The Orders list", message);
    }

    /** Checks which orders are selected, by number, in any order; {@code none}: no order is selected. */
    public static void selectedOrdersAre(final Client ui, String orders) {
        requireOrdersList(ui);
        List<Map<String, String>> rows = orderRows(ui);
        Set<String> wanted = new TreeSet<String>(isNone(orders) ? Collections.<String>emptyList()
                : asListed(names(orders), numbers(rows)));
        Set<String> selected = new TreeSet<String>(selectedNumbers(ui, rows));
        if (!selected.equals(wanted)) {
            throw new AssertionError("The selected orders are " + (selected.isEmpty() ? "none" : join(new ArrayList<String>(selected)))
                    + ", expected " + (wanted.isEmpty() ? "none" : join(new ArrayList<String>(wanted))));
        }
    }

    // ---- what the choices and other screens read

    /** The order numbers the Orders list shows, in order. */
    static List<String> listedOrders(Client ui) {
        return numbers(orderRows(ui));
    }

    /**
     * Fails fast, in words, unless the main window shows with no dialog over it: the Orders tab is found within about a
     * second, and no modal dialog is open.
     */
    static void requireMain(Client ui) {
        if (!showing(ui.tab(ORDERS_TAB))) throw new DriverException(Reason.NOT_FOUND, NO_MAIN);
        String modal = modal(ui);
        if (modal != null) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "A dialog is open over the main window (" + modal
                    + "): answer or close it first");
        }
    }

    /** Fails fast, in words, unless the Orders tab is the one showing. */
    static void requireOrdersList(Client ui) {
        if (showing(ordersTab(ui).field(SEARCH_FIELD))) return;
        throw new DriverException(Reason.NOT_FOUND, presentNow(ui.tab(ORDERS_TAB)) ? NOT_ON_ORDERS : NO_MAIN);
    }

    /** The selected orders' numbers, in the list's order. */
    static List<String> selectedNumbers(Client ui, List<Map<String, String>> rows) {
        List<Integer> at;
        try {
            at = ordersTable(ui).selectedRows();
        } catch (UiActionFailed e) {
            if (e.reason() == Reason.NOT_FOUND) return Collections.emptyList();
            throw e;
        }
        List<String> out = new ArrayList<String>();
        for (int i : at) {
            out.add(i < rows.size() ? rows.get(i).get(ORDER_COLUMN) : ordersTable(ui).rowAt(i).cell(ORDER_COLUMN).shownText());
        }
        return out;
    }

    /** The row of order {@code number} in the Orders list. */
    static Target row(Client ui, String number) {
        return ordersTable(ui).row(ORDER_COLUMN, number);
    }

    /**
     * The Orders list's rows (the first ones as the read gives them, every column); the rest are read one by one by row
     * index, their Order and {@code columns}. Empty while the list's empty text replaces the table.
     */
    static List<Map<String, String>> orderRows(Client ui, String... columns) {
        Target table = ordersTable(ui);
        List<Map<String, String>> rows;
        int count;
        try {
            rows = new ArrayList<Map<String, String>>(table.rows());
            count = table.rowCount();
        } catch (UiActionFailed e) {
            if (e.reason() == Reason.NOT_FOUND) return new ArrayList<Map<String, String>>();
            throw e;
        }
        for (int i = rows.size(); i < count; i++) {
            Map<String, String> row = new LinkedHashMap<String, String>();
            String number = table.rowAt(i).cell(ORDER_COLUMN).shownText();
            row.put(ORDER_COLUMN, number);
            for (String col : columns) {
                if (!ORDER_COLUMN.equals(col)) row.put(col, shown(table.row(ORDER_COLUMN, number).cell(col)));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * The orders {@code orders} names (parted by ;, each as {@link #resolveOrder} takes it), in the order given, one named
     * twice once.
     */
    static List<String> resolveOrders(Client ui, String orders, List<Map<String, String>> rows) {
        List<String> wanted = names(orders);
        if (wanted.isEmpty()) throw new DriverException(Reason.BAD_INPUT, "Name one or more orders, parted by ;");
        Set<String> out = new LinkedHashSet<String>();
        List<Map<String, String>> full = rows;
        for (String w : wanted) {
            if (full == rows && !hasNumber(rows, w)) full = withCustomers(ui, rows);
            out.add(resolveOrder(ui, w, full));
        }
        return new ArrayList<String>(out);
    }

    /**
     * The order a tester means by {@code order}: the one whose number is it (any case) or ends with it as digits
     * ("1640"); else the one order of the customer whose name is it or starts with it. A customer with several orders
     * listed fails naming them; none fits: fails naming the orders listed and what filters them.
     */
    static String resolveOrder(Client ui, String order, List<Map<String, String>> rows) {
        String w = order.trim();
        for (Map<String, String> r : rows) {
            String n = r.get(ORDER_COLUMN);
            if (n != null && (n.equalsIgnoreCase(w) || isDigits(w) && n.endsWith("-" + w))) return n;
        }
        List<String> customers = new ArrayList<String>(new LinkedHashSet<String>(column(rows, CUSTOMER_COLUMN)));
        String lw = w.toLowerCase(Locale.ROOT);
        List<String> fit = new ArrayList<String>();
        for (String c : customers) if (c.equalsIgnoreCase(w)) fit = new ArrayList<String>(Collections.singletonList(c));
        if (fit.isEmpty()) for (String c : customers) if (c.toLowerCase(Locale.ROOT).startsWith(lw)) fit.add(c);
        if (fit.size() > 1) {
            throw new DriverException(Reason.AMBIGUOUS, quoted(order) + " fits " + fit.size() + " customers: " + join(fit)
                    + ". Give the order number");
        }
        if (fit.size() == 1) {
            List<String> theirs = new ArrayList<String>();
            for (Map<String, String> r : rows) if (fit.get(0).equals(r.get(CUSTOMER_COLUMN))) theirs.add(r.get(ORDER_COLUMN));
            if (theirs.size() == 1) return theirs.get(0);
            throw new DriverException(Reason.AMBIGUOUS, fit.get(0) + " has " + theirs.size() + " orders listed: "
                    + join(theirs) + ". Give the order number");
        }
        throw new DriverException(Reason.NOT_FOUND, "No order fits " + quoted(order) + ". The Orders list shows: "
                + (rows.isEmpty() ? "none" : join(numbers(rows))) + filters(ui));
    }

    /** The Orders tab: its scope. */
    static Target ordersTab(Client ui) {
        return ui.tab(ORDERS_TAB);
    }

    // ---- inside

    /**
     * The main window with no dialog over it, then a click on the Orders tab; returns the tab selected before, for
     * {@link #back}.
     */
    private static String goToOrders(Client ui) {
        requireMain(ui);
        String before = OrderDeskMainSteps.selectedTab(ui);
        ui.tab(ORDERS_TAB).click();
        return before;
    }

    /** After a failure before anything on the list was clicked: the tab that was selected before, selected again. */
    private static void back(final Client ui, final String before) {
        if (before != null && !ORDERS_TAB.equals(before)) quietly(() -> ui.tab(before).click());
    }

    /** {@link #resolveOrders} on the list as it shows; a miss selects the tab {@code before} again. */
    private static List<String> resolveOrders(Client ui, String orders, String before) {
        return resolveOrders(ui, orders, before, orderRows(ui));
    }

    /** {@link #resolveOrders} on {@code rows}, the list as it shows; a miss selects the tab {@code before} again. */
    private static List<String> resolveOrders(Client ui, String orders, String before, List<Map<String, String>> rows) {
        try {
            return resolveOrders(ui, orders, rows);
        } catch (RuntimeException e) {
            back(ui, before);
            throw e;
        }
    }

    /** Closes the tab of order {@code number}: selects it, then File ▸ Close tab (an order's tab closes at once). */
    private static void closeOrderTab(Client ui, String number) {
        ui.tab(number).click();
        ui.menu(FILE_MENU, CLOSE_TAB_ITEM);
    }

    /**
     * After rows were clicked: the orders {@code selected} before are selected again; none before: the orders
     * {@code clicked} are unselected again (a Cmd or Ctrl click each).
     */
    private static void reselect(Client ui, List<String> selected, List<String> clicked) {
        if (!selected.isEmpty()) {
            select(ui, selected);
            return;
        }
        for (String n : clicked) row(ui, n).click(null, 0, ADD_TO_SELECTION);
    }

    /**
     * After a sort that failed: puts the list back in the order it read in ({@code start}, its rows then) with clicks on
     * the header of a column that read sorted then (the Order column at first), at most two each; true when the orders
     * read as they did. Quiet: a click that fails ends it.
     */
    private static boolean restoreOrder(Client ui, List<Map<String, String>> start) {
        try {
            List<String> was = numbers(start);
            if (numbers(orderRows(ui)).equals(was)) return true;
            for (String c : COLUMNS) {
                Integer d = directionOf(column(start, c), comparator(c));
                if (d == null || d == 0) continue;
                for (int click = 0; click < 2; click++) {
                    ordersTable(ui).cell(c).click();
                    if (numbers(orderRows(ui)).equals(was)) return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Target ordersTable(Client ui) {
        return ordersTab(ui).table();
    }

    /** The top modal dialog's title and message, or null when none is open (or the app cannot say). */
    private static String modal(Client ui) {
        try {
            return ui.modalShowing() ? ui.describeModal() : null;
        } catch (UiActionFailed e) {
            return null;
        }
    }

    /** Selects {@code numbers}: a click on the first's row, the others with Cmd or Ctrl held. */
    private static void select(Client ui, List<String> numbers) {
        for (int i = 0; i < numbers.size(); i++) {
            if (i == 0) row(ui, numbers.get(i)).click();
            else row(ui, numbers.get(i)).click(null, 0, ADD_TO_SELECTION);
        }
    }

    /**
     * Selects the orders and opens the row menu's Cancel orders…; an order that is not Placed fails first, naming its
     * status, as the app's reason does.
     */
    private static List<String> askToCancel(Client ui, String orders) {
        String before = goToOrders(ui);
        List<Map<String, String>> rows = orderRows(ui, CUSTOMER_COLUMN, STATUS_COLUMN);
        List<String> numbers = resolveOrders(ui, orders, before);
        List<String> notPlaced = new ArrayList<String>();
        for (String n : numbers) {
            String status = valueOf(rows, n, STATUS_COLUMN);
            if (status != null && !PLACED_STATUS.equals(status)) notPlaced.add(n + " is " + status.toLowerCase(Locale.ROOT));
        }
        if (!notPlaced.isEmpty()) {
            back(ui, before);
            throw new DriverException(Reason.NOT_ACTIONABLE, CANCEL_HINT + ": " + join(notPlaced));
        }
        select(ui, numbers);
        row(ui, numbers.get(0)).rightClick(CANCEL_ORDERS_ITEM);
        return numbers;
    }

    /** The rows with their Customer (the first ones have it already; the rest read now). */
    private static List<Map<String, String>> withCustomers(Client ui, List<Map<String, String>> rows) {
        for (Map<String, String> r : rows) if (!r.containsKey(CUSTOMER_COLUMN)) return orderRows(ui, CUSTOMER_COLUMN);
        return rows;
    }

    private static boolean hasNumber(List<Map<String, String>> rows, String w) {
        for (Map<String, String> r : rows) {
            String n = r.get(ORDER_COLUMN);
            if (n != null && (n.equalsIgnoreCase(w.trim()) || isDigits(w.trim()) && n.endsWith("-" + w.trim()))) return true;
        }
        return false;
    }

    private static boolean isDigits(String w) {
        return !w.isEmpty() && w.chars().allMatch(Character::isDigit);
    }

    private static List<String> numbers(List<Map<String, String>> rows) {
        return column(rows, ORDER_COLUMN);
    }

    /** The tester's order numbers as the list writes them ("1640" is "ORD-1640"); one the list lacks stays as given. */
    private static List<String> asListed(List<String> wanted, List<String> listed) {
        List<String> out = new ArrayList<String>();
        for (String w : wanted) {
            String as = w;
            for (String n : listed) if (n.equalsIgnoreCase(w) || isDigits(w) && n.endsWith("-" + w)) as = n;
            out.add(as);
        }
        return out;
    }

    /** What narrows the list, for a miss: the search and the status filter, with the step that lifts each. */
    private static String filters(Client ui) {
        String why = "";
        String search = shown(ordersTab(ui).field(SEARCH_FIELD));
        if (search != null && !search.isEmpty()) {
            why += ". Search holds " + quoted(search) + ": \"Clear the order search\" lists every order";
        }
        String status = shownValue(ordersTab(ui).combo(STATUS_COMBO));
        if (status != null && !ALL_STATUSES.equals(status)) {
            why += ". The list shows " + status + " orders only: \"Filter the orders by status All\" lists every order";
        }
        return why;
    }

    private static String shownValue(Target combo) {
        try {
            return combo.timeout(java.time.Duration.ofMillis(1)).value();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    private static String valueOf(List<Map<String, String>> rows, String number, String col) {
        for (Map<String, String> r : rows) if (number.equals(r.get(ORDER_COLUMN))) return r.get(col);
        return null;
    }

    /** The column a tester means: its header or a word for it ("number": Order, "amount": Total, "units": Items). */
    private static String sortColumn(String column) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String c : COLUMNS) words.put(c, c);
        words.put("Order number", ORDER_COLUMN);
        words.put("Number", ORDER_COLUMN);
        words.put("Amount", TOTAL_COLUMN);
        words.put("Units", ITEMS_COLUMN);
        return resolve(column, words, "column", "columns", "The Orders list's columns");
    }

    /** +1 for ascending ("asc", "up", "oldest first", "lowest first"), -1 for descending ("newest first"). */
    static int direction(String direction) {
        String d = direction.trim().toLowerCase(Locale.ROOT);
        for (String a : Arrays.asList("asc", "up", "a-z", "a to z", "oldest", "lowest", "smallest", "fewest", "earliest", "first to last")) {
            if (d.startsWith(a)) return 1;
        }
        for (String b : Arrays.asList("desc", "down", "z-a", "z to a", "newest", "highest", "largest", "biggest", "most", "latest", "last to first")) {
            if (d.startsWith(b)) return -1;
        }
        throw new DriverException(Reason.BAD_INPUT, "No sort direction fits " + quoted(direction)
                + ". Give ascending or descending (newest first, oldest first)");
    }

    private static String word(int direction) {
        return direction > 0 ? ASCENDING : DESCENDING;
    }

    /**
     * The direction {@code values} read in: +1 ascending, -1 descending, 0 when every value is the same (no direction
     * shows), null when they are in neither order.
     */
    static Integer directionOf(List<String> values, Comparator<String> order) {
        boolean up = true;
        boolean down = true;
        for (int i = 1; i < values.size(); i++) {
            int c = order.compare(values.get(i - 1), values.get(i));
            if (c > 0) up = false;
            if (c < 0) down = false;
        }
        if (up && down) return 0;
        if (up) return 1;
        if (down) return -1;
        return null;
    }

    /**
     * How the app's table sorter orders a column, by what it shows: Items and Total as numbers, Date as a date, Status in
     * the filter's order (the app's own), and Order and Customer, which are text, with the collator of the JVM's locale
     * ({@code Collator.getInstance()}, as the sorter takes it for a text column): "ORD-1000" before "ORD-999". A step
     * runs in the app's JVM, so it is the app's locale.
     */
    static Comparator<String> comparator(String column) {
        if (ITEMS_COLUMN.equals(column) || TOTAL_COLUMN.equals(column)) {
            return Comparator.comparing(OrderDeskOrdersSteps::amount);
        }
        if (DATE_COLUMN.equals(column)) return Comparator.comparing(OrderDeskOrdersSteps::date);
        if (STATUS_COLUMN.equals(column)) return Comparator.comparing(s -> STATUSES.indexOf(s));
        final Collator names = Collator.getInstance();
        return names::compare;
    }

    /** The number in "55" or "€1,234.50" (a minus sign counts). */
    private static BigDecimal amount(String text) {
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) return BigDecimal.ZERO;
        BigDecimal n = new BigDecimal(digits);
        return text.contains("−") || text.contains("-") && !text.matches(".*[A-Za-z]-.*") ? n.negate() : n;
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {
            return LocalDate.MIN;
        }
    }
}
