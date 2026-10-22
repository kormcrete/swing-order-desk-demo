package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.FakeApp.modal;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * An order's own tab's steps and checks on a stand-in app ({@link OrderTabFake}): the steps each takes (Save receipt…
 * and its file chooser, the question about a file that exists, Cancel order and its question), what each check reads
 * (the status, the facts, the lines, the Delivery and Invoice tabs, which a check shows for the read and then hides
 * again), and how each fails, in words: the order's tab not open or not selected, an order that cannot be cancelled, a
 * receipt the app cannot save, a value that is not what was expected.
 */
class OrderDeskOrderTabStepsTest {

    private static final String TO_TAB = "{'action':'click','target':{'role':'tab','name':'ORD-1639'}}";
    private static final String SAVE = "{'action':'click','target':{'scope':['tab:ORD-1639'],'role':'button','name':'Save receipt…'}}";
    private static final String CANCEL = "{'action':'click','target':{'scope':['tab:ORD-1639'],'role':'button','name':'Cancel order'}}";

    private static String chooseFile(String path) {
        return "{'action':'chooseFile','path':'" + path + "'}";
    }

    private static String answer(String button) {
        return "{'action':'answer','button':'" + button + "'}";
    }

    private static String subTab(String name) {
        return "{'action':'click','target':{'scope':['tab:ORD-1639'],'role':'tab','name':'" + name + "'}}";
    }

    // ---- save the receipt

    @Test
    void saveReceiptGoesToTheOrdersTabPressesSaveReceiptAndChoosesTheFile() {
        OrderTabFake app = new OrderTabFake();

        Map<String, Object> saved = OrderDeskOrderTabSteps.saveReceipt(app.client(), "ORD-1639", "receipts/ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("receipts/ORD-1639") + "]"));
        // .txt is added when missing; the path is the one the status line names
        assertThat(saved).containsEntry("path", "/home/ada/receipts/ORD-1639.txt").containsEntry("replaced", false);
    }

