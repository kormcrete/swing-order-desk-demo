package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Rows;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiActionFailed;
import io.github.kormcrete.swingui.junit.UiAssertionError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.LOOK;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.column;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.isNone;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.join;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.look;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quietly;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.names;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.namesAre;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.notShown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.pick;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quoted;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shows;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.showing;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.*;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;

/**
 * The steps and checks of a new order's tab, as Java: the customer, the products (categories, search, stock), the cart
 * (lines, quantities, amounts) and the reasons a button is disabled. {@link OrderDeskVocabulary} runs them for a
 * recorded test's {@code do} steps; a Java UI test calls them directly.
 *
 * <p>Every step first looks, for about a second, for the new order it needs, and says so in words when none is
 * showing. A product, a cart line or a customer is given by its name or the start of it, any case; when none or
 * several fit, the step fails naming the ones that fit. A check fails at once with what was expected and what the app
 * shows. Several names in one param are parted by {@code ;} (product names hold commas); {@code none} stands for "not
 * shown". Client calls only, and the names the app shows come from {@link OrderDeskSteps}' constants.
 */
public final class OrderDeskNewOrderSteps {

    /** The columns of the products table a check reads (Product names the row). */
    static final List<String> PRODUCT_CHECK_COLUMNS = Collections.unmodifiableList(Arrays.asList(SKU_COLUMN, PRICE_COLUMN, STOCK_COLUMN));
    /** The columns of the cart a check reads (Product names the line). */
    static final List<String> CART_CHECK_COLUMNS = Collections.unmodifiableList(Arrays.asList(QTY_COLUMN, PRICE_COLUMN, LINE_TOTAL_COLUMN));
    /** The cart's amounts, in the order they show. */
    static final List<String> CART_AMOUNTS = Collections.unmodifiableList(Arrays.asList(
            SUBTOTAL_LABEL, VOLUME_DISCOUNT_LABEL, SHIPPING_LABEL, VAT_LABEL, TOTAL_LABEL));
    /** The new order's buttons that show a reason while disabled. */
    static final List<String> REASON_BUTTONS = Collections.unmodifiableList(Arrays.asList(
            ADD_TO_CART_BUTTON, REMOVE_BUTTON, CHECKOUT_BUTTON));
    /** Where a button name that fits none was looked for. */
    private static final String REASON_BUTTONS_WHERE = "The buttons that give a reason (in the checkout also Next,"
            + " Express, Invoice; on an order's own tab Cancel order)";
    private static final String ONE_LINE_ASKS_NOTHING = "Removing one line asks nothing: name two or more cart lines, parted by ;";
    /** What a step says when the new order it needs is not showing. */
    static final String NO_NEW_ORDER = "No new order is showing. Open one first with \"Open a new order for <customer>\","
            + " or select its tab";

    private OrderDeskNewOrderSteps() {
    }

    // ---- the customer

    /**
     * Opens a new order (File ▸ New order) and picks its customer as {@link #chooseCustomer} does. Ends on the new order,
     * titled "New order · Ada Shop Ltd".
     */
    public static void openNewOrder(Client ui, String customer) {
        ui.menu(FILE_MENU, NEW_ORDER_ITEM);
        try {
            pickCustomer(ui, ui.tab(NEW_ORDER_TAB).field(CUSTOMER_FIELD), customer);
        } catch (RuntimeException e) {
            // no customer fits: close the empty tab this step opened (nothing in its cart, so no question)
            quietly(() -> ui.menu(FILE_MENU, CLOSE_TAB_ITEM));
            throw e;
        }
    }

    /**
     * Picks the customer of the showing new order: types {@code customer} into Customer, then takes the suggestion it
     * means (Enter for the first, a click for another). The app suggests every customer whose name or city holds what
     * was typed; the step takes the one whose name starts with it, or the only suggestion. Several fail naming them
     * ("am": Café Lumen in Amsterdam, Fjord Supplies in Hamburg); none fails naming every customer the app offers.
     */
    public static void chooseCustomer(Client ui, String customer) {
        requireNewOrder(ui);
        final Target field = newOrder(ui).field(CUSTOMER_FIELD);
        final String before = shown(field);
        try {
            pickCustomer(ui, field, customer);
        } catch (RuntimeException e) {
            // no customer fits: give the field back what it held (the customer chosen before, picked again)
            quietly(() -> restoreCustomer(ui, field, before));
            throw e;
        }
    }

