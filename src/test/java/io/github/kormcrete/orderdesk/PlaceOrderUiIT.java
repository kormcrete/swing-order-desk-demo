package io.github.kormcrete.orderdesk;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.UiTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.github.kormcrete.swingui.junit.Rows.row;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;
import static io.github.kormcrete.swingui.junit.UiAssertions.eventually;

/**
 * A new order for Ada Shop Ltd, from File ▸ New order through checkout to the placed order, with a 2x screenshot
 * after every step. Recorded through swing-ui-agent and reviewed.
 */
@UiTest
class PlaceOrderUiIT {

    @Test
    void placesAnOrderForAda(Client ui) {
        ui.menu("File", "New order");

        assertThat(ui.tab("New order")).isSelected();
        ui.inWindow("main").saveScreenshot("PlaceOrderUiIT/01-new-order.png", 2);

        ui.tab("New order").field("Customer").type("Ada");
        ui.press("Enter");

        assertThat(ui.tab("New order · Ada Shop Ltd")).isSelected();
        assertThat(ui.tab("New order · Ada Shop Ltd")
                .label("Account · Invoice 30 days · Credit limit €2,000.00").contains()).isVisible();
        ui.inWindow("main").saveScreenshot("PlaceOrderUiIT/02-customer.png", 2);

        ui.tab("New order · Ada Shop Ltd").table().row("Product", "A4 paper, 500 sheets").cell("SKU").click();
        ui.tab("New order · Ada Shop Ltd").button("Add to cart").click();
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "Gel pen, black").cell("SKU").click();
        ui.tab("New order · Ada Shop Ltd").button("Add to cart").click();
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "A4 paper, 500 sheets").cell("Qty")
                .type("3", null, Boolean.TRUE);
        ui.tab("New order · Ada Shop Ltd").table().row("Product", "Gel pen, black").cell("Qty")
                .type("10", null, Boolean.TRUE);

        assertThat(ui.tab("New order · Ada Shop Ltd").label("Subtotal")).hasText("€26.70");
        assertThat(ui.tab("New order · Ada Shop Ltd").label("Volume discount")).hasText("−€0.60");
        assertThat(ui.tab("New order · Ada Shop Ltd").label("VAT (20%)")).hasText("€5.22");
        assertThat(ui.tab("New order · Ada Shop Ltd").label("Total")).hasText("€31.32");
        ui.inWindow("main").saveScreenshot("PlaceOrderUiIT/03-cart.png", 2);

        ui.tab("New order · Ada Shop Ltd").button("Checkout…").click();

        assertThat(ui.dialog().field("Postal code")).hasValue("10119");
        ui.inWindow("dialog").saveScreenshot("PlaceOrderUiIT/04-address.png", 2);

        ui.dialog().button("Next").click();

        assertThat(ui.dialog().radio("Standard · 3–5 business days")).isChecked();
        ui.inWindow("dialog").saveScreenshot("PlaceOrderUiIT/05-shipping.png", 2);

        ui.dialog().button("Next").click();

        assertThat(ui.dialog().label("Enter the PO number")).isVisible();
        assertThat(ui.dialog().button("Next")).isDisabled();
        ui.inWindow("dialog").saveScreenshot("PlaceOrderUiIT/06-payment.png", 2);

        ui.dialog().field("PO number").type("PO-7781");
        ui.dialog().button("Next").click();

        assertThat(ui.dialog().label("Total")).hasText("€37.20");
        ui.inWindow("dialog").saveScreenshot("PlaceOrderUiIT/07-review.png", 2);

        ui.dialog().button("Place order").click();
        ui.inWindow("dialog").saveScreenshot("PlaceOrderUiIT/08-confirm.png", 2);
        ui.answer("Place order");

        // the number is the app's next one: wait for the status bar to name the order whose tab is selected, so a
        // rerun against a running app passes too (the last run's "Order … placed" shows until this one replaces it)
        String order = eventually("the status bar naming the new order", Duration.ofMillis(8000L), () -> {
            String placed = ui.main().label("placed").contains().text();   // "Order ORD-1640 placed"
            String number = placed.substring("Order ".length(), placed.indexOf(" placed"));
            return ui.tab(number).states().contains("selected") ? number : null;
        }, number -> number != null);
        assertThat(ui.tab(order)).isSelected();
        ui.inWindow("main").saveScreenshot("PlaceOrderUiIT/09-placed.png", 2);

        ui.tab("Orders").click();

        assertThat(ui.tab("Orders").table().cell("Status")).hasCell(row("Order", order), "Status", "Placed");
        assertThat(ui.tab("Orders").table().cell("Status")).hasCell(row("Order", order), "Total", "€37.20");
        ui.inWindow("main").saveScreenshot("PlaceOrderUiIT/10-orders-after.png", 2);
    }
}
