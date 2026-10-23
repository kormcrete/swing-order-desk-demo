package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.orderdesk.uitest.OrderDeskMainSteps.TabAt;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.FileChooser;
import io.github.kormcrete.swingui.junit.Rows;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiActionFailed;
import io.github.kormcrete.swingui.junit.UiAssertionError;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.LOOK;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.column;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.isNone;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.join;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.letters;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.look;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.names;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.notShown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.oneLine;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.pick;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quietly;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quoted;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.resolve;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.showing;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shows;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.partsShow;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskOrdersSteps.NO_MAIN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskOrdersSteps.requireMain;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.*;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;

/**
 * The steps and checks of an order's own tab, as Java: its status, the line beside it (customer, date, total), Save
 * receipt… (with the question about a file that exists and the error when the app cannot write it), Cancel order (and
 * its reason while disabled), and its Items, Delivery and Invoice tabs. {@link OrderDeskOrderTabVocabulary} runs them
 * for a recorded test's {@code do} steps; a Java UI test calls them directly.
 *
 * <p>An order is given by its number ("ORD-1640") or its digits ("1640"), among the order tabs that are open. An act
 * first looks, for about a second, for the main window with no dialog over it, then clicks the order's tab, as a person
 * goes there first; after a failure it selects the tab that was selected before. A check reads the order's tab as it
 * shows and fails fast, in words, when another tab is selected; one that reads Items, Delivery or Invoice shows that
 * tab for the read (a click on it, as a person does to look) and then the one that showed before. Client calls only,
 * the names the app shows from {@link OrderDeskSteps}' constants.
 */
public final class OrderDeskOrderTabSteps {