    /** Types {@code text} into the showing new order's Customer field and picks nothing: the suggestions show. */
    public static void typeCustomer(Client ui, String text) {
        requireNewOrder(ui);
        newOrder(ui).field(CUSTOMER_FIELD).type(text);
    }

    /**
     * Checks the Customer field's suggestions, in order, by the customers' names ({@code "Café Lumen; Fjord Supplies"});
     * {@code none}: "No customer matches".
     */
    public static void customerSuggestionsAre(Client ui, String customers) {
        requireNewOrder(ui);
        List<String> offered = suggestions(ui);
        if (isNone(customers)) {
            if (!offered.isEmpty()) throw new AssertionError("The customer suggestions are " + join(offered) + ", expected none");
            shows(ui.popup().label(NO_CUSTOMER_MATCHES), "The customer suggestions", NO_CUSTOMER_MATCHES);
            return;
        }
        List<String> expected = names(customers);
        boolean same = offered.size() == expected.size();
        for (int i = 0; same && i < offered.size(); i++) same = isSuggestionOf(offered.get(i), expected.get(i));
        if (!same) {
            throw new AssertionError("The customer suggestions are " + (offered.isEmpty() ? "none" : join(offered))
                    + ", expected " + join(expected));
        }
    }

    /**
     * Checks whose new order is showing, by its tab's title ("New order · Ada Shop Ltd"): the customer's whole name, any
     * case (only the shown customer is known here, so a start of a name could mean another one); {@code none}: no
     * customer chosen yet (the tab reads "New order").
     */
    public static void newOrderIsFor(Client ui, String customer) {
        requireNewOrder(ui);
        String title = selected(ui.tab(ORDERS_TAB));
        String prefix = NEW_ORDER_TAB + TITLE_SEPARATOR;
        String chosen = title != null && title.startsWith(prefix) ? title.substring(prefix.length()) : null;
        if (isNone(customer)) {
            if (chosen != null) throw new AssertionError("The new order is for " + chosen + ", expected no customer yet");
            return;
        }
        if (chosen == null) {
            throw new AssertionError("The new order has no customer yet (its tab reads " + quoted(String.valueOf(title))
                    + "), expected " + customer);
        }
        String wanted = customer.trim();
        if (!chosen.equalsIgnoreCase(wanted)) {
            boolean start = chosen.toLowerCase(Locale.ROOT).startsWith(wanted.toLowerCase(Locale.ROOT));
            throw new AssertionError("The new order is for " + chosen + ", expected " + wanted
                    + (start ? " (only the start of its name: give the whole name)" : ""));
        }
    }

    /** Checks the customer's terms beside the Customer field ("Card only"); {@code none}: no customer, no terms. */
    public static void customerTermsAre(Client ui, String terms) {
        requireNewOrder(ui);
        Target card = newOrder(ui).byId(CUSTOMER_TERMS_ID);
        if (isNone(terms)) notShown(card, "The customer's terms");
        else shows(card, "The customer's terms", terms);
    }

    /**
     * Checks the customers the toolbar's ▾ (New order for a recent customer) offers, newest first: opens its menu,
     * reads it and closes it again with Escape.
     */
    public static void recentCustomersAre(final Client ui, String customers) {
        Target opener = ui.button(RECENT_CUSTOMER_BUTTON);
        if (!showing(opener)) {
            throw new DriverException(Reason.NOT_FOUND, "The toolbar's New order for a recent customer (▾) is not showing");
        }
        opener.click();
        List<String> offered;
        try {
            offered = look(() -> {
                List<String> items = ui.popup().menuitem(null).texts();
                return items.isEmpty() ? null : items;
            });
        } finally {
            ui.press("Escape");
        }
        namesAre(offered == null ? Collections.<String>emptyList() : offered, names(customers), "The recent customers are");
    }

    // ---- products

    /**
     * Adds one of {@code product} (its name or the start of it) to the cart: selects its row, then Add to cart. Again
     * adds one more to its line. A product Add to cart refuses (out of stock) fails with the app's reason.
     */
    public static void addToCart(Client ui, String product) {
        add(ui, product);
    }

    /** Adds {@code product} to the cart, then sets its line's quantity to {@code qty}. */
    public static void addQtyToCart(Client ui, String product, int qty) {
        String name = add(ui, product);
        typeQuantity(ui, name, qty);
    }

