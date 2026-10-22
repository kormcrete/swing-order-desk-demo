package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;
import io.github.kormcrete.swingui.junit.UiAssertionError;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.LOOK;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.column;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.isNone;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.join;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.look;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.names;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.namesAre;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.notShown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.oneLine;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.pick;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.quoted;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.resolve;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.showing;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shown;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskLooks.shows;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskNewOrderSteps.requireNewOrder;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.*;
import static io.github.kormcrete.swingui.junit.UiAssertions.assertThat;
import static io.github.kormcrete.swingui.junit.UiAssertions.eventually;

/**
 * The checkout's steps and checks, as Java: the modal "Checkout — Ada Shop Ltd" dialog's four steps (Address, Shipping,
 * Payment, Review), going on and back, the credit-limit warning, the place-order confirmation and placing the order.
 * {@link OrderDeskVocabulary} runs them for a recorded test's {@code do} steps; a Java UI test calls them directly.
 *
 * <p>Every step first looks, for about a second, for the checkout and the step of it that it needs, and says so in
 * words when another one shows (or a message is open over it). A field, a shipping method, a way to pay or a section is
 * given by its name or the start of it, any case. A check fails at once with what was expected and what the app shows;
 * {@code none} stands for "not shown", several lines are parted by {@code ;}. A step that fails after opening something
 * (a checkout, a confirmation) closes it again, so the screen stays as it was. Client calls only, and the names the app
 * shows come from {@link OrderDeskSteps}' constants.
 */
public final class OrderDeskCheckoutSteps {

    /** The checkout's steps: the indicator's word, and the heading the step shows (none on Review). */
    enum Step {
        ADDRESS(ADDRESS_STEP, ADDRESS_HEADING), SHIPPING(SHIPPING_STEP, SHIPPING_HEADING),
        PAYMENT(PAYMENT_STEP, PAYMENT_HEADING), REVIEW(REVIEW_STEP, null);

        final String word;
        final String heading;

        Step(String word, String heading) {
            this.word = word;
            this.heading = heading;
        }
    }

    /** A field of the checkout: the step it is on, the captions it shows, its error's code name (none: no error). */
    static final class Field {
        final String name;
        final Step step;
        final List<String> captions;
        final String errorId;
        final boolean combo;

        Field(String name, Step step, String errorId, boolean combo, String... captions) {
            this.name = name;
            this.step = step;
            this.errorId = errorId;
            this.combo = combo;
            this.captions = Arrays.asList(captions);
        }
    }

    static final List<Field> FIELDS = Collections.unmodifiableList(Arrays.asList(
            new Field(NAME_FIELD, Step.ADDRESS, "nameError", false, NAME_FIELD),
            new Field(STREET_FIELD, Step.ADDRESS, "streetError", false, STREET_FIELD),
            new Field(POSTAL_CODE_FIELD, Step.ADDRESS, "postalCodeError", false, POSTAL_CODE_FIELD),
            new Field(CITY_FIELD, Step.ADDRESS, "cityError", false, CITY_FIELD),
            new Field(COUNTRY_COMBO, Step.ADDRESS, null, true, COUNTRY_COMBO),
            new Field("Phone", Step.ADDRESS, "phoneError", false, PHONE_FIELD),
            new Field("Delivery note", Step.SHIPPING, null, false, DELIVERY_NOTE_FIELD),
            new Field(PO_NUMBER_FIELD, Step.PAYMENT, "poNumberError", false, PO_NUMBER_FIELD, PO_NUMBER_OPTIONAL_FIELD),
            new Field(CARD_NUMBER_FIELD, Step.PAYMENT, "cardNumberError", false, CARD_NUMBER_FIELD),
            new Field(EXPIRY_FIELD, Step.PAYMENT, "expiryError", false, EXPIRY_FIELD),
            new Field(NAME_ON_CARD_FIELD, Step.PAYMENT, "nameOnCardError", false, NAME_ON_CARD_FIELD)));

