package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;

import java.time.Duration;

import static io.github.kormcrete.swingui.junit.UiAssertions.eventually;

/**
 * Order Desk's domain steps as Java: the one place that knows how to add a product to the cart, check out or place
 * an order. The plugin's vocabulary ({@link OrderDeskVocabulary}) runs these methods for a recorded test's {@code do}
 * steps, and a Java UI test calls them directly ({@code OrderDeskSteps.addToCart(ui, "Gel pen, black")}).
 *
 * <p>Each name the app shows (a button, a column, a tab) is written once, in the constants below: the methods use them,
 * and so do the vocabulary's patterns that recognise a person's recorded clicks. When the app renames something, say
 * the "Add to cart" button, change its constant and both kinds of test pass again, and a person's clicks still fold.
 *
 * <p>The steps of a new order's tab are in {@link OrderDeskNewOrderSteps}; the ones here that act there (open a new
 * order, add to the cart, set a quantity, the cart total) call it, and keep their names for the Java tests and the
 * recorded tests that use them.
 *
 * <p>Client calls only: no app classes, so the plugin needs nothing from the app's build.
 */
public final class OrderDeskSteps {

    // ---- the names the app shows

    static final String FILE_MENU = "File";
    static final String NEW_ORDER_ITEM = "New order";
    /** File ▸ Close tab: closes the selected tab (an empty new order without a question). */
    static final String CLOSE_TAB_ITEM = "Close tab";
    /** The toolbar button that opens a new order. */
    static final String NEW_ORDER_BUTTON = "New order";
    /** The toolbar button that opens a new order for a customer picked from its menu. */
    static final String RECENT_CUSTOMER_BUTTON = "New order for a recent customer";
    /** A new order's tab title until its customer is chosen; then "New order · Ada Shop Ltd". */
    static final String NEW_ORDER_TAB = "New order";
    static final String CUSTOMER_FIELD = "Customer";
    static final String PRODUCT_COLUMN = "Product";
    /** Only the products table has it: the cart table lists a product too once it is in the cart. */
    static final String SKU_COLUMN = "SKU";
    static final String QTY_COLUMN = "Qty";
    static final String ADD_TO_CART_BUTTON = "Add to cart";
    /** The products table's context menu item. */
    static final String ADD_TO_CART_ITEM = "Add to cart";
    static final String CHECKOUT_BUTTON = "Checkout…";
    static final String NEXT_BUTTON = "Next";
    static final String PO_NUMBER_FIELD = "PO number";
    static final String PLACE_ORDER_BUTTON = "Place order";
    /** The confirmation's button. */
    static final String PLACE_ORDER_ANSWER = "Place order";
    static final String ORDERS_TAB = "Orders";
    static final String ORDER_COLUMN = "Order";
    static final String CUSTOMER_COLUMN = "Customer";
    /** The caption of the cart's total. */
    static final String TOTAL_LABEL = "Total";

    // ---- a new order's tab (OrderDeskNewOrderSteps)

    /** The Customer field's suggestion when nothing fits what was typed. */
    static final String NO_CUSTOMER_MATCHES = "No customer matches";
    /** The terms card beside the Customer field, by its code name. */
    static final String CUSTOMER_TERMS_ID = "customerTerms";
    /** The products' Search field (the Orders tab has one too, outside a new order). */
    static final String SEARCH_FIELD = "Search";
    static final String PRICE_COLUMN = "Price";
    static final String STOCK_COLUMN = "Stock";
    /** Only the cart table has it. */
    static final String LINE_TOTAL_COLUMN = "Line total";
    /** The products count under the products, by its code name: "22 products · 2 selected". */
    static final String PRODUCT_COUNT_ID = "productCount";
    /** The text that replaces the products table when nothing fits the search: "No products match ‘zzz’". */
    static final String NO_PRODUCTS_MATCH = "No products match";
    /** Add to cart's reason while it is disabled, by its code name. */
    static final String ADD_TO_CART_REASON_ID = "addToCartReason";
    /** The products table's context menu item. */
    static final String SHOW_IN_CATEGORY_ITEM = "Show in category";
    /** The category every product is in, first in the tree. */
    static final String ALL_PRODUCTS_CATEGORY = "All products";
    /** How the tree's read names a category under another: "Office > Desk". */
    static final String CATEGORY_PATH_SEPARATOR = " > ";
    /** The cart's text while it is empty. */
    static final String EMPTY_CART = "Select products and press Add to cart.";
    /** Under the cart while a line asks for more than is in stock: "Only 4 Staplers in stock". */
    static final String IN_STOCK_WARNING = "in stock";
    static final String REMOVE_BUTTON = "Remove";
    /** Remove's reason while lines are in the cart and none is selected (fixed text, no code name). */
    static final String SELECT_A_CART_LINE_FIRST = "Select a cart line first";
    /** The answers to "Remove 2 lines from the cart?". */
    static final String REMOVE_LINES_ANSWER = "Remove lines";
    static final String KEEP_ANSWER = "Keep";
    /** Checkout…'s reason while it is disabled, by its code name. */
    static final String CHECKOUT_REASON_ID = "checkoutReason";
    /** The captions of the cart's amounts, as they show under the cart. */
    static final String SUBTOTAL_LABEL = "Subtotal";
    static final String VOLUME_DISCOUNT_LABEL = "Volume discount";
    static final String SHIPPING_LABEL = "Shipping";
    static final String VAT_LABEL = "VAT (20%)";
    /** What a new order's title puts between "New order" and the customer: "New order · Ada Shop Ltd". */
    static final String TITLE_SEPARATOR = " · ";
    /** The key a multi-select click holds: Cmd on a Mac, Ctrl elsewhere. */
    static final String ADD_TO_SELECTION = "Mod";