    /**
     * Adds several products at once ({@code "Stapler; Gel pen, black"}): selects their rows (the first, then the others
     * with Cmd or Ctrl held; a product named twice counts once), then one Add to cart. The app leaves out-of-stock
     * products out; returns, as the cart shows it afterwards, which were added and which were left out:
     * {@code {"added": [...], "leftOut": [...]}}. All out of stock fails with the app's reason.
     */
    public static Map<String, Object> addProductsToCart(Client ui, String products) {
        requireNewOrder(ui);
        List<Map<String, String>> rows = productRows(ui);
        List<String> wanted = names(products);
        if (wanted.isEmpty()) throw new DriverException(Reason.BAD_INPUT, "Name one or more products, parted by ;");
        Set<String> picked = new LinkedHashSet<String>();
        for (String w : wanted) picked.add(resolveProduct(ui, w, rows));
        List<String> chosen = new ArrayList<String>(picked);
        for (int i = 0; i < chosen.size(); i++) {
            Target sku = productRow(ui, chosen.get(i)).cell(SKU_COLUMN);
            if (i == 0) sku.click();
            else sku.click(null, 0, ADD_TO_SELECTION);
        }
        Map<String, String> before = quantities(cartRows(ui));
        pressAddToCart(ui, join(chosen));
        Map<String, String> after = quantities(cartRows(ui));
        List<String> added = new ArrayList<String>();
        List<String> leftOut = new ArrayList<String>();
        for (String name : chosen) {
            String was = before.get(name);
            String now = after.get(name);
            if (now != null && !now.equals(was)) added.add(name);
            else leftOut.add(name);
        }
        Map<String, Object> made = new LinkedHashMap<String, Object>();
        made.put("added", added);
        made.put("leftOut", leftOut);
        return made;
    }

    /**
     * Shows the products of a category: clicks it in Categories, by its name ("Desk", "All products") or its path
     * ("Office > Desk").
     */
    public static void browseCategory(Client ui, String category) {
        requireNewOrder(ui);
        Target tree = newOrder(ui).tree(null);
        String path = pickCategory(category, tree.items());
        tree.item(path.split(CATEGORY_PATH_SEPARATOR)).click();
    }

    /** Checks the category selected in Categories ("Desk", or its path "Office > Desk"). */
    public static void selectedCategoryIs(Client ui, String category) {
        requireNewOrder(ui);
        String actual = selected(newOrder(ui).tree(null));
        if (actual == null || !lastSegment(actual).equalsIgnoreCase(lastSegment(category))) {
            throw new AssertionError("The selected category is " + (actual == null ? "none" : actual) + ", expected " + category);
        }
    }

    /** Types {@code text} into the products' Search: the list shows the products whose SKU or name holds it. */
    public static void searchProducts(Client ui, String text) {
        requireNewOrder(ui);
        newOrder(ui).field(SEARCH_FIELD).type(text);
    }

    /** Empties the products' Search with its X; nothing to do when it is empty. */
    public static void clearProductSearch(Client ui) {
        requireNewOrder(ui);
        Target search = newOrder(ui).field(SEARCH_FIELD);
        String text = shown(search);
        if (text != null && !text.isEmpty()) search.clearValue();
    }

    /** Selects the category of {@code product} in Categories, through its row's menu ▸ Show in category. */
    public static void showInCategory(Client ui, String product) {
        requireNewOrder(ui);
        String name = resolveProduct(ui, product, productRows(ui));
        productRow(ui, name).cell(SKU_COLUMN).rightClick(SHOW_IN_CATEGORY_ITEM);
    }

    /**
     * Checks which products the list shows, in order ({@code "Stapler; Desk lamp"}); {@code none}: nothing fits the
     * search ("No products match ‘zzz’").
     */
    public static void productsListedAre(Client ui, String products) {
        requireNewOrder(ui);
        List<String> listed = column(productRows(ui), PRODUCT_COLUMN);
        if (isNone(products)) {
            if (!listed.isEmpty()) {
                throw new AssertionError("The products list shows " + listed.size() + " products: " + join(listed) + ", expected none");
            }
            if (!showing(newOrder(ui).label(NO_PRODUCTS_MATCH).contains())) {
                throw new AssertionError("The products list shows neither products nor " + quoted(NO_PRODUCTS_MATCH + " …"));
            }
            return;
        }
        namesAre(listed, names(products), "The products listed are");
    }