    @Test
    void saveReceiptOverAFileThatExistsAnswersReplace() {
        OrderTabFake app = new OrderTabFake().withFile("ORD-1639.txt");

        Map<String, Object> saved = OrderDeskOrderTabSteps.saveReceipt(app.client(), "1639", "ORD-1639.txt");

        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("ORD-1639.txt") + "," + answer("Replace") + "]"));
        assertThat(saved).containsEntry("path", "/home/ada/ORD-1639.txt").containsEntry("replaced", true);
    }

    @Test
    void saveReceiptTheAppCannotWriteFailsWithItsMessageAndLeavesTheScreenAsFound() {
        // started on the Orders tab: the error is answered OK and the Orders tab shows again
        OrderTabFake app = new OrderTabFake().showTabs("Orders", "Orders", "ORD-1639").failing("/nope/ORD-1639.txt");

        Throwable failed = catchThrowable(() -> OrderDeskOrderTabSteps.saveReceipt(app.client(), "ORD-1639", "/nope/ORD-1639"));

        assertThat(failed).isInstanceOf(DriverException.class)
                .hasMessage("NOT_ACTIONABLE: Save receipt says “The receipt could not be saved: /nope/ORD-1639.txt”");
        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("/nope/ORD-1639") + "," + answer("OK")
                + ",{'action':'click','target':{'role':'tab','name':'Orders'}}]"));
    }

    @Test
    void saveReceiptWithNoFileFailsBeforeAnythingIsClicked() {
        OrderTabFake app = new OrderTabFake();

        Throwable none = catchThrowable(() -> OrderDeskOrderTabSteps.saveReceipt(app.client(), "ORD-1639", " "));

        assertThat(none).isInstanceOf(DriverException.class)
                .hasMessage("BAD_INPUT: Give the file to save the receipt as, e.g. receipts/ORD-1639.txt");
        assertThat(app.steps()).isEqualTo(json("[]"));
    }

    @Test
    void theMessageIsReadFromTheTopOfWhatTheActionChanged() {
        // the outcome's text: a nested "message" (a text that changed) is not the dialog's message
        String changed = "{\"texts\":{\"message\":\"x\"},\"opened\":{\"dialogs\":[\"Save receipt\"]},"
                + "\"message\":\"ORD-1639.txt already exists.\\nDo you want \\\"it\\\"? \\u00e9\"}";

        assertThat(OrderDeskOrderTabSteps.topLevelText(changed, "message"))
                .isEqualTo("ORD-1639.txt already exists.\nDo you want \"it\"? é");
        assertThat(OrderDeskOrderTabSteps.topLevelText("{\"texts\":{\"message\":\"x\"}}", "message")).isNull();
        assertThat(OrderDeskOrderTabSteps.topLevelText("{}", "message")).isNull();
    }

    @Test
    void keepExistingReceiptAnswersCancelToTheReplaceQuestion() {
        OrderTabFake app = new OrderTabFake().withFile("ORD-1639.txt");

        OrderDeskOrderTabSteps.keepExistingReceipt(app.client(), "ORD-1639", "ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("ORD-1639") + "," + answer("Cancel") + "]"));
    }

    @Test
    void keepExistingReceiptOnAFileThatDidNotExistFailsSayingItWasSaved() {
        OrderTabFake app = new OrderTabFake();

        Throwable saved = catchThrowable(() -> OrderDeskOrderTabSteps.keepExistingReceipt(app.client(), "ORD-1639", "new.txt"));

        assertThat(saved).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Save receipt asked nothing: new.txt"
                + " did not exist, so the receipt was saved at once (the status line reads “Receipt saved to /home/ada/new.txt”)");
    }

    @Test
    void receiptSaveErrorIsReadsTheErrorAndAnswersIt() {
        // the whole message, the sentence before the colon, or the reason after it
        for (String expected : new String[]{"The receipt could not be saved: /nope/x.txt", "The receipt could not be saved",
                "The receipt could not be saved:", "/nope/x.txt"}) {
            OrderTabFake app = new OrderTabFake().failing("/nope/x.txt");

            OrderDeskOrderTabSteps.receiptSaveErrorIs(app.client(), "ORD-1639", "/nope/x", expected);

            assertThat(app.steps()).as(expected).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("/nope/x") + "," + answer("OK") + "]"));
        }
    }

    @Test
    void receiptSaveErrorIsFailsWithExpectedAndActual() {
        OrderTabFake failing = new OrderTabFake().failing("/nope/x.txt");
        OrderTabFake writes = new OrderTabFake();

        Throwable other = catchThrowable(() -> OrderDeskOrderTabSteps.receiptSaveErrorIs(failing.client(), "ORD-1639", "/nope/x", "Disk full"));
        Throwable saved = catchThrowable(() -> OrderDeskOrderTabSteps.receiptSaveErrorIs(writes.client(), "ORD-1639", "x",
                "The receipt could not be saved"));

        assertThat(other).isInstanceOf(AssertionError.class)
                .hasMessage("Save receipt says “The receipt could not be saved: /nope/x.txt”, expected “Disk full”");
        assertThat(saved).isInstanceOf(AssertionError.class).hasMessage("The receipt was saved (the status line reads"
                + " “Receipt saved to /home/ada/x.txt”), expected the error “The receipt could not be saved”");
    }

    // ---- cancel the order

    @Test
    void aReceiptStepThatFailsSelectsTheTabThatWasSelectedAgain() {
        OrderTabFake saves = new OrderTabFake().showTabs("Orders", "Orders", "ORD-1639");
        OrderTabFake failing = new OrderTabFake().showTabs("Orders", "Orders", "ORD-1639").failing("/nope/x.txt");
        String back = ",{'action':'click','target':{'role':'tab','name':'Orders'}}]";

        catchThrowable(() -> OrderDeskOrderTabSteps.keepExistingReceipt(saves.client(), "ORD-1639", "new"));
        catchThrowable(() -> OrderDeskOrderTabSteps.receiptSaveErrorIs(failing.client(), "ORD-1639", "/nope/x", "Disk full"));

        assertThat(saves.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("new") + back));
        assertThat(failing.steps()).isEqualTo(json("[" + TO_TAB + "," + SAVE + "," + chooseFile("/nope/x") + "," + answer("OK") + back));
    }

    @Test
    void cancelOrderPressesCancelOrderAndConfirms() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.cancelOrder(app.client(), "ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + CANCEL + "," + answer("Cancel order") + "]"));
        assertThat(app.status()).isEqualTo("Cancelled");
    }

    @Test
    void keepOrderAnswersKeep() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.keepOrder(app.client(), "ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_TAB + "," + CANCEL + "," + answer("Keep") + "]"));
        assertThat(app.status()).isEqualTo("Placed");
    }

    @Test
    void cancelOrderOfAShippedOrderFailsWithTheAppsReasonBeforeClickingIt() {
        OrderTabFake app = new OrderTabFake("ORD-1636", "Shipped").showTabs("Orders", "Orders", "ORD-1636");

        Throwable shipped = catchThrowable(() -> OrderDeskOrderTabSteps.cancelOrder(app.client(), "ORD-1636"));

        assertThat(shipped).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Cancel order is disabled on"
                + " ORD-1636: “Only placed orders can be cancelled — this one is shipped”");
        // its tab was shown to read the button; the Orders tab shows again
        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'role':'tab','name':'ORD-1636'}},"
                + "{'action':'click','target':{'role':'tab','name':'Orders'}}]"));
    }

    // ---- which order: its tab open, selected

    @Test
    void partialOrderNameResolvesOrNamesChoices() {
        // the digits of the number, any case of it; one that fits no open order tab names those that are open
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderStatusIs(app.client(), "1639", "Placed");
        OrderDeskOrderTabSteps.orderStatusIs(app.client(), "ord-1639", "Placed");
        Throwable none = catchThrowable(() -> OrderDeskOrderTabSteps.cancelOrder(app.client(), "ORD-1638"));

        assertThat(none).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: No open order tab fits “ORD-1638”."
                + " The open order tabs: ORD-1639. Open it first with \"Open order ORD-1638\"");
        assertThat(app.steps()).isEqualTo(json("[]"));
    }

    @Test
    void orderItemIsOnTwoMatchesFailsNamingBoth() {
        OrderTabFake app = new OrderTabFake();

        Throwable two = catchThrowable(() -> OrderDeskOrderTabSteps.orderItemIs(app.client(), "ORD-1639", "gel pen", "Qty", "50"));

        assertThat(two).isInstanceOf(DriverException.class)
                .hasMessage("AMBIGUOUS: “gel pen” fits 2 lines: Gel pen, black; Gel pen, blue. Give more of the name");
    }

    @Test
    void anOrderTabCheckFailsFastWhenItsTabIsNotSelected() {
        OrderTabFake app = new OrderTabFake().showTabs("Orders", "Orders", "ORD-1639");
        long start = System.currentTimeMillis();

        Throwable elsewhere = catchThrowable(() -> OrderDeskOrderTabSteps.orderStatusIs(app.client(), "ORD-1639", "Placed"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(elsewhere).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: Order ORD-1639's tab is not"
                + " selected (Orders is). Switch to it first with \"Switch to the tab ORD-1639\"");
        assertThat(app.steps()).isEqualTo(json("[]"));
    }

    @Test
    void anOrderTabActFailsFastWhileADialogIsOpen() {
        OrderTabFake app = new OrderTabFake();
        app.windows(modal("Cancel order", "Cancel order ORD-1639?"));

        Throwable dialog = catchThrowable(() -> OrderDeskOrderTabSteps.saveReceipt(app.client(), "ORD-1639", "x"));

        assertThat(dialog).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: A dialog is open over the main"
                + " window (Cancel order: Cancel order ORD-1639?): answer or close it first");
        assertThat(app.steps()).isEqualTo(json("[]"));
    }

    @Test
    void openOrderTabsAreTheTabsTitledWithAnOrderNumber() {
        OrderTabFake app = new OrderTabFake().showTabs("ORD-1639", "Orders", "New order · Ada Shop Ltd", "ORD-1639");

        assertThat(OrderDeskOrderTabSteps.openOrderTabs(app.client())).containsExactly("ORD-1639");
    }

    // ---- the checks

    @Test
    void orderStatusIsReadsTheStatusAnyCase() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderStatusIs(app.client(), "ORD-1639", "placed");
    }

    @Test
    void orderStatusIsFailsWithExpectedAndActual() {
        OrderTabFake app = new OrderTabFake();

        Throwable wrong = catchThrowable(() -> OrderDeskOrderTabSteps.orderStatusIs(app.client(), "ORD-1639", "Shipped"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's status reads “Placed”, expected “Shipped”");
    }

    @Test
    void orderFactsAreTheWholeLineOrOneOfItsParts() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderFactsAre(app.client(), "ORD-1639", "Ada Shop Ltd · 9 Oct 2026 · €95.52");
        OrderDeskOrderTabSteps.orderFactsAre(app.client(), "ORD-1639", "Ada Shop Ltd");
        OrderDeskOrderTabSteps.orderFactsAre(app.client(), "ORD-1639", "€95.52");
        Throwable wrong = catchThrowable(() -> OrderDeskOrderTabSteps.orderFactsAre(app.client(), "ORD-1639", "Birch & Co"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's customer, date and total read"
                + " “Ada Shop Ltd · 9 Oct 2026 · €95.52”, expected “Birch & Co”");
    }

    @Test
    void orderItemsAreTheLinesInOrderByTheStartOfTheirNames() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderItemsAre(app.client(), "ORD-1639", "gel pen, black; A4; paper clips; Gel pen, blue");
        Throwable wrong = catchThrowable(() -> OrderDeskOrderTabSteps.orderItemsAre(app.client(), "ORD-1639", "A4; Gel pen, black"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's lines are Gel pen, black; A4 paper, 500"
                + " sheets; Paper clips, box of 100; Gel pen, blue, expected A4 paper, 500 sheets; Gel pen, black");
    }

    @Test
    void orderItemIsReadsTheLinesCellByItsSkuAndName() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderItemIs(app.client(), "ORD-1639", "paper clips", "Line total", "€3.00");
        // the SKU names the line too (the Check menu keys a line by it), and its own cell is checked as well
        OrderDeskOrderTabSteps.orderItemIs(app.client(), "ORD-1639", "pen-gel-blk", "quantity", "50");
        OrderDeskOrderTabSteps.orderItemIs(app.client(), "ORD-1639", "PEN-GEL-BLU", "SKU", "PEN-GEL-BLU");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'expect','target':{'scope':['tab:ORD-1639','tab:Items'],'role':'table','row':{'SKU':'DSK-CLIPS',"
                + "'Product':'Paper clips, box of 100'},'column':'Line total'},'text':'€3.00','timeout':1000},"
                + "{'action':'expect','target':{'scope':['tab:ORD-1639','tab:Items'],'role':'table','row':{'SKU':'PEN-GEL-BLK',"
                + "'Product':'Gel pen, black'},'column':'Qty'},'text':'50','timeout':1000},"
                + "{'action':'expect','target':{'scope':['tab:ORD-1639','tab:Items'],'role':'table','row':{'SKU':'PEN-GEL-BLU',"
                + "'Product':'Gel pen, blue'},'column':'SKU'},'text':'PEN-GEL-BLU','timeout':1000}]"));
    }

    @Test
    void orderItemIsFailsWithExpectedAndActual() {
        OrderTabFake app = new OrderTabFake();

        Throwable wrong = catchThrowable(() -> OrderDeskOrderTabSteps.orderItemIs(app.client(), "ORD-1639", "A4", "Qty", "5"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("A4 paper, 500 sheets's Qty on ORD-1639 reads “3”, expected “5”");
    }

    @Test
    void orderItemsSummaryIsTheWholeCountOrOneOfItsParts() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderItemsSummaryIs(app.client(), "ORD-1639", "56 units in 4 lines");
        OrderDeskOrderTabSteps.orderItemsSummaryIs(app.client(), "ORD-1639", "4 lines");
        Throwable wrong = catchThrowable(() -> OrderDeskOrderTabSteps.orderItemsSummaryIs(app.client(), "ORD-1639", "3 lines"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's units read “56 units in 4 lines”, expected “3 lines”");
    }

    @Test
    void anItemsCheckShowsTheItemsTabForTheReadThenTheTabThatShowedBefore() {
        OrderTabFake app = new OrderTabFake().onSubTab("Invoice");

        OrderDeskOrderTabSteps.orderItemsSummaryIs(app.client(), "ORD-1639", "4 lines");

        assertThat(app.steps()).isEqualTo(json("[" + subTab("Items") + "," + subTab("Invoice") + "]"));
    }

    @Test
    void orderDeliveryIsReadsTheDeliveryTabThenShowsTheTabThatShowedBefore() {
        OrderTabFake app = new OrderTabFake();

        OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "shipping", "Standard · 3–5 business days · €4.90");

        assertThat(app.steps()).isEqualTo(json("[" + subTab("Delivery") + ","
                + "{'action':'expect','target':{'scope':['tab:ORD-1639','tab:Delivery'],'role':'label','name':'Shipping method'},"
                + "'text':'Standard · 3–5 business days · €4.90','timeout':1000}," + subTab("Items") + "]"));
    }

    @Test
    void orderDeliveryIsTakesTheAddressOrOneOfItsLinesAndNoneForNoNote() {
        OrderTabFake app = new OrderTabFake().onSubTab("Delivery");

        OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "address",
                "Ada Lindqvist; Torstraße 118; 10119 Berlin; Germany; +49 30 5550 1188");
        OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "Delivery address", "10119 Berlin");
        OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "note", "none");
        OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "Delivery note", "—");
    }

    @Test
    void orderDeliveryIsFailsWithExpectedAndActual() {
        OrderTabFake app = new OrderTabFake().onSubTab("Delivery");

        Throwable address = catchThrowable(() -> OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "address", "Paris"));
        Throwable note = catchThrowable(() -> OrderDeskOrderTabSteps.orderDeliveryIs(app.client(), "ORD-1639", "note", "Ring twice."));

        assertThat(address).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's delivery address reads “Ada Lindqvist;"
                + " Torstraße 118; 10119 Berlin; Germany; +49 30 5550 1188”, expected “Paris”");
        assertThat(note).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's Delivery note reads “—”, expected “Ring twice.”");
    }

    @Test
    void orderInvoiceIsReadsAnAmountOrThePaymentByItsCaption() {
        OrderTabFake app = new OrderTabFake().onSubTab("Invoice");

        OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "Total", "€95.52");
        OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "date", "9 Oct 2026");
        OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "vat", "€15.92");

        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'expect','target':{'scope':['tab:ORD-1639','tab:Invoice'],"
                + "'role':'label','name':'Total'},'text':'€95.52','timeout':1000}"));
        assertThat(app.steps().get(1).get("target").get("name").asText()).isEqualTo("Order date");
        assertThat(app.steps().get(2).get("target").get("name").asText()).isEqualTo("VAT (20%)");
    }

    @Test
    void orderInvoiceIsFailsWithExpectedAndActual() {
        OrderTabFake app = new OrderTabFake().onSubTab("Invoice");

        Throwable total = catchThrowable(() -> OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "Total", "€1.00"));
        Throwable discount = catchThrowable(() -> OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "discount", "none"));

        assertThat(total).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's Total reads “€95.52”, expected “€1.00”");
        assertThat(discount).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's Volume discount reads “−€3.00”, expected none");
    }

    @Test
    void aSubTabCheckThatMustShowItsTabFailsFastWhileADialogIsOpen() {
        OrderTabFake app = new OrderTabFake();
        app.windows(modal("Save receipt", "ORD-1639.txt already exists."));

        Throwable dialog = catchThrowable(() -> OrderDeskOrderTabSteps.orderInvoiceIs(app.client(), "ORD-1639", "Total", "€95.52"));

        assertThat(dialog).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: A dialog is open over the main"
                + " window (Save receipt: ORD-1639.txt already exists.): answer or close it first");
        assertThat(app.steps()).isEqualTo(json("[]"));
    }

    // ---- Cancel order and Save receipt…'s state (the new order's buttonIsDisabled and buttonIsEnabled read them)

    @Test
    void cancelOrderIsDisabledForAShippedOrderWithTheAppsReason() {
        OrderTabFake shipped = new OrderTabFake("ORD-1636", "Shipped");

        OrderDeskNewOrderSteps.buttonIsDisabled(shipped.client(), "Cancel order",
                "Only placed orders can be cancelled — this one is shipped");
        OrderDeskNewOrderSteps.buttonIsDisabled(shipped.client(), "cancel order", "this one is shipped");
        OrderDeskNewOrderSteps.buttonIsEnabled(shipped.client(), "Save receipt…");
    }

    @Test
    void cancelOrderIsDisabledFailsWithExpectedAndActual() {
        OrderTabFake placed = new OrderTabFake();
        OrderTabFake cancelled = new OrderTabFake("ORD-1634", "Cancelled");

        Throwable enabled = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(placed.client(), "Cancel order", "this one is shipped"));
        Throwable reason = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(cancelled.client(), "Cancel order", "this one is shipped"));
        Throwable disabled = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsEnabled(cancelled.client(), "Cancel order"));

        assertThat(enabled).isInstanceOf(AssertionError.class)
                .hasMessage("Cancel order is enabled on ORD-1639, expected disabled: “this one is shipped”");
        assertThat(reason).isInstanceOf(AssertionError.class).hasMessage("Cancel order's reason on ORD-1634 reads “Only placed"
                + " orders can be cancelled — this one is cancelled”, expected “this one is shipped”");
        assertThat(disabled).isInstanceOf(AssertionError.class).hasMessage("Cancel order is disabled on ORD-1634 (“Only"
                + " placed orders can be cancelled — this one is cancelled”), expected enabled");
    }

    @Test
    void anOrderTabButtonCheckFailsFastWhenNoOrderTabIsSelected() {
        OrderTabFake app = new OrderTabFake().showTabs("Orders", "Orders", "ORD-1639");
        long start = System.currentTimeMillis();

        Throwable elsewhere = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsEnabled(app.client(), "Save receipt…"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(elsewhere).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrderTabSteps.NO_ORDER_TAB
                + " (Orders is selected)");
    }
}