    /** The tabs inside an order's own tab, in order. */
    static final List<String> SUB_TABS = Collections.unmodifiableList(Arrays.asList(ITEMS_TAB, DELIVERY_TAB, INVOICE_TAB));
    /** The Items table's columns, in order (SKU names a line, as the Check menu keys it). */
    static final List<String> ITEM_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            SKU_COLUMN, PRODUCT_COLUMN, QTY_COLUMN, PRICE_COLUMN, LINE_TOTAL_COLUMN));
    /** What the Delivery tab shows, by caption. */
    static final List<String> DELIVERY_DETAILS = Collections.unmodifiableList(Arrays.asList(
            DELIVERY_ADDRESS_LABEL, SHIPPING_METHOD_LABEL, DELIVERY_NOTE_LABEL));
    /** What the Invoice tab shows, by caption. */
    static final List<String> INVOICE_DETAILS = Collections.unmodifiableList(Arrays.asList(
            PAYMENT_LABEL, ORDER_DATE_LABEL, SUBTOTAL_LABEL, VOLUME_DISCOUNT_LABEL, SHIPPING_LABEL, VAT_LABEL, TOTAL_LABEL));
    /** An order's status, as its tab and the Orders list show it. */
    static final List<String> STATUSES = Collections.unmodifiableList(Arrays.asList(PLACED_STATUS, SHIPPED_STATUS, CANCELLED_STATUS));
    /** The order tab's buttons whose state the new order's buttonIsDisabled and buttonIsEnabled read here. */
    static final List<String> BUTTONS = Collections.unmodifiableList(Arrays.asList(CANCEL_ORDER_BUTTON, SAVE_RECEIPT_BUTTON));

    /** What a check of an order tab's button says when no order's own tab is selected. */
    static final String NO_ORDER_TAB = "No order's own tab is selected. Open one first with \"Open order <order>\", or"
            + " switch to its tab with \"Switch to the tab <order>\"";

    private OrderDeskOrderTabSteps() {
    }

    // ---- save the receipt

    /**
     * Saves the order's receipt: its tab, Save receipt…, then {@code file} in the file chooser (a path, absolute or in the
     * folder the chooser opens in; the app adds .txt when it is missing). A file that exists is replaced: the question
     * "ORD-1640.txt already exists." is answered Replace. Returns where it was saved, as the status line says it
     * ({@code {"path":"/Users/ada/ORD-1640.txt","replaced":false}}). When the app cannot save it, the step answers its
     * message OK, selects the tab selected before and fails with that message.
     *
     * <p>It reads what the file chooser's choice made show with {@code ui.outcomeOf}: a hand-written Java test must not
     * call it inside its own {@code ui.outcomeOf(...)} (IllegalStateException: one at a time).
     */
    public static Map<String, Object> saveReceipt(Client ui, String order, String file) {
        String name = receiptName(file);
        At at = goToOrderTab(ui, order);
        String message = chooseReceipt(ui, at.number, file);
        boolean replaced = false;
        if (message != null && isReplaceQuestion(message)) {
            replaced = true;
            message = messageOf(ui, () -> ui.answer(REPLACE_ANSWER));
        }
        if (message != null) {
            dismiss(ui, message);
            back(ui, at);
            throw new DriverException(Reason.NOT_ACTIONABLE, "Save receipt says " + quoted(oneLine(message)));
        }
        String path = savedPath(ui, name);
        if (path == null) {
            back(ui, at);
            String status = shown(statusLine(ui));
            throw new DriverException(Reason.NOT_ACTIONABLE, "The status line reads " + quoted(String.valueOf(status))
                    + ", expected " + quoted(RECEIPT_SAVED + "…" + name));
        }
        Map<String, Object> saved = new LinkedHashMap<String, Object>();
        saved.put("path", path);
        saved.put("replaced", replaced);
        return saved;
    }

    /**
     * Asks to save the order's receipt over a file that exists, then keeps the file: Save receipt…, {@code file}, and
     * Cancel to "ORD-1640.txt already exists. Do you want to replace it?". A file that does not exist is saved at once (the
     * app asks nothing): the step then fails saying so. Not inside a Java test's own {@code ui.outcomeOf} (see
     * {@link #saveReceipt}).
     */
    public static void keepExistingReceipt(Client ui, String order, String file) {
        String name = receiptName(file);
        At at = goToOrderTab(ui, order);
        String message = chooseReceipt(ui, at.number, file);
        if (message == null) {
            String status = shown(statusLine(ui));
            back(ui, at);
            throw new DriverException(Reason.NOT_ACTIONABLE, "Save receipt asked nothing: " + name + " did not exist, so"
                    + " the receipt was saved at once (the status line reads " + quoted(String.valueOf(status)) + ")");
        }
        if (!isReplaceQuestion(message)) {
            dismiss(ui, message);
            back(ui, at);
            throw new DriverException(Reason.NOT_ACTIONABLE, "Save receipt says " + quoted(oneLine(message))
                    + ", expected the question " + quoted(name + ALREADY_EXISTS));
        }
        ui.answer(CANCEL_ANSWER);
    }

    /**
     * Checks the error Save receipt… shows when the app cannot write {@code file} (a folder that does not exist, say):
     * "The receipt could not be saved: <the reason>". {@code message}: the whole text, the sentence before the colon, or
     * the reason after it. The error is answered OK and the tab selected before shows again; a file the app can write is
     * saved, and the check fails. Not inside a Java test's own {@code ui.outcomeOf} (see {@link #saveReceipt}).
     */
    public static void receiptSaveErrorIs(Client ui, String order, String file, String message) {
        receiptName(file);
        At at = goToOrderTab(ui, order);
        String shown = chooseReceipt(ui, at.number, file);
        if (shown == null) {
            String status = shown(statusLine(ui));
            back(ui, at);
            throw new AssertionError("The receipt was saved (the status line reads " + quoted(String.valueOf(status))
                    + "), expected the error " + quoted(oneLine(message)));
        }
        dismiss(ui, shown);
        back(ui, at);
        if (!errorFits(oneLine(shown), oneLine(message))) {
            throw new AssertionError("Save receipt says " + quoted(oneLine(shown)) + ", expected " + quoted(oneLine(message)));
        }
    }

    /**
     * Checks the file name Save receipt… suggests for the order (its number, "ORD-1640.txt", in the folder the chooser
     * opens in): its tab, Save receipt…, reads the file chooser, then Cancel, and the tab selected before shows again.
     * {@code file}: the name, with or without .txt, any case, or the whole path (the folder and the name).
     */
    public static void suggestedReceiptFileIs(Client ui, String order, String file) {
        At at = goToOrderTab(ui, order);
        ui.tab(at.number).button(SAVE_RECEIPT_BUTTON).click();
        String name;
        String folder;
        try {
            FileChooser chooser = ui.fileChooser();
            name = chooser.name();
            folder = chooser.folder();
        } finally {
            quietly(() -> ui.chooseFile(at.number + RECEIPT_EXTENSION, false));
            back(ui, at);
        }
        String want = oneLine(file);
        String shown = name == null ? "" : name;
        String path = folder == null ? shown : folder + (folder.endsWith("/") || folder.endsWith("\\") ? "" : "/") + shown;
        boolean fits = want.equalsIgnoreCase(shown) || (want + RECEIPT_EXTENSION).equalsIgnoreCase(shown)
                || want.equals(path) || want.replace('\\', '/').equals(path.replace('\\', '/'));
        if (!fits) {
            throw new AssertionError("Save receipt… suggests " + quoted(shown) + (folder == null ? "" : " in " + folder)
                    + ", expected " + quoted(want));
        }
    }

    // ---- cancel the order

    /**
     * Cancels the order from its own tab: Cancel order, then Cancel order to "Cancel order ORD-1640?"; its status then
     * reads Cancelled. A shipped or cancelled order fails before anything is clicked on its tab, with the app's reason
     * ("Only placed orders can be cancelled — this one is shipped").
     */
    public static void cancelOrder(final Client ui, String order) {
        final At at = askToCancel(ui, order);
        ui.answer(CANCEL_ORDER_ANSWER);
        final Target pill = ui.tab(at.number).byId(ORDER_STATUS_ID);
        if (look(() -> CANCELLED_STATUS.equals(shown(pill)) ? Boolean.TRUE : null) == null) {
            back(ui, at);
            throw new DriverException(Reason.NOT_ACTIONABLE, at.number + "'s status reads " + quoted(String.valueOf(shown(pill)))
                    + " after Cancel order, expected " + quoted(CANCELLED_STATUS));
        }
    }

    /** Asks to cancel the order from its own tab (Cancel order), then answers Keep: nothing changes. */
    public static void keepOrder(Client ui, String order) {
        askToCancel(ui, order);
        ui.answer(KEEP_ANSWER);
    }

    // ---- checks of the order's tab

    /** Checks the status beside the order's number: Placed, Shipped or Cancelled, any case. */
    public static void orderStatusIs(Client ui, String order, String status) {
        final String number = requireSelected(ui, order);
        final String wanted = pick(status, STATUSES, "status", "statuses", "An order's status is one of");
        final Target pill = ui.tab(number).byId(ORDER_STATUS_ID);
        if (look(() -> wanted.equals(shown(pill)) ? Boolean.TRUE : null) == null) {
            String actual = shown(pill);
            throw new AssertionError(number + "'s status " + (actual == null ? "is not shown" : "reads " + quoted(actual))
                    + ", expected " + quoted(wanted));
        }
    }

    /**
     * Checks the line beside the order's number and status: the customer, the date and the total ("Ada Shop Ltd · 9 Oct
     * 2026 · €95.52"), or one of the three.
     */
    public static void orderFactsAre(Client ui, String order, String facts) {
        String number = requireSelected(ui, order);
        partsShow(ui.tab(number).byId(ORDER_FACTS_ID), facts, FACTS_SEPARATOR, number + "'s customer, date and total read");
    }

    /** Checks the order's lines, top to bottom, by product name or the start of it, any case, parted by ;. */
    public static void orderItemsAre(final Client ui, String order, String lines) {
        final String number = requireSelected(ui, order);
        final List<String> given = names(lines);
        onSubTab(ui, number, ITEMS_TAB, () -> {
            List<String> actual = column(itemRows(ui, number), PRODUCT_COLUMN);
            List<String> wanted = new ArrayList<String>();
            for (String g : given) wanted.add(startOf(g, actual));
            if (!actual.equals(wanted)) {
                throw new AssertionError(number + "'s lines are " + (actual.isEmpty() ? "none" : join(actual))
                        + ", expected " + join(wanted));
            }
        });
    }

    /**
     * Checks one value of a line: its SKU, Product, Qty, Price or Line total. {@code product}: the name, its start (any
     * case) or its SKU.
     */
    public static void orderItemIs(final Client ui, String order, final String product, final String column, final String value) {
        final String number = requireSelected(ui, order);
        onSubTab(ui, number, ITEMS_TAB, () -> {
            List<Map<String, String>> rows = itemRows(ui, number);
            String name = null;
            for (Map<String, String> r : rows) if (product.trim().equalsIgnoreCase(r.get(SKU_COLUMN))) name = r.get(PRODUCT_COLUMN);
            if (name == null) name = pick(product, column(rows, PRODUCT_COLUMN), "line", "lines", number + "'s lines");
            String col = itemColumn(column);
            String sku = null;
            for (Map<String, String> r : rows) if (name.equals(r.get(PRODUCT_COLUMN))) sku = r.get(SKU_COLUMN);
            shows(itemsTable(ui, number).row(Rows.row(SKU_COLUMN, sku, PRODUCT_COLUMN, name)).cell(col),
                    name + "'s " + col + " on " + number, value);
        });
    }

    /** Checks the count under the lines: "55 units in 3 lines", or one of its two parts ("55 units", "3 lines"). */
    public static void orderItemsSummaryIs(final Client ui, String order, final String summary) {
        final String number = requireSelected(ui, order);
        onSubTab(ui, number, ITEMS_TAB, () -> partsShow(ui.tab(number).tab(ITEMS_TAB).byId(ITEMS_SUMMARY_ID), summary,
                SUMMARY_SEPARATOR, number + "'s units read"));
    }

    /**
     * Checks the Delivery tab: the Delivery address (its lines parted by ;, or one of them), the Shipping method
     * ("Standard · 3–5 business days · €4.90") or the Delivery note ("—", or none, when there is none).
     */
    public static void orderDeliveryIs(final Client ui, String order, String detail, final String value) {
        final String number = requireSelected(ui, order);
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String d : DELIVERY_DETAILS) words.put(d, d);
        words.put("Address", DELIVERY_ADDRESS_LABEL);
        words.put("Note", DELIVERY_NOTE_LABEL);
        words.put("Method", SHIPPING_METHOD_LABEL);
        final String caption = resolve(detail, words, "detail", "details", "The Delivery tab shows");
        onSubTab(ui, number, DELIVERY_TAB, () -> {
            Target tab = ui.tab(number).tab(DELIVERY_TAB);
            if (DELIVERY_ADDRESS_LABEL.equals(caption)) {
                addressIs(tab, number, value);
                return;
            }
            String expected = DELIVERY_NOTE_LABEL.equals(caption) && isNone(value) ? NO_NOTE : value;
            if (SHIPPING_METHOD_LABEL.equals(caption)) {
                // "Standard · 3–5 business days · €4.90": the method's name alone, or another part, passes too
                partsShow(tab.label(caption), expected, PARTS_SEPARATOR, number + "'s " + caption + " reads");
            } else {
                shows(tab.label(caption), number + "'s " + caption, expected);
            }
        });
    }

    /**
     * Checks the Invoice tab: the Payment ("Invoice (30 days) · PO PO-2026-131", "Card ending 4242"), the Order date, or an
     * amount (Subtotal, Volume discount, Shipping, VAT (20%), Total); {@code none}: not shown (no volume discount).
     */
    public static void orderInvoiceIs(final Client ui, String order, String detail, final String value) {
        final String number = requireSelected(ui, order);
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String d : INVOICE_DETAILS) words.put(d, d);
        words.put("Date", ORDER_DATE_LABEL);
        words.put("Discount", VOLUME_DISCOUNT_LABEL);
        final String caption = resolve(detail, words, "detail", "details", "The Invoice tab shows");
        onSubTab(ui, number, INVOICE_TAB, () -> {
            Target label = ui.tab(number).tab(INVOICE_TAB).label(caption);
            if (isNone(value)) notShown(label, number + "'s " + caption);
            else shows(label, number + "'s " + caption, value);
        });
    }

    // ---- Cancel order's and Save receipt…'s state (OrderDeskNewOrderSteps.buttonIsDisabled, buttonIsEnabled)

    /** The order tab's button {@code button} means ("Cancel order", "cancel", "Save receipt…", "save"); null: none. */
    static String button(String button) {
        String b = letters(button);
        if (b.isEmpty()) return null;
        for (String name : BUTTONS) {
            String first = letters(name.split(" ")[0]);
            if (letters(name).equals(b) || b.startsWith(first) && letters(name).startsWith(b)) return name;
        }
        return null;
    }

    /** Checks that {@code name} is disabled on the selected order's tab, and the reason beside it (whole, or after the dash). */
    static void buttonIsDisabled(Client ui, String name, String reason) {
        final String number = requireOrderTabSelected(ui);
        try {
            assertThat(ui.tab(number).button(name)).within(LOOK).isDisabled();
        } catch (UiAssertionError e) {
            throw new AssertionError(name + " is enabled on " + number + ", expected disabled: " + quoted(reason));
        }
        final Target hint = ui.tab(number).label(CANCEL_HINT).contains();
        final String want = oneLine(reason);
        String fits = look(() -> {
            String actual = shown(hint);
            return actual != null && reasonFits(oneLine(actual), want) ? actual : null;
        });
        if (fits == null) {
            String actual = shown(hint);
            throw new AssertionError(name + "'s reason on " + number + (actual == null ? " is not shown" : " reads " + quoted(actual))
                    + ", expected " + quoted(want));
        }
    }

    /** Checks that {@code name} is enabled on the selected order's tab; when not, the failure gives the reason beside it. */
    static void buttonIsEnabled(Client ui, String name) {
        String number = requireOrderTabSelected(ui);
        try {
            assertThat(ui.tab(number).button(name)).within(LOOK).isEnabled();
        } catch (UiAssertionError e) {
            String reason = shown(ui.tab(number).label(CANCEL_HINT).contains());
            throw new AssertionError(name + " is disabled on " + number + (reason == null ? "" : " (" + quoted(reason) + ")")
                    + ", expected enabled");
        }
    }

    // ---- what the choices read

    /** The open tabs titled with an order number, left to right. */
    static List<String> openOrderTabs(Client ui) {
        List<String> out = new ArrayList<String>();
        for (String t : OrderDeskMainSteps.mainTabs(ui)) if (t.startsWith(ORDER_NUMBER_PREFIX)) out.add(t);
        return out;
    }

    /** The lines of the selected order (its Items tab showing), by product name. */
    static List<String> selectedOrderItems(Client ui) {
        String number = requireOrderTabSelected(ui);
        return column(itemRows(ui, number), PRODUCT_COLUMN);
    }

    // ---- inside

    /** Where an act started: the order's number, and the tab selected before it clicked that order's tab. */
    private static final class At {
        final String number;
        final TabAt before;

        At(String number, TabAt before) {
            this.number = number;
            this.before = before;
        }
    }

    /** The main window with no dialog over it, then a click on the order's tab (open, by its number or digits). */
    private static At goToOrderTab(Client ui, String order) {
        requireMain(ui);
        TabAt before = OrderDeskMainSteps.selectedTabAt(ui);
        String number = openOrderTab(ui, order);
        ui.tab(number).click();
        return new At(number, before);
    }

    /** After a failure: the tab that was selected before the act clicked the order's, selected again. */
    private static void back(final Client ui, final At at) {
        if (at.before != null && !at.before.title.equals(at.number)) quietly(() -> OrderDeskMainSteps.select(ui, at.before));
    }

    /** The open order tab {@code order} names: its number, any case, or its digits; none fails naming the open ones. */
    private static String openOrderTab(Client ui, String order) {
        String w = order.trim();
        List<String> open = openOrderTabs(ui);
        for (String n : open) {
            if (n.equalsIgnoreCase(w) || !w.isEmpty() && w.chars().allMatch(Character::isDigit) && n.endsWith("-" + w)) return n;
        }
        throw new DriverException(Reason.NOT_FOUND, "No open order tab fits " + quoted(order) + ". The open order tabs: "
                + (open.isEmpty() ? "none" : join(open)) + ". Open it first with \"Open order " + w + "\"");
    }

    /** Fails fast, in words, unless the main window shows and {@code order}'s own tab is the one selected. */
    private static String requireSelected(Client ui, String order) {
        requireMainShowing(ui);
        String number = openOrderTab(ui, order);
        String selected = OrderDeskMainSteps.selectedTab(ui);
        if (!number.equals(selected)) {
            throw new DriverException(Reason.NOT_FOUND, "Order " + number + "'s tab is not selected ("
                    + (selected == null ? "no tab" : selected) + " is). Switch to it first with \"Switch to the tab " + number + "\"");
        }
        return number;
    }

    /** Fails fast, in words, unless the main window shows with an order's own tab selected; returns its number. */
    private static String requireOrderTabSelected(Client ui) {
        requireMainShowing(ui);
        String selected = OrderDeskMainSteps.selectedTab(ui);
        if (selected == null || !selected.startsWith(ORDER_NUMBER_PREFIX)) {
            throw new DriverException(Reason.NOT_FOUND, NO_ORDER_TAB + (selected == null ? "" : " (" + selected + " is selected)"));
        }
        return selected;
    }

    /** Fails fast, in words, unless the main window shows (a dialog over it may be open: a read sees through). */
    private static void requireMainShowing(Client ui) {
        if (!showing(ui.tab(ORDERS_TAB))) throw new DriverException(Reason.NOT_FOUND, NO_MAIN);
    }

    /**
     * Runs {@code read} with {@code sub} showing inside the order's tab: when another one shows, a click on {@code sub}
     * first (no dialog may be open), and on the one that showed before after it, also when the read fails.
     */
    private static void onSubTab(final Client ui, final String number, String sub, Runnable read) {
        final String was = selectedSubTab(ui, number);
        if (was == null) {
            // a click without knowing what showed could not be undone
            throw new DriverException(Reason.NOT_FOUND, number + "'s " + ITEMS_TAB + ", " + DELIVERY_TAB + " and " + INVOICE_TAB
                    + " tabs are not found");
        }
        boolean go = !sub.equals(was);
        if (go) {
            requireMain(ui);
            ui.tab(number).tab(sub).click();
        }
        try {
            read.run();
        } finally {
            if (go) quietly(() -> ui.tab(number).tab(was).click());
        }
    }

    /** The tab showing inside the order's tab (Items, Delivery, Invoice), or null. */
    private static String selectedSubTab(Client ui, String number) {
        try {
            return ui.tab(number).tab(ITEMS_TAB).timeout(Duration.ofMillis(1)).selectedText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    private static Target itemsTable(Client ui, String number) {
        return ui.tab(number).tab(ITEMS_TAB).table();
    }

    /** The Items table's rows (every column); the rows past a read's first ones are read one by one, by row index. */
    private static List<Map<String, String>> itemRows(Client ui, String number) {
        Target table = itemsTable(ui, number);
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
            for (String col : ITEM_COLUMNS) row.put(col, shown(table.rowAt(i).cell(col)));
            rows.add(row);
        }
        return rows;
    }

    /** The Items column {@code column} means: its header, its start or "quantity" for Qty. */
    private static String itemColumn(String column) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String c : ITEM_COLUMNS) words.put(c, c);
        words.put("Quantity", QTY_COLUMN);
        return resolve(column, words, "column", "columns", "The Items tab's columns");
    }

    /** The one of {@code actual} that is {@code given} or starts with it, any case; else {@code given} as it is. */
    private static String startOf(String given, List<String> actual) {
        String g = given.trim().toLowerCase(Locale.ROOT);
        String fit = null;
        for (String a : actual) {
            String la = a.toLowerCase(Locale.ROOT);
            if (la.equals(g)) return a;
            if (la.startsWith(g)) {
                if (fit != null) return given.trim();
                fit = a;
            }
        }
        return fit != null ? fit : given.trim();
    }

    /** The address lines, read within {@link OrderDeskLooks#LOOK}; given parted by ; they must all match, one line may match one. */
    private static void addressIs(final Target tab, String number, String value) {
        final Target lines = tab.byId(DELIVERY_ADDRESS_ID).label(null);
        List<String> actual = look(() -> {
            List<String> texts = lines.texts();
            List<String> values = lines.values();
            List<String> out = new ArrayList<String>();
            for (int i = 0; i < texts.size(); i++) {
                String v = i < values.size() ? values.get(i) : null;
                out.add(oneLine(v != null && !v.isEmpty() ? v : texts.get(i)));
            }
            return out.isEmpty() ? null : out;
        });
        if (actual == null) actual = Collections.emptyList();
        List<String> wanted = new ArrayList<String>();
        for (String w : names(value)) wanted.add(oneLine(w));
        boolean same = wanted.size() == 1 ? actual.contains(wanted.get(0)) : actual.equals(wanted);
        if (!same) {
            throw new AssertionError(number + "'s delivery address " + (actual.isEmpty() ? "is not shown"
                    : "reads " + quoted(join(actual))) + ", expected " + quoted(join(wanted)));
        }
    }

    /** The receipt's file name as the app writes it: {@code file}'s last part, .txt added when missing. */
    private static String receiptName(String file) {
        if (file == null || file.trim().isEmpty()) {
            throw new DriverException(Reason.BAD_INPUT, "Give the file to save the receipt as, e.g. receipts/ORD-1639.txt");
        }
        String f = file.trim();
        String name = f.substring(Math.max(f.lastIndexOf('/'), f.lastIndexOf('\\')) + 1);
        return name.toLowerCase(Locale.ROOT).endsWith(RECEIPT_EXTENSION) ? name : name + RECEIPT_EXTENSION;
    }

    /** Save receipt… on the order's tab, then {@code file} in its file chooser; the message that then shows, or null. */
    private static String chooseReceipt(final Client ui, String number, final String file) {
        ui.tab(number).button(SAVE_RECEIPT_BUTTON).click();
        return messageOf(ui, () -> ui.chooseFile(file.trim()));
    }

    /**
     * The text of the question or error {@code action} makes show, as the app's reading of what changed has it (the whole
     * message: a long one's labels are not all in the tree); null when it shows none. Read from the outcome's text, the
     * reading taken right after the action: its JSON objects are the agent's own (shaded) classes, which a plugin does not
     * link against, and a read by path would wait for a message that never comes when the receipt was saved.
     */
    private static String messageOf(Client ui, Runnable action) {
        String message = OrderDeskLooks.topLevelText(ui.outcomeOf(action).toString(), "message");
        return message == null || message.isEmpty() ? null : message;
    }

    private static boolean isReplaceQuestion(String message) {
        return message.contains(ALREADY_EXISTS);
    }

    /** Answers the question or error Save receipt… showed: Cancel to the question, OK to the error, else Escape. */
    private static void dismiss(final Client ui, String message) {
        final String button = isReplaceQuestion(message) ? CANCEL_ANSWER : message.startsWith(RECEIPT_NOT_SAVED) ? OK_ANSWER : "escape";
        quietly(() -> ui.answer(button));
    }

    /** The path the status line says the receipt was saved to, when it names {@code name}; null when it does not within {@link OrderDeskLooks#LOOK}. */
    private static String savedPath(final Client ui, final String name) {
        return look(() -> {
            String status = shown(statusLine(ui));
            if (status == null || !status.startsWith(RECEIPT_SAVED)) return null;
            String path = status.substring(RECEIPT_SAVED.length());
            return path.endsWith("/" + name) || path.endsWith("\\" + name) || path.equals(name) ? path : null;
        });
    }

    private static Target statusLine(Client ui) {
        return ui.main().byId(STATUS_MESSAGE_ID);
    }

    /** "The receipt could not be saved: /x": the whole of it, the sentence before the colon (with or without it), or after. */
    private static boolean errorFits(String shown, String want) {
        if (shown.equals(want)) return true;
        int colon = shown.indexOf(": ");
        if (colon < 0) return false;
        String reason = shown.substring(0, colon);
        String w = want.endsWith(":") ? want.substring(0, want.length() - 1).trim() : want;
        return reason.equals(w) || shown.substring(colon + 2).trim().equals(want);
    }

    /** "Only placed orders can be cancelled — this one is shipped": the whole of it, or what follows the dash, any case. */
    private static boolean reasonFits(String shown, String want) {
        if (shown.equalsIgnoreCase(want)) return true;
        int dash = shown.indexOf(HINT_DASH.trim());
        return dash >= 0 && shown.substring(dash + 1).trim().equalsIgnoreCase(want);
    }

    /** Goes to the order's tab and presses Cancel order; a disabled one fails first, with the reason beside it. */
    private static At askToCancel(Client ui, String order) {
        At at = goToOrderTab(ui, order);
        final Target cancel = ui.tab(at.number).button(CANCEL_ORDER_BUTTON);
        List<String> states = look(cancel::states);
        if (states != null && states.contains("disabled")) {
            String hint = shown(ui.tab(at.number).label(CANCEL_HINT).contains());
            back(ui, at);
            throw new DriverException(Reason.NOT_ACTIONABLE, "Cancel order is disabled on " + at.number
                    + (hint == null ? "" : ": " + quoted(hint)));
        }
        cancel.click();
        return at;
    }
}