    /**
     * Checks one value of a listed product: its SKU, Price or Stock ("Out of stock" at 0). The product is given by its
     * name, the start of it, or its SKU ("DSK-STAPLER", any case), as the palette's Check menu names a product's row.
     */
    public static void productIs(Client ui, String product, String column, String value) {
        requireNewOrder(ui);
        List<Map<String, String>> rows = productRows(ui);
        String name = bySku(product, rows);
        if (name == null) name = resolveProduct(ui, product, rows);
        // the Product column names the row: the Check menu offers its check too
        String col = PRODUCT_COLUMN.equalsIgnoreCase(column.trim()) ? PRODUCT_COLUMN
                : pick(column, PRODUCT_CHECK_COLUMNS, "column", "columns", "The products' columns");
        shows(productCell(ui, rows, name, col), name + "'s " + col, value);
    }

    /** Checks the low-stock warning a product's Stock shows on hover ("Low stock: 4 left"); {@code none}: no warning. */
    public static void lowStockWarningIs(Client ui, String product, String warning) {
        requireNewOrder(ui);
        List<Map<String, String>> rows = productRows(ui);
        String name = resolveProduct(ui, product, rows);
        Target stock = productCell(ui, rows, name, STOCK_COLUMN);
        boolean none = isNone(warning);
        try {
            assertThat(stock).within(LOOK).hasTooltip(none ? "" : warning);
        } catch (UiAssertionError e) {
            String actual = stock.tooltip();
            throw new AssertionError(name + "'s stock " + (actual == null || actual.isEmpty() ? "shows no warning"
                    : "warns " + quoted(actual)) + ", expected " + (none ? "no warning" : quoted(warning)));
        }
    }

    /** Checks the products count under the list ("22 products · 2 selected"). */
    public static void productCountIs(Client ui, String count) {
        requireNewOrder(ui);
        shows(newOrder(ui).byId(PRODUCT_COUNT_ID), "The product count", count);
    }

    // ---- the cart

    /** Sets the quantity of a cart line (the product's name or the start of it): its Qty, then Enter. */
    public static void setQuantity(Client ui, String product, int qty) {
        requireNewOrder(ui);
        typeQuantity(ui, resolveCartLine(ui, product), qty);
    }

    /**
     * Removes cart lines ({@code "Stapler"} or {@code "Stapler; Gel pen"}): selects them, then Remove; for several the
     * app asks "Remove 2 lines from the cart?", answered Remove lines.
     */
    public static void removeFromCart(Client ui, String products) {
        List<String> lines = selectCartLines(ui, products);
        newOrder(ui).button(REMOVE_BUTTON).click();
        if (lines.size() > 1) ui.answer(REMOVE_LINES_ANSWER);
    }

    /** Asks to remove two or more cart lines, then answers Keep: the cart stays as it was. */
    public static void keepCartLines(Client ui, String products) {
        if (names(products).size() < 2) throw new DriverException(Reason.BAD_INPUT, ONE_LINE_ASKS_NOTHING);
        List<String> lines = selectCartLines(ui, products);
        if (lines.size() < 2) throw new DriverException(Reason.BAD_INPUT, ONE_LINE_ASKS_NOTHING);
        newOrder(ui).button(REMOVE_BUTTON).click();
        ui.answer(KEEP_ANSWER);
    }

    /** Checks the cart's lines, in the order they were added ({@code "Stapler; Gel pen, black"}); {@code none}: empty. */
    public static void cartLinesAre(Client ui, String lines) {
        requireNewOrder(ui);
        List<String> actual = cartLines(ui);
        if (isNone(lines)) {
            if (!actual.isEmpty()) throw new AssertionError("The cart holds " + join(actual) + ", expected an empty cart");
            shows(newOrder(ui).label(EMPTY_CART), "The empty cart's text", EMPTY_CART);
            return;
        }
        namesAre(actual, names(lines), "The cart holds");
    }

    /** Checks one value of a cart line: its Qty, Price or Line total. */
    public static void cartLineIs(Client ui, String product, String column, String value) {
        requireNewOrder(ui);
        List<Map<String, String>> rows = cartRows(ui);
        String name = pick(product, column(rows, PRODUCT_COLUMN), "cart line", "cart lines", "The cart holds");
        // the Product column names the line: the Check menu offers its check too
        String col = column.trim().toLowerCase(Locale.ROOT).startsWith("quant") ? QTY_COLUMN
                : PRODUCT_COLUMN.equalsIgnoreCase(column.trim()) ? PRODUCT_COLUMN
                : pick(column, CART_CHECK_COLUMNS, "column", "columns", "The cart's columns");
        Map<String, Object> row = Rows.row(PRODUCT_COLUMN, name);
        // Price and Product are products columns too: the line's own Line total tells the cart's row apart
        if (PRICE_COLUMN.equals(col) || PRODUCT_COLUMN.equals(col)) row = Rows.row(PRODUCT_COLUMN, name, LINE_TOTAL_COLUMN, valueOf(rows, name, LINE_TOTAL_COLUMN));
        shows(newOrder(ui).table().row(row).cell(col), name + "'s " + col + " in the cart", value);
    }

