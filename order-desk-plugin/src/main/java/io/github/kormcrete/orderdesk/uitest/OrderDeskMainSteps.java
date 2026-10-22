package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiActionFailed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.join;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.letters;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.look;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.messageIs;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.messageLines;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.names;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.oneLine;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quoted;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.resolve;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.showing;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shows;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskOrdersSteps.NO_MAIN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskOrdersSteps.requireMain;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.*;

/**
 * The steps and checks of Order Desk's main window, as Java: its tabs (switch, close, which are open), its menus (which
 * items are enabled), the status bar (the status line, the open orders), the questions File ▸ Close tab and File ▸ Exit
 * ask, and Help ▸ About Order Desk. {@link OrderDeskMainVocabulary} runs them for a recorded test's {@code do} steps; a
 * Java UI test calls them directly.
 *
 * <p>Every step first looks, for about a second, for the main window (an act: with no dialog over it) and says so in
 * words when it is not there. No step ends the app: File ▸ Exit is chosen only while a new order with something in its
 * cart is showing, so the app asks first, and the question is answered Cancel. Client calls only, the names the app
 * shows from {@link OrderDeskSteps}' constants.
 */
public final class OrderDeskMainSteps {

    /** The menu bar's menus, in order. */
    static final List<String> MENUS = Collections.unmodifiableList(Arrays.asList(FILE_MENU, ORDER_MENU, VIEW_MENU, HELP_MENU));
    /** The menus whose own items a check reads (View holds only the Theme submenu). */
    private static final List<String> ITEM_MENUS = Collections.unmodifiableList(Arrays.asList(FILE_MENU, ORDER_MENU, HELP_MENU));
    /** Where an item a row menu holds is opened: the Orders list's rows. */
    private static final String ROW_MENU = "the Orders list's row menu";
    static final List<String> STATES = Collections.unmodifiableList(Arrays.asList("enabled", "disabled"));

    private OrderDeskMainSteps() {
    }

    // ---- tabs

