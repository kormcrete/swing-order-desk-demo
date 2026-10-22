package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.agent.BodyTrace;
import io.github.kormcrete.swingui.client.TracingVerbs;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;
import io.github.kormcrete.swingui.spi.Params;
import io.github.kormcrete.swingui.vocab.Pattern;
import io.github.kormcrete.swingui.vocab.PatternCompiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** The vocabulary as the agent reads it when the plugin becomes ready, and how a person's steps fold into it. */
class OrderDeskVocabularyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<DomainStep> steps = new OrderDeskVocabulary(null).steps();

    /** A value for every param of every step, each one the stand-in app ({@link #app}) knows. */
    private static final Map<String, String> SAMPLE = new HashMap<String, String>();
    static {
        String[] pairs = {"customer", "Ada Shop Ltd", "product", "Stapler", "qty", "3", "po", "PO-7781", "order", "ORD-1640",
                "total", "€31.32", "text", "pen", "customers", "Ada Shop Ltd", "terms", "Card only", "category", "Desk",
                "products", "Stapler", "lines", "Stapler; Gel pen, black", "column", "Stock", "value", "4",
                "warning", "Low stock: 4 left", "count", "22 products", "amount", "Subtotal", "reason", "x",
                "button", "Add to cart", "field", "Street", "method", "Express", "number", "4242424242424242",
                "expiry", "12/29", "name", "Ada Lindqvist", "section", "Payment", "step", "Address", "answer", "card",
                "message", "Owes €940.00 now, this order adds €96.60.", "error", "none", "note", "Debit or credit card",
                "price", "€14.90"};
        for (int i = 0; i < pairs.length; i += 2) SAMPLE.put(pairs[i], pairs[i + 1]);
    }

    /** Where a step needs other values than {@link #SAMPLE}'s for its checks to pass on the stand-in app. */
    private static final Map<String, Map<String, String>> OVERRIDES = new HashMap<String, Map<String, String>>();
    static {
        override("cartLineIs", "column", "Line total", "value", "€1.00");
        override("buttonIsDisabled", "button", "Remove", "reason", "Select a cart line first");
        override("keepCartLines", "products", "Stapler; Gel pen, black");
        override("productsListedAre", "products", "A4 paper, 500 sheets; Gel pen, black; Gel pen, blue; Stapler; Desk lamp");
        override("cartAmountIs", "value", "€26.70");
        override("cartStockWarningIs", "warning", "Only 4 Staplers in stock");
        override("setCheckoutField", "value", "Elmstraße 9");
        override("checkoutFieldIs", "value", "Torstraße 118");
        override("placeOrderConfirmationIs", "message", "Place order for €41.16?");
        override("reviewSectionIs", "section", "Shipping", "text", "Standard · 3–5 business days · €4.90");
        override("reviewLinesAre", "lines", "A4 paper, 500 sheets; Stapler; Gel pen, black");
        override("reviewLineIs", "column", "Qty", "value", "1");
        override("reviewAmountIs", "amount", "Total", "value", "€41.16");
        override("paymentNoteIs", "method", "Card");
    }

    /** The checkout step each checkout step runs on in these tests (the credit-limit ones with its warning open). */
    private static final Map<String, String> CHECKOUT_ON = new HashMap<String, String>();
    static {
        String[] pairs = {"checkoutWithPo", "Address", "placeOrder", "Review", "startCheckout", "Address",
                "setCheckoutField", "Address", "chooseShipping", "Shipping", "payByInvoice", "Payment", "payByCard", "Payment",
                "checkoutNext", "Address", "checkoutBack", "Shipping", "editReviewSection", "Review",
                "answerCreditLimit", "Payment", "cancelCheckout", "Address", "cancelPlaceOrder", "Review",
                "checkoutCustomerIs", "Address", "checkoutStepIs", "Address", "checkoutFieldIs", "Address",
                "checkoutFieldErrorIs", "Address", "shippingPriceIs", "Shipping", "paymentNoteIs", "Payment",
                "creditLimitWarningIs", "Payment", "placeOrderConfirmationIs", "Review", "reviewSectionIs", "Review",
                "reviewLinesAre", "Review", "reviewLineIs", "Review", "reviewAmountIs", "Review"};
        for (int i = 0; i < pairs.length; i += 2) CHECKOUT_ON.put(pairs[i], pairs[i + 1]);
    }

    /** The steps that need the credit-limit warning, not the checkout. */
    private static final List<String> CREDIT_WARNING_STEPS = Arrays.asList("answerCreditLimit", "creditLimitWarningIs");
    /** The checkout steps that start from a new order. */
    private static final List<String> FROM_NEW_ORDER = Arrays.asList("startCheckout", "checkoutWithPo");

    private static void override(String id, String... pairs) {
        Map<String, String> v = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) v.put(pairs[i], pairs[i + 1]);
        OVERRIDES.put(id, v);
    }

    /** The values step {@code id} runs with in these tests. */
    private static Map<String, String> sample(String id) {
        Map<String, String> v = new HashMap<String, String>(SAMPLE);
        if (OVERRIDES.containsKey(id)) v.putAll(OVERRIDES.get(id));
        return v;
    }

    /** Written-pattern entries compared with their body's steps (autoFold steps whose body takes steps). */
    private static final int CHECKED_PATTERN_ENTRIES = 51;

    @Test
    void theFirstEightStepsKeepTheirSentences() {
        // the bundled recorded tests run these by id and sentence
        assertThat(sentences().subList(0, 8)).containsExactly(
                "openNewOrder: Open a new order for {customer}",
                "addToCart: Add {product} to the cart",
                "setQuantity: Set the quantity of {product} to {qty}",
                "addQtyToCart: Add {qty} × {product} to the cart",
                "checkoutWithPo: Check out with PO number {po}",
                "placeOrder: Place the order",
                "openOrder: Open order {order}",
                "cartTotalIs: The cart total is {total}");
        assertThat(step("setQuantity").params().get(1).type()).isEqualTo(ParamType.INT);
        assertThat(step("addQtyToCart").params().get(1).type()).isEqualTo(ParamType.INT);
    }

    @Test
    void theNewOrderStepsAndChecks() {
        assertThat(sentences().subList(8, 32)).containsExactly(
                "chooseCustomer: Choose {customer} as the customer",
                "typeCustomer: Type {text} into Customer without picking a customer",
                "customerSuggestionsAre: The customer suggestions are {customers}",
                "newOrderIsFor: The new order is for {customer}",
                "customerTermsAre: The customer's terms are {terms}",
                "recentCustomersAre: The recent customers are {customers}",
                "browseCategory: Browse category {category}",
                "selectedCategoryIs: The selected category is {category}",
                "searchProducts: Search the products for {text}",
                "clearProductSearch: Clear the product search",
                "addProductsToCart: Add {products} to the cart together",
                "showInCategory: Show {product} in its category",
                "productsListedAre: The products listed are {products}",
                "productIs: The {column} of {product} is {value}",
                "lowStockWarningIs: The low-stock warning of {product} is {warning}",
                "productCountIs: The product count reads {count}",
                "removeFromCart: Remove {products} from the cart",
                "keepCartLines: Ask to remove {products} from the cart, then keep them",
                "cartLinesAre: The cart holds {lines}",
                "cartLineIs: The {column} of {product} in the cart is {value}",
                "cartAmountIs: The cart's {amount} is {value}",
                "cartStockWarningIs: The cart warns {warning}",
                "buttonIsDisabled: {button} is disabled: {reason}",
                "buttonIsEnabled: {button} is enabled");
    }

    @Test
    void theCheckoutStepsAndChecks() {
        assertThat(sentences().subList(32, sentences().size())).containsExactly(
                "startCheckout: Start the checkout",
                "setCheckoutField: Set the checkout's {field} to {value}",
                "chooseShipping: Choose {method} shipping",
                "payByInvoice: Pay by invoice with PO number {po}",
                "payByCard: Pay by card {number}, expiry {expiry}, name on card {name}",
                "checkoutNext: Go on to the next checkout step",
                "checkoutBack: Go back a checkout step",
                "editReviewSection: Edit the {section} on the review",
                "answerCreditLimit: Answer the credit-limit warning with {answer}",
                "cancelCheckout: Cancel the checkout",
                "cancelPlaceOrder: Ask to place the order, then cancel",
                "checkoutCustomerIs: The checkout is for {customer}",
                "checkoutStepIs: The checkout is on the {step} step",
                "checkoutFieldIs: The checkout's {field} reads {value}",
                "checkoutFieldErrorIs: The checkout's {field} error reads {error}",
                "shippingPriceIs: The {method} shipping price is {price}",
                "paymentNoteIs: The note under {method} reads {note}",
                "creditLimitWarningIs: The credit-limit warning reads {message}",
                "placeOrderConfirmationIs: The place-order confirmation reads {message}",
                "reviewSectionIs: The review's {section} reads {text}",
                "reviewLinesAre: The review lists {lines}",
                "reviewLineIs: The {column} of {product} on the review is {value}",
                "reviewAmountIs: The review's {amount} is {value}");
        assertThat(step("placeOrder").outcomeBody()).isNotNull();
    }

    @Test
    void anActStepSaysWhatItNeedsAndWhereItEndsAndACheckWhatItReads() {
        List<String> acts = Collections.singletonList("openOrder");   // its screen comes later
        for (DomainStep s : steps) {
            boolean check = s.id().endsWith("Is") || s.id().endsWith("Are") || s.id().equals("newOrderIsFor")
                    || s.id().equals("buttonIsDisabled") || s.id().equals("buttonIsEnabled");
            assertThat(s.kind()).as(s.id()).isEqualTo(check ? DomainStep.Kind.CHECK : DomainStep.Kind.ACT);
            assertThat(s.about()).as(s.id()).isNotBlank();
            if (s.kind() == DomainStep.Kind.ACT && !acts.contains(s.id())) {
                assertThat(s.about()).as(s.id()).startsWith("Needs: ").contains(". Ends: ");
            }
            if (s.kind() == DomainStep.Kind.CHECK) assertThat(s.about()).as(s.id()).startsWith("Reads ");
        }
    }

    @Test
    void everyStepHasAPatternAndNoProblems() {
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            assertThat(c.problems()).as(c.step().id()).isEmpty();
            assertThat(c.why()).as(c.step().id()).isNull();
            assertThat(c.patterns()).as(c.step().id()).isNotEmpty();
        }
    }

    @Test
    void everyStepNamesItsJavaMethod() throws Exception {
        for (DomainStep s : steps) {
            Class<?> type = Class.forName(s.javaClass());
            assertThat(s.javaClass()).as(s.id()).isIn(OrderDeskSteps.class.getName(), OrderDeskNewOrderSteps.class.getName(),
                    OrderDeskCheckoutSteps.class.getName());
            String call = s.javaCall();
            String name = call.substring(0, call.indexOf('('));
            int args = call.split(",").length;
            Method m = method(type, name);
            assertThat(m).as(call).isNotNull();
            assertThat(Modifier.isStatic(m.getModifiers())).as(call).isTrue();
            assertThat(m.getParameterCount()).as(call).isEqualTo(args);
        }
        assertThat(step("placeOrder").javaCall()).isEqualTo("placeOrder(ui)");
        assertThat(method(OrderDeskSteps.class, "placeOrder").getReturnType()).isEqualTo(String.class);
        assertThat(step("addProductsToCart").outcomeBody()).isNotNull();
        assertThat(method(OrderDeskNewOrderSteps.class, "addProductsToCart").getReturnType()).isEqualTo(Map.class);
    }

    @Test
    void eachBodyTakesTheStepsOfItsJavaMethod() throws Exception {
        // every step, with a value for each param, on the same app: the body and the method its java() names
        int compared = 0;
        for (DomainStep s : steps) {
            JsonNode body = run(s.id(), sample(s.id()));
            JsonNode method = traced(s, ui -> invoke(s, ui, sample(s.id())));
            assertThat(body).as(s.id()).isEqualTo(method);
            compared++;
        }
        assertThat(compared).isEqualTo(steps.size());
    }

    @Test
    void eachNewOrderStepFailsFastWhenItsScreenIsNotShowing() {
        // the Orders tab selected, or no new order open: every step of a new order's tab says so within about a second
        List<String> elsewhere = new ArrayList<String>(Arrays.asList("openNewOrder", "recentCustomersAre", "openOrder"));
        for (String id : CHECKOUT_ON.keySet()) if (!FROM_NEW_ORDER.contains(id)) elsewhere.add(id);
        int checked = 0;
        for (final DomainStep s : steps) {
            if (elsewhere.contains(s.id())) continue;
            final FakeApp nothing = new FakeApp();
            final Map<String, String> v = sample(s.id());
            long start = System.currentTimeMillis();

            Throwable gone = catchThrowable(() -> s.body().run(nothing.client(), new Params() {
                @Override public String text(String name) { return v.get(name); }
                @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
            }));

            assertThat(System.currentTimeMillis() - start).as(s.id()).isLessThan(2000);
            assertThat(gone).as(s.id()).isInstanceOf(DriverException.class)
                    .hasMessage("NOT_FOUND: " + OrderDeskNewOrderSteps.NO_NEW_ORDER);
            assertThat(nothing.steps()).as(s.id()).isEmpty();
            checked++;
        }
        assertThat(checked).isEqualTo(steps.size() - elsewhere.size());
    }

    @Test
    void eachCheckoutStepFailsFastWhenItsScreenIsNotShowing() {
        // no checkout open (the credit-limit steps: no warning): each says so within about a second and takes no step
        int checked = 0;
        for (final DomainStep s : steps) {
            if (!CHECKOUT_ON.containsKey(s.id()) || FROM_NEW_ORDER.contains(s.id())) continue;
            final FakeApp nothing = new FakeApp();
            final Map<String, String> v = sample(s.id());
            long start = System.currentTimeMillis();

            Throwable gone = catchThrowable(() -> s.body().run(nothing.client(), new Params() {
                @Override public String text(String name) { return v.get(name); }
                @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
            }));

            assertThat(System.currentTimeMillis() - start).as(s.id()).isLessThan(2000);
            assertThat(gone).as(s.id()).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: "
                    + (CREDIT_WARNING_STEPS.contains(s.id()) ? OrderDeskCheckoutSteps.NO_CREDIT_WARNING : OrderDeskCheckoutSteps.NO_CHECKOUT));
            assertThat(nothing.steps()).as(s.id()).isEmpty();
            checked++;
        }
        assertThat(checked).isEqualTo(CHECKOUT_ON.size() - FROM_NEW_ORDER.size());
    }

    @Test
    void everyWrittenPatternNamesWhatItsBodyClicks() throws IOException {
        // a written pattern (match) and the body take their names from OrderDeskSteps' constants; this pins that a
        // rename there reaches both: each entry's fixed parts equal the body's step at the same place. A pattern that
        // only suggests its step (autoFold false) is looser than the body on purpose, and a check that only reads
        // (newOrderIsFor) takes no steps to compare
        int checked = 0;
        for (DomainStep s : steps) {
            if (s.match().isEmpty() || s.match().get(0).contains("\"use\"") || !s.autoFold()) continue;
            JsonNode body = run(s.id(), sample(s.id()));
            if (body.size() == 0) continue;
            assertThat(body.size()).as(s.id() + " steps").isGreaterThanOrEqualTo(s.match().size());
            for (int i = 0; i < s.match().size(); i++) {
                JsonNode entry = JSON.readTree(s.match().get(i));
                assertThat(fixedParts(entry, body.get(i))).as(s.id() + " match " + (i + 1) + " vs " + body.get(i)).isEmpty();
                checked++;
            }
        }
        assertThat(checked).isEqualTo(CHECKED_PATTERN_ENTRIES);
    }

    @Test
    void aNameIsPlainPatternText() {
        assertThat(OrderDeskVocabulary.text("Save *all*")).isEqualTo("\"Save \\\\*all\\\\*\"");
        assertThat(OrderDeskVocabulary.prefix("tab:New order")).isEqualTo("\"tab:New order*\"");
        assertThat(OrderDeskVocabulary.key("Line \"total\"")).isEqualTo("\"Line \\\"total\\\"\"");
    }

    // ---- folding: a person's steps as the recorder saw them (the element's scope, role and every cell of its row)

    @Test
    void aNewOrderForARecentCustomerFoldsIntoOpenNewOrder() {
        JsonNode recent = json("{'action':'click','target':{'window':'main','role':'button',"
                + "'name':'New order for a recent customer'}}");
        JsonNode pick = json("{'action':'pick','path':['Birch & Co']}");

        Pattern.Match m = null;
        for (Pattern p : patterns("openNewOrder")) if (m == null) m = p.match(Arrays.asList(recent, pick));

        assertThat(m).isNotNull();
        assertThat(m.params()).containsEntry("customer", "Birch & Co");
    }

    @Test
    void aClickOnAProductsNameThenAddToCartFoldsIntoAddToCart() {
        JsonNode product = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'SKU':'PEN-GEL-BLK','Product':'Gel pen, black','Price':'€1.20','Stock':'800'},"
                + "'column':'Product'}}");

        Pattern.Match m = pattern("addToCart").match(Arrays.asList(product, addToCartButton()));

        assertThat(m).isNotNull();
        assertThat(m.params()).containsEntry("product", "Gel pen, black");
    }

    @Test
    void aClickOnACartLineIsNoProductToAdd() {
        JsonNode cartLine = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'Gel pen, black','Qty':'1','Price':'€1.20','Line total':'€1.20'},"
                + "'column':'Product'}}");

        assertThat(pattern("addToCart").match(Arrays.asList(cartLine, addToCartButton()))).isNull();
    }

    @Test
    void addingAProductThenTypingItsQuantityFoldsIntoAddQtyToCart() {
        JsonNode product = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'SKU':'PAP-A4-500','Product':'A4 paper, 500 sheets','Price':'€4.90','Stock':'120'},"
                + "'column':'SKU'}}");
        JsonNode qty = json("{'action':'type','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'A4 paper, 500 sheets','Qty':'1','Price':'€4.90','Line total':'€4.90'},"
                + "'column':'Qty'},'text':'3','commit':true}");

        Pattern.Match m = pattern("addQtyToCart").match(Arrays.asList(product, addToCartButton(), qty));

        assertThat(m).isNotNull();
        assertThat(m.params()).containsEntry("product", "A4 paper, 500 sheets").containsEntry("qty", "3");
    }

    @Test
    void aDoubleClickOnAnOrderRowOpensThatOrderWithOrWithoutTheTabClick() {
        JsonNode tab = json("{'action':'click','target':{'window':'main','role':'tab','name':'Orders'}}");
        JsonNode row = json("{'action':'click','target':{'window':'main','scope':['tab:Orders'],'role':'table',"
                + "'row':{'Order':'ORD-1640','Date':'12 Oct 2026','Customer':'Ada Shop Ltd','Items':'13','Total':'€37.20',"
                + "'Status':'Placed'},'column':'Customer'},'count':2}");

        assertThat(pattern("openOrder").match(Arrays.asList(tab, row)).params()).containsEntry("order", "ORD-1640");
        assertThat(pattern("openOrder").match(Collections.singletonList(row)).params()).containsEntry("order", "ORD-1640");
    }

    @Test
    void theTotalCheckMatchesTheCartsTotalNotTheReviewStepsTotal() {
        JsonNode cart = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'label','name':'Total'},'text':'€31.32'}");
        JsonNode review = json("{'action':'expect','target':{'window':'dialog','role':'label','name':'Total'},'text':'€37.20'}");

        assertThat(pattern("cartTotalIs").match(Collections.singletonList(cart)).params()).containsEntry("total", "€31.32");
        assertThat(pattern("cartTotalIs").match(Collections.singletonList(review))).isNull();
    }

    @Test
    void theSelectedNewOrderTabCheckFoldsIntoNewOrderIsFor() {
        // the check the bundled recorded tests make after picking the customer
        JsonNode tab = json("{'action':'expect','target':{'window':'main','role':'tab','name':'New order · Ada Shop Ltd'},"
                + "'state':['selected']}");

        assertThat(pattern("newOrderIsFor").match(Collections.singletonList(tab)).params())
                .containsEntry("customer", "Ada Shop Ltd");
    }

    @Test
    void aPersonsCheckOnALineTotalFoldsIntoCartLineIsNotIntoAProductCheck() {
        JsonNode lineTotal = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'A4 paper, 500 sheets','Qty':'3','Price':'€4.90','Line total':'€14.70'},"
                + "'column':'Line total'},'text':'€14.70'}");

        Pattern.Match m = pattern("cartLineIs").match(Collections.singletonList(lineTotal));

        assertThat(m.params()).containsEntry("product", "A4 paper, 500 sheets").containsEntry("column", "Line total")
                .containsEntry("value", "€14.70");
        assertThat(pattern("productIs").match(Collections.singletonList(lineTotal))).isNull();
    }

    @Test
    void typingACustomerAndEnterInAnOpenOrderFoldsIntoChooseCustomer() {
        JsonNode type = json("{'action':'type','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],'role':'text',"
                + "'name':'Customer'},'text':'Birch'}");
        JsonNode enter = json("{'action':'press','keys':'Enter'}");

        assertThat(pattern("chooseCustomer").match(Arrays.asList(type, enter)).params()).containsEntry("customer", "Birch");
    }

    @Test
    void aCartLineClickThenRemoveFoldsIntoRemoveFromCart() {
        JsonNode line = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'Stapler','Qty':'1','Price':'€12.50','Line total':'€12.50'},'column':'Product'}}");
        JsonNode remove = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'button','name':'Remove'}}");

        assertThat(pattern("removeFromCart").match(Arrays.asList(line, remove)).params()).containsEntry("products", "Stapler");
    }

    @Test
    void theBundledPlaceOrderRecordingsCheckoutFoldsIntoCheckoutWithPo() {
        String dialog = "'window':'dialog','role':'button','name':'Next'";
        List<JsonNode> recorded = Arrays.asList(
                json("{'action':'click','target':{'scope':['tab:New order · Ada Shop Ltd'],'role':'button','name':'Checkout…'}}"),
                json("{'action':'click','target':{" + dialog + "}}"),
                json("{'action':'click','target':{" + dialog + "}}"),
                json("{'action':'type','target':{'window':'dialog','role':'text','name':'PO number'},'text':'PO-7781'}"),
                json("{'action':'click','target':{" + dialog + "}}"));

        assertThat(pattern("checkoutWithPo").match(recorded).params()).containsEntry("po", "PO-7781");
    }

    @Test
    void anExpressClickFoldsIntoChooseShippingAndACardClickDoesNot() {
        JsonNode express = json("{'action':'click','target':{'window':'dialog','role':'radio','name':'Express · next business day'}}");
        JsonNode card = json("{'action':'click','target':{'window':'dialog','role':'radio','name':'Card'}}");

        assertThat(pattern("chooseShipping").match(Collections.singletonList(express)).params()).containsEntry("method", "Express");
        assertThat(pattern("chooseShipping").match(Collections.singletonList(card))).isNull();
    }

    @Test
    void aPersonsEnabledCheckOnACheckoutButtonOrRadioDoesNotFoldIntoButtonIsEnabled() {
        // the Check menu's "Is enabled" records state !disabled; a pattern never compares the window, so only the new
        // order's scope keeps Back, Place order or Standard (which buttonIsEnabled cannot replay) out
        for (String target : Arrays.asList("'role':'button','name':'Back'", "'role':'button','name':'Place order'",
                "'role':'radio','name':'Standard · 3–5 business days'", "'role':'button','name':'Next'")) {
            JsonNode enabled = json("{'action':'expect','target':{'window':'dialog'," + target + "},'state':['!disabled']}");
            for (Pattern p : patterns("buttonIsEnabled")) {
                assertThat(p.match(Collections.singletonList(enabled))).as(target).isNull();
            }
        }
        JsonNode checkout = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'button','name':'Checkout…'},'state':['!disabled']}");
        assertThat(pattern("buttonIsEnabled").match(Collections.singletonList(checkout)).params()).containsEntry("button", "Checkout…");
    }

    @Test
    void theBundledPostalCodeCheckIsSuggestedAsCheckoutFieldIsNeverFolded() {
        // place-order.json's "The postal code is 10119", as the Check menu records a field's value
        JsonNode postalCode = json("{'action':'expect','target':{'window':'dialog','role':'text','name':'Postal code'},'value':'10119'}");
        JsonNode customer = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'text','name':'Customer'},'value':'Ada Shop Ltd'}");

        assertThat(pattern("checkoutFieldIs").match(Collections.singletonList(postalCode)).params())
                .containsEntry("field", "Postal code").containsEntry("value", "10119");
        // the window is not compared, so a new order's Customer check fits too: the step only suggests itself
        assertThat(pattern("checkoutFieldIs").match(Collections.singletonList(customer))).isNotNull();
        assertThat(step("checkoutFieldIs").autoFold()).isFalse();
        assertThat(step("reviewLineIs").autoFold()).isFalse();
    }

    @Test
    void aPersonsCheckOfAFieldErrorFoldsIntoCheckoutFieldErrorIs() {
        JsonNode error = json("{'action':'expect','target':{'window':'dialog','id':'postalCodeError'},'text':'Postal code is required'}");

        Pattern.Match m = pattern("checkoutFieldErrorIs").match(Collections.singletonList(error));

        assertThat(m.params()).containsEntry("field", "postalCode").containsEntry("error", "Postal code is required");
        assertThat(OrderDeskCheckoutSteps.field(m.params().get("field")).name).isEqualTo("Postal code");
    }

    // ---- helpers

    private static JsonNode addToCartButton() {
        return json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],'role':'button',"
                + "'name':'Add to cart'}}");
    }

    private DomainStep step(String id) {
        for (DomainStep s : steps) if (s.id().equals(id)) return s;
        throw new AssertionError("no step " + id);
    }

    private Pattern pattern(String id) {
        return patterns(id).get(0);
    }

    /** The step's own pattern, then one per matchAlso. */
    private List<Pattern> patterns(String id) {
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            if (c.step().id().equals(id)) return c.patterns();
        }
        throw new AssertionError("no step " + id);
    }

    /** The tracer the agent uses at ready: a Java body's steps for any values, or why it has none. */
    private static PatternCompiler.Traced trace(DomainStep s) {
        BodyTrace.Result r = BodyTrace.of(s);
        return r.derivable() ? PatternCompiler.Traced.steps(r.steps()) : PatternCompiler.Traced.none(r.why());
    }

    private List<String> sentences() {
        List<String> out = new ArrayList<String>();
        for (DomainStep s : steps) out.add(s.id() + ": " + s.sentence());
        return out;
    }

    private static Method method(Class<?> type, String name) {
        for (Method m : type.getMethods()) if (m.getName().equals(name)) return m;
        return null;
    }

    /** The steps {@code id}'s body takes with these values (name, value, ...). */
    private JsonNode run(String id, String... values) {
        Map<String, String> v = new HashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) v.put(values[i], values[i + 1]);
        return run(id, v);
    }

    /**
     * The steps {@code id}'s body takes with these values: the steps of a new order's tab on a stand-in app showing a
     * new order ({@link #app}), the others traced on a Client that reaches no app (they do not look first).
     */
    private JsonNode run(String id, final Map<String, String> v) {
        final DomainStep s = step(id);
        final Params params = new Params() {
            @Override public String text(String name) { return v.get(name); }
            @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
        };
        return traced(s, ui -> s.body().run(ui, params));
    }

    /** The method the step's java() names, called with {@code values} in the order the call gives them. */
    private static void invoke(DomainStep s, Client ui, Map<String, String> values) {
        String call = s.javaCall();
        String[] names = call.substring(call.indexOf('(') + 1, call.indexOf(')')).split(",\\s*");
        try {
            Method m = method(Class.forName(s.javaClass()), call.substring(0, call.indexOf('(')));
            Object[] args = new Object[names.length];
            args[0] = ui;
            for (int i = 1; i < names.length; i++) {
                String value = values.get(names[i].replace("{", "").replace("}", ""));
                args[i] = m.getParameterTypes()[i] == int.class ? (Object) Integer.parseInt(value) : value;
            }
            m.invoke(null, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            if (e.getCause() instanceof Error) throw (Error) e.getCause();
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(call, e);
        }
    }

    /** The steps that run takes on the app {@code s} needs; a failure after them (a trace's unanswered read) counts. */
    private static JsonNode traced(DomainStep s, Body body) {
        if (CHECKOUT_ON.containsKey(s.id())) {
            CheckoutFake checkout = new CheckoutFake(CHECKOUT_ON.get(s.id()));
            if (CREDIT_WARNING_STEPS.contains(s.id())) checkout.creditWarning();
            body.run(checkout.client());
            return checkout.steps();
        }
        if ("openOrder".equals(s.id())) {
            TracingVerbs verbs = new TracingVerbs();
            try {
                body.run(new Client(verbs, verbs.app(), "trace"));
            } catch (RuntimeException e) {
                // placeOrder reads the new number, which a trace cannot answer: its steps up to there count; any other
                // throw is a broken body
                if (verbs.derivable()) throw e;
            }
            assertThat(verbs.steps()).as("the body's steps").isNotEmpty();
            return JSON.valueToTree(verbs.steps());
        }
        FakeApp app = app();
        body.run(app.client());
        return app.steps();
    }

    /** A new order with every value the sample checks expect, so each check passes. */
    private static FakeApp app() {
        FakeApp app = FakeApp.withNewOrder("Stapler", "Gel pen, black");
        app.answer("tab 'Orders'", FakeApp.label("Orders", "New order · Ada Shop Ltd"));
        app.answer("element #customerTerms", FakeApp.label("customerTerms", "Card only"));
        app.answer("button 'New order for a recent customer'", FakeApp.node("button", "New order for a recent customer"));
        app.answer("popup menuitem", FakeApp.node("menuitem", "Ada Shop Ltd"));
        app.answer("tree", treeOnDesk());
        app.answer("button 'Remove'", FakeApp.withState(FakeApp.node("button", "Remove"), "disabled"));
        app.answer("label 'Select a cart line first'", FakeApp.label("Select a cart line first"));
        app.answer("text 'Search'", FakeApp.label("Search", "pen"));
        app.answer("table row=DSK-STAPLER,Stapler col=Stock", FakeApp.withTooltip(FakeApp.cell("Stock", "4"), "Low stock: 4 left"));
        app.answer("element #productCount", FakeApp.label("productCount", "22 products"));
        app.answer("table row=Stapler col=Line total", FakeApp.cell("Line total", "€1.00"));
        app.answer("label 'Subtotal'", FakeApp.label("Subtotal", "€26.70"));
        app.answer("label 'in stock'", FakeApp.label("Only 4 Staplers in stock"));
        return app;
    }

    /** The tree: its items, and "Desk" as what is selected (a remember of the tree reads its value). */
    private static io.github.kormcrete.swingui.driver.model.Node treeOnDesk() {
        io.github.kormcrete.swingui.driver.model.Node tree = FakeApp.items("tree", Arrays.asList("All products", "Office",
                "Office > Paper", "Office > Pens", "Office > Desk", "Coffee", "Coffee > Beans", "Coffee > Machines",
                "Cleaning"), 4);
        tree.value = "Desk";
        return tree;
    }

    /**
     * Where a pattern entry's fixed parts differ from a body step: every field the entry has, except {@code optional},
     * must be in the step and equal, unless the entry's text holds a {@code {param}} or a {@code *} (it matches more).
     */
    private static List<String> fixedParts(JsonNode entry, JsonNode step) {
        List<String> diffs = new ArrayList<String>();
        fixedParts("", entry, step, diffs);
        return diffs;
    }

    private static void fixedParts(String at, JsonNode entry, JsonNode step, List<String> diffs) {
        if (entry.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = entry.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> f = it.next();
                if (at.isEmpty() && f.getKey().equals("optional")) continue;
                JsonNode s = step == null ? null : step.get(f.getKey());
                if (s == null && f.getValue().isTextual() && open(f.getValue().asText())) continue;
                if (s == null) diffs.add(at + "/" + f.getKey() + " missing");
                else fixedParts(at + "/" + f.getKey(), f.getValue(), s, diffs);
            }
        } else if (entry.isArray()) {
            if (step.size() != entry.size()) diffs.add(at + " " + entry + " vs " + step);
            else for (int i = 0; i < entry.size(); i++) fixedParts(at + "/" + i, entry.get(i), step.get(i), diffs);
        } else if (!entry.equals(step) && !(entry.isTextual() && open(entry.asText()))) {
            diffs.add(at + " " + entry + " vs " + step);
        }
    }

    /** Text that matches more than itself. */
    private static boolean open(String text) {
        return text.contains("{") || text.contains("*");
    }

    private interface Body {
        void run(Client ui);
    }
}