    /**
     * Checks one of the cart's amounts: Subtotal, Volume discount (shown only for a line of 10 or more), Shipping
     * ("chosen at checkout"), VAT (20%) or Total; {@code none}: not shown.
     */
    public static void cartAmountIs(Client ui, String amount, String value) {
        requireNewOrder(ui);
        String caption = pick(amount, CART_AMOUNTS, "amount", "amounts", "The cart's amounts");
        Target label = newOrder(ui).label(caption);
        if (isNone(value)) notShown(label, "The cart's " + caption);
        else shows(label, "The cart's " + caption, value);
    }

    /** Checks the cart total ("€31.32"). */
    public static void cartTotalIs(Client ui, String total) {
        requireNewOrder(ui);
        shows(newOrder(ui).label(TOTAL_LABEL), "The cart total", total);
    }

    /** Checks the warning under the cart while a line asks for more than is in stock; {@code none}: no warning. */
    public static void cartStockWarningIs(Client ui, String warning) {
        requireNewOrder(ui);
        Target label = newOrder(ui).label(IN_STOCK_WARNING).contains();
        if (isNone(warning)) notShown(label, "The cart's stock warning");
        else shows(label, "The cart's stock warning", warning);
    }

    /**
     * Checks that Add to cart, Remove or Checkout… is disabled, with the reason beside it ("The cart is empty"); or, in
     * the checkout, Next, Express or Invoice ({@link OrderDeskCheckoutSteps}); or, on an order's own tab, Cancel order
     * ("Only placed orders can be cancelled — this one is shipped", {@link OrderDeskOrderTabSteps}).
     */
    public static void buttonIsDisabled(Client ui, String button, String reason) {
        String control = OrderDeskCheckoutSteps.controlName(button);
        if (control != null) {
            OrderDeskCheckoutSteps.controlIsDisabled(ui, control, reason);
            return;
        }
        String onOrder = OrderDeskOrderTabSteps.button(button);
        if (onOrder != null) {
            OrderDeskOrderTabSteps.buttonIsDisabled(ui, onOrder, reason);
            return;
        }
        requireNewOrder(ui);
        String name = pick(button, REASON_BUTTONS, "button", "buttons", REASON_BUTTONS_WHERE);
        try {
            assertThat(newOrder(ui).button(name)).within(LOOK).isDisabled();
        } catch (UiAssertionError e) {
            throw new AssertionError(name + " is enabled, expected disabled: " + quoted(reason));
        }
        Target beside = reasonOf(ui, name);
        if (REMOVE_BUTTON.equals(name) && !SELECT_A_CART_LINE_FIRST.equals(reason)) {
            // Remove's reason has one text and no code name: it is found by that text, so read it to say what shows
            String actual = shown(beside);
            throw new AssertionError(name + "'s reason " + (actual == null ? "is not shown" : "reads " + quoted(actual))
                    + ", expected " + quoted(reason));
        }
        shows(beside, name + "'s reason", reason);
    }

    /**
     * Checks that Add to cart, Remove or Checkout… (in the checkout: Next, Express or Invoice; on an order's own tab:
     * Cancel order or Save receipt…) is enabled; when not, the failure gives its reason.
     */
    public static void buttonIsEnabled(Client ui, String button) {
        String control = OrderDeskCheckoutSteps.controlName(button);
        if (control != null) {
            OrderDeskCheckoutSteps.controlIsEnabled(ui, control);
            return;
        }
        String onOrder = OrderDeskOrderTabSteps.button(button);
        if (onOrder != null) {
            OrderDeskOrderTabSteps.buttonIsEnabled(ui, onOrder);
            return;
        }
        requireNewOrder(ui);
        String name = pick(button, REASON_BUTTONS, "button", "buttons", REASON_BUTTONS_WHERE);
        try {
            assertThat(newOrder(ui).button(name)).within(LOOK).isEnabled();
        } catch (UiAssertionError e) {
            String reason = shown(reasonOf(ui, name));
            throw new AssertionError(name + " is disabled" + (reason == null ? "" : " (" + quoted(reason) + ")") + ", expected enabled");
        }
    }

    // ---- what the choices read (OrderDeskVocabulary)

