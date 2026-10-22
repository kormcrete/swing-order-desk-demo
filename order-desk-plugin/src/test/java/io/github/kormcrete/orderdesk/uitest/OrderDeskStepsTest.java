package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.client.TracingVerbs;
import io.github.kormcrete.swingui.junit.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each of Order Desk's steps as the steps it takes: traced on a Client that reaches no app, or, for the steps of a new
 * order's tab (which look at the app first), on a stand-in app ({@link FakeApp}).
 */
class OrderDeskStepsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NEW_ORDER = "'scope':['tab:New order*']";

    private final TracingVerbs verbs = new TracingVerbs();
    private final Client ui = new Client(verbs, verbs.app(), "trace");

    @Test
    void openNewOrderPicksTheCustomerInANewOrderTab() {
        FakeApp app = FakeApp.withNewOrder();

        OrderDeskSteps.openNewOrder(app.client(), "Ada Shop Ltd");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'menu','path':['File','New order']},"
                + "{'action':'type','target':{'scope':['tab:New order'],'role':'text','name':'Customer'},'text':'Ada Shop Ltd'},"
                + "{'action':'press','keys':'Enter'}]"));
    }

    @Test
    void addToCartSelectsTheProductByItsSkuCellThenAddsIt() {
        FakeApp app = FakeApp.withNewOrder();

        OrderDeskSteps.addToCart(app.client(), "Gel pen, black");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Add to cart'}}]"));
    }

    @Test
    void setQuantityTypesTheNumberIntoTheQtyCellAndCommits() {
        FakeApp app = FakeApp.withNewOrder("A4 paper, 500 sheets");

        OrderDeskSteps.setQuantity(app.client(), "A4 paper, 500 sheets", 3);

        assertThat(app.steps()).isEqualTo(json("[{'action':'type','target':{" + NEW_ORDER + ",'role':'table',"
                + "'row':{'Product':'A4 paper, 500 sheets'},'column':'Qty'},'text':'3','commit':true}]"));
    }

    @Test
    void addQtyToCartAddsTheProductThenSetsItsQuantity() {
        FakeApp app = FakeApp.withNewOrder("Gel pen, black");

        OrderDeskSteps.addQtyToCart(app.client(), "Gel pen, black", 10);

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Add to cart'}},"
                + "{'action':'type','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'Qty'},"
                + "'text':'10','commit':true}]"));
    }

    @Test
    void checkoutWithPoEndsOnTheReviewStep() {
        CheckoutFake app = new CheckoutFake("Address");

        OrderDeskSteps.checkoutWithPo(app.client(), "PO-7781");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Checkout…'}},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}},"
                + "{'action':'type','target':{'window':'dialog','role':'text','name':'PO number'},'text':'PO-7781'},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}}]"));
    }

    @Test
    void placeOrderConfirmsThenReadsTheNewNumberFromTheStatusLine() {
        CheckoutFake app = new CheckoutFake("Review");

        String number = OrderDeskSteps.placeOrder(app.client());

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Place order'}},"
                + "{'action':'answer','button':'Place order'}]"));
        assertThat(number).isEqualTo("ORD-1640");
    }

    @Test
    void placeOrderReadsTheNewNumberFromTheStatusLineByItsCodeName() {
        // the status line is named "statusMessage" (reach fix R1), so a label found by the text it holds ("placed")
        // reads that code name as its name: cutting the number out of it threw "String index out of range: -7"
        FakeApp app = FakeApp.onCheckoutReview();
        app.answer("label 'placed'", FakeApp.label("statusMessage", "Order ORD-1640 placed"));
        app.answer("element #statusMessage", FakeApp.label("statusMessage", "Order ORD-1640 placed"));
        app.answer("tab 'ORD-1640'", FakeApp.withState(FakeApp.node("tab", "ORD-1640"), "selected"));

        String number = OrderDeskSteps.placeOrder(app.client());

        assertThat(number).isEqualTo("ORD-1640");
    }

    @Test
    void openOrderDoubleClicksItsRowInTheOrdersList() {
        OrderDeskSteps.openOrder(ui, "ORD-1640");

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'click','target':{'role':'tab','name':'Orders'}},"
                + "{'action':'click','target':{'scope':['tab:Orders'],'role':'table','row':{'Order':'ORD-1640'}},'count':2}]"));
        assertThat(verbs.derivable()).isTrue();
    }

    @Test
    void cartTotalIsChecksTheLabelCaptionedTotal() {
        FakeApp app = FakeApp.withNewOrder();

        OrderDeskSteps.cartTotalIs(app.client(), "€31.32");

        assertThat(app.steps()).isEqualTo(json("[{'action':'expect','target':{" + NEW_ORDER + ",'role':'label','name':'Total'},"
                + "'text':'€31.32','timeout':1000}]"));
    }

    private JsonNode trace() {
        return JSON.valueToTree(verbs.steps());
    }

    static JsonNode json(String singleQuoted) {
        try {
            return JSON.readTree(singleQuoted.replace('\'', '"'));
        } catch (IOException e) {
            throw new IllegalArgumentException(singleQuoted, e);
        }
    }
}
