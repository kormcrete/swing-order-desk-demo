package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.FakeApp.label;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.node;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.withState;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The checkout's steps and checks on a stand-in app ({@link CheckoutFake}): the steps each takes, how it finds a field, a
 * shipping method, a way to pay or an answer by the tester's words, and how it fails, in words, when the checkout or its
 * step is not showing, a step of the app blocks it, or what the app shows is not what was expected.
 */
class OrderDeskCheckoutStepsTest {

    private static final String NO = "'scope':['tab:New order*']";
    private static final String D = "'window':'dialog'";

    // ---- going through the checkout

    @Test
    void startCheckoutPressesCheckout() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.startCheckout(app.client());

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{" + NO + ",'role':'button','name':'Checkout…'}}]"));
    }

    @Test
    void startCheckoutFailsWithTheReasonBesideADisabledCheckout() {
        CheckoutFake app = new CheckoutFake("Address");
        app.answer("button 'Checkout…'", withState(node("button", "Checkout…"), "disabled"));
        app.answer("element #checkoutReason", label("checkoutReason", "The cart is empty"));

        Throwable disabled = catchThrowable(() -> OrderDeskCheckoutSteps.startCheckout(app.client()));

        assertThat(disabled).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Checkout… is disabled: The cart is empty");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void checkoutWithPoGoesPastTheAddressAndTheShippingAndTypesThePo() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskSteps.checkoutWithPo(app.client(), "PO-7781");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Checkout…'}},"
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Next'}},"
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Next'}},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'PO number'},'text':'PO-7781'},"
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Next'}}]"));
    }

    @Test
    void checkoutWithPoTypesIntoTheOptionalPoNumberOfACustomerThatNeedsNone() {
        CheckoutFake app = new CheckoutFake("Address");
        app.forget("dialog text 'PO number'");
        app.answer("dialog text 'PO number (optional)'", node("text", "PO number (optional)"));

        OrderDeskSteps.checkoutWithPo(app.client(), "PO-B12");

        assertThat(app.steps().get(3)).isEqualTo(json("{'action':'type','target':{" + D + ",'role':'text',"
                + "'name':'PO number (optional)'},'text':'PO-B12'}"));
    }

    @Test
    void checkoutWithPoFailsWithTheAppsReasonWhenTheAddressBlocksItAndCancelsTheCheckout() {
        // Elm Street Studio has no postal code: the Address step shows the error at once and Next is disabled
        CheckoutFake app = new CheckoutFake("Address");
        app.answer("dialog button 'Next'", withState(node("button", "Next"), "disabled"));
        app.answer("dialog element #nextReason", label("nextReason", "Fix the fields marked in red"));
        app.answer("dialog element #postalCodeError", label("postalCodeError", "Postal code is required"));

        Throwable blocked = catchThrowable(() -> OrderDeskSteps.checkoutWithPo(app.client(), "PO-1"));

        assertThat(blocked).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Next is disabled on the"
                + " checkout's Address step: Fix the fields marked in red (Postal code: Postal code is required)");
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Checkout…'}},"
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Cancel'}}]"));
    }

    @Test
    void checkoutWithPoForACardOnlyCustomerFailsWithItsNoteAndCancelsTheCheckout() {
        CheckoutFake app = new CheckoutFake("Address");
        app.answer("dialog radio 'Invoice (30 days)'", withState(node("radio", "Invoice (30 days)"), "disabled"));
        app.answer("dialog element #invoiceNote", label("invoiceNote", "Café Lumen pays by card"));

        Throwable card = catchThrowable(() -> OrderDeskSteps.checkoutWithPo(app.client(), "PO-1"));

        assertThat(card).hasMessageContaining("No PO number to give: Café Lumen pays by card").hasMessageContaining("Pay by card");
        assertThat(app.steps().get(app.steps().size() - 1))
                .isEqualTo(json("{'action':'click','target':{" + D + ",'role':'button','name':'Cancel'}}"));
    }

    @Test
    void checkoutWithPoFailsFastWhenItsScreenIsNotShowing() {
        FakeApp nothing = new FakeApp();
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskSteps.checkoutWithPo(nothing.client(), "PO-1"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).hasMessage("NOT_FOUND: " + OrderDeskNewOrderSteps.NO_NEW_ORDER);
        assertThat(nothing.steps()).isEmpty();
    }

    @Test
    void setCheckoutFieldTypesTheValueThenLeavesTheFieldSoItsErrorShows() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.setCheckoutField(app.client(), "street", "Elmstraße 9");
        OrderDeskCheckoutSteps.setCheckoutField(app.client(), "Postal code", "none");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'type','target':{" + D + ",'role':'text','name':'Street'},'text':'Elmstraße 9'},"
                + "{'action':'press','keys':'Tab','target':{" + D + ",'role':'text','name':'Street'}},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'Postal code'},'text':''},"
                + "{'action':'press','keys':'Tab','target':{" + D + ",'role':'text','name':'Postal code'}}]"));
    }

    @Test
    void setCheckoutFieldChoosesACountryFromItsListByTheStartOfItsName() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.setCheckoutField(app.client(), "country", "neth");

        assertThat(app.steps()).isEqualTo(json("[{'action':'select','target':{" + D + ",'role':'combo','name':'Country'},"
                + "'items':['Netherlands']}]"));
    }

    @Test
    void setCheckoutFieldOnAnotherStepFailsFastSayingWhereTheFieldIs() {
        CheckoutFake app = new CheckoutFake("Shipping");
        long start = System.currentTimeMillis();

        Throwable elsewhere = catchThrowable(() -> OrderDeskCheckoutSteps.setCheckoutField(app.client(), "Street", "x"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(elsewhere).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: Street is on the checkout's Address"
                + " step, and the checkout shows its Shipping step: go back with \"Go back a checkout step\"");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void partialFieldNameResolvesOrNamesChoices() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.setCheckoutField(app.client(), "post", "10115");
        Throwable two = catchThrowable(() -> OrderDeskCheckoutSteps.setCheckoutField(app.client(), "p", "1"));
        Throwable none = catchThrowable(() -> OrderDeskCheckoutSteps.setCheckoutField(app.client(), "zip", "1"));

        assertThat(app.steps().get(0).get("target").get("name").asText()).isEqualTo("Postal code");
        assertThat(two).isInstanceOf(DriverException.class).hasMessageContaining("“p” fits 3 checkout fields: Postal code;"
                + " Phone; PO number");
        assertThat(((DriverException) two).reason()).isEqualTo(Reason.AMBIGUOUS);
        assertThat(none).hasMessageContaining("No checkout field fits “zip”").hasMessageContaining("Name; Street; Postal code");
        assertThat(OrderDeskCheckoutSteps.field("note").name).isEqualTo("Delivery note");
        assertThat(OrderDeskCheckoutSteps.field("postalCode").name).isEqualTo("Postal code");
        assertThat(OrderDeskCheckoutSteps.field("name on card").name).isEqualTo("Name on card");
        assertThat(OrderDeskCheckoutSteps.field("PO number (optional)").name).isEqualTo("PO number");
        assertThat(OrderDeskCheckoutSteps.field("PO").name).isEqualTo("PO number");
    }

    @Test
    void setCheckoutFieldOfADisabledCardFieldSaysChooseCardFirst() {
        CheckoutFake app = new CheckoutFake("Payment");
        app.answer("dialog text 'Card number'", withState(node("text", "Card number"), "disabled"));

        Throwable disabled = catchThrowable(() -> OrderDeskCheckoutSteps.setCheckoutField(app.client(), "card number", "4242"));

        assertThat(disabled).hasMessageContaining("Card number is disabled: Invoice is chosen").hasMessageContaining("Pay by card");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void chooseShippingTakesTheTestersWordForTheMethod() {
        CheckoutFake app = new CheckoutFake("Shipping");

        OrderDeskCheckoutSteps.chooseShipping(app.client(), "express");
        OrderDeskCheckoutSteps.chooseShipping(app.client(), "Standard · 3–5 business days");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'role':'radio','name':'Express · next business day'}},"
                + "{'action':'click','target':{" + D + ",'role':'radio','name':'Standard · 3–5 business days'}}]"));
    }

    @Test
    void chooseShippingExpressOutsideGermanyAndAustriaFailsWithTheAppsReason() {
        CheckoutFake app = new CheckoutFake("Shipping");
        app.answer("dialog radio 'Express · next business day'", withState(node("radio", "Express · next business day"), "disabled"));
        app.answer("dialog label 'Express delivers to Germany and Austria only'", label("Express delivers to Germany and Austria only"));

        Throwable refused = catchThrowable(() -> OrderDeskCheckoutSteps.chooseShipping(app.client(), "Express"));

        assertThat(refused).hasMessage("NOT_ACTIONABLE: Express cannot be chosen: Express delivers to Germany and Austria only");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void chooseShippingFailsFastWhenItsScreenIsNotShowing() {
        CheckoutFake app = new CheckoutFake("Payment");
        long start = System.currentTimeMillis();

        Throwable elsewhere = catchThrowable(() -> OrderDeskCheckoutSteps.chooseShipping(app.client(), "Express"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(elsewhere).hasMessage("NOT_FOUND: Shipping is on the checkout's Shipping step, and the checkout shows its"
                + " Payment step: go back with \"Go back a checkout step\"");
    }

    @Test
    void payByInvoiceChoosesInvoiceTypesThePoAndLeavesIt() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskCheckoutSteps.payByInvoice(app.client(), "PO-7781");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'role':'radio','name':'Invoice (30 days)'}},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'PO number'},'text':'PO-7781'},"
                + "{'action':'press','keys':'Tab','target':{" + D + ",'role':'text','name':'PO number'}}]"));
    }

    @Test
    void payByInvoiceForACardOnlyCustomerFailsWithItsNote() {
        CheckoutFake app = new CheckoutFake("Payment");
        app.answer("dialog radio 'Invoice (30 days)'", withState(node("radio", "Invoice (30 days)"), "disabled"));
        app.answer("dialog element #invoiceNote", label("invoiceNote", "Café Lumen pays by card"));

        Throwable card = catchThrowable(() -> OrderDeskCheckoutSteps.payByInvoice(app.client(), "none"));

        assertThat(card).hasMessage("NOT_ACTIONABLE: Invoice cannot be chosen: Café Lumen pays by card");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void payByCardChoosesCardAndFillsItsFields() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskCheckoutSteps.payByCard(app.client(), "4242424242424242", "12/29", "Ada Lindqvist");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'role':'radio','name':'Card'}},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'Card number'},'text':'4242424242424242'},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'Expiry'},'text':'12/29'},"
                + "{'action':'type','target':{" + D + ",'role':'text','name':'Name on card'},'text':'Ada Lindqvist'},"
                + "{'action':'press','keys':'Tab','target':{" + D + ",'role':'text','name':'Name on card'}}]"));
    }

    @Test
    void payByCardNoneLeavesThatFieldEmpty() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskCheckoutSteps.payByCard(app.client(), "4242", "09/25", "none");

        assertThat(app.steps().get(3)).isEqualTo(json("{'action':'type','target':{" + D + ",'role':'text','name':'Name on card'},"
                + "'text':''}"));
    }

    @Test
    void checkoutNextPressesNextOrFailsWithItsReason() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskCheckoutSteps.checkoutNext(app.client());
        app.answer("dialog button 'Next'", withState(node("button", "Next"), "disabled"));
        app.answer("dialog element #nextReason", label("nextReason", "Complete the card details"));
        app.answer("dialog element #expiryError", label("expiryError", "This card has expired"));
        Throwable disabled = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutNext(app.client()));

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{" + D + ",'role':'button','name':'Next'}}]"));
        assertThat(disabled).hasMessage("NOT_ACTIONABLE: Next is disabled on the checkout's Payment step: Complete the card"
                + " details (Expiry: This card has expired)");
    }

    @Test
    void checkoutNextOnTheReviewSaysPlaceOrderComesNext() {
        CheckoutFake app = new CheckoutFake("Review");

        Throwable review = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutNext(app.client()));

        assertThat(review).hasMessageContaining("Place the order");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void checkoutBackPressesBackExceptOnTheFirstStep() {
        CheckoutFake app = new CheckoutFake("Shipping");

        OrderDeskCheckoutSteps.checkoutBack(app.client());
        app.on("Address");
        Throwable first = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutBack(app.client()));

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{" + D + ",'role':'button','name':'Back'}}]"));
        assertThat(first).hasMessageContaining("first step");
    }

    @Test
    void editReviewSectionPressesThatSectionsEdit() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.editReviewSection(app.client(), "address");
        OrderDeskCheckoutSteps.editReviewSection(app.client(), "pay");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'scope':['section:Delivery address'],'role':'button','name':'Edit'}},"
                + "{'action':'click','target':{" + D + ",'scope':['section:Payment'],'role':'button','name':'Edit'}}]"));
    }

    @Test
    void answerCreditLimitTakesTheTestersWords() {
        CheckoutFake app = new CheckoutFake("Payment").creditWarning();

        OrderDeskCheckoutSteps.answerCreditLimit(app.client(), "card");
        app.creditWarning();
        OrderDeskCheckoutSteps.answerCreditLimit(app.client(), "continue");

        assertThat(app.steps()).isEqualTo(json("[{'action':'answer','button':'Pay by card instead'},"
                + "{'action':'answer','button':'Continue with invoice'}]"));
    }

    @Test
    void answerCreditLimitOnTwoMatchesFailsNamingBoth() {
        // "c": Card (Pay by card instead) and Continue with invoice
        CheckoutFake app = new CheckoutFake("Payment").creditWarning();

        Throwable two = catchThrowable(() -> OrderDeskCheckoutSteps.answerCreditLimit(app.client(), "c"));

        assertThat(two).hasMessageContaining("“c” fits 2 answers: Pay by card instead; Continue with invoice");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void answerCreditLimitFailsFastWhenItsScreenIsNotShowing() {
        CheckoutFake app = new CheckoutFake("Payment");
        long start = System.currentTimeMillis();

        Throwable none = catchThrowable(() -> OrderDeskCheckoutSteps.answerCreditLimit(app.client(), "card"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(none).hasMessage("NOT_FOUND: " + OrderDeskCheckoutSteps.NO_CREDIT_WARNING);
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void aCheckoutStepWhileTheCreditWarningIsOpenSaysAnswerItFirst() {
        CheckoutFake app = new CheckoutFake("Payment").creditWarning();

        Throwable open = catchThrowable(() -> OrderDeskCheckoutSteps.cancelCheckout(app.client()));

        assertThat(open).hasMessageContaining("The credit-limit warning is open: answer it first");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void cancelCheckoutPressesCancel() {
        CheckoutFake app = new CheckoutFake("Shipping");

        OrderDeskCheckoutSteps.cancelCheckout(app.client());

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{" + D + ",'role':'button','name':'Cancel'}}]"));
    }

    @Test
    void cancelPlaceOrderAnswersCancelAtTheConfirmation() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.cancelPlaceOrder(app.client());

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Place order'}},"
                + "{'action':'answer','button':'Cancel'}]"));
    }

    @Test
    void placeOrderConfirmsAndReturnsTheNumberAndWhatPlacingShowed() {
        CheckoutFake app = new CheckoutFake("Review");

        Map<String, Object> made = OrderDeskCheckoutSteps.placeOrderOutcome(app.client());

        assertThat(made).containsEntry("number", "ORD-1640").containsEntry("placing", "Order for €41.16 · Ada Shop Ltd");
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + D + ",'role':'button','name':'Place order'}},"
                + "{'action':'answer','button':'Place order'}]"));
    }

    @Test
    void placeOrderWhenPlacingFailsSaysWhatTheAppSaid() {
        CheckoutFake app = new CheckoutFake("Review").placingFails();

        Throwable failed = catchThrowable(() -> OrderDeskCheckoutSteps.placeOrder(app.client()));

        assertThat(failed).isInstanceOf(DriverException.class).hasMessageContaining("The order could not be placed.")
                .hasMessageContaining("Stapler: only 0 left");
    }

    @Test
    void placeOrderFailsFastWhenItsScreenIsNotShowing() {
        CheckoutFake app = new CheckoutFake("Payment");
        long start = System.currentTimeMillis();

        Throwable early = catchThrowable(() -> OrderDeskCheckoutSteps.placeOrder(app.client()));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(early).hasMessage("NOT_FOUND: Place order is on the checkout's Review step, and the checkout shows its"
                + " Payment step: go on with \"Go on to the next checkout step\"");
        assertThat(app.steps()).isEmpty();
    }

    // ---- checks

    @Test
    void checkoutCustomerIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.checkoutCustomerIs(app.client(), "ada shop ltd");
        Throwable other = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutCustomerIs(app.client(), "Birch & Co"));
        Throwable start = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutCustomerIs(app.client(), "Ada"));

        assertThat(other).isInstanceOf(AssertionError.class).hasMessage("The checkout is for Ada Shop Ltd, expected Birch & Co");
        assertThat(start).hasMessageContaining("give the whole name");
    }

    @Test
    void checkoutStepIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Shipping");

        OrderDeskCheckoutSteps.checkoutStepIs(app.client(), "shipping");
        OrderDeskCheckoutSteps.checkoutStepIs(app.client(), "Shipping method");
        OrderDeskCheckoutSteps.checkoutStepIs(app.client(), "2");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutStepIs(app.client(), "Payment"));
        app.on("Review");
        OrderDeskCheckoutSteps.checkoutStepIs(app.client(), "review");

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The checkout is on its Shipping step, expected Payment");
    }

    @Test
    void checkoutStepIsFailsFastWhenNoCheckoutIsOpen() {
        FakeApp nothing = new FakeApp();
        long start = System.currentTimeMillis();

        Throwable none = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutStepIs(nothing.client(), "Address"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(none).hasMessage("NOT_FOUND: " + OrderDeskCheckoutSteps.NO_CHECKOUT);
    }

    @Test
    void checkoutFieldIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskCheckoutSteps.checkoutFieldIs(app.client(), "Street", "Torstraße 118");
        OrderDeskCheckoutSteps.checkoutFieldIs(app.client(), "country", "Germany");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutFieldIs(app.client(), "street", "Elmstraße 7"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The checkout's Street reads “Torstraße 118”, expected “Elmstraße 7”");
    }

    @Test
    void checkoutFieldErrorIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Address");
        app.answer("dialog element #postalCodeError", label("postalCodeError", "Germany uses a 5-digit postal code"));

        OrderDeskCheckoutSteps.checkoutFieldErrorIs(app.client(), "postalCode", "Germany uses a 5-digit postal code");
        OrderDeskCheckoutSteps.checkoutFieldErrorIs(app.client(), "Street", "none");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutFieldErrorIs(app.client(), "Postal code", "none"));
        Throwable other = catchThrowable(() -> OrderDeskCheckoutSteps.checkoutFieldErrorIs(app.client(), "Postal code",
                "Postal code is required"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The checkout's Postal code error reads “Germany uses a 5-digit postal code”, expected none");
        assertThat(other).hasMessage("The checkout's Postal code error reads “Germany uses a 5-digit postal code”,"
                + " expected “Postal code is required”");
    }

    @Test
    void shippingPriceIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Shipping");

        OrderDeskCheckoutSteps.shippingPriceIs(app.client(), "express", "€14.90");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.shippingPriceIs(app.client(), "Standard", "Free"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The Standard shipping price reads “€4.90”, expected “Free”");
    }

    @Test
    void paymentNoteIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskCheckoutSteps.paymentNoteIs(app.client(), "invoice", "Credit limit €2,000.00 · owes €450.00");
        OrderDeskCheckoutSteps.paymentNoteIs(app.client(), "Card", "Debit or credit card");
        Throwable invoice = catchThrowable(() -> OrderDeskCheckoutSteps.paymentNoteIs(app.client(), "Invoice", "Card only"));
        Throwable card = catchThrowable(() -> OrderDeskCheckoutSteps.paymentNoteIs(app.client(), "card", "Credit card"));

        assertThat(invoice).hasMessage("The note under Invoice reads “Credit limit €2,000.00 · owes €450.00”, expected “Card only”");
        assertThat(card).hasMessage("The note under Card reads “Debit or credit card”, expected “Credit card”");
    }

    @Test
    void creditLimitWarningIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Payment").creditWarning();

        OrderDeskCheckoutSteps.creditLimitWarningIs(app.client(), "Owes €940.00 now, this order adds €96.60.");
        OrderDeskCheckoutSteps.creditLimitWarningIs(app.client(), "This order takes Birch & Co to €1,036.60 of its"
                + " €1,000.00 credit limit.\nOwes €940.00 now, this order adds €96.60.");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.creditLimitWarningIs(app.client(), "Over the limit"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The credit-limit warning reads “This order takes"
                + " Birch & Co to €1,036.60 of its €1,000.00 credit limit. Owes €940.00 now, this order adds €96.60.”,"
                + " expected “Over the limit”");
    }

    @Test
    void placeOrderConfirmationIsReadsItThenAnswersCancelEvenWhenItDiffers() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.placeOrderConfirmationIs(app.client(), "Place order for €41.16?");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.placeOrderConfirmationIs(app.client(), "Place order for €1.00?"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The place-order confirmation reads “Place order for"
                + " €41.16? Order for Ada Shop Ltd, 12 items.”, expected “Place order for €1.00?”");
        String click = "{'action':'click','target':{" + D + ",'role':'button','name':'Place order'}},";
        assertThat(app.steps()).isEqualTo(json("[" + click + "{'action':'answer','button':'Cancel'}," + click
                + "{'action':'answer','button':'Cancel'}]"));
    }

    @Test
    void reviewSectionIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.reviewSectionIs(app.client(), "shipping", "Standard · 3–5 business days · €4.90");
        OrderDeskCheckoutSteps.reviewSectionIs(app.client(), "address",
                "Ada Lindqvist; Torstraße 118; 10119 Berlin; Germany; +49 30 5550 1188");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.reviewSectionIs(app.client(), "Payment", "Card ending 4242"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The review's Payment reads “Invoice (30 days) · PO PO-7781”, expected “Card ending 4242”");
    }

    @Test
    void reviewLinesAreFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.reviewLinesAre(app.client(), "A4 paper, 500 sheets; Stapler; Gel pen, black");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.reviewLinesAre(app.client(), "Stapler"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The review lists A4 paper, 500 sheets; Stapler; Gel pen, black, expected Stapler");
    }

    @Test
    void reviewLineIsFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.reviewLineIs(app.client(), "stapler", "quantity", "1");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.reviewLineIs(app.client(), "Stapler", "Line total", "€25.00"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("Stapler's Line total on the review reads “€12.50”, expected “€25.00”");
    }

    @Test
    void reviewLineIsOnTwoMatchesFailsNamingBoth() {
        CheckoutFake app = new CheckoutFake("Review");
        app.answer("dialog table", FakeApp.table(java.util.Arrays.asList("Product", "Qty", "Price", "Line total"), 2,
                java.util.Arrays.asList(java.util.Arrays.asList("Gel pen, black", "1", "€1.20", "€1.20"),
                        java.util.Arrays.asList("Gel pen, blue", "1", "€1.20", "€1.20"))));

        Throwable two = catchThrowable(() -> OrderDeskCheckoutSteps.reviewLineIs(app.client(), "gel pen", "Qty", "1"));

        assertThat(two).hasMessageContaining("“gel pen” fits 2 order lines: Gel pen, black; Gel pen, blue");
    }

    @Test
    void reviewAmountIsReadsInsideTheTotalsAndFailsWithExpectedAndActual() {
        CheckoutFake app = new CheckoutFake("Review");

        OrderDeskCheckoutSteps.reviewAmountIs(app.client(), "total", "€41.16");
        OrderDeskCheckoutSteps.reviewAmountIs(app.client(), "Volume discount", "none");
        Throwable wrong = catchThrowable(() -> OrderDeskCheckoutSteps.reviewAmountIs(app.client(), "Shipping", "Free"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The review's Shipping reads “€4.90”, expected “Free”");
        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'expect','target':{" + D + ",'scope':['id:totals'],"
                + "'role':'label','name':'Total'},'text':'€41.16','timeout':1000}"));
    }

    @Test
    void buttonIsDisabledReadsTheCheckoutsNextExpressAndInvoice() {
        CheckoutFake app = new CheckoutFake("Address");
        Client ui = app.client();
        app.answer("dialog button 'Next'", withState(node("button", "Next"), "disabled"));
        app.answer("dialog element #nextReason", label("nextReason", "Fix the fields marked in red"));

        OrderDeskNewOrderSteps.buttonIsDisabled(ui, "Next", "Fix the fields marked in red");
        Throwable next = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(ui, "next", "Enter the PO number"));
        app.on("Shipping");
        app.answer("dialog radio 'Express · next business day'", withState(node("radio", "Express · next business day"), "disabled"));
        app.answer("dialog label 'Express delivers to Germany and Austria only'", label("Express delivers to Germany and Austria only"));
        OrderDeskNewOrderSteps.buttonIsDisabled(ui, "Express · next business day", "Express delivers to Germany and Austria only");
        Throwable express = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(ui, "express", "Outside Germany"));
        app.on("Payment");
        app.answer("dialog radio 'Invoice (30 days)'", withState(node("radio", "Invoice (30 days)"), "disabled"));
        app.answer("dialog element #invoiceNote", label("invoiceNote", "Café Lumen pays by card"));
        OrderDeskNewOrderSteps.buttonIsDisabled(ui, "invoice", "Café Lumen pays by card");

        assertThat(next).isInstanceOf(AssertionError.class)
                .hasMessage("Next's reason reads “Fix the fields marked in red”, expected “Enter the PO number”");
        assertThat(express).hasMessage("Express's reason reads “Express delivers to Germany and Austria only”, expected"
                + " “Outside Germany”");
    }

    @Test
    void buttonIsEnabledOfADisabledNextFailsNamingItsReason() {
        CheckoutFake app = new CheckoutFake("Payment");

        OrderDeskNewOrderSteps.buttonIsEnabled(app.client(), "Next");
        app.answer("dialog button 'Next'", withState(node("button", "Next"), "disabled"));
        app.answer("dialog element #nextReason", label("nextReason", "Enter the PO number"));
        Throwable disabled = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsEnabled(app.client(), "Next"));

        assertThat(disabled).isInstanceOf(AssertionError.class).hasMessage("Next is disabled (“Enter the PO number”), expected enabled");
    }
}