    /** The products the showing new order lists, e.g. "Gel pen, black"; empty when nothing fits its search. */
    static List<String> listedProducts(Client ui) {
        return column(productRows(ui), PRODUCT_COLUMN);
    }

    /** The products in the showing new order's cart, in order. */
    static List<String> cartLines(Client ui) {
        return column(cartRows(ui), PRODUCT_COLUMN);
    }

    /** The categories, as the tree names them: "All products", "Office", "Office > Paper", ... */
    static List<String> categories(Client ui) {
        return newOrder(ui).tree(null).items();
    }

    // ---- inside

    /** Fails fast, in words, when no new order is showing (none open, or another tab selected). */
    static void requireNewOrder(Client ui) {
        if (!showing(newOrder(ui).field(CUSTOMER_FIELD))) throw new DriverException(Reason.NOT_FOUND, NO_NEW_ORDER);
    }

    /**
     * Types {@code customer} and takes the suggestion it means. A miss fails with the suggestion list closed again (for
     * none: the field emptied, as {@link #everyCustomer} leaves it; for several: Escape); the caller puts back the rest.
     */
    private static void pickCustomer(final Client ui, Target field, String customer) {
        field.type(customer);
        List<String> offered = suggestions(ui);
        if (offered.isEmpty()) {
            throw new DriverException(Reason.NOT_FOUND, "No customer fits " + quoted(customer) + ". Order Desk offers: "
                    + join(everyCustomer(ui, field)));
        }
        int at;
        try {
            at = suggestionFor(customer, offered);
        } catch (RuntimeException e) {
            quietly(() -> ui.press("Escape"));
            throw e;
        }
        if (at == 0) ui.press("Enter");
        else ui.popup().list(null).item(offered.get(at)).click();
    }

    /** The Customer field as it was before a miss: empty, or {@code before} typed again and picked when it is a customer. */
    private static void restoreCustomer(Client ui, Target field, String before) {
        if (before == null || before.isEmpty()) {
            String now = shown(field);
            if (now != null && !now.isEmpty()) field.clearValue();
            return;
        }
        field.type(before);
        List<String> offered = suggestions(ui);
        int at = -1;
        for (int i = 0; i < offered.size(); i++) {
            if (isSuggestionOf(offered.get(i), before)) at = at == -1 ? i : -2;
        }
        if (at == 0) ui.press("Enter");
        else if (at > 0) ui.popup().list(null).item(offered.get(at)).click();
        else if (!offered.isEmpty()) ui.press("Escape");
    }


    /**
     * The suggestion a tester means by {@code customer}: the only one whose text (name, city, terms) starts with it,
     * else the only one that starts with it as a whole word, else the only suggestion there is.
     */
    private static int suggestionFor(String customer, List<String> offered) {
        String w = customer.trim().toLowerCase(Locale.ROOT);
        List<Integer> starting = new ArrayList<Integer>();
        List<Integer> word = new ArrayList<Integer>();
        for (int i = 0; i < offered.size(); i++) {
            String item = offered.get(i).toLowerCase(Locale.ROOT);
            if (item.startsWith(w)) starting.add(i);
            if (item.startsWith(w + " ")) word.add(i);
        }
        if (starting.size() == 1) return starting.get(0);
        if (word.size() == 1) return word.get(0);
        if (starting.isEmpty() && offered.size() == 1) return 0;
        List<String> fit = new ArrayList<String>();
        if (starting.isEmpty()) fit.addAll(offered);
        else for (int i : starting) fit.add(offered.get(i));
        throw new DriverException(Reason.AMBIGUOUS, quoted(customer) + " fits " + fit.size() + " customers: " + join(fit)
                + ". Give more of the name");
    }

    private static boolean isSuggestionOf(String suggestion, String name) {
        return suggestion.equals(name) || suggestion.startsWith(name + " ");
    }

    /** The Customer field's suggestions now ("Café Lumen Amsterdam · Card only"); empty for "No customer matches". */
    private static List<String> suggestions(final Client ui) {
        List<String> items = look(() -> {
            Target list = ui.popup().list(null);
            if (list.presence() == Target.Presence.ONE) return list.items();
            if (ui.popup().label(NO_CUSTOMER_MATCHES).presence() != Target.Presence.NONE) return Collections.<String>emptyList();
            return null;
        });
        return items == null ? Collections.<String>emptyList() : items;
    }

