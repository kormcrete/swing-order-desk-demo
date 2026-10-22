package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;

/**
 * Order Desk's domain steps as Java: the one place that knows how to add a product to the cart, check out or place
 * an order. The plugin's vocabulary ({@link OrderDeskVocabulary}) runs these methods for a recorded test's {@code do}
 * steps, and a Java UI test calls them directly ({@code OrderDeskSteps.addToCart(ui, "Gel pen, black")}).
 *
 * <p>Each name the app shows (a button, a column, a tab) is written once, in the constants below: the methods use them,
 * and so do the vocabulary's patterns that recognise a person's recorded clicks. When the app renames something, say
 * the "Add to cart" button, change its constant and both kinds of test pass again, and a person's clicks still fold.
 *
 * <p>The steps of a new order's tab are in {@link OrderDeskNewOrderSteps}, the checkout's in
 * {@link OrderDeskCheckoutSteps}; the ones here that act there (open a new order, add to the cart, set a quantity, the
 * cart total, check out with a PO number, place the order) call them, and keep their names for the Java tests and the
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

    // ---- the checkout (OrderDeskCheckoutSteps)

    /** The checkout dialog's title before the customer's name: "Checkout — Ada Shop Ltd". */
    static final String CHECKOUT_TITLE = "Checkout — ";
    /** Each checkout step's heading, by its code name (none on the Review step). */
    static final String CHECKOUT_STEP_ID = "checkoutStep";
    static final String ADDRESS_HEADING = "Delivery address";
    static final String SHIPPING_HEADING = "Shipping method";
    static final String PAYMENT_HEADING = "Payment";
    /** The step indicator's words for the four steps. */
    static final String ADDRESS_STEP = "Address";
    static final String SHIPPING_STEP = "Shipping";
    static final String PAYMENT_STEP = "Payment";
    static final String REVIEW_STEP = "Review";
    /** Next's reason while it is disabled, by its code name. */
    static final String NEXT_REASON_ID = "nextReason";
    static final String BACK_BUTTON = "Back";
    static final String CANCEL_BUTTON = "Cancel";
    /** Each review section's Edit. */
    static final String EDIT_BUTTON = "Edit";
    /** The Address step's fields, by caption. */
    static final String NAME_FIELD = "Name";
    static final String STREET_FIELD = "Street";
    static final String POSTAL_CODE_FIELD = "Postal code";
    static final String CITY_FIELD = "City";
    static final String COUNTRY_COMBO = "Country";
    static final String PHONE_FIELD = "Phone (optional)";
    /** The Shipping step: its two choices, their prices by code name, Express's reason, the note. */
    static final String STANDARD_RADIO = "Standard · 3–5 business days";
    static final String EXPRESS_RADIO = "Express · next business day";
    static final String STANDARD_PRICE_ID = "standardPrice";
    static final String EXPRESS_PRICE_ID = "expressPrice";
    static final String EXPRESS_REASON = "Express delivers to Germany and Austria only";
    static final String DELIVERY_NOTE_FIELD = "Delivery note (optional)";
    /** The Payment step: Invoice and its note (code name), Card and its fixed hint, the fields. */
    static final String INVOICE_RADIO = "Invoice (30 days)";
    static final String INVOICE_NOTE_ID = "invoiceNote";
    static final String CARD_RADIO = "Card";
    static final String CARD_HINT = "Debit or credit card";
    /** The PO number's caption for a customer that does not need one (those that do see {@link #PO_NUMBER_FIELD}). */
    static final String PO_NUMBER_OPTIONAL_FIELD = "PO number (optional)";
    static final String CARD_NUMBER_FIELD = "Card number";
    static final String EXPIRY_FIELD = "Expiry";
    static final String NAME_ON_CARD_FIELD = "Name on card";
    /** The question after Next on an invoice over the credit limit, and its answers. */
    static final String CREDIT_LIMIT_TITLE = "Credit limit";
    static final String PAY_BY_CARD_ANSWER = "Pay by card instead";
    static final String CONTINUE_WITH_INVOICE_ANSWER = "Continue with invoice";
    /** The confirmation's title (also the title of "The order could not be placed."), and its other answer. */
    static final String PLACE_ORDER_TITLE = "Place order";
    static final String CANCEL_ANSWER = "Cancel";
    /** While the order is placed: "Placing order…", then "Order for €37.20 · Ada Shop Ltd". */
    static final String PLACING_ORDER = "Placing order…";
    static final String PLACING_FOR = "Order for ";
    static final String PLACING_FAILED = "The order could not be placed.";
    /** The answer to "The order could not be placed.": back on the Review step. */
    static final String OK_ANSWER = "OK";
    /** The review's totals panel, by its code name: its Shipping is told apart from the step indicator's. */
    static final String TOTALS_ID = "totals";
    /** The review's sections. */
    static final String DELIVERY_ADDRESS_SECTION = "Delivery address";
    static final String SHIPPING_SECTION = "Shipping";
    static final String PAYMENT_SECTION = "Payment";
    /** The status line, by its code name: "Order ORD-1640 placed". */
    static final String STATUS_MESSAGE_ID = "statusMessage";

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
     * Checks out the open new order and pays on invoice with a PO number, as {@link OrderDeskCheckoutSteps#checkoutWithPo}
     * does: Checkout…, Next past the address and the shipping, {@code po} into PO number, Next. Ends on the Review step,
     * with Place order to press; a step that blocks fails with the app's reason, the checkout cancelled.
     */
    public static void checkoutWithPo(Client ui, String po) {
        OrderDeskCheckoutSteps.checkoutWithPo(ui, po);
    }

    /**
     * Places the order on the checkout's Review step and confirms it; returns the new order's number, e.g.
     * {@code ORD-1640}, once its tab is open ({@link OrderDeskCheckoutSteps#placeOrder}).
     */
    public static String placeOrder(Client ui) {
        return OrderDeskCheckoutSteps.placeOrder(ui);
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
