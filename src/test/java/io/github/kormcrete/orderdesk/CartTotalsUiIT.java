package io.github.kormcrete.orderdesk;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.UiTest;
import org.junit.jupiter.api.Test;

import static io.github.kormcrete.swingui.junit.Rows.row;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;

/**
 * A cart for Ada Shop Ltd: A4 paper x3 and Gel pen x10, checked by a line total and the order total. Recorded
 * through swing-ui-agent; the home page of its site plays this recording and replay.
 */
@UiTest
class CartTotalsUiIT {

    @Test
    void totalsTheCart(Client ui) {
        ui.menu("File", "New order");
        ui.tab("New order").field("Customer").type("Ada");
        ui.press("Enter");

        assertThat(ui.tab("New order · Ada Shop Ltd")).isSelected();

        ui.tab("New order · Ada Shop Ltd").table().row("Product", "A4 paper, 500 sheets").cell("SKU").click();
        ui.tab("New order · Ada Shop Ltd").button("Add to cart").click();
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "Gel pen, black").cell("SKU").click();
        ui.tab("New order · Ada Shop Ltd").button("Add to cart").click();
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "A4 paper, 500 sheets").cell("Qty")
                .type("3", null, Boolean.TRUE);
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "Gel pen, black").cell("Qty")
                .type("10", null, Boolean.TRUE);

        assertThat(ui.tab("New order · Ada Shop Ltd").table().cell("Line total"))
                .hasCell(row("Product", "A4 paper, 500 sheets"), "Line total", "€14.70");
        assertThat(ui.tab("New order · Ada Shop Ltd").label("Total")).hasText("€31.32");

        // discard the cart: a rerun, or PlaceOrderUiIT next, opens its own "New order · Ada Shop Ltd" tab
        ui.menu("File", "Close tab");
        ui.answer("Discard");

        assertThat(ui.tab("New order · Ada Shop Ltd")).isAbsent();
    }
}