    /** Every customer the Customer field offers: emptied, Down opens all suggestions; Escape closes them again. */
    private static List<String> everyCustomer(final Client ui, Target field) {
        field.clearValue();
        field.press("Down");
        List<String> all = look(() -> {
            Target list = ui.popup().list(null);
            return list.presence() == Target.Presence.ONE ? list.items() : null;
        });
        ui.press("Escape");
        return all == null ? Collections.<String>emptyList() : all;
    }

    private static String add(Client ui, String product) {
        requireNewOrder(ui);
        String name = resolveProduct(ui, product, productRows(ui));
        productRow(ui, name).cell(SKU_COLUMN).click();
        pressAddToCart(ui, name);
        return name;
    }

    /** Add to cart, unless it is disabled: then its reason, as the step's failure. */
    private static void pressAddToCart(Client ui, String what) {
        Target add = newOrder(ui).button(ADD_TO_CART_BUTTON);
        if (add.states().contains("disabled")) {
            String reason = shown(newOrder(ui).byId(ADD_TO_CART_REASON_ID));
            throw new DriverException(Reason.NOT_ACTIONABLE, what + " cannot be added to the cart: "
                    + (reason == null ? "Add to cart is disabled" : reason));
        }
        add.click();
    }

    private static void typeQuantity(Client ui, String line, int qty) {
        newOrder(ui).table().row(PRODUCT_COLUMN, line).cell(QTY_COLUMN).type(String.valueOf(qty), null, Boolean.TRUE);
    }

    /** Selects the cart lines {@code products} names: the first by a click, the others with Cmd or Ctrl held. */
    private static List<String> selectCartLines(Client ui, String products) {
        requireNewOrder(ui);
        List<String> held = cartLines(ui);
        List<String> wanted = names(products);
        if (wanted.isEmpty()) throw new DriverException(Reason.BAD_INPUT, "Name one or more cart lines, parted by ;");
        Set<String> picked = new LinkedHashSet<String>();   // a line named twice counts once: a second click would unselect it
        for (String w : wanted) picked.add(pick(w, held, "cart line", "cart lines", "The cart holds"));
        List<String> lines = new ArrayList<String>(picked);
        for (int i = 0; i < lines.size(); i++) {
            Target total = newOrder(ui).table().row(PRODUCT_COLUMN, lines.get(i)).cell(LINE_TOTAL_COLUMN);
            if (i == 0) total.click();
            else total.click(null, 0, ADD_TO_SELECTION);
        }
        return lines;
    }

    private static String resolveCartLine(Client ui, String product) {
        return pick(product, cartLines(ui), "cart line", "cart lines", "The cart holds");
    }

    /** {@link OrderDeskLooks#pick} among the listed products; a miss also says what narrows the list. */
    private static String resolveProduct(Client ui, String product, List<Map<String, String>> rows) {
        try {
            return pick(product, column(rows, PRODUCT_COLUMN), "product", "products", "The products listed");
        } catch (DriverException e) {
            if (e.reason() != Reason.NOT_FOUND) throw e;
            String why = DriverException.stripReason(e.getMessage(), e.reason());
            String search = shown(newOrder(ui).field(SEARCH_FIELD));
            if (search != null && !search.isEmpty()) {
                why += ". Search holds " + quoted(search) + ": \"Clear the product search\" lists every product";
            }
            String category = shownSelection(newOrder(ui).tree(null));
            if (category != null && !ALL_PRODUCTS_CATEGORY.equals(category)) {
                why += ". The list shows " + category + " only: \"Browse category " + ALL_PRODUCTS_CATEGORY + "\" lists every product";
            }
            throw new DriverException(Reason.NOT_FOUND, why);
        }
    }

    /** The name of the listed product whose SKU is {@code product} (any case); null when none is. */
    private static String bySku(String product, List<Map<String, String>> rows) {
        for (Map<String, String> r : rows) {
            if (product.trim().equalsIgnoreCase(r.get(SKU_COLUMN))) return r.get(PRODUCT_COLUMN);
        }
        return null;
    }

    /** A product's row by its name: the click locator; the SKU column tells the products table from the cart. */
    private static Target productRow(Client ui, String name) {
        return newOrder(ui).table().row(PRODUCT_COLUMN, name);
    }

    /** One cell of a product's row, by its SKU and name: the SKU key keeps the cart's row of the same product out. */
    private static Target productCell(Client ui, List<Map<String, String>> rows, String name, String col) {
        return newOrder(ui).table().row(Rows.row(SKU_COLUMN, valueOf(rows, name, SKU_COLUMN), PRODUCT_COLUMN, name)).cell(col);
    }

