package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.BACK_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CARD_NUMBER_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CARD_RADIO;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CHECKOUT_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CHECKOUT_STEP_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CHECKOUT_TITLE;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CREDIT_LIMIT_TITLE;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.EDIT_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.EXPIRY_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.INVOICE_RADIO;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NAME_ON_CARD_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEXT_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_TITLE;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PO_NUMBER_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.TOTALS_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.NEW_ORDER_SCOPE;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.json;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.key;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.plain;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.quote;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.text;

/**
 * The checkout's steps and checks beyond {@code checkoutWithPo} and {@code placeOrder} ({@link OrderDeskVocabulary}):
 * start it, set its fields, choose shipping, pay by invoice or by card, go on and back, edit a review section, the
 * credit-limit warning and its answers, cancel, and the checks of what each step shows. Each body calls
 * {@link OrderDeskCheckoutSteps}. Every body looks at the app first (the checkout and its step), so each says its
 * {@code match}; a match looser than the step only suggests it ({@code autoFold(false)}).
 */
final class OrderDeskCheckoutVocabulary {

    private static final String STEPS = OrderDeskCheckoutSteps.class.getName();
    /** The checkout dialog as a recording names it while it is the dialog showing. */
    private static final String DIALOG = "\"window\":\"dialog\"";
    private static final String FIELDS = "Name, Street, Postal code, City, Country, Phone (Address step); Delivery note"
            + " (Shipping step); PO number, Card number, Expiry, Name on card (Payment step)";

    private final Supplier<List<String>> customers;
    private final Supplier<List<String>> reviewProducts;

    OrderDeskCheckoutVocabulary(Supplier<List<String>> customers, Supplier<List<String>> reviewProducts) {
        this.customers = customers;
        this.reviewProducts = reviewProducts;
    }