    /** The fields by name, for a param's choices: "Name", "Street", ..., "Name on card". */
    static final List<String> FIELD_NAMES;
    /** The steps by the indicator's word: Address, Shipping, Payment, Review. */
    static final List<String> STEP_NAMES;
    static final String STANDARD = "Standard";
    static final String EXPRESS = "Express";
    static final List<String> SHIPPING_METHODS = Collections.unmodifiableList(Arrays.asList(STANDARD, EXPRESS));
    static final String INVOICE = "Invoice";
    static final String CARD = "Card";
    static final List<String> PAYMENT_METHODS = Collections.unmodifiableList(Arrays.asList(INVOICE, CARD));
    static final List<String> SECTIONS = Collections.unmodifiableList(Arrays.asList(
            DELIVERY_ADDRESS_SECTION, SHIPPING_SECTION, PAYMENT_SECTION));
    static final List<String> CREDIT_ANSWERS = Collections.unmodifiableList(Arrays.asList(
            PAY_BY_CARD_ANSWER, CONTINUE_WITH_INVOICE_ANSWER));
    /** The checkout's controls that show a reason while disabled (with a new order's, in buttonIsDisabled). */
    static final List<String> REASON_CONTROLS = Collections.unmodifiableList(Arrays.asList(NEXT_BUTTON, EXPRESS, INVOICE));
    /** The review's line columns a check reads (Product names the line). */
    static final List<String> REVIEW_LINE_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            QTY_COLUMN, PRICE_COLUMN, LINE_TOTAL_COLUMN));
    static final List<String> REVIEW_AMOUNTS = OrderDeskNewOrderSteps.CART_AMOUNTS;

    static {
        List<String> fields = new ArrayList<String>();
        for (Field f : FIELDS) fields.add(f.name);
        FIELD_NAMES = Collections.unmodifiableList(fields);
        List<String> steps = new ArrayList<String>();
        for (Step s : Step.values()) steps.add(s.word);
        STEP_NAMES = Collections.unmodifiableList(steps);
    }

    /** What a step says when no checkout is open. */
    static final String NO_CHECKOUT = "No checkout is open. Start one from a new order with \"Start the checkout\"";
    /** What a step says when the credit-limit warning it needs is not showing. */
    static final String NO_CREDIT_WARNING = "No credit-limit warning is showing: it shows after Next on the checkout's"
            + " Payment step, when an invoice takes the customer over the credit limit";
    /** How long placing an order may take before the new order's number shows (the app takes about 2 s). */
    static final Duration PLACING_WAIT = Duration.ofSeconds(8);
    /** The status line once an order is placed: "Order ORD-1640 placed". */
    private static final Pattern PLACED = Pattern.compile("Order (\\S+) placed");

    private OrderDeskCheckoutSteps() {
    }

    // ---- going through the checkout

    /**
     * Checks out the showing new order: Checkout…. Ends on the checkout's Address step. A disabled Checkout… fails
     * with its reason ("The cart is empty").
     */
    public static void startCheckout(Client ui) {
        requireNewOrder(ui);
        Target checkout = newOrder(ui).button(CHECKOUT_BUTTON);
        if (checkout.states().contains("disabled")) {
            String reason = shown(newOrder(ui).byId(CHECKOUT_REASON_ID));
            throw new DriverException(Reason.NOT_ACTIONABLE, CHECKOUT_BUTTON + " is disabled: "
                    + (reason == null ? "no reason shows" : reason));
        }
        checkout.click();
    }

    /**
     * Checks out the showing new order and pays on invoice with a PO number: Checkout…, Next past the address and the
     * shipping, {@code po} into PO number ("PO number (optional)" for a customer that does not need one; {@code none}:
     * nothing typed), Next. Ends on the Review step; over the customer's credit limit, on the credit-limit warning. A
     * step that blocks fails with the app's reason (Elm Street Studio has no postal code; Café Lumen pays by card), the
     * checkout cancelled.
     */
    public static void checkoutWithPo(final Client ui, String po) {
        startCheckout(ui);
        try {
            pressNext(ui, Step.ADDRESS);
            pressNext(ui, Step.SHIPPING);
            Target invoice = ui.dialog().radio(INVOICE_RADIO);
            if (invoice.states().contains("disabled")) {
                throw new DriverException(Reason.NOT_ACTIONABLE, "No PO number to give: " + invoiceReason(ui)
                        + ". Pay with \"Pay by card <number>, expiry <MM/YY>, name on card <name>\"");
            }
            if (!isNone(po)) poField(ui).type(po);
            pressNext(ui, Step.PAYMENT);
        } catch (RuntimeException e) {
            // the step opened the checkout: close it again, so the new order shows as it was
            quietly(() -> ui.dialog().button(CANCEL_BUTTON).click());
            throw e;
        }
    }

    /**
     * Sets one field of the showing checkout step: types {@code value} ({@code none}: empties it), then leaves the field
     * with Tab, as a person does, so its error shows. Country is chosen from its list (its name or the start of it).
     * {@code field}: Name, Street, Postal code, City, Country, Phone (Address); Delivery note (Shipping); PO number, Card
     * number, Expiry, Name on card (Payment).
     */
    public static void setCheckoutField(Client ui, String field, String value) {
        Field f = field(field);
        requireOn(ui, f.step, f.name);
        if (f.combo) {
            Target combo = ui.dialog().combo(f.captions.get(0));
            combo.select(pick(value, combo.items(), "country", "countries", f.name + " offers"));
            return;
        }
        Target target = fieldTarget(ui, f);
        if (target.states().contains("disabled")) {
            throw new DriverException(Reason.NOT_ACTIONABLE, f.name + " is disabled" + (PO_NUMBER_FIELD.equals(f.name)
                    ? ": Card is chosen (\"Pay by invoice with PO number <po>\" chooses Invoice)"
                    : ": Invoice is chosen (\"Pay by card <number>, expiry <MM/YY>, name on card <name>\" chooses Card)"));
        }
        target.type(orEmpty(value));
        target.press("Tab");
    }

    /** Chooses Standard or Express on the Shipping step; Express outside Germany and Austria fails with the app's reason. */
    public static void chooseShipping(Client ui, String method) {
        requireOn(ui, Step.SHIPPING, "Shipping");
        String m = shippingMethod(method);
        Target radio = ui.dialog().radio(radioOf(m));
        if (radio.states().contains("disabled")) {
            String reason = shown(ui.dialog().label(EXPRESS_REASON));
            throw new DriverException(Reason.NOT_ACTIONABLE, m + " cannot be chosen: " + (reason == null ? "it is disabled" : reason));
        }
        radio.click();
    }

    /**
     * Pays on invoice on the Payment step: Invoice (30 days), then {@code po} into PO number ({@code none}: emptied),
     * then Tab. A card-only customer fails with the app's note ("Café Lumen pays by card").
     */
    public static void payByInvoice(Client ui, String po) {
        requireOn(ui, Step.PAYMENT, "Paying");
        Target invoice = ui.dialog().radio(INVOICE_RADIO);
        if (invoice.states().contains("disabled")) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "Invoice cannot be chosen: " + invoiceReason(ui));
        }
        invoice.click();
        Target field = poField(ui);
        if (!isNone(po)) {
            field.type(po);
        } else {
            String now = shown(field);
            if (now != null && !now.isEmpty()) field.type("");
        }
        field.press("Tab");
    }

    /**
     * Pays by card on the Payment step: Card, then Card number, Expiry (MM/YY) and Name on card ({@code none}: emptied),
     * then Tab.
     */
    public static void payByCard(Client ui, String number, String expiry, String name) {
        requireOn(ui, Step.PAYMENT, "Paying");
        ui.dialog().radio(CARD_RADIO).click();
        ui.dialog().field(CARD_NUMBER_FIELD).type(orEmpty(number));
        ui.dialog().field(EXPIRY_FIELD).type(orEmpty(expiry));
        Target last = ui.dialog().field(NAME_ON_CARD_FIELD);
        last.type(orEmpty(name));
        last.press("Tab");
    }

    /** {@code value}, or nothing for {@code none}. */
    private static String orEmpty(String value) {
        return isNone(value) ? "" : value;
    }

    /**
     * Next: on to the checkout's next step. A disabled Next fails with its reason ("Fix the fields marked in red", and
     * which fields: "Postal code: Postal code is required"). On Payment with an invoice over the credit limit, the
     * credit-limit warning shows instead.
     */
    public static void checkoutNext(Client ui) {
        Step now = requireCheckout(ui);
        if (now == Step.REVIEW) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "The checkout is on its Review step: Place order comes next"
                    + " (\"Place the order\")");
        }
        pressNext(ui, now);
    }

    /** Back: to the checkout's step before. */
    public static void checkoutBack(Client ui) {
        Step now = requireCheckout(ui);
        if (now == Step.ADDRESS) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "The checkout is on its first step, Address: no step before it");
        }
        ui.dialog().button(BACK_BUTTON).click();
    }

    /** A review section's Edit: back to its step (Delivery address, Shipping or Payment). */
    public static void editReviewSection(Client ui, String section) {
        requireOn(ui, Step.REVIEW, "Edit");
        ui.dialog().section(section(section)).button(EDIT_BUTTON).click();
    }

    /**
     * Answers the credit-limit warning: Pay by card instead (back on Payment, Card chosen) or Continue with invoice (on
     * to Review). {@code answer}: the button's words or the start of them ("card", "continue").
     */
    public static void answerCreditLimit(Client ui, String answer) {
        if (!showing(creditWarning(ui))) throw new DriverException(Reason.NOT_FOUND, NO_CREDIT_WARNING);
        Map<String, String> words = new LinkedHashMap<String, String>();
        words.put(PAY_BY_CARD_ANSWER, PAY_BY_CARD_ANSWER);
        words.put(CARD, PAY_BY_CARD_ANSWER);
        words.put(CONTINUE_WITH_INVOICE_ANSWER, CONTINUE_WITH_INVOICE_ANSWER);
        words.put(INVOICE, CONTINUE_WITH_INVOICE_ANSWER);
        ui.answer(resolve(answer, words, "answer", "answers", "The warning's answers"));
    }

    /** Cancel: leaves the checkout, no question; the new order stays as it was. */
    public static void cancelCheckout(Client ui) {
        requireCheckout(ui);
        ui.dialog().button(CANCEL_BUTTON).click();
    }

    /** Place order on the Review step, then Cancel at the confirmation: still on Review. */
    public static void cancelPlaceOrder(Client ui) {
        requireOn(ui, Step.REVIEW, "Place order");
        ui.dialog().button(PLACE_ORDER_BUTTON).click();
        ui.answer(CANCEL_ANSWER);
    }

    /**
     * Place order on the Review step, confirmed; returns the new order's number (ORD-1640) once the status line names it
     * and its tab, which takes the new order's place, is selected.
     */
    public static String placeOrder(Client ui) {
        return (String) placeOrderOutcome(ui).get("number");
    }

    /**
     * As {@link #placeOrder}, and what it showed while placing: {@code {"number": "ORD-1640", "placing": "Order for
     * €37.20 · Ada Shop Ltd"}} ({@code placing} null when it was not seen). "The order could not be placed." fails with
     * the app's detail, after answering it OK (the checkout is back on its Review step).
     */
    static Map<String, Object> placeOrderOutcome(final Client ui) {
        requireOn(ui, Step.REVIEW, "Place order");
        ui.dialog().button(PLACE_ORDER_BUTTON).click();
        ui.answer(PLACE_ORDER_ANSWER);
        String placing = look(() -> shown(ui.dialog().label(PLACING_FOR).contains()));
        // the status line names the new order; a rerun against a running app still shows the last one's until this one
        // replaces it, so wait for the number whose tab is selected
        String number = eventually("the status line naming the new order, its tab selected", PLACING_WAIT,
                () -> placedNumber(ui), n -> n != null);
        Map<String, Object> made = new LinkedHashMap<String, Object>();
        made.put("number", number);
        made.put("placing", placing);
        return made;
    }

    // ---- checks

    /** Checks whose checkout shows, by its title ("Checkout — Ada Shop Ltd"): the customer's whole name, any case. */
    public static void checkoutCustomerIs(Client ui, String customer) {
        requireCheckout(ui);
        String title = ui.describeModal();
        String actual = title.startsWith(CHECKOUT_TITLE) ? title.substring(CHECKOUT_TITLE.length()) : title;
        int at = actual.indexOf(": ");
        if (at >= 0) actual = actual.substring(0, at);   // describeModal adds the first label: "...: Address"
        if (!actual.equalsIgnoreCase(customer.trim())) {
            boolean start = actual.toLowerCase(Locale.ROOT).startsWith(customer.trim().toLowerCase(Locale.ROOT));
            throw new AssertionError("The checkout is for " + actual + ", expected " + customer.trim()
                    + (start ? " (only the start of its name: give the whole name)" : ""));
        }
    }

    /** Checks which step the checkout shows: Address, Shipping, Payment or Review (its heading or number works too). */
    public static void checkoutStepIs(Client ui, String step) {
        Step want = step(step);
        Step now = requireCheckout(ui);
        if (now != want) throw new AssertionError("The checkout is on its " + now.word + " step, expected " + want.word);
    }

    /** Checks what a checkout field shows ({@code none}: empty); its step must show. */
    public static void checkoutFieldIs(Client ui, String field, String value) {
        Field f = field(field);
        requireOn(ui, f.step, f.name);
        shows(fieldTarget(ui, f), "The checkout's " + f.name, isNone(value) ? "" : value);
    }

    /** Checks the error under a checkout field ("Postal code is required"); {@code none}: no error shows. */
    public static void checkoutFieldErrorIs(Client ui, String field, String error) {
        Field f = field(field);
        if (f.errorId == null) throw new DriverException(Reason.BAD_INPUT, f.name + " shows no error of its own");
        requireOn(ui, f.step, f.name);
        Target label = ui.dialog().byId(f.errorId);
        if (isNone(error)) notShown(label, "The checkout's " + f.name + " error");
        else shows(label, "The checkout's " + f.name + " error", error);
    }

    /** Checks a shipping price on the Shipping step: Standard (€4.90, "Free" from €100.00) or Express (€14.90). */
    public static void shippingPriceIs(Client ui, String method, String price) {
        requireOn(ui, Step.SHIPPING, "The shipping prices");
        String m = shippingMethod(method);
        shows(ui.dialog().byId(STANDARD.equals(m) ? STANDARD_PRICE_ID : EXPRESS_PRICE_ID), "The " + m + " shipping price", price);
    }

    /**
     * Checks the note under a way to pay on the Payment step: Invoice's ("Credit limit €2,000.00 · owes €450.00", or
     * "Café Lumen pays by card") or Card's ("Debit or credit card").
     */
    public static void paymentNoteIs(Client ui, String method, String note) {
        requireOn(ui, Step.PAYMENT, "The payment notes");
        String m = paymentMethod(method);
        if (INVOICE.equals(m)) {
            shows(ui.dialog().byId(INVOICE_NOTE_ID), "The note under Invoice", note);
            return;
        }
        // Card's note has one text and no code name: it is found by that text, so read it to say what shows
        Target hint = ui.dialog().label(CARD_HINT);
        if (!CARD_HINT.equals(note.trim())) {
            String actual = shown(hint);
            throw new AssertionError("The note under Card " + (actual == null ? "is not shown" : "reads " + quoted(actual))
                    + ", expected " + quoted(note));
        }
        shows(hint, "The note under Card", CARD_HINT);
    }

    /**
     * Checks the credit-limit warning's text: its message ("This order takes Birch & Co to €1,036.60 of its €1,000.00
     * credit limit."), its detail ("Owes €940.00 now, this order adds €96.60.") or both, one after the other.
     */
    public static void creditLimitWarningIs(final Client ui, String message) {
        List<String> lines = messageLines(creditWarning(ui));
        if (lines == null) throw new DriverException(Reason.NOT_FOUND, NO_CREDIT_WARNING);
        messageIs(lines, message, "The credit-limit warning");
    }

    /**
     * Checks the confirmation Place order asks ("Place order for €122.34?", "Order for Ada Shop Ltd, 11 items.", or
     * both): presses Place order on the Review step, reads it, then answers Cancel, still on Review.
     */
    public static void placeOrderConfirmationIs(Client ui, String message) {
        requireOn(ui, Step.REVIEW, "Place order");
        ui.dialog().button(PLACE_ORDER_BUTTON).click();
        List<String> lines;
        try {
            lines = messageLines(ui.dialog(PLACE_ORDER_TITLE).label(null));
        } finally {
            ui.answer(CANCEL_ANSWER);
        }
        if (lines == null) throw new AssertionError("Place order asked nothing, expected " + quoted(message));
        messageIs(lines, message, "The place-order confirmation");
    }

    /**
     * Checks a review section's lines, parted by ;: Delivery address ("Ada Lindqvist; Torstraße 118; 10119 Berlin;
     * Germany"), Shipping ("Standard · 3–5 business days · €4.90", then "Note: …" when there is one) or Payment
     * ("Invoice (30 days) · PO PO-7781", "Card ending 4242").
     */
    public static void reviewSectionIs(Client ui, String section, String text) {
        requireOn(ui, Step.REVIEW, "The review");
        String s = section(section);
        List<String> lines = ui.dialog().section(s).label(null).texts();
        List<String> expected = names(text);
        if (!lines.equals(expected)) {
            throw new AssertionError("The review's " + s + " reads " + quoted(join(lines)) + ", expected " + quoted(join(expected)));
        }
    }

    /** Checks the order's lines on the review, by product, in order ({@code "Stapler; Gel pen, black"}). */
    public static void reviewLinesAre(Client ui, String lines) {
        requireOn(ui, Step.REVIEW, "The review");
        namesAre(reviewProducts(ui), isNone(lines) ? Collections.<String>emptyList() : names(lines), "The review lists");
    }

    /** Checks one value of an order line on the review: its Qty, Price or Line total. */
    public static void reviewLineIs(Client ui, String product, String column, String value) {
        requireOn(ui, Step.REVIEW, "The review");
        String name = pick(product, reviewProducts(ui), "order line", "order lines", "The review lists");
        String col = column.trim().toLowerCase(Locale.ROOT).startsWith("quant") ? QTY_COLUMN
                : pick(column, REVIEW_LINE_COLUMNS, "column", "columns", "The review's columns");
        shows(ui.dialog().table().row(PRODUCT_COLUMN, name).cell(col), name + "'s " + col + " on the review", value);
    }

    /**
     * Checks one of the review's amounts: Subtotal, Volume discount ({@code none} while it does not show), Shipping
     * ("Free" from €100.00), VAT (20%) or Total. Read inside its totals: the step indicator says Shipping too.
     */
    public static void reviewAmountIs(Client ui, String amount, String value) {
        requireOn(ui, Step.REVIEW, "The review");
        String caption = pick(amount, REVIEW_AMOUNTS, "amount", "amounts", "The review's amounts");
        Target label = ui.dialog().byId(TOTALS_ID).label(caption);
        if (isNone(value)) notShown(label, "The review's " + caption);
        else shows(label, "The review's " + caption, value);
    }

    /** Next, Express or Invoice disabled, with the reason beside it ({@code buttonIsDisabled}). */
    static void controlIsDisabled(Client ui, String control, String reason) {
        Target target = control(ui, control);
        try {
            assertThat(target).within(LOOK).isDisabled();
        } catch (UiAssertionError e) {
            throw new AssertionError(control + " is enabled, expected disabled: " + quoted(reason));
        }
        if (EXPRESS.equals(control) && !EXPRESS_REASON.equals(reason.trim())) {
            // Express's reason has one text and no code name: it is found by that text, so read it to say what shows
            String actual = shown(ui.dialog().label(EXPRESS_REASON));
            throw new AssertionError("Express's reason " + (actual == null ? "is not shown" : "reads " + quoted(actual))
                    + ", expected " + quoted(reason));
        }
        shows(reasonOf(ui, control), control + "'s reason", reason);
    }

    /** Next, Express or Invoice enabled; when not, the failure gives its reason ({@code buttonIsEnabled}). */
    static void controlIsEnabled(Client ui, String control) {
        Target target = control(ui, control);
        try {
            assertThat(target).within(LOOK).isEnabled();
        } catch (UiAssertionError e) {
            String reason = shown(reasonOf(ui, control));
            throw new AssertionError(control + " is disabled" + (reason == null ? "" : " (" + quoted(reason) + ")") + ", expected enabled");
        }
    }

    /** The checkout control a tester names (Next, Express, Invoice, or their labels), or null when it is none of them. */
    static String controlName(String control) {
        String w = OrderDeskLooks.letters(control);
        if (w.isEmpty()) return null;
        for (String c : REASON_CONTROLS) {
            String label = OrderDeskLooks.letters(c.equals(EXPRESS) ? EXPRESS_RADIO : c.equals(INVOICE) ? INVOICE_RADIO : c);
            if (OrderDeskLooks.letters(c).startsWith(w) || label.equals(w) || w.startsWith(OrderDeskLooks.letters(c))) return c;
        }
        return null;
    }

    // ---- what the choices read (OrderDeskVocabulary)

    /** The products the review lists, in order. */
    static List<String> reviewProducts(Client ui) {
        return column(ui.dialog().table().rows(), PRODUCT_COLUMN);
    }

    // ---- inside

    /**
     * The step the checkout shows, after a short look for it; fails fast, in words, when no checkout is open, a message
     * is open over it (answer it first), or the order is being placed.
     */
    static Step requireCheckout(Client ui) {
        String over = messageOver(ui);
        if (over != null) throw new DriverException(Reason.NOT_ACTIONABLE, over);
        if (!showing(checkout(ui).button(CANCEL_BUTTON))) throw new DriverException(Reason.NOT_FOUND, NO_CHECKOUT);
        Step now = currentStep(ui);
        if (now == null) {
            throw new DriverException(Reason.NOT_ACTIONABLE, "The order is being placed (" + PLACING_ORDER + "): every"
                    + " button of the checkout waits until it is done");
        }
        return now;
    }

    /** {@link #requireCheckout}, on step {@code need}: else says where the checkout is and how to get there. */
    static void requireOn(Client ui, Step need, String what) {
        Step now = requireCheckout(ui);
        if (now == need) return;
        String way = need.ordinal() > now.ordinal() ? "go on with \"Go on to the next checkout step\""
                : now == Step.REVIEW ? "go back with \"Edit the <section> on the review\" or \"Go back a checkout step\""
                : "go back with \"Go back a checkout step\"";
        throw new DriverException(Reason.NOT_FOUND, what + " is on the checkout's " + need.word + " step, and the checkout"
                + " shows its " + now.word + " step: " + way);
    }

    /** The message open over the checkout (the credit-limit warning, Place order's question), in words; null for none. */
    private static String messageOver(Client ui) {
        if (creditWarning(ui).presence() != Target.Presence.NONE) {
            return "The credit-limit warning is open: answer it first with \"Answer the credit-limit warning with "
                    + PAY_BY_CARD_ANSWER + "\" (or " + CONTINUE_WITH_INVOICE_ANSWER + ")";
        }
        Target asked = ui.dialog(PLACE_ORDER_TITLE).label(null);
        if (asked.presence() != Target.Presence.NONE) {
            List<String> lines = messageLines(asked);
            return "A Place order message is open (" + (lines == null ? "" : quoted(join(lines))) + "): answer it first";
        }
        return null;
    }

    /** The step the checkout shows now, by its heading (Review has none, but Place order); null while placing. */
    private static Step currentStep(Client ui) {
        String heading = shown(checkout(ui).byId(CHECKOUT_STEP_ID));
        if (heading != null) {
            for (Step s : Step.values()) if (heading.equals(s.heading)) return s;
        }
        if (checkout(ui).button(PLACE_ORDER_BUTTON).presence() != Target.Presence.NONE) return Step.REVIEW;
        return null;
    }

    /** Next on step {@code now}, unless it is disabled: then its reason, and the errors the fields show. */
    private static void pressNext(Client ui, Step now) {
        Target next = ui.dialog().button(NEXT_BUTTON);
        if (next.states().contains("disabled")) {
            String reason = shown(ui.dialog().byId(NEXT_REASON_ID));
            List<String> errors = new ArrayList<String>();
            for (Field f : FIELDS) {
                if (f.step != now || f.errorId == null) continue;
                String error = shown(ui.dialog().byId(f.errorId));
                if (error != null && !error.isEmpty()) errors.add(f.name + ": " + error);
            }
            throw new DriverException(Reason.NOT_ACTIONABLE, "Next is disabled on the checkout's " + now.word + " step: "
                    + (reason == null ? "no reason shows" : reason) + (errors.isEmpty() ? "" : " (" + join(errors) + ")"));
        }
        next.click();
    }

    /** The new order's number once the status line names it and its tab is selected; null until then. */
    private static String placedNumber(Client ui) {
        Target failed = ui.dialog(PLACE_ORDER_TITLE).label(PLACING_FAILED);
        if (failed.presence() != Target.Presence.NONE) {
            List<String> lines = messageLines(ui.dialog(PLACE_ORDER_TITLE).label(null));
            // OK closes the message: the checkout is back on its Review step, as the step found it
            quietly(() -> ui.answer(OK_ANSWER));
            throw new DriverException(Reason.APP_ERROR, lines == null ? PLACING_FAILED : join(lines));
        }
        String message = shown(ui.main().byId(STATUS_MESSAGE_ID));
        Matcher m = PLACED.matcher(message == null ? "" : message);
        if (!m.matches()) return null;
        String number = m.group(1);
        return ui.tab(number).states().contains("selected") ? number : null;
    }

    /** A message's lines, read within {@link OrderDeskLooks#LOOK}; null when it does not show. */
    private static List<String> messageLines(final Target labels) {
        return look(() -> {
            List<String> lines = labels.texts();
            return lines.isEmpty() ? null : lines;
        });
    }

    /** The message is {@code expected}: its whole text, or one of its lines, white space aside. */
    private static void messageIs(List<String> lines, String expected, String what) {
        String want = oneLine(expected);
        String all = oneLine(String.join(" ", lines));
        if (want.equals(all)) return;
        for (String line : lines) if (want.equals(oneLine(line))) return;
        throw new AssertionError(what + " reads " + quoted(all) + ", expected " + quoted(want));
    }

    private static Target checkout(Client ui) {
        return ui.dialog(CHECKOUT_TITLE + "*");
    }

    private static Target creditWarning(Client ui) {
        return ui.dialog(CREDIT_LIMIT_TITLE).label(null);
    }

    /** The PO number field as it shows: "PO number", or "PO number (optional)"; a card-only customer has none. */
    private static Target poField(Client ui) {
        Target required = ui.dialog().field(PO_NUMBER_FIELD);
        if (required.presence() != Target.Presence.NONE) return required;
        Target optional = ui.dialog().field(PO_NUMBER_OPTIONAL_FIELD);
        if (optional.presence() != Target.Presence.NONE) return optional;
        throw new DriverException(Reason.NOT_FOUND, "No PO number field shows: " + invoiceReason(ui));
    }

    private static Target fieldTarget(Client ui, Field f) {
        if (f.combo) return ui.dialog().combo(f.captions.get(0));
        if (PO_NUMBER_FIELD.equals(f.name)) return poField(ui);
        return ui.dialog().field(f.captions.get(0));
    }

    /** Why Invoice is disabled: its note ("Café Lumen pays by card"). */
    private static String invoiceReason(Client ui) {
        String note = shown(ui.dialog().byId(INVOICE_NOTE_ID));
        return note == null ? "Invoice (30 days) is disabled" : note;
    }

    private static Target control(Client ui, String control) {
        if (NEXT_BUTTON.equals(control)) {
            Step now = requireCheckout(ui);
            if (now == Step.REVIEW) {
                throw new AssertionError("The checkout is on its Review step: there, Next is " + PLACE_ORDER_BUTTON);
            }
            return ui.dialog().button(NEXT_BUTTON);
        }
        if (EXPRESS.equals(control)) {
            requireOn(ui, Step.SHIPPING, EXPRESS);
            return ui.dialog().radio(EXPRESS_RADIO);
        }
        requireOn(ui, Step.PAYMENT, INVOICE);
        return ui.dialog().radio(INVOICE_RADIO);
    }

    private static Target reasonOf(Client ui, String control) {
        if (NEXT_BUTTON.equals(control)) return ui.dialog().byId(NEXT_REASON_ID);
        if (EXPRESS.equals(control)) return ui.dialog().label(EXPRESS_REASON);
        return ui.dialog().byId(INVOICE_NOTE_ID);
    }

    static Field field(String field) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (Field f : FIELDS) {
            words.put(f.name, f.name);
            for (String c : f.captions) words.put(c, f.name);
            if (f.errorId != null) words.put(f.errorId, f.name);
        }
        words.put("PO", PO_NUMBER_FIELD);   // what a tester calls it; "po" alone would fit Postal code too
        String name = resolve(field, words, "checkout field", "checkout fields", "The checkout's fields");
        for (Field f : FIELDS) if (f.name.equals(name)) return f;
        throw new IllegalStateException(name);
    }

    static Step step(String step) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (Step s : Step.values()) {
            words.put(s.word, s.word);
            if (s.heading != null) words.put(s.heading, s.word);
            words.put(String.valueOf(s.ordinal() + 1), s.word);
        }
        String word = resolve(step, words, "checkout step", "checkout steps", "The checkout's steps");
        for (Step s : Step.values()) if (s.word.equals(word)) return s;
        throw new IllegalStateException(word);
    }

    private static String shippingMethod(String method) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        words.put(STANDARD, STANDARD);
        words.put(STANDARD_RADIO, STANDARD);
        words.put(STANDARD_PRICE_ID, STANDARD);
        words.put(EXPRESS, EXPRESS);
        words.put(EXPRESS_RADIO, EXPRESS);
        words.put(EXPRESS_PRICE_ID, EXPRESS);
        return resolve(method, words, "shipping method", "shipping methods", "The shipping methods");
    }

    private static String radioOf(String method) {
        return STANDARD.equals(method) ? STANDARD_RADIO : EXPRESS_RADIO;
    }

    private static String paymentMethod(String method) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        words.put(INVOICE, INVOICE);
        words.put(INVOICE_RADIO, INVOICE);
        words.put(INVOICE_NOTE_ID, INVOICE);
        words.put(CARD, CARD);
        return resolve(method, words, "way to pay", "ways to pay", "The ways to pay");
    }

    private static String section(String section) {
        Map<String, String> words = new LinkedHashMap<String, String>();
        for (String s : SECTIONS) words.put(s, s);
        words.put(ADDRESS_STEP, DELIVERY_ADDRESS_SECTION);
        return resolve(section, words, "review section", "review sections", "The review's sections");
    }

    /** Runs a step that puts the screen back after a failure; its own failure must not hide the first one. */
    private static void quietly(Runnable undo) {
        try {
            undo.run();
        } catch (RuntimeException ignored) {
            // the step's own failure is the one to report
        }
    }
}