    private OrderDeskSteps() {
    }

    /**
     * Opens a new order and picks its customer: File ▸ New order, then the customer as
     * {@link OrderDeskNewOrderSteps#openNewOrder} picks it (its name, or the start of it, e.g. "Ada"). The tab is then
     * titled "New order · Ada Shop Ltd".
     */
    public static void openNewOrder(Client ui, String customer) {
        OrderDeskNewOrderSteps.openNewOrder(ui, customer);
    }

    /** Adds one of {@code product} (its name or the start of it) to the cart of the open new order. */
    public static void addToCart(Client ui, String product) {
        OrderDeskNewOrderSteps.addToCart(ui, product);
    }

    /** Sets the quantity of {@code product}, already in the cart, to {@code qty}. */
    public static void setQuantity(Client ui, String product, int qty) {
        OrderDeskNewOrderSteps.setQuantity(ui, product, qty);
    }

    /** Adds {@code product} to the cart and sets its quantity to {@code qty}. */
    public static void addQtyToCart(Client ui, String product, int qty) {
        OrderDeskNewOrderSteps.addQtyToCart(ui, product, qty);
    }

    /**
     * Checks out the open new order and pays by PO: Checkout…, Next past the address and the shipping, {@code po} into
     * PO number, Next. Ends on the Review step, with Place order to press.
     */
    public static void checkoutWithPo(Client ui, String po) {
        newOrder(ui).button(CHECKOUT_BUTTON).click();
        ui.dialog().button(NEXT_BUTTON).click();
        ui.dialog().button(NEXT_BUTTON).click();
        ui.dialog().field(PO_NUMBER_FIELD).type(po);
        ui.dialog().button(NEXT_BUTTON).click();
    }

    /**
     * Places the order on the checkout's Review step and confirms it; returns the new order's number, e.g.
     * {@code ORD-1640}, once its tab is open.
     */
    public static String placeOrder(Client ui) {
        ui.dialog().button(PLACE_ORDER_BUTTON).click();
        ui.answer(PLACE_ORDER_ANSWER);
        // the status bar names the new order; a rerun against a running app still shows the last one's until this one
        // replaces it, so wait for the number whose tab is selected
        return eventually("the status bar naming the new order", Duration.ofSeconds(8), () -> {
            String placed = ui.main().label("placed").contains().text();   // "Order ORD-1640 placed"
            String number = placed.substring("Order ".length(), placed.indexOf(" placed"));
            return ui.tab(number).states().contains("selected") ? number : null;
        }, number -> number != null);
    }

    /** Opens the order {@code order} (e.g. {@code ORD-1640}) from the Orders list in a tab of its own. */
    public static void openOrder(Client ui, String order) {
        ui.tab(ORDERS_TAB).click();
        ui.tab(ORDERS_TAB).table().row(ORDER_COLUMN, order).doubleClick();
    }

    /** Checks the cart total of the open new order, e.g. {@code €31.32}. */
    public static void cartTotalIs(Client ui, String total) {
        OrderDeskNewOrderSteps.cartTotalIs(ui, total);
    }

    /** The open new order's tab: "New order", or "New order · Ada Shop Ltd" once its customer is chosen. */
    static Target newOrder(Client ui) {
        return ui.tab(NEW_ORDER_TAB + "*");
    }
}