    List<DomainStep> steps() {
        return Arrays.asList(
                // ---- going through it
                DomainStep.act("startCheckout", "Start the checkout")
                        .about("Needs: a new order showing, its Checkout… enabled. Ends: on the checkout's Address step"
                                + " (the dialog \"Checkout — Ada Shop Ltd\"). A disabled Checkout… fails with its reason"
                                + " (\"The cart is empty\")")
                        .code((ui, p) -> OrderDeskCheckoutSteps.startCheckout(ui))
                        .match(json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}", NEW_ORDER_SCOPE,
                                text(CHECKOUT_BUTTON)))
                        .java(STEPS, "startCheckout(ui)")
                        .build(),
                DomainStep.act("setCheckoutField", "Set the checkout's {field} to {value}")
                        .about("Needs: the checkout on the field's step. Ends: there, the field holding the value and left"
                                + " (Tab), so its error shows. {field}: " + FIELDS + "; the name or its start. {value}:"
                                + " the text; none empties it; Country: Germany, Austria, Netherlands or France")
                        .param("field", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.FIELD_NAMES)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.setCheckoutField(ui, p.text("field"), p.text("value")))
                        .match(json("{'action':'type','target':{%s,'role':'text','name':'{field}'},'text':'{value}'}", DIALOG))
                        .matchAlso(json("{'action':'select','target':{%s,'role':'combo','name':'{field}'},'items':['{value}']}", DIALOG))
                        .autoFold(false)
                        .java(STEPS, "setCheckoutField(ui, {field}, {value})")
                        .build(),
                DomainStep.act("chooseShipping", "Choose {method} shipping")
                        .about("Needs: the checkout's Shipping step. Ends: there, the method chosen. {method}: Standard"
                                + " (3–5 business days) or Express (next business day); Express outside Germany and Austria"
                                + " fails with the app's reason (\"Express delivers to Germany and Austria only\")")
                        .param("method", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.SHIPPING_METHODS)
                        .code((ui, p) -> OrderDeskCheckoutSteps.chooseShipping(ui, p.text("method")))
                        .match(json("{'action':'click','target':{%s,'role':'radio','name':'{method} · *'}}", DIALOG))
                        .java(STEPS, "chooseShipping(ui, {method})")
                        .build(),
                DomainStep.act("payByInvoice", "Pay by invoice with PO number {po}")
                        .about("Needs: the checkout's Payment step. Ends: there, Invoice (30 days) chosen and the PO number"
                                + " typed (\"PO number\", or \"PO number (optional)\"; none: left empty). A card-only"
                                + " customer fails with the app's note (\"Café Lumen pays by card\")")
                        .param("po", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.payByInvoice(ui, p.text("po")))
                        .match(json("{'action':'click','target':{%s,'role':'radio','name':%s}}", DIALOG, text(INVOICE_RADIO)),
                                json("{'action':'type','target':{%s,'role':'text','name':%s},'text':'{po}'}", DIALOG,
                                        quote(plain(PO_NUMBER_FIELD) + "*")),
                                json("{'optional':true,'action':'press','keys':'Tab'}"))
                        .java(STEPS, "payByInvoice(ui, {po})")
                        .build(),
                DomainStep.act("payByCard", "Pay by card {number}, expiry {expiry}, name on card {name}")
                        .about("Needs: the checkout's Payment step. Ends: there, Card chosen and its fields filled (the"
                                + " number shows in groups of four). {expiry}: MM/YY; none leaves a field empty. A wrong"
                                + " value shows its error once"
                                + " the field is left (The checkout's … error reads …)")
                        .param("number", ParamType.TEXT)
                        .param("expiry", ParamType.TEXT)
                        .param("name", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.payByCard(ui, p.text("number"), p.text("expiry"), p.text("name")))
                        .match(json("{'action':'click','target':{%s,'role':'radio','name':%s}}", DIALOG, text(CARD_RADIO)),
                                json("{'action':'type','target':{%s,'role':'text','name':%s},'text':'{number}'}", DIALOG,
                                        text(CARD_NUMBER_FIELD)),
                                json("{'action':'type','target':{%s,'role':'text','name':%s},'text':'{expiry}'}", DIALOG,
                                        text(EXPIRY_FIELD)),
                                json("{'action':'type','target':{%s,'role':'text','name':%s},'text':'{name}'}", DIALOG,
                                        text(NAME_ON_CARD_FIELD)),
                                json("{'optional':true,'action':'press','keys':'Tab'}"))
                        .java(STEPS, "payByCard(ui, {number}, {expiry}, {name})")
                        .build(),
                DomainStep.act("checkoutNext", "Go on to the next checkout step")
                        .about("Needs: the checkout on Address, Shipping or Payment. Ends: on its next step; on Payment"
                                + " with an invoice over the customer's credit limit, on the credit-limit warning (Answer"
                                + " the credit-limit warning with …). A disabled Next fails with its reason and the"
                                + " fields' errors")
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutNext(ui))
                        .match(json("{'action':'click','target':{%s,'role':'button','name':%s}}", DIALOG, text(NEXT_BUTTON)))
                        .java(STEPS, "checkoutNext(ui)")
                        .build(),
                DomainStep.act("checkoutBack", "Go back a checkout step")
                        .about("Needs: the checkout on Shipping, Payment or Review. Ends: on its step before, as it was left")
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutBack(ui))
                        .match(json("{'action':'click','target':{%s,'role':'button','name':%s}}", DIALOG, text(BACK_BUTTON)))
                        .java(STEPS, "checkoutBack(ui)")
                        .build(),
                DomainStep.act("editReviewSection", "Edit the {section} on the review")
                        .about("Needs: the checkout's Review step. Ends: on that section's step (Next then goes on from"
                                + " there). {section}: Delivery address, Shipping or Payment")
                        .param("section", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.SECTIONS)
                        .code((ui, p) -> OrderDeskCheckoutSteps.editReviewSection(ui, p.text("section")))
                        .match(json("{'action':'click','target':{%s,'scope':['section:{section}'],'role':'button','name':%s}}",
                                DIALOG, text(EDIT_BUTTON)))
                        .java(STEPS, "editReviewSection(ui, {section})")
                        .build(),
                DomainStep.act("answerCreditLimit", "Answer the credit-limit warning with {answer}")
                        .about("Needs: the credit-limit warning (after Next on Payment, invoice over the limit). Ends: Pay"
                                + " by card instead: on Payment, Card chosen, its fields to fill; Continue with invoice: on"
                                + " the Review step. {answer}: the button's words or their start (\"card\", \"continue\")")
                        .param("answer", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.CREDIT_ANSWERS)
                        .code((ui, p) -> OrderDeskCheckoutSteps.answerCreditLimit(ui, p.text("answer")))
                        .match(json("{'action':'answer','button':'{answer}'}"))
                        .autoFold(false)
                        .java(STEPS, "answerCreditLimit(ui, {answer})")
                        .build(),
                DomainStep.act("cancelCheckout", "Cancel the checkout")
                        .about("Needs: the checkout on any step, no message over it. Ends: on the new order, as it was"
                                + " (Cancel asks nothing)")
                        .code((ui, p) -> OrderDeskCheckoutSteps.cancelCheckout(ui))
                        .match(json("{'action':'click','target':{%s,'role':'button','name':%s}}", DIALOG, text(CANCEL_BUTTON)))
                        .java(STEPS, "cancelCheckout(ui)")
                        .build(),
                DomainStep.act("cancelPlaceOrder", "Ask to place the order, then cancel")
                        .about("Needs: the checkout's Review step. Ends: still on it, after answering Cancel to \"Place"
                                + " order for €122.34?\"")
                        .code((ui, p) -> OrderDeskCheckoutSteps.cancelPlaceOrder(ui))
                        .match(json("{'action':'click','target':{%s,'role':'button','name':%s}}", DIALOG, text(PLACE_ORDER_BUTTON)),
                                json("{'action':'answer','button':%s}", text(CANCEL_ANSWER)))
                        .java(STEPS, "cancelPlaceOrder(ui)")
                        .build(),

                // ---- checks
                DomainStep.check("checkoutCustomerIs", "The checkout is for {customer}")
                        .about("Reads whose checkout shows, by its title \"Checkout — Ada Shop Ltd\": the customer's whole"
                                + " name, any case")
                        .param("customer", ParamType.TEXT).choices(customers)
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutCustomerIs(ui, p.text("customer")))
                        .match(json("{'action':'expect','target':{%s,'name':%s},'shown':true}", DIALOG,
                                quote(plain(CHECKOUT_TITLE) + "{customer}")))
                        .autoFold(false)
                        .java(STEPS, "checkoutCustomerIs(ui, {customer})")
                        .build(),
                DomainStep.check("checkoutStepIs", "The checkout is on the {step} step")
                        .about("Reads which step the checkout shows: Address (heading \"Delivery address\"), Shipping"
                                + " (\"Shipping method\"), Payment or Review (no heading, Place order instead)")
                        .param("step", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.STEP_NAMES)
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutStepIs(ui, p.text("step")))
                        .match(json("{'action':'expect','target':{%s,'id':%s},'text':'{step}'}", DIALOG, text(CHECKOUT_STEP_ID)))
                        .java(STEPS, "checkoutStepIs(ui, {step})")
                        .build(),
                DomainStep.check("checkoutFieldIs", "The checkout's {field} reads {value}")
                        .about("Reads a checkout field on its step, e.g. the Street filled in from the customer"
                                + " (\"Torstraße 118\"), the Card number in groups of four (\"4242 4242 4242 4242\"), the"
                                + " Country (\"Germany\"); none: empty. {field}: " + FIELDS)
                        .param("field", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.FIELD_NAMES)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutFieldIs(ui, p.text("field"), p.text("value")))
                        // a person's Check menu records a field's or a combo's value as "value"; a pattern never
                        // compares the window, so it fits any text field (Customer, Search): it only suggests the step
                        .match(json("{'action':'expect','target':{%s,'role':'text','name':'{field}'},'value':'{value}'}", DIALOG))
                        .matchAlso(json("{'action':'expect','target':{%s,'role':'combo','name':'{field}'},'value':'{value}'}", DIALOG))
                        .autoFold(false)
                        .java(STEPS, "checkoutFieldIs(ui, {field}, {value})")
                        .build(),
                DomainStep.check("checkoutFieldErrorIs", "The checkout's {field} error reads {error}")
                        .about("Reads the red error under a checkout field, once it was left (at once for the customer's"
                                + " own incomplete address), e.g. Postal code \"Postal code is required\" or \"Germany uses"
                                + " a 5-digit postal code\", Expiry \"This card has expired\"; none: no error")
                        .param("field", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.FIELD_NAMES)
                        .param("error", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.checkoutFieldErrorIs(ui, p.text("field"), p.text("error")))
                        .match(json("{'action':'expect','target':{%s,'id':'{field}Error'},'text':'{error}'}", DIALOG))
                        .java(STEPS, "checkoutFieldErrorIs(ui, {field}, {error})")
                        .build(),
                DomainStep.check("shippingPriceIs", "The {method} shipping price is {price}")
                        .about("Reads a price on the Shipping step: Standard (€4.90; \"Free\" from €100.00 after the"
                                + " discount) or Express (€14.90)")
                        .param("method", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.SHIPPING_METHODS)
                        .param("price", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.shippingPriceIs(ui, p.text("method"), p.text("price")))
                        .match(json("{'action':'expect','target':{%s,'id':'{method}Price'},'text':'{price}'}", DIALOG))
                        .java(STEPS, "shippingPriceIs(ui, {method}, {price})")
                        .build(),
                DomainStep.check("paymentNoteIs", "The note under {method} reads {note}")
                        .about("Reads the note under a way to pay on the Payment step: Invoice's (\"Credit limit €2,000.00 ·"
                                + " owes €450.00\", or \"Café Lumen pays by card\" for a card-only customer) or Card's"
                                + " (\"Debit or credit card\")")
                        .param("method", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.PAYMENT_METHODS)
                        .param("note", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.paymentNoteIs(ui, p.text("method"), p.text("note")))
                        .match(json("{'action':'expect','target':{%s,'id':'{method}Note'},'text':'{note}'}", DIALOG))
                        .java(STEPS, "paymentNoteIs(ui, {method}, {note})")
                        .build(),
                DomainStep.check("creditLimitWarningIs", "The credit-limit warning reads {message}")
                        .about("Reads the warning Next shows on an invoice over the credit limit: its message (\"This order"
                                + " takes Birch & Co to €1,036.60 of its €1,000.00 credit limit.\"), its detail (\"Owes"
                                + " €940.00 now, this order adds €96.60.\") or both")
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.creditLimitWarningIs(ui, p.text("message")))
                        .match(json("{'action':'expect','target':{'window':%s,'role':'label','name':'{message}'},'shown':true}",
                                text("dialog:" + CREDIT_LIMIT_TITLE)))
                        .autoFold(false)
                        .java(STEPS, "creditLimitWarningIs(ui, {message})")
                        .build(),
                DomainStep.check("placeOrderConfirmationIs", "The place-order confirmation reads {message}")
                        .about("Reads what Place order asks on the Review step (\"Place order for €122.34?\", \"Order for"
                                + " Ada Shop Ltd, 11 items.\", or both), then answers Cancel: still on Review")
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.placeOrderConfirmationIs(ui, p.text("message")))
                        .match(json("{'action':'click','target':{%s,'role':'button','name':%s}}", DIALOG, text(PLACE_ORDER_BUTTON)),
                                json("{'action':'expect','target':{'window':%s,'role':'label','name':'{message}'},'shown':true}",
                                        text("dialog:" + PLACE_ORDER_TITLE)),
                                json("{'action':'answer','button':%s}", text(CANCEL_ANSWER)))
                        .autoFold(false)
                        .java(STEPS, "placeOrderConfirmationIs(ui, {message})")
                        .build(),
                DomainStep.check("reviewSectionIs", "The review's {section} reads {text}")
                        .about("Reads a review section's lines, parted by ;: Delivery address (\"Ada Lindqvist; Torstraße"
                                + " 118; 10119 Berlin; Germany\"), Shipping (\"Standard · 3–5 business days · €4.90\", then"
                                + " \"Note: …\" when there is one) or Payment (\"Invoice (30 days) · PO PO-7781\", \"Card"
                                + " ending 4242\")")
                        .param("section", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.SECTIONS)
                        .param("text", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.reviewSectionIs(ui, p.text("section"), p.text("text")))
                        .match(json("{'action':'expect','target':{%s,'scope':['section:{section}'],'role':'label','name':'{text}'},"
                                + "'shown':true}", DIALOG))
                        .autoFold(false)
                        .java(STEPS, "reviewSectionIs(ui, {section}, {text})")
                        .build(),
                DomainStep.check("reviewLinesAre", "The review lists {lines}")
                        .about("Reads the order's lines on the Review step, in order, by product name parted by ;:"
                                + " \"A4 paper, 500 sheets; Gel pen, black\"")
                        .param("lines", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.reviewLinesAre(ui, p.text("lines")))
                        .match(json("{'action':'expect','target':{%s,'role':'table','row':{%s:'{lines}'}},'shown':true}",
                                DIALOG, key(PRODUCT_COLUMN)))
                        .autoFold(false)
                        .java(STEPS, "reviewLinesAre(ui, {lines})")
                        .build(),
                DomainStep.check("reviewLineIs", "The {column} of {product} on the review is {value}")
                        .about("Reads one value of an order line on the Review step: its Qty, Price or Line total, e.g. the"
                                + " Line total of Stapler on the review is €12.50")
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.REVIEW_LINE_COLUMNS)
                        .param("product", ParamType.TEXT).choices(reviewProducts)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.reviewLineIs(ui, p.text("product"), p.text("column"), p.text("value")))
                        .match(json("{'action':'expect','target':{%s,'role':'table','row':{%s:'{product}'},'column':'{column}'},"
                                + "'text':'{value}'}", DIALOG, key(PRODUCT_COLUMN)))
                        // the window is never compared, so this fits the cart's and an order tab's lines too: suggest only
                        .autoFold(false)
                        .java(STEPS, "reviewLineIs(ui, {product}, {column}, {value})")
                        .build(),
                DomainStep.check("reviewAmountIs", "The review's {amount} is {value}")
                        .about("Reads one amount under the review's lines: Subtotal, Volume discount (none while it does"
                                + " not show), Shipping (\"Free\" or €4.90, €14.90), VAT (20%) or Total, e.g. the review's"
                                + " Total is €122.34")
                        .param("amount", ParamType.TEXT).choices(() -> OrderDeskCheckoutSteps.REVIEW_AMOUNTS)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskCheckoutSteps.reviewAmountIs(ui, p.text("amount"), p.text("value")))
                        .match(json("{'action':'expect','target':{%s,'scope':[%s],'role':'label','name':'{amount}'},'text':'{value}'}",
                                DIALOG, text("id:" + TOTALS_ID)))
                        .java(STEPS, "reviewAmountIs(ui, {amount}, {value})")
                        .build());
    }
}
