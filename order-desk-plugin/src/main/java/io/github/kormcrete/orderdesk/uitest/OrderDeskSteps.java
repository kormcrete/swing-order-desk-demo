package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;

import java.time.Duration;

import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;
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
 * <p>Client calls only: no app classes, so the plugin needs nothing from the app's build.
 */
public final class OrderDeskSteps {

    // ---- the names the app shows

    static final String FILE_MENU = "File";
    static final String NEW_ORDER_ITEM = "New order";
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

    private OrderDeskSteps() {
    }

    /**
     * Opens a new order and picks its customer: File ▸ New order, the customer's name (or its start, e.g. "Ada") into
     * Customer, Enter. The tab is then titled "New order · Ada Shop Ltd".
     */
    public static void openNewOrder(Client ui, String customer) {
        ui.menu(FILE_MENU, NEW_ORDER_ITEM);
        ui.tab(NEW_ORDER_TAB).field(CUSTOMER_FIELD).type(customer);
        ui.press("Enter");
    }

    /** Adds one of {@code product} to the cart of the open new order: selects the product, then Add to cart. */
    public static void addToCart(Client ui, String product) {
        // the SKU cell: the cart table lists the product too once it is in the cart, but has no SKU column
        newOrder(ui).table().row(PRODUCT_COLUMN, product).cell(SKU_COLUMN).click();
        newOrder(ui).button(ADD_TO_CART_BUTTON).click();
    }

    /** Sets the quantity of {@code product}, already in the cart, to {@code qty}. */
    public static void setQuantity(Client ui, String product, int qty) {
        newOrder(ui).table().row(PRODUCT_COLUMN, product).cell(QTY_COLUMN).type(String.valueOf(qty), null, Boolean.TRUE);
    }

    /** Adds {@code product} to the cart and sets its quantity to {@code qty}. */
    public static void addQtyToCart(Client ui, String product, int qty) {
        addToCart(ui, product);
        setQuantity(ui, product, qty);
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
        assertThat(newOrder(ui).label(TOTAL_LABEL)).hasText(total);
    }

    /** The open new order's tab: "New order", or "New order · Ada Shop Ltd" once its customer is chosen. */
    private static Target newOrder(Client ui) {
        return ui.tab(NEW_ORDER_TAB + "*");
    }
}
