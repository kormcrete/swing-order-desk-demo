package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;

import java.time.Duration;

import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;
import static io.github.kormcrete.swingui.junit.UiAssertions.eventually;

/**
 * Order Desk's domain steps as Java: the one place that knows how to add a product to the cart, check out or place
 * an order. The plugin's vocabulary ({@link OrderDeskVocabulary}) runs these methods for a recorded test's {@code do}
 * steps, and a Java UI test calls them directly ({@code OrderDeskSteps.addToCart(ui, "Gel pen, black")}). When the app
 * changes, say a button is renamed, fix it here once and both kinds of test pass again.
 *
 * <p>Client calls only: no app classes, so the plugin needs nothing from the app's build.
 */
public final class OrderDeskSteps {

    private OrderDeskSteps() {
    }

    /**
     * Opens a new order and picks its customer: File ▸ New order, the customer's name (or its start, e.g. "Ada") into
     * Customer, Enter. The tab is then titled "New order · Ada Shop Ltd".
     */
    public static void openNewOrder(Client ui, String customer) {
        ui.menu("File", "New order");
        ui.tab("New order").field("Customer").type(customer);
        ui.press("Enter");
    }

    /** Adds one of {@code product} to the cart of the open new order: selects the product, then Add to cart. */
    public static void addToCart(Client ui, String product) {
        // the SKU cell: the cart table lists the product too once it is in the cart, but has no SKU column
        newOrder(ui).table().row("Product", product).cell("SKU").click();
        newOrder(ui).button("Add to cart").click();
    }

    /** Sets the quantity of {@code product}, already in the cart, to {@code qty}. */
    public static void setQuantity(Client ui, String product, int qty) {
        newOrder(ui).table().row("Product", product).cell("Qty").type(String.valueOf(qty), null, Boolean.TRUE);
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
        newOrder(ui).button("Checkout…").click();
        ui.dialog().button("Next").click();
        ui.dialog().button("Next").click();
        ui.dialog().field("PO number").type(po);
        ui.dialog().button("Next").click();
    }

    /**
     * Places the order on the checkout's Review step and confirms it; returns the new order's number, e.g.
     * {@code ORD-1640}, once its tab is open.
     */
    public static String placeOrder(Client ui) {
        ui.dialog().button("Place order").click();
        ui.answer("Place order");
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
        ui.tab("Orders").click();
        ui.tab("Orders").table().row("Order", order).doubleClick();
    }

    /** Checks the cart total of the open new order, e.g. {@code €31.32}. */
    public static void cartTotalIs(Client ui, String total) {
        assertThat(newOrder(ui).label("Total")).hasText(total);
    }

    /** The open new order's tab: "New order", or "New order · Ada Shop Ltd" once its customer is chosen. */
    private static Target newOrder(Client ui) {
        return ui.tab("New order*");
    }
}
