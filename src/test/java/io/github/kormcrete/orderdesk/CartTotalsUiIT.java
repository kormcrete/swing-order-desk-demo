package io.github.kormcrete.orderdesk;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.UiTest;
import org.junit.jupiter.api.Test;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.addQtyToCart;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.cartTotalIs;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.openNewOrder;
import static io.github.kormcrete.swingui.junit.Rows.row;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;

/**
 * A cart for Ada Shop Ltd: A4 paper x3 and Gel pen x10, checked by a line total and the order total. Written with
 * Order Desk's domain steps ({@code OrderDeskSteps}, from order-desk-plugin/): how to open an order or add to the cart
 * lives there once, for this test and for the recorded tests' {@code do} steps alike.
 */
@UiTest
class CartTotalsUiIT {

    @Test
    void totalsTheCart(Client ui) {
        openNewOrder(ui, "Ada");

        assertThat(ui.tab("New order · Ada Shop Ltd")).isSelected();

        addQtyToCart(ui, "A4 paper, 500 sheets", 3);
        addQtyToCart(ui, "Gel pen, black", 10);

        assertThat(ui.tab("New order · Ada Shop Ltd").table().cell("Line total"))
                .hasCell(row("Product", "A4 paper, 500 sheets"), "Line total", "€14.70");
        cartTotalIs(ui, "€31.32");

        // discard the cart: a rerun, or PlaceOrderUiIT next, opens its own "New order · Ada Shop Ltd" tab
        ui.menu("File", "Close tab");
        ui.answer("Discard");

        assertThat(ui.tab("New order · Ada Shop Ltd")).isAbsent();
    }
}