    /**
     * The products table's rows (SKU, Product, Price, Stock); empty while "No products match" replaces it. A read
     * gives the first rows only: the rest are read one by one, by row index (SKU), then by SKU (the others).
     */
    private static List<Map<String, String>> productRows(Client ui) {
        Target table = newOrder(ui).table().cell(SKU_COLUMN);
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
            String sku = newOrder(ui).table().rowAt(i).cell(SKU_COLUMN).shownText();
            Map<String, String> row = new LinkedHashMap<String, String>();
            row.put(SKU_COLUMN, sku);
            for (String col : Arrays.asList(PRODUCT_COLUMN, PRICE_COLUMN, STOCK_COLUMN)) {
                row.put(col, shown(newOrder(ui).table().row(SKU_COLUMN, sku).cell(col)));
            }
            rows.add(row);
        }
        return rows;
    }

    /** The cart's rows (Product, Qty, Price, Line total); empty while the cart is empty. */
    private static List<Map<String, String>> cartRows(Client ui) {
        try {
            return newOrder(ui).table().cell(LINE_TOTAL_COLUMN).rows();
        } catch (UiActionFailed e) {
            if (e.reason() == Reason.NOT_FOUND) return new ArrayList<Map<String, String>>();
            throw e;
        }
    }

    /** Each cart line's Qty, by product. */
    private static Map<String, String> quantities(List<Map<String, String>> cart) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map<String, String> row : cart) out.put(row.get(PRODUCT_COLUMN), row.get(QTY_COLUMN));
        return out;
    }

    private static String valueOf(List<Map<String, String>> rows, String name, String col) {
        for (Map<String, String> row : rows) {
            if (name.equals(row.get(PRODUCT_COLUMN))) return row.get(col);
        }
        return null;
    }

    /** The reason beside a button while it is disabled. */
    private static Target reasonOf(Client ui, String button) {
        if (ADD_TO_CART_BUTTON.equals(button)) return newOrder(ui).byId(ADD_TO_CART_REASON_ID);
        if (CHECKOUT_BUTTON.equals(button)) return newOrder(ui).byId(CHECKOUT_REASON_ID);
        return newOrder(ui).label(SELECT_A_CART_LINE_FIRST);
    }

    /** What is selected in {@code target} (a tab's pane: the selected tab's title; a tree: its item), or null. */
    private static String selected(Target target) {
        try {
            return target.timeout(LOOK).selectedText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    /** {@link #selected} without waiting: for a failure's words. */
    private static String shownSelection(Target target) {
        try {
            return target.timeout(java.time.Duration.ofMillis(1)).selectedText();
        } catch (UiActionFailed e) {
            return null;
        } catch (DriverException e) {
            return null;
        }
    }

    /**
     * The category {@code wanted} names among the tree's {@code paths}: equal to a path or a name, any case; else the
     * only name, then the only path, that starts with it. None or several fail naming them.
     */
    private static String pickCategory(String wanted, List<String> paths) {
        String w = normalPath(wanted).toLowerCase(Locale.ROOT);
        List<String> byName = new ArrayList<String>();
        List<String> byPath = new ArrayList<String>();
        for (String p : paths) {
            String lp = p.toLowerCase(Locale.ROOT);
            String name = lastSegment(p).toLowerCase(Locale.ROOT);
            if (lp.equals(w) || name.equals(w)) return p;
            if (name.startsWith(w)) byName.add(p);
            if (lp.startsWith(w)) byPath.add(p);
        }
        List<String> fit = byName.isEmpty() ? byPath : byName;
        if (fit.size() == 1) return fit.get(0);
        if (fit.size() > 1) {
            throw new DriverException(Reason.AMBIGUOUS, quoted(wanted) + " fits " + fit.size() + " categories: " + join(fit)
                    + ". Give more of the name");
        }
        throw new DriverException(Reason.NOT_FOUND, "No category fits " + quoted(wanted) + ". The categories: " + join(paths));
    }

    /** "Office ▸ Desk", "office>desk" as the tree writes it: "office > desk". */
    private static String normalPath(String path) {
        return path.trim().replace('▸', '>').replaceAll("\\s*>\\s*", CATEGORY_PATH_SEPARATOR);
    }

    private static String lastSegment(String path) {
        String p = normalPath(path);
        int at = p.lastIndexOf(CATEGORY_PATH_SEPARATOR);
        return at < 0 ? p : p.substring(at + CATEGORY_PATH_SEPARATOR.length());
    }
}
