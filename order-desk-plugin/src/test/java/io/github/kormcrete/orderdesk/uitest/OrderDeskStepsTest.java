package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.client.TracingVerbs;
import io.github.kormcrete.swingui.junit.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** Each of Order Desk's steps as the steps it takes: traced on a Client that reaches no app. */
class OrderDeskStepsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NEW_ORDER = "'scope':['tab:New order*']";

    private final TracingVerbs verbs = new TracingVerbs();
    private final Client ui = new Client(verbs, verbs.app(), "trace");

    @Test
    void openNewOrderPicksTheCustomerInANewOrderTab() {
        OrderDeskSteps.openNewOrder(ui, "Ada Shop Ltd");

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'menu','path':['File','New order']},"
                + "{'action':'type','target':{'scope':['tab:New order'],'role':'text','name':'Customer'},'text':'Ada Shop Ltd'},"
                + "{'action':'press','keys':'Enter'}]"));
        assertThat(verbs.derivable()).isTrue();
    }

    @Test
    void addToCartSelectsTheProductByItsSkuCellThenAddsIt() {
        OrderDeskSteps.addToCart(ui, "Gel pen, black");

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Add to cart'}}]"));
        assertThat(verbs.derivable()).isTrue();
    }

    @Test
    void setQuantityTypesTheNumberIntoTheQtyCellAndCommits() {
        OrderDeskSteps.setQuantity(ui, "A4 paper, 500 sheets", 3);

        assertThat(trace()).isEqualTo(json("[{'action':'type','target':{" + NEW_ORDER + ",'role':'table',"
                + "'row':{'Product':'A4 paper, 500 sheets'},'column':'Qty'},'text':'3','commit':true}]"));
    }

    @Test
    void addQtyToCartAddsTheProductThenSetsItsQuantity() {
        OrderDeskSteps.addQtyToCart(ui, "Gel pen, black", 10);

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Add to cart'}},"
                + "{'action':'type','target':{" + NEW_ORDER + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'Qty'},"
                + "'text':'10','commit':true}]"));
    }

    @Test
    void checkoutWithPoEndsOnTheReviewStep() {
        OrderDeskSteps.checkoutWithPo(ui, "PO-7781");

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'click','target':{" + NEW_ORDER + ",'role':'button','name':'Checkout…'}},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}},"
                + "{'action':'type','target':{'window':'dialog','role':'text','name':'PO number'},'text':'PO-7781'},"
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Next'}}]"));
        assertThat(verbs.derivable()).isTrue();
    }

    @Test
    void placeOrderConfirmsThenReadsTheNewNumberFromTheStatusBar() {
        Throwable read = catchThrowable(() -> OrderDeskSteps.placeOrder(ui));

        assertThat(trace()).isEqualTo(json("["
                + "{'action':'click','target':{'window':'dialog','role':'button','name':'Place order'}},"
                + "{'action':'answer','button':'Place order'}]"));
        // the number comes from the app: a trace has no answer, which is why the vocabulary gives placeOrder a match
        assertThat(read).isNotNull();
        assertThat(verbs.notDerivable()).isEqualTo("the body reads the app (read), which a trace cannot answer");
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
        OrderDeskSteps.cartTotalIs(ui, "€31.32");

        assertThat(trace()).isEqualTo(json("[{'action':'expect','target':{" + NEW_ORDER + ",'role':'label','name':'Total'},"
                + "'text':'€31.32'}]"));
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
