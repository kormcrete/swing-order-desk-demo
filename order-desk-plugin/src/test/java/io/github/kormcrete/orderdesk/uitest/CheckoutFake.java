package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;

import java.util.Arrays;
import java.util.List;

/**
 * A new order for Ada Shop Ltd (A4 paper ×1, Stapler ×1, Gel pen, black ×10) with its checkout open on one step, as
 * {@link FakeApp} answers reads: every step's fields at once (only the step heading says which shows), and the messages
 * that come and go: Place order's question after a click on Place order, gone at its answer; placing (the "Order for …"
 * line, then the status line and the new order's selected tab) after "Place order"; the credit-limit warning when
 * {@link #creditWarning} opens it.
 */
class CheckoutFake extends FakeApp {

    /** The checkout dialog's key, by its title. */
    static final String CO = "dialog:Checkout — * ";
    static final String CONFIRMATION = "dialog:Place order label";
    static final String CREDIT_WARNING = "dialog:Credit limit label";

    private boolean placingFails;

    /** The checkout on {@code step}: Address, Shipping, Payment or Review. */
    CheckoutFake(String step) {
        answer("text 'Customer'", node("text", "Customer"));
        answer("button 'Checkout…'", node("button", "Checkout…"));
        answer(CO + "button 'Cancel'", node("button", "Cancel"));
        on(step);
        answer("dialog button 'Next'", node("button", "Next"));
        answer("dialog button 'Back'", node("button", "Back"));
        // Address
        answer("dialog text 'Name'", field("Name", "Ada Lindqvist"));
        answer("dialog text 'Street'", field("Street", "Torstraße 118"));
        answer("dialog text 'Postal code'", field("Postal code", "10119"));
        answer("dialog text 'City'", field("City", "Berlin"));
        Node country = items("combo", Arrays.asList("Germany", "Austria", "Netherlands", "France"), 0);
        country.name = "Country";
        country.value = "Germany";
        answer("dialog combo 'Country'", country);
        answer("dialog text 'Phone (optional)'", field("Phone (optional)", "+49 30 5550 1188"));
        // Shipping
        answer("dialog radio 'Standard · 3–5 business days'", withState(node("radio", "Standard · 3–5 business days"), "checked"));
        answer("dialog radio 'Express · next business day'", node("radio", "Express · next business day"));
        answer("dialog element #standardPrice", label("standardPrice", "€4.90"));
        answer("dialog element #expressPrice", label("expressPrice", "€14.90"));
        answer("dialog text 'Delivery note (optional)'", field("Delivery note (optional)", ""));
        // Payment
        answer("dialog radio 'Invoice (30 days)'", withState(node("radio", "Invoice (30 days)"), "checked"));
        answer("dialog element #invoiceNote", label("invoiceNote", "Credit limit €2,000.00 · owes €450.00"));
        answer("dialog text 'PO number'", field("PO number", ""));
        answer("dialog radio 'Card'", node("radio", "Card"));
        answer("dialog label 'Debit or credit card'", label("Debit or credit card"));
        answer("dialog text 'Card number'", field("Card number", ""));
        answer("dialog text 'Expiry'", field("Expiry", ""));
        answer("dialog text 'Name on card'", field("Name on card", ""));
        // Review
        answer("dialog label in section:Delivery address", label("Ada Lindqvist"), label("Torstraße 118"), label("10119 Berlin"),
                label("Germany"), label("+49 30 5550 1188"));
        answer("dialog label in section:Shipping", label("Standard · 3–5 business days · €4.90"));
        answer("dialog label in section:Payment", label("Invoice (30 days) · PO PO-7781"));
        List<List<String>> lines = Arrays.asList(
                Arrays.asList("A4 paper, 500 sheets", "1", "€4.90", "€4.90"),
                Arrays.asList("Stapler", "1", "€12.50", "€12.50"),
                Arrays.asList("Gel pen, black", "10", "€1.20", "€11.40"));
        answer("dialog table", table(Arrays.asList("Product", "Qty", "Price", "Line total"), lines.size(), lines));
        answer("dialog table row=Stapler col=Qty", cell("Qty", "1"));
        answer("dialog table row=Stapler col=Line total", cell("Line total", "€12.50"));
        answer("dialog label 'Subtotal' in id:totals", label("Subtotal", "€29.40"));
        answer("dialog label 'Shipping' in id:totals", label("Shipping", "€4.90"));
        answer("dialog label 'VAT (20%)' in id:totals", label("VAT (20%)", "€6.86"));
        answer("dialog label 'Total' in id:totals", label("Total", "€41.16"));
        windows(modal("Checkout — Ada Shop Ltd", "Address"));
    }

    /** The checkout shows {@code step} now. */
    CheckoutFake on(String step) {
        forget(CO + "element #checkoutStep");
        forget(CO + "button 'Place order'");
        if ("Review".equals(step)) answer(CO + "button 'Place order'", node("button", "Place order"));
        else answer(CO + "element #checkoutStep", label("checkoutStep", heading(step)));
        return this;
    }

    /** The credit-limit warning opens over the checkout. */
    CheckoutFake creditWarning() {
        answer(CREDIT_WARNING, label("This order takes Birch & Co to €1,036.60 of its €1,000.00 credit limit."),
                label("Owes €940.00 now, this order adds €96.60."));
        return this;
    }

    /** Placing the order fails: "The order could not be placed." instead of the new order. */
    CheckoutFake placingFails() {
        placingFails = true;
        return this;
    }

    private static String heading(String step) {
        if ("Address".equals(step)) return "Delivery address";
        if ("Shipping".equals(step)) return "Shipping method";
        if ("Payment".equals(step)) return "Payment";
        throw new IllegalArgumentException(step);
    }

    private static Node field(String caption, String value) {
        Node n = node("text", caption);
        n.value = value;
        return n;
    }

    @Override
    public VerbOutcome click(Locator target, String button, Integer count, List<String> modifiers, Long timeout) {
        if ("dialog".equals(target.window) && "Place order".equals(target.name)) {
            answer(CONFIRMATION, label("Place order for €41.16?"), label("Order for Ada Shop Ltd, 12 items."));
        }
        return super.click(target, button, count, modifiers, timeout);
    }

    @Override
    public VerbOutcome answer(String button, Long timeout) {
        forget(CONFIRMATION);
        forget(CREDIT_WARNING);
        forget("dialog:Place order label 'The order could not be placed.'");
        if ("Place order".equals(button)) {
            answer("dialog label 'Order for '", label("Order for €41.16 · Ada Shop Ltd"));
            if (placingFails) {
                answer("dialog:Place order label 'The order could not be placed.'", label("The order could not be placed."));
                answer(CONFIRMATION, label("The order could not be placed."), label("Stapler: only 0 left"));
            } else {
                answer("element #statusMessage", label("statusMessage", "Order ORD-1640 placed"));
                answer("tab 'ORD-1640'", withState(node("tab", "ORD-1640"), "selected"));
            }
        }
        return super.answer(button, timeout);
    }
}