    /**
     * Switches to an open tab by a click on it: its title or the start of it, any case ("ORD-1640", "orders", "Ada" for
     * "New order · Ada Shop Ltd"), or, while an order's own tab shows, its Items, Delivery or Invoice. Two new orders for
     * one customer share a title: the step takes the newest (the rightmost).
     */
    public static void selectTab(Client ui, String tab) {
        requireMain(ui);
        List<String> main = mainTabs(ui);
        String selected = selectedTab(ui);
        List<String> inner = innerTabs(ui, selected);
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String t : main) words.put(t, t);
        for (String t : inner) words.put(t, t);
        String title = resolve(tab, words, "tab", "tabs", "The open tabs");
        if (!main.contains(title)) {
            ui.tab(selected).tab(title).click();
            return;
        }
        int same = Collections.frequency(main, title);
        if (same > 1) ui.tab(title).nth(same - 1).click();
        else ui.tab(title).click();
    }

    /**
     * Closes the selected tab: File ▸ Close tab. A new order with something in its cart asks "Discard this new order?",
     * answered Discard. The Orders tab never closes: it fails saying so.
     */
    public static void closeTab(Client ui) {
        requireMain(ui);
        String selected = closable(ui);
        boolean asks = asksBeforeClosing(ui, selected);
        ui.menu(FILE_MENU, CLOSE_TAB_ITEM);
        if (asks) ui.answer(DISCARD_ANSWER);
    }

    /**
     * Asks to close the selected new order (File ▸ Close tab) and answers Cancel to "Discard this new order?": the tab
     * stays as it was. Only a new order with something in its cart asks.
     */
    public static void keepTab(Client ui) {
        requireMain(ui);
        String selected = closable(ui);
        if (!asksBeforeClosing(ui, selected)) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "Close tab asks only for a new order with something in its"
                    + " cart; " + selected + " would close at once, so nothing was done");
        }
        ui.menu(FILE_MENU, CLOSE_TAB_ITEM);
        ui.answer(CANCEL_ANSWER);
    }

    /** Checks the open tabs' titles, left to right ({@code "Orders; New order · Ada Shop Ltd; ORD-1640"}), any case. */
    public static void openTabsAre(final Client ui, String tabs) {
        requireMainShowing(ui);
        final List<String> wanted = names(tabs);
        List<String> same = look(() -> {
            List<String> open = mainTabs(ui);
            return sameIgnoringCase(open, wanted) ? open : null;
        });
        if (same == null) throw new AssertionError("The open tabs are " + join(mainTabs(ui)) + ", expected " + join(wanted));
    }

    /**
     * Checks which tab is selected, by its whole title, any case ("Orders", "ORD-1640"); an order's own tab's Items,
     * Delivery or Invoice counts too while that order's tab is selected.
     */
    public static void selectedTabIs(final Client ui, final String tab) {
        requireMainShowing(ui);
        final String wanted = tab.trim();
        Boolean same = look(() -> {
            String selected = selectedTab(ui);
            if (wanted.equalsIgnoreCase(selected)) return Boolean.TRUE;
            String inner = selectedInnerTab(ui, selected);
            return wanted.equalsIgnoreCase(inner) ? Boolean.TRUE : null;
        });
        if (same == null) {
            String selected = selectedTab(ui);
            String inner = selectedInnerTab(ui, selected);
            throw new AssertionError("The selected tab is " + selected + (inner == null ? "" : " (its " + inner + ")")
                    + ", expected " + wanted);
        }
    }

    // ---- menus

    /**
     * Checks that a menu item is enabled or disabled: one of the menu bar's ("Close tab", "File > Close tab",
     * "Checkout…", "Remove from cart", "About Order Desk"), read by opening its menu and closing it again; or one of the
     * Orders list's row menu (Open, Cancel orders…, Copy order number), read on the selected orders by opening their row
     * menu. Cancel orders… disabled for a selection also shows "Only placed orders can be cancelled" beside the filter.
     */
    public static void menuItemIs(final Client ui, String item, String state) {
        requireMain(ui);
        boolean enabled = enabled(state);
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String menu : ITEM_MENUS) {
            for (String i : ui.menuItems(menu)) {
                words.put(i, menu + " > " + i);
                words.put(menu + " > " + i, menu + " > " + i);
            }
        }
        for (String i : OrderDeskOrdersSteps.ROW_MENU_ITEMS) words.put(i, ROW_MENU + " > " + i);
        String path = resolve(item.replace('▸', '>'), words, "menu item", "menu items", "The menu items");
        String menu = path.substring(0, path.indexOf(" > "));
        final String name = path.substring(path.indexOf(" > ") + 3);
        if (ROW_MENU.equals(menu)) {
            rowMenuItemIs(ui, name, enabled);
            return;
        }
        ui.main().role("menu", menu).click();
        List<String> states;
        try {
            states = look(() -> ui.popup().menuitem(name).states());
        } finally {
            ui.press("Escape");
        }
        boolean on = states != null && !states.contains("disabled");
        if (states == null) throw new AssertionError(menu + " ▸ " + name + " is not shown, expected " + word(enabled));
        if (on != enabled) throw new AssertionError(menu + " ▸ " + name + " is " + word(on) + ", expected " + word(enabled));
    }

    // ---- the status bar

    /** Checks the status line on the left of the status bar: "Ready", "Order ORD-1640 placed", "Copied ORD-1633". */
    public static void statusLineIs(Client ui, String message) {
        requireMainShowing(ui);
        shows(ui.main().byId(STATUS_MESSAGE_ID), "The status line", message);
    }

    /**
     * Checks the open orders on the right of the status bar: "4 open orders", "1 open order", or just the number
     * ("4"): the orders still to ship.
     */
    public static void openOrdersCountIs(Client ui, String count) {
        requireMainShowing(ui);
        String c = count.trim();
        String expected = c.matches("\\d+") ? c + " " + OPEN_ORDERS_LABEL + ("1".equals(c) ? "" : "s") : c;
        shows(ui.main().label(OPEN_ORDERS_LABEL).contains(), "The open orders count", expected);
    }

    // ---- exit

    /**
     * Asks to exit (File ▸ Exit) and answers Cancel to "Discard the open new orders?": the app and its tabs stay as they
     * were. Only while a new order with something in its cart is showing, so the app asks: without one, File ▸ Exit
     * would end the app, and the step does nothing but fail.
     */
    public static void cancelExit(Client ui) {
        askToExit(ui);
        ui.answer(CANCEL_ANSWER);
    }

    /**
     * Checks the question File ▸ Exit asks ("Discard the open new orders?"): chooses File ▸ Exit, reads it, then answers
     * Cancel (also when it differs). Needs a new order with something in its cart showing, as {@link #cancelExit} does.
     */
    public static void exitQuestionIs(Client ui, String message) {
        askToExit(ui);
        List<String> lines;
        try {
            lines = messageLines(ui.dialog(EXIT_TITLE).label(null));
        } finally {
            ui.answer(CANCEL_ANSWER);
        }
        if (lines == null) throw new AssertionError("File ▸ Exit asked nothing, expected " + quoted(message));
        messageIs(lines, message, "The exit question");
    }

    // ---- help

    /**
     * Checks what Help ▸ About Order Desk shows: opens it, reads its text, closes it (OK). Passes when the text shows
     * {@code text} as whole words, white space aside ("A small, fictional order-entry app", or the whole text).
     */
    public static void aboutShows(Client ui, String text) {
        requireMain(ui);
        ui.menu(HELP_MENU, ABOUT_ITEM);
        List<String> lines;
        try {
            lines = messageLines(ui.dialog(ABOUT_TITLE).label(null));
        } finally {
            ui.answer(OK_ANSWER);
        }
        String all = lines == null ? "" : oneLine(String.join(" ", lines));
        String want = oneLine(text);
        if (want.isEmpty() || !wordsAt(all, want)) {
            throw new AssertionError("About Order Desk reads " + quoted(all) + ", expected it to show " + quoted(want));
        }
    }

    // ---- what the choices read

    /** The open tabs' titles, left to right (an order's own Items, Delivery and Invoice left out). */
    static List<String> mainTabs(Client ui) {
        List<String> all = new ArrayList<String>(ui.tab(null).texts());
        all.removeAll(innerTabs(ui, selectedTab(ui)));
        return all;
    }

    /** The menu bar's items, as "File > Close tab", and the Orders list's row menu's. */
    static List<String> menuItemNames(Client ui) {
        List<String> out = new ArrayList<String>();
        for (String menu : ITEM_MENUS) for (String i : ui.menuItems(menu)) out.add(menu + " > " + i);
        out.addAll(OrderDeskOrdersSteps.ROW_MENU_ITEMS);
        return out;
    }

    // ---- inside

    /** Fails fast, in words, unless the main window shows (a dialog over it may be open: a read sees through). */
    private static void requireMainShowing(Client ui) {
        if (!showing(ui.tab(ORDERS_TAB))) throw new DriverException(Reason.NOT_FOUND, NO_MAIN);
    }

    /** The selected tab's title, when it is one File ▸ Close tab closes; the Orders tab fails, saying why. */
    private static String closable(Client ui) {
        String selected = selectedTab(ui);
        if (selected == null || ORDERS_TAB.equals(selected)) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "The Orders tab never closes (File ▸ Close tab is disabled"
                    + " on it). Switch to the tab to close first with \"Switch to the tab <title>\"");
        }
        return selected;
    }

    /** Close tab asks first for a new order with something in its cart. */
    private static boolean asksBeforeClosing(Client ui, String title) {
        return title.startsWith(NEW_ORDER_TAB) && !OrderDeskNewOrderSteps.cartLines(ui).isEmpty();
    }

    /** File ▸ Exit, once a new order with something in its cart shows (so the app asks instead of ending). */
    private static void askToExit(Client ui) {
        requireMain(ui);
        String selected = selectedTab(ui);
        if (selected == null || !selected.startsWith(NEW_ORDER_TAB) || OrderDeskNewOrderSteps.cartLines(ui).isEmpty()) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "File ▸ Exit asks \"Discard the open new orders?\" only while"
                    + " a new order has something in its cart; without one it ends the app, so the step chose nothing."
                    + " Show a new order with something in its cart first (" + (selected == null ? "no tab" : selected)
                    + " is selected)");
        }
        ui.menu(FILE_MENU, EXIT_ITEM);
    }

    /** The selected tab's title (the main tabs'), or null. */
    static String selectedTab(Client ui) {
        try {
            return ui.tab(ORDERS_TAB).timeout(java.time.Duration.ofMillis(1)).selectedText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    /** The tabs inside the selected order's own tab (Items, Delivery, Invoice); none for other tabs. */
    private static List<String> innerTabs(Client ui, String selected) {
        if (selected == null || ORDERS_TAB.equals(selected) || selected.startsWith(NEW_ORDER_TAB)) {
            return Collections.emptyList();
        }
        try {
            return ui.tab(selected).tab(null).texts();
        } catch (UiActionFailed e) {
            return Collections.emptyList();
        }
    }

    /** The selected tab inside the selected order's own tab, or null. */
    private static String selectedInnerTab(Client ui, String selected) {
        List<String> inner = innerTabs(ui, selected);
        if (inner.isEmpty()) return null;
        try {
            return ui.tab(selected).tab(inner.get(0)).timeout(java.time.Duration.ofMillis(1)).selectedText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    /** Reads a row menu item's state: opens the selected orders' row menu, reads, closes it (Escape). */
    private static void rowMenuItemIs(final Client ui, final String name, boolean enabled) {
        OrderDeskOrdersSteps.requireOrdersList(ui);
        List<String> selected = OrderDeskOrdersSteps.selectedNumbers(ui, OrderDeskOrdersSteps.orderRows(ui));
        if (selected.isEmpty()) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "No order is selected: the row menu acts on the selected"
                    + " orders. Select them first with \"Select the orders <orders>\"");
        }
        OrderDeskOrdersSteps.row(ui, selected.get(0)).click("right", 1);
        List<String> states;
        try {
            states = look(() -> ui.popup().menuitem(name).states());
        } finally {
            ui.press("Escape");
        }
        if (states == null) throw new AssertionError("The row menu's " + name + " is not shown, expected " + word(enabled));
        boolean on = !states.contains("disabled");
        String hint = CANCEL_ORDERS_ITEM.equals(name) ? shown(OrderDeskOrdersSteps.ordersTab(ui).label(CANCEL_HINT)) : null;
        if (on != enabled) {
            throw new AssertionError("The row menu's " + name + " is " + word(on) + " for " + join(selected)
                    + (hint == null ? "" : " (" + quoted(hint) + ")") + ", expected " + word(enabled));
        }
        if (CANCEL_ORDERS_ITEM.equals(name) && !enabled && hint == null) {
            throw new AssertionError("Cancel orders… is disabled for " + join(selected) + " but " + quoted(CANCEL_HINT)
                    + " is not shown beside the filter");
        }
    }

    /** "enabled" (also "on", "!disabled") or "disabled" ("greyed out", "off"). */
    private static boolean enabled(String state) {
        String s = letters(state);
        if (s.startsWith("enab") || s.equals("on") || s.equals("active") || s.equals("disabled") && state.trim().startsWith("!")) {
            return true;
        }
        if (s.startsWith("disab") || s.startsWith("grey") || s.startsWith("gray") || s.equals("off") || s.equals("inactive")) {
            return false;
        }
        throw new DriverException(Reason.BAD_INPUT, "No state fits " + quoted(state) + ". Give enabled or disabled");
    }

    private static String word(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static boolean sameIgnoringCase(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).equalsIgnoreCase(b.get(i))) return false;
        return true;
    }

    /** {@code want} occurs in {@code all} as whole words, any case. */
    private static boolean wordsAt(String all, String want) {
        String a = " " + all.toLowerCase(Locale.ROOT) + " ";
        String w = want.toLowerCase(Locale.ROOT);
        int at = a.indexOf(w);
        while (at >= 0) {
            boolean before = at == 0 || !Character.isLetterOrDigit(a.charAt(at - 1));
            int end = at + w.length();
            boolean after = end >= a.length() || !Character.isLetterOrDigit(a.charAt(end));
            if (before && after) return true;
            at = a.indexOf(w, at + 1);
        }
        return false;
    }
}
