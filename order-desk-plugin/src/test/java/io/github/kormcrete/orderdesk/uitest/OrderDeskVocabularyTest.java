package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.agent.BodyTrace;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.inspect.CheckMenuForms;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;
import io.github.kormcrete.swingui.spi.Params;
import io.github.kormcrete.swingui.vocab.Matcher;
import io.github.kormcrete.swingui.vocab.Pattern;
import io.github.kormcrete.swingui.vocab.PatternCompiler;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

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
        String[] pairs = {"customer", "Ada Shop Ltd", "product", "Stapler", "qty", "3", "po", "PO-7781", "order", "ORD-1639",
                "total", "€31.32", "text", "pen", "customers", "Ada Shop Ltd", "terms", "Card only", "category", "Desk",
                "products", "Stapler", "lines", "Stapler; Gel pen, black", "column", "Stock", "value", "4",
                "warning", "Low stock: 4 left", "count", "22 products", "amount", "Subtotal", "reason", "x",
                "button", "Add to cart", "field", "Street", "method", "Express", "number", "4242424242424242",
                "expiry", "12/29", "name", "Ada Lindqvist", "section", "Payment", "step", "Address", "answer", "card",
                "message", "Owes €940.00 now, this order adds €96.60.", "error", "none", "note", "Debit or credit card",
                "price", "€14.90", "status", "placed", "orders", "ORD-1639", "direction", "descending", "tab", "Orders",
                "tabs", "Orders", "item", "Close tab", "state", "disabled", "theme", "Dark", "option", "Invoice"};
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
        override("sortOrders", "column", "Total");
        override("orderIs", "column", "Total", "value", "€95.52");
        override("ordersListedAre", "orders", "ORD-1639; ORD-1638; ORD-1636; ORD-1634");
        override("ordersEmptyMessageIs", "message", "none");
        override("selectedOrdersAre", "orders", "none");
        override("statusLineIs", "message", "Ready");
        override("openOrdersCountIs", "count", "2");
        override("ordersCountIs", "count", "4");
        override("cancelQuestionIs", "message", "Cancel order ORD-1639?");
        override("suggestedReceiptFileIs", "file", "ORD-1639.txt");
        override("exitQuestionIs", "message", "Discard the open new orders?");
        override("aboutShows", "text", "fictional");
        override("saveReceipt", "file", "ORD-1639.txt");
        override("keepExistingReceipt", "file", "ORD-1639.txt");
        override("receiptSaveErrorIs", "file", "/nope/x", "message", "The receipt could not be saved");
        override("orderFactsAre", "facts", "Ada Shop Ltd");
        override("orderItemsAre", "lines", "Gel pen, black; A4 paper, 500 sheets; Paper clips, box of 100; Gel pen, blue");
        override("orderItemIs", "product", "Paper clips", "column", "Line total", "value", "€3.00");
        override("orderItemsSummaryIs", "summary", "56 units in 4 lines");
        override("orderDeliveryIs", "detail", "Shipping method", "value", "Standard · 3–5 business days · €4.90");
        override("orderInvoiceIs", "detail", "Total", "value", "€95.52");
    }

    /** The Orders list's steps (openOrder too) and the main window's: they run on {@link OrdersFake}. */
    private static final List<String> ORDERS_LIST = Arrays.asList("openOrder", "searchOrders", "clearOrderSearch",
            "filterOrders", "sortOrders", "selectOrders", "cancelOrders", "keepOrders", "copyOrderNumbers", "orderIs",
            "ordersListedAre", "ordersEmptyMessageIs", "selectedOrdersAre", "showOrders", "ordersCountIs", "cancelQuestionIs");
    private static final List<String> MAIN_WINDOW = Arrays.asList("selectTab", "closeTab", "keepTab", "openTabsAre",
            "selectedTabIs", "menuItemIs", "statusLineIs", "openOrdersCountIs", "cancelExit", "exitQuestionIs", "aboutShows",
            "switchTheme");
    /** An order's own tab's steps: they run on {@link OrderTabFake}. */
    private static final List<String> ORDER_TAB = Arrays.asList("saveReceipt", "keepExistingReceipt", "cancelOrder",
            "keepOrder", "receiptSaveErrorIs", "orderStatusIs", "orderFactsAre", "orderItemsAre", "orderItemIs",
            "orderItemsSummaryIs", "orderDeliveryIs", "orderInvoiceIs", "suggestedReceiptFileIs");
    /** The main window's steps that need a new order with something in its cart. */
    private static final List<String> ON_A_CART = Arrays.asList("closeTab", "keepTab", "cancelExit", "exitQuestionIs");

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
                "reviewLinesAre", "Review", "reviewLineIs", "Review", "reviewAmountIs", "Review", "checkoutChoiceIs", "Payment"};
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
    private static final int CHECKED_PATTERN_ENTRIES = 88;

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
        assertThat(sentences().subList(8, 34)).containsExactly(
                "chooseCustomer: Choose {customer} as the customer",
                "typeCustomer: Type {text} into Customer without picking a customer",
                "customerSuggestionsAre: The customer suggestions are {customers}",
                "newOrderIsFor: The new order is for {customer}",
                "customerTermsAre: The customer's terms are {terms}",
                "recentCustomersAre: The recent customers are {customers}",
                "openNewOrderFromRecent: Open a new order for {customer} from the recent customers",
                "browseCategory: Browse category {category}",
                "selectedCategoryIs: The selected category is {category}",
                "searchProducts: Search the products for {text}",
                "clearProductSearch: Clear the product search",
                "addProductsToCart: Add {products} to the cart together",
                "selectProducts: Select {products} in the products list",
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
        assertThat(sentences().subList(34, 58)).containsExactly(
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
                "reviewAmountIs: The review's {amount} is {value}",
                "checkoutChoiceIs: {option} is chosen in the checkout");
        assertThat(step("placeOrder").outcomeBody()).isNotNull();
    }

    @Test
    void theOrdersListAndMainWindowStepsAndChecks() {
        assertThat(sentences().subList(58, 85)).containsExactly(
                "searchOrders: Search the orders for {text}",
                "clearOrderSearch: Clear the order search",
                "filterOrders: Filter the orders by status {status}",
                "sortOrders: Sort the orders by {column}, {direction}",
                "selectOrders: Select the orders {orders}",
                "cancelOrders: Cancel the orders {orders}",
                "keepOrders: Ask to cancel the orders {orders}, then keep them",
                "copyOrderNumbers: Copy the order numbers of {orders}",
                "showOrders: Show the Orders list",
                "orderIs: The {column} of order {order} is {value}",
                "ordersListedAre: The orders listed are {orders}",
                "ordersCountIs: The number of orders listed is {count}",
                "ordersEmptyMessageIs: The orders list reads {message}",
                "selectedOrdersAre: The selected orders are {orders}",
                "cancelQuestionIs: Asking to cancel the orders {orders} reads {message}",
                "selectTab: Switch to the tab {tab}",
                "closeTab: Close the selected tab",
                "keepTab: Ask to close the selected tab, then cancel",
                "openTabsAre: The open tabs are {tabs}",
                "selectedTabIs: The selected tab is {tab}",
                "menuItemIs: The menu item {item} is {state}",
                "statusLineIs: The status line reads {message}",
                "openOrdersCountIs: The open orders count reads {count}",
                "cancelExit: Ask to exit, then cancel",
                "exitQuestionIs: The exit question reads {message}",
                "aboutShows: About Order Desk shows {text}",
                "switchTheme: Switch the theme to {theme}");
        for (String id : Arrays.asList("sortOrders", "searchOrders", "clearOrderSearch", "filterOrders", "showOrders")) {
            assertThat(step(id).outcomeBody()).as(id).isNotNull();
        }
        assertThat(step("ordersCountIs").params().get(0).type()).isEqualTo(ParamType.INT);
        assertThat(step("sortOrders").params().get(1).optional()).isTrue();
        assertThat(step("sortOrders").params().get(1).defaultValue()).isEqualTo("ascending");
    }

    @Test
    void theOrderTabStepsAndChecks() {
        assertThat(sentences().subList(85, sentences().size())).containsExactly(
                "saveReceipt: Save the receipt of order {order} as {file}",
                "keepExistingReceipt: Ask to save the receipt of order {order} over {file}, then keep the file",
                "cancelOrder: Cancel order {order} from its tab",
                "keepOrder: Ask to cancel order {order} from its tab, then keep it",
                "receiptSaveErrorIs: Saving the receipt of order {order} as {file} fails with {message}",
                "orderStatusIs: The status on order {order}'s tab is {status}",
                "orderFactsAre: The customer, date and total on order {order}'s tab read {facts}",
                "orderItemsAre: The lines on order {order}'s Items tab are {lines}",
                "orderItemIs: The {column} of {product} on order {order}'s Items tab is {value}",
                "orderItemsSummaryIs: The units on order {order}'s Items tab read {summary}",
                "orderDeliveryIs: The {detail} on order {order}'s Delivery tab is {value}",
                "orderInvoiceIs: The {detail} on order {order}'s Invoice tab is {value}",
                "suggestedReceiptFileIs: Save receipt… on order {order} suggests {file}");
        assertThat(step("saveReceipt").outcomeBody()).isNotNull();
        assertThat(ORDER_TAB).hasSize(sentences().size() - 85);
    }

    @Test
    void anActStepSaysWhatItNeedsAndWhereItEndsAndACheckWhatItReads() {
        for (DomainStep s : steps) {
            boolean check = s.id().endsWith("Is") || s.id().endsWith("Are") || s.id().equals("newOrderIsFor")
                    || s.id().equals("buttonIsDisabled") || s.id().equals("buttonIsEnabled") || s.id().equals("aboutShows");
            assertThat(s.kind()).as(s.id()).isEqualTo(check ? DomainStep.Kind.CHECK : DomainStep.Kind.ACT);
            assertThat(s.about()).as(s.id()).isNotBlank();
            if (s.kind() == DomainStep.Kind.ACT) {
                assertThat(s.about()).as(s.id()).startsWith("Needs: ").contains(". Ends: ");
            }
            if (s.kind() == DomainStep.Kind.CHECK) assertThat(s.about()).as(s.id()).startsWith("Reads ");
        }
    }

    @Test
    void everyStepHasAPatternAndNoProblems() {
        // the list checks (orderItemsAre, cartLinesAre, reviewLinesAre, ordersListedAre, productsListedAre) have none on
        // purpose: a recording holds one row of the list they read, and a pattern of that row would offer a false
        // sentence ("The cart holds Stapler" for a cart of three lines)
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            assertThat(c.problems()).as(c.step().id()).isEmpty();
            // what session lists under vocabularyProblems besides dropped steps: a written pattern that names what a
            // pattern does not compare (a window) is a note here, and none is allowed
            assertThat(c.notes()).as(c.step().id()).isEmpty();
            if (NO_PATTERN.contains(c.step().id())) {
                assertThat(c.patterns()).as(c.step().id()).isEmpty();
                assertThat(c.step().match()).as(c.step().id()).isEmpty();
                continue;
            }
            assertThat(c.why()).as(c.step().id()).isNull();
            assertThat(c.patterns()).as(c.step().id()).isNotEmpty();
        }
    }

    /** The steps a recording never folds into or offers: no pattern at all. */
    private static final List<String> NO_PATTERN = Arrays.asList("orderItemsAre", "cartLinesAre", "reviewLinesAre",
            "ordersListedAre", "productsListedAre", "showOrders", "cancelQuestionIs", "suggestedReceiptFileIs");

    @Test
    void everyStepNamesItsJavaMethod() throws Exception {
        for (DomainStep s : steps) {
            Class<?> type = Class.forName(s.javaClass());
            assertThat(s.javaClass()).as(s.id()).isIn(OrderDeskSteps.class.getName(), OrderDeskNewOrderSteps.class.getName(),
                    OrderDeskCheckoutSteps.class.getName(), OrderDeskOrdersSteps.class.getName(), OrderDeskMainSteps.class.getName(),
                    OrderDeskOrderTabSteps.class.getName());
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
        assertThat(method(OrderDeskOrdersSteps.class, "sortOrders").getReturnType()).isEqualTo(Map.class);
        assertThat(method(OrderDeskOrdersSteps.class, "showOrders").getReturnType()).isEqualTo(Map.class);
        assertThat(method(OrderDeskOrdersSteps.class, "searchOrders").getReturnType()).isEqualTo(Map.class);
        assertThat(method(OrderDeskOrderTabSteps.class, "saveReceipt").getReturnType()).isEqualTo(Map.class);
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
        List<String> elsewhere = new ArrayList<String>(Arrays.asList("openNewOrder", "recentCustomersAre",
                "openNewOrderFromRecent"));
        elsewhere.addAll(ORDERS_LIST);
        elsewhere.addAll(MAIN_WINDOW);
        elsewhere.addAll(ORDER_TAB);
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
    void eachOrdersListAndMainWindowStepFailsFastWhenItsScreenIsNotShowing() {
        // no main window: every step of the Orders list and the main window says so within about a second, and none
        // takes a step (no File ▸ Exit, above all)
        int checked = 0;
        for (final DomainStep s : steps) {
            if (!ORDERS_LIST.contains(s.id()) && !MAIN_WINDOW.contains(s.id())) continue;
            final FakeApp nothing = new FakeApp();
            final Map<String, String> v = sample(s.id());
            long start = System.currentTimeMillis();

            Throwable gone = catchThrowable(() -> s.body().run(nothing.client(), new Params() {
                @Override public String text(String name) { return v.get(name); }
                @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
            }));

            assertThat(System.currentTimeMillis() - start).as(s.id()).isLessThan(2000);
            assertThat(gone).as(s.id()).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NO_MAIN);
            assertThat(nothing.steps()).as(s.id()).isEmpty();
            checked++;
        }
        assertThat(checked).isEqualTo(ORDERS_LIST.size() + MAIN_WINDOW.size());
    }

    @Test
    void eachOrderTabStepFailsFastWhenItsScreenIsNotShowing() {
        // no main window: every step of an order's own tab says so within about a second, and none takes a step
        int checked = 0;
        for (final DomainStep s : steps) {
            if (!ORDER_TAB.contains(s.id())) continue;
            final FakeApp nothing = new FakeApp();
            final Map<String, String> v = sample(s.id());
            long start = System.currentTimeMillis();

            Throwable gone = catchThrowable(() -> s.body().run(nothing.client(), new Params() {
                @Override public String text(String name) { return v.get(name); }
                @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
            }));

            assertThat(System.currentTimeMillis() - start).as(s.id()).isLessThan(2000);
            assertThat(gone).as(s.id()).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NO_MAIN);
            assertThat(nothing.steps()).as(s.id()).isEmpty();
            checked++;
        }
        assertThat(checked).isEqualTo(ORDER_TAB.size());
    }

    @Test
    void everyWrittenPatternNamesWhatItsBodyClicks() throws IOException {
        // a written pattern (match) and the body take their names from OrderDeskSteps' constants; this pins that a
        // rename there reaches both: each entry's fixed parts equal the body's step at the same place. A pattern that
        // only suggests its step (autoFold false) is looser than the body on purpose, and a check that only reads
        // (newOrderIsFor) takes no steps to compare; a cell check is compared as the body writes it (asBodyStep). A
        // check's matchAlso is compared too (each is another way to record the same read); an act's matchAlso is
        // another way to do it (a row menu for a button), not the body's
        int checked = 0;
        for (DomainStep s : steps) {
            if (s.match().isEmpty() || s.match().get(0).contains("\"use\"") || !s.autoFold()) continue;
            JsonNode body = run(s.id(), sample(s.id()));
            if (body.size() == 0) continue;
            List<List<String>> written = new ArrayList<List<String>>(Collections.singletonList(s.match()));
            if (s.kind() == DomainStep.Kind.CHECK) written.addAll(s.matchAlso());
            for (List<String> match : written) {
                assertThat(body.size()).as(s.id() + " steps").isGreaterThanOrEqualTo(match.size());
                for (int i = 0; i < match.size(); i++) {
                    JsonNode entry = asBodyStep(JSON.readTree(match.get(i)));
                    assertThat(fixedParts(entry, body.get(i))).as(s.id() + " match " + (i + 1) + " vs " + body.get(i)).isEmpty();
                    checked++;
                }
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
    void aNewOrderForARecentCustomerFoldsIntoOpenNewOrderFromRecent() {
        // the toolbar's ▾ and a pick replay exactly as the step that takes the same way, not as File ▸ New order
        JsonNode recent = json("{'action':'click','target':{'window':'main','role':'button',"
                + "'name':'New order for a recent customer'}}");
        JsonNode pick = json("{'action':'pick','path':['Birch & Co']}");

        Pattern.Match m = null;
        for (Pattern p : patterns("openNewOrderFromRecent")) if (m == null) m = p.match(Arrays.asList(recent, pick));
        Pattern.Match fileMenu = null;
        for (Pattern p : patterns("openNewOrder")) if (fileMenu == null) fileMenu = p.match(Arrays.asList(recent, pick));

        assertThat(m).isNotNull();
        assertThat(m.params()).containsEntry("customer", "Birch & Co");
        assertThat(fileMenu).isNull();
        assertThat(folds(recent, pick)).containsExactly("openNewOrderFromRecent");
    }

    @Test
    void aThemeFromTheViewMenuFoldsIntoSwitchTheme() {
        JsonNode dark = json("{'action':'menu','path':['View','Theme','Dark']}");

        assertThat(folds(dark)).containsExactly("switchTheme");
        assertThat(pattern("switchTheme").match(Collections.singletonList(dark)).params()).containsEntry("theme", "Dark");
    }

    @Test
    void aClickOnTheOrdersTabThenASearchStillFoldsIntoSearchOrders() {
        // Show the Orders list has no pattern: a person's click on the Orders tab is no reading
        JsonNode tab = json("{'action':'click','target':{'window':'main','role':'tab','name':'Orders'}}");
        JsonNode search = json("{'action':'type','target':{'window':'main','scope':['tab:Orders'],'role':'text','name':'Search'},'text':'ada'}");

        assertThat(folds(tab, search)).containsExactly("searchOrders");
    }

    @Test
    void anAgentsRowCountOfTheOrdersFoldsIntoOrdersCountIs() {
        JsonNode count = json("{'action':'expect','target':{'scope':['tab:Orders'],'role':'table'},'rows':{'op':'eq','value':2}}");
        JsonNode cart = json("{'action':'expect','target':{'scope':['tab:New order · Ada Shop Ltd'],'role':'table'},"
                + "'rows':{'op':'eq','value':2}}");

        assertThat(pattern("ordersCountIs").match(Collections.singletonList(count)).params()).containsEntry("count", "2");
        assertThat(pattern("ordersCountIs").match(Collections.singletonList(cart))).isNull();
    }

    @Test
    void aPersonsCheckOfTheChosenRadioFoldsIntoCheckoutChoiceIs() {
        JsonNode card = json("{'action':'expect','target':{'window':'dialog','role':'radio','name':'Card'},'state':['checked']}");

        assertThat(pattern("checkoutChoiceIs").match(Collections.singletonList(card)).params()).containsEntry("option", "Card");
    }

    @Test
    void aClickOnAProductOnlySuggestsSelectProductsAndAClickThenAddToCartStillFoldsIntoAddToCart() {
        // a recording folds a run as soon as it fits: an auto-folding select would take the click before Add to cart
        JsonNode lamp = json("{'action':'click','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'SKU':'DSK-LAMP','Product':'Desk lamp','Price':'€39.00','Stock':'Out of stock'},"
                + "'column':'Product'}}");

        assertThat(step("selectProducts").autoFold()).isFalse();
        assertThat(pattern("selectProducts").match(Collections.singletonList(lamp)).params())
                .containsEntry("products", "Desk lamp");
        assertThat(folds(lamp)).isEmpty();
        assertThat(folds(lamp, addToCartButton())).containsExactly("addToCart");
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
    void theBundledCartTotalsLineTotalCheckFoldsIntoCartLineIs() throws IOException {
        // the agent's shape of a cell check (the target with its row and column, then text), as cart-totals.json holds
        // it, seen as the recorder sees the cell (every cell of its row)
        JsonNode check = null;
        for (JsonNode step : JSON.readTree(new java.io.File("../uitest/cart-totals.json")).path("steps")) {
            for (JsonNode c : step.path("checks")) if ("The A4 paper line comes to €14.70".equals(c.path("say").asText())) check = c;
        }
        assertThat(check).isNotNull();
        com.fasterxml.jackson.databind.node.ObjectNode recorded = check.deepCopy();
        recorded.remove("say");
        recorded.put("action", "expect");
        JsonNode seen = json("{'target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],'role':'table',"
                + "'row':{'Product':'A4 paper, 500 sheets','Qty':'3','Price':'€4.90','Line total':'€14.70'},'column':'Line total'}}");

        JsonNode form = Matcher.matchForm(recorded, seen, false);

        assertThat(form.path("text").asText()).isEqualTo("€14.70");
        assertThat(matchAny("cartLineIs", form)).containsEntry("product", "A4 paper, 500 sheets")
                .containsEntry("column", "Line total").containsEntry("value", "€14.70");
        assertThat(first(form)).isEqualTo("cartLineIs");
    }

    @Test
    void anAgentsCheckOnALineTotalFoldsIntoCartLineIsNotIntoAProductCheck() {
        JsonNode lineTotal = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'A4 paper, 500 sheets','Qty':'3','Price':'€4.90','Line total':'€14.70'},"
                + "'column':'Line total'},'text':'€14.70'}");
        JsonNode price = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'SKU':'DSK-STAPLER','Product':'Stapler','Price':'€12.50','Stock':'4'},"
                + "'column':'Price'},'text':'€12.50'}");

        assertThat(matchAny("cartLineIs", lineTotal)).containsEntry("product", "A4 paper, 500 sheets")
                .containsEntry("column", "Line total").containsEntry("value", "€14.70");
        assertThat(matchAny("productIs", lineTotal)).isNull();
        assertThat(matchAny("productIs", price)).containsEntry("product", "Stapler").containsEntry("column", "Price")
                .containsEntry("value", "€12.50");
        assertThat(matchAny("cartLineIs", price)).isNull();
    }

    @Test
    void anAgentsCheckOfAnOrdersCellFoldsIntoOrderIsAndOfAReviewLineIntoReviewLineIs() {
        JsonNode status = json("{'action':'expect','target':{" + ORDERS_TABLE + "," + ORD_1639 + ",'column':'Status'},'text':'Placed'}");
        JsonNode cart = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],'role':'table',"
                + "'row':{'Product':'Stapler','Qty':'1','Price':'€12.50','Line total':'€12.50'},'column':'Qty'},'text':'1'}");
        JsonNode review = json("{'action':'expect','target':{'window':'dialog','role':'table','name':'reviewLines',"
                + "'row':{'Product':'Stapler','Qty':'1','Price':'€12.50','Line total':'€12.50'},'column':'Qty'},'text':'1'}");

        assertThat(matchAny("orderIs", status)).containsEntry("order", "ORD-1639").containsEntry("column", "Status")
                .containsEntry("value", "Placed");
        assertThat(matchAny("orderIs", cart)).isNull();
        assertThat(matchAny("reviewLineIs", review)).containsEntry("product", "Stapler").containsEntry("column", "Qty");
        // the review's table has a name of its own: a cart line is no review line
        assertThat(matchAny("reviewLineIs", cart)).isNull();
    }

    @Test
    void thePersonsCheckOfACartLineOffersCartLineIsAndOfAProductProductIs() {
        // as the palette's Check menu records a cell check: the bare table, then the cell by its row key and column
        JTable cart = table(CART, line("A4 paper, 500 sheets", "3", "€4.90", "€14.70"), line("Stapler", "1", "€12.50", "€12.50"));
        JTable products = table(PRODUCTS, line("PAP-A4-500", "A4 paper, 500 sheets", "€4.90", "120"),
                line("DSK-STAPLER", "Stapler", "€12.50", "4"));

        List<String> onLineTotal = offered(CheckMenuForms.cell(cart, 0, 3, "main", NEW_ORDER));
        List<String> onPrice = offered(CheckMenuForms.cell(products, 1, 2, "main", NEW_ORDER));

        assertThat(onLineTotal).contains("cartLineIs{column=Line total, product=A4 paper, 500 sheets, value=€14.70}")
                .noneMatch(c -> c.startsWith("productIs"));
        // the products table's rows are keyed by SKU, the first column: productIs takes the SKU for the product
        assertThat(onPrice).contains("productIs{column=Price, product=DSK-STAPLER, value=€12.50}")
                .noneMatch(c -> c.startsWith("cartLineIs"));
    }

    @Test
    void thePersonsCheckOfAKeyCellOffersAStepThatPasses() {
        // a check on the cell that names the row (the cart's Product, the products' SKU) is offered too
        JTable cart = table(CART, line("Stapler", "1", "€12.50", "€12.50"));
        JTable products = table(PRODUCTS, line("DSK-STAPLER", "Stapler", "€12.50", "4"));

        assertThat(offered(CheckMenuForms.cell(cart, 0, 0, "main", NEW_ORDER)))
                .contains("cartLineIs{column=Product, product=Stapler, value=Stapler}");
        assertThat(offered(CheckMenuForms.cell(products, 0, 0, "main", NEW_ORDER)))
                .contains("productIs{column=SKU, product=DSK-STAPLER, value=DSK-STAPLER}");
        assertThat(offered(CheckMenuForms.cell(products, 0, 1, "main", NEW_ORDER)))
                .contains("productIs{column=Product, product=DSK-STAPLER, value=Stapler}");
    }

    @Test
    void thePersonsCheckOfAReviewLineOffersReviewLineIsAndOfACartLineDoesNot() {
        // the review's table is named reviewLines (the app names it for a plugin): only it offers reviewLineIs
        JTable review = table(CART, line("Stapler", "1", "€12.50", "€12.50"));
        review.setName("reviewLines");
        JTable cart = table(CART, line("Stapler", "1", "€12.50", "€12.50"));

        assertThat(offered(CheckMenuForms.cell(review, 0, 1, "dialog", null)))
                .contains("reviewLineIs{column=Qty, product=Stapler, value=1}")
                .noneMatch(c -> c.startsWith("cartLineIs"));
        assertThat(offered(CheckMenuForms.cell(cart, 0, 1, "main", NEW_ORDER)))
                .contains("cartLineIs{column=Qty, product=Stapler, value=1}")
                .noneMatch(c -> c.startsWith("reviewLineIs"));
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

    // ---- the Orders list and the main window: what folds, and what only looks like it

    private static final String ORD_1639 = "'row':{'Order':'ORD-1639','Date':'9 Oct 2026','Customer':'Ada Shop Ltd',"
            + "'Items':'55','Total':'€95.52','Status':'Placed'}";
    private static final String ORD_1638 = "'row':{'Order':'ORD-1638','Date':'7 Oct 2026','Customer':'Café Lumen',"
            + "'Items':'8','Total':'€181.44','Status':'Placed'}";
    private static final String ORDERS_TABLE = "'window':'main','scope':['tab:Orders'],'role':'table'";

    @Test
    void aPersonsCancelOfOneOrderFoldsIntoCancelOrders() {
        JsonNode click = json("{'action':'click','target':{" + ORDERS_TABLE + "," + ORD_1639 + ",'column':'Customer'}}");
        JsonNode menu = json("{'action':'contextMenu','target':{" + ORDERS_TABLE + "," + ORD_1639 + "},'path':['Cancel orders…']}");
        JsonNode yes = json("{'action':'answer','button':'Cancel order'}");

        assertThat(pattern("cancelOrders").match(Arrays.asList(click, menu, yes)).params()).containsEntry("orders", "ORD-1639");
        // a right-click alone selects the order under it: the order cancelled is that one
        assertThat(pattern("cancelOrders").match(Arrays.asList(menu, yes)).params()).containsEntry("orders", "ORD-1639");
        assertThat(step("cancelOrders").autoFold()).isTrue();
    }

    @Test
    void aCancelOfSeveralOrdersOrOfAnotherRowThanTheOneClickedIsNoCancelOrdersFold() {
        // "Cancel orders" answers a question about several selected orders: one captured order cannot replay it
        JsonNode add = json("{'action':'click','target':{" + ORDERS_TABLE + "," + ORD_1638 + "},'modifiers':['Mod']}");
        JsonNode menu = json("{'action':'contextMenu','target':{" + ORDERS_TABLE + "," + ORD_1638 + "},'path':['Cancel orders…']}");
        JsonNode several = json("{'action':'answer','button':'Cancel orders'}");
        JsonNode click = json("{'action':'click','target':{" + ORDERS_TABLE + "," + ORD_1639 + "}}");
        JsonNode one = json("{'action':'answer','button':'Cancel order'}");

        for (Pattern p : patterns("cancelOrders")) {
            assertThat(p.match(Arrays.asList(add, menu, several))).isNull();
            assertThat(p.match(Arrays.asList(menu, several))).isNull();
            assertThat(p.match(Arrays.asList(click, menu, one))).isNull();
        }
    }

    @Test
    void theOrderTabsOwnCancelOrderFoldsIntoCancelOrderNotCancelOrders() {
        JsonNode button = json("{'action':'click','target':{'window':'main','scope':['tab:ORD-1639'],'role':'button','name':'Cancel order'}}");
        JsonNode yes = json("{'action':'answer','button':'Cancel order'}");
        JsonNode keep = json("{'action':'answer','button':'Keep'}");
        JsonNode tab = json("{'action':'click','target':{'window':'main','role':'tab','name':'ORD-1639'}}");

        assertThat(first(button, yes)).isEqualTo("cancelOrder");
        assertThat(first(tab, button, yes)).isEqualTo("cancelOrder");
        assertThat(choices(button, yes)).noneMatch(c -> c.step.equals("cancelOrders"));
        assertThat(pattern("cancelOrder").match(Arrays.asList(button, yes)).params()).containsEntry("order", "ORD-1639");
        assertThat(first(button, keep)).isEqualTo("keepOrder");
    }

    @Test
    void closeTabAndDiscardFoldIntoCloseTabAndCancelIntoKeepTab() {
        // the bundled cart-totals test ends with these two steps
        JsonNode close = json("{'action':'menu','path':['File','Close tab']}");
        JsonNode discard = json("{'action':'answer','button':'Discard'}");
        JsonNode cancel = json("{'action':'answer','button':'Cancel'}");

        assertThat(first(close, discard)).isEqualTo("closeTab");
        assertThat(first(close)).isEqualTo("closeTab");
        assertThat(first(close, cancel)).isEqualTo("keepTab");
    }

    @Test
    void aCheckOfOneRowIsNeverOfferedAsACheckOfTheWholeList() {
        // a person checks one row of a list; "The cart holds Stapler" for a cart of three lines fails on the next replay
        JTable cart = table(CART, line("A4 paper, 500 sheets", "3", "€4.90", "€14.70"), line("Stapler", "1", "€12.50", "€12.50"),
                line("Gel pen, black", "2", "€1.20", "€2.40"));
        JTable review = table(CART, line("A4 paper, 500 sheets", "3", "€4.90", "€14.70"), line("Stapler", "1", "€12.50", "€12.50"),
                line("Gel pen, black", "2", "€1.20", "€2.40"));
        review.setName("reviewLines");
        JTable orders = table(OrdersFake.COLUMNS, OrdersFake.ORDERS.toArray(new List[0]));
        JTable products = table(PRODUCTS, line("PAP-A4-500", "A4 paper, 500 sheets", "€4.90", "120"),
                line("DSK-STAPLER", "Stapler", "€12.50", "4"), line("PEN-GEL-BLK", "Gel pen, black", "€1.20", "300"));
        List<String> lists = Arrays.asList("cartLinesAre", "reviewLinesAre", "ordersListedAre", "productsListedAre");

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < CART.size(); col++) {
                assertThat(offered(CheckMenuForms.cell(cart, row, col, "main", NEW_ORDER)))
                        .as("cart " + row + "," + col).noneMatch(c -> lists.contains(c.substring(0, c.indexOf('{'))));
                assertThat(offered(CheckMenuForms.cell(review, row, col, "dialog", null)))
                        .as("review " + row + "," + col).noneMatch(c -> lists.contains(c.substring(0, c.indexOf('{'))));
                assertThat(offered(CheckMenuForms.cell(products, row, col, "main", NEW_ORDER)))
                        .as("products " + row + "," + col).noneMatch(c -> lists.contains(c.substring(0, c.indexOf('{'))));
            }
            for (int col = 0; col < OrdersFake.COLUMNS.size(); col++) {
                assertThat(offered(CheckMenuForms.cell(orders, row, col, "main", Collections.singletonList("tab:Orders"))))
                        .as("orders " + row + "," + col).noneMatch(c -> lists.contains(c.substring(0, c.indexOf('{'))));
            }
        }
        // nor is a recorded check of one row (as an agent writes it) suggested as one
        JsonNode cartRow = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'Product':'Stapler','Line total':'€12.50'}},'shown':true}");
        JsonNode reviewRow = json("{'action':'expect','target':{'window':'dialog','role':'table','row':{'Product':'Stapler'}},"
                + "'shown':true}");
        JsonNode orderRow = json("{'action':'expect','target':{'window':'main','scope':['tab:Orders'],'role':'table',"
                + "'row':{'Order':'ORD-1638'}},'shown':true}");
        JsonNode productRow = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'table','row':{'SKU':'DSK-STAPLER','Product':'Stapler'}},'shown':true}");
        for (JsonNode one : Arrays.asList(cartRow, reviewRow, orderRow, productRow)) {
            assertThat(choices(one)).as(one.toString()).noneMatch(c -> lists.contains(c.step));
        }
        for (String id : lists) assertThat(step(id).match()).as(id).isEmpty();
    }

    @Test
    void thePersonsCheckOfAnOrdersCellOffersOrderIsAndACartOrItemsCellDoesNot() {
        // as the palette's Check menu records a cell check: the bare table, then the cell by its row key and column
        JTable orders = table(OrdersFake.COLUMNS, OrdersFake.ORDERS.toArray(new List[0]));
        JTable cart = table(CART, line("Stapler", "1", "€12.50", "€12.50"));
        JTable items = table(Arrays.asList("SKU", "Product", "Qty", "Price", "Line total"),
                line("DSK-STAPLER", "Stapler", "1", "€12.50", "€12.50"));

        assertThat(offered(CheckMenuForms.cell(orders, 0, 5, "main", Collections.singletonList("tab:Orders"))))
                .contains("orderIs{column=Status, order=ORD-1639, value=Placed}");
        assertThat(offered(CheckMenuForms.cell(orders, 2, 4, "main", Collections.singletonList("tab:Orders"))))
                .contains("orderIs{column=Total, order=ORD-1636, value=€40.56}");
        // the Order cell names the row; its check is offered too, and passes (OrderDeskOrdersStepsTest)
        assertThat(offered(CheckMenuForms.cell(orders, 1, 0, "main", Collections.singletonList("tab:Orders"))))
                .contains("orderIs{column=Order, order=ORD-1638, value=ORD-1638}");
        assertThat(offered(CheckMenuForms.cell(cart, 0, 1, "main", NEW_ORDER))).noneMatch(c -> c.startsWith("orderIs"));
        assertThat(offered(CheckMenuForms.cell(items, 0, 2, "main", Arrays.asList("tab:ORD-1639", "tab:Items"))))
                .noneMatch(c -> c.startsWith("orderIs"));
    }

    @Test
    void anOrdersSearchFoldsIntoSearchOrdersAndANewOrdersSearchIntoSearchProducts() {
        JsonNode orders = json("{'action':'type','target':{'window':'main','scope':['tab:Orders'],'role':'text','name':'Search'},'text':'ada'}");
        JsonNode products = json("{'action':'type','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],'role':'text',"
                + "'name':'Search'},'text':'pen'}");

        assertThat(first(orders)).isEqualTo("searchOrders");
        assertThat(first(products)).isEqualTo("searchProducts");
        assertThat(pattern("searchOrders").match(Collections.singletonList(products))).isNull();
    }

    @Test
    void theStatusLineAndTheEmptyListChecksFoldWhereTheyReplay() {
        // a named label as a recording sees it: its id from the recorded locator, its role and name (the code name)
        JsonNode status = json("{'action':'expect','target':{'id':'statusMessage','window':'main','role':'label',"
                + "'name':'statusMessage'},'text':'Order ORD-1640 placed'}");
        JsonNode empty = json("{'action':'expect','target':{'id':'ordersEmpty','window':'main','scope':['tab:Orders'],"
                + "'role':'label','name':'ordersEmpty'},'text':'No orders match ‘zz’'}");
        JsonNode products = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'label','name':'No products match ‘zz’'},'text':'No products match ‘zz’'}");

        assertThat(first(status)).isEqualTo("statusLineIs");
        assertThat(first(empty)).isEqualTo("ordersEmptyMessageIs");
        assertThat(pattern("ordersEmptyMessageIs").match(Collections.singletonList(products))).isNull();
    }

    @Test
    void aSelectedTabCheckOfANewOrderGoesToNewOrderIsForAndOfAnOrderToSelectedTabIs() {
        JsonNode newOrder = json("{'action':'expect','target':{'window':'main','role':'tab','name':'New order · Ada Shop Ltd'},"
                + "'state':['selected']}");
        JsonNode order = json("{'action':'expect','target':{'window':'main','role':'tab','name':'ORD-1639'},'state':['selected']}");

        assertThat(first(newOrder)).isEqualTo("newOrderIsFor");
        assertThat(first(order)).isEqualTo("selectedTabIs");
    }

    @Test
    void theOpenOrdersCountCheckFoldsIntoOpenOrdersCountIs() {
        JsonNode count = json("{'action':'expect','target':{'window':'main','role':'label','name':'4 open orders'},'text':'4 open orders'}");

        assertThat(pattern("openOrdersCountIs").match(Collections.singletonList(count)).params())
                .containsEntry("count", "4 open orders");
    }

    @Test
    void stepsWhoseRecordingCannotSayWhatTheyDidOnlySuggestThemselves() {
        // a header click sorts one way or the other; a click with Cmd held adds to a selection; one of two tabs with one
        // title; a menu or message check fits any popup or message; Exit's question depends on every new order's cart
        for (String id : Arrays.asList("sortOrders", "selectOrders", "keepOrders", "copyOrderNumbers", "ordersListedAre",
                "selectedOrdersAre", "selectTab", "openTabsAre", "menuItemIs", "cancelExit", "exitQuestionIs", "aboutShows")) {
            assertThat(step(id).autoFold()).as(id).isFalse();
        }
        JsonNode delivery = json("{'action':'click','target':{'window':'main','scope':['tab:ORD-1639'],'role':'tab','name':'Delivery'}}");
        assertThat(pattern("selectTab").match(Collections.singletonList(delivery)).params()).containsEntry("tab", "Delivery");
    }

    // ---- an order's own tab: what folds, and what only looks like it

    private static final List<String> ITEMS_SCOPE = Arrays.asList("tab:ORD-1639", "tab:Items");

    @Test
    void thePersonsCheckOfAnItemsCellOffersOrderItemIsAndNoCartProductOrOrdersCheck() {
        // the Check menu keys an Items line by its SKU (the first column with unique values)
        JTable items = table(OrderTabFake.ITEM_COLUMNS, OrderTabFake.LINES.toArray(new List[0]));

        List<String> qty = offered(CheckMenuForms.cell(items, 0, 2, "main", ITEMS_SCOPE));
        List<String> sku = offered(CheckMenuForms.cell(items, 2, 0, "main", ITEMS_SCOPE));

        assertThat(qty).contains("orderItemIs{column=Qty, order=ORD-1639, product=PEN-GEL-BLK, value=50}")
                .noneMatch(c -> c.startsWith("cartLineIs") || c.startsWith("productIs") || c.startsWith("orderIs"));
        assertThat(sku).contains("orderItemIs{column=SKU, order=ORD-1639, product=DSK-CLIPS, value=DSK-CLIPS}");
        // both replay: the SKU names the line, and the key cell is read too
        OrderDeskOrderTabSteps.orderItemIs(new OrderTabFake().client(), "ORD-1639", "PEN-GEL-BLK", "Qty", "50");
        OrderDeskOrderTabSteps.orderItemIs(new OrderTabFake().client(), "ORD-1639", "DSK-CLIPS", "SKU", "DSK-CLIPS");
        // a cart line is no order's line
        JTable cart = table(CART, line("Stapler", "1", "€12.50", "€12.50"));
        assertThat(offered(CheckMenuForms.cell(cart, 0, 1, "main", NEW_ORDER))).noneMatch(c -> c.startsWith("orderItemIs"));
    }

    @Test
    void anAgentsCheckOfAnItemsCellFoldsIntoOrderItemIs() {
        JsonNode cell = json("{'action':'expect','target':{'window':'main','scope':['tab:ORD-1639','tab:Items'],'role':'table',"
                + "'row':{'SKU':'PEN-GEL-BLK','Product':'Gel pen, black','Qty':'50','Price':'€1.20','Line total':'€60.00'},"
                + "'column':'Line total'},'text':'€60.00'}");

        assertThat(matchAny("orderItemIs", cell)).containsEntry("order", "ORD-1639").containsEntry("product", "Gel pen, black")
                .containsEntry("column", "Line total").containsEntry("value", "€60.00");
        assertThat(matchAny("cartLineIs", cell)).isNull();
        assertThat(first(cell)).isEqualTo("orderItemIs");
    }

    @Test
    void theOrderTabsNamedValuesFoldWhereTheyReplay() {
        JsonNode status = json("{'action':'expect','target':{'id':'orderStatus','window':'main','scope':['tab:ORD-1639'],"
                + "'role':'label','name':'orderStatus'},'text':'Placed'}");
        JsonNode facts = json("{'action':'expect','target':{'id':'orderFacts','window':'main','scope':['tab:ORD-1639'],"
                + "'role':'label','name':'orderFacts'},'text':'Ada Shop Ltd · 9 Oct 2026 · €95.52'}");
        JsonNode units = json("{'action':'expect','target':{'id':'itemsSummary','window':'main','scope':['tab:ORD-1639','tab:Items'],"
                + "'role':'label','name':'itemsSummary'},'text':'55 units in 3 lines'}");
        JsonNode total = json("{'action':'expect','target':{'window':'main','scope':['tab:ORD-1639','tab:Invoice'],'role':'label',"
                + "'name':'Total'},'text':'€95.52'}");
        JsonNode cartTotal = json("{'action':'expect','target':{'window':'main','scope':['tab:New order · Ada Shop Ltd'],"
                + "'role':'label','name':'Total'},'text':'€31.32'}");

        assertThat(first(status)).isEqualTo("orderStatusIs");
        assertThat(pattern("orderStatusIs").match(Collections.singletonList(status)).params())
                .containsEntry("order", "ORD-1639").containsEntry("status", "Placed");
        assertThat(first(facts)).isEqualTo("orderFactsAre");
        assertThat(first(units)).isEqualTo("orderItemsSummaryIs");
        assertThat(first(total)).isEqualTo("orderInvoiceIs");
        assertThat(pattern("orderInvoiceIs").match(Collections.singletonList(total)).params()).containsEntry("detail", "Total");
        assertThat(first(cartTotal)).isEqualTo("cartTotalIs");
        assertThat(pattern("orderInvoiceIs").match(Collections.singletonList(cartTotal))).isNull();
    }

    @Test
    void aDeliveryCheckOnlySuggestsOrderDeliveryIs() {
        // the Delivery tab's caption and address lines are labels too: a pattern cannot tell them from a captioned value
        JsonNode method = json("{'action':'expect','target':{'window':'main','scope':['tab:ORD-1639','tab:Delivery'],"
                + "'role':'label','name':'Shipping method'},'text':'Standard · 3–5 business days · €4.90'}");

        assertThat(pattern("orderDeliveryIs").match(Collections.singletonList(method)).params())
                .containsEntry("detail", "Shipping method");
        assertThat(step("orderDeliveryIs").autoFold()).isFalse();
    }

    private static final JsonNode SAVE = json("{'action':'click','target':{'window':'main','scope':['tab:ORD-1639'],"
            + "'role':'button','name':'Save receipt…'}}");
    private static final JsonNode CHOOSE = json("{'action':'chooseFile','path':'/Users/ada/ORD-1639.txt'}");
    /** A person's Check between two steps, as the recording holds it: a barrier. */
    private static final Object CHECK = "check";

    @Test
    void aSaveWhoseOutcomeTheClicksCannotSayNeverFoldsOnItsOwn() {
        // Save receipt… and the file chooser's choice end in a save, the replace question or the error: the fold is made
        // as soon as the choice is recorded, before the answer, so saveReceipt (it answers Replace, fails on the error)
        // only suggests itself
        JsonNode replace = json("{'action':'answer','button':'Replace'}");
        JsonNode ok = json("{'action':'answer','button':'OK'}");
        JsonNode cancel = json("{'action':'answer','button':'Cancel'}");
        JsonNode tab = json("{'action':'click','target':{'window':'main','role':'tab','name':'ORD-1639'}}");

        assertThat(folds(SAVE, CHOOSE)).isEmpty();
        assertThat(folds(SAVE, CHOOSE, replace)).isEmpty();
        // the error, checked or not, then OK
        assertThat(folds(SAVE, CHOOSE, CHECK, ok)).isEmpty();
        assertThat(folds(SAVE, CHOOSE, ok)).isEmpty();
        // the replace question checked, then kept: the check splits the run, nothing folds
        assertThat(folds(SAVE, CHOOSE, CHECK, cancel)).isEmpty();
        assertThat(folds(tab, SAVE, CHOOSE, CHECK, cancel)).isEmpty();
        // saveReceipt is still suggested, with its values
        assertThat(pattern("saveReceipt").match(Arrays.asList(SAVE, CHOOSE, replace)).params())
                .containsEntry("order", "ORD-1639").containsEntry("file", "/Users/ada/ORD-1639.txt");
        assertThat(step("saveReceipt").autoFold()).isFalse();
    }

    @Test
    void keepingAFileThatExistsFoldsOnlyWithItsCancelInTheSameRun() {
        // Cancel answers only the replace question (the error has OK only), and a fold never spans a check
        JsonNode cancel = json("{'action':'answer','button':'Cancel'}");

        assertThat(folds(SAVE, CHOOSE, cancel)).containsExactly("keepExistingReceipt");
        assertThat(folds(SAVE, CHOOSE, CHECK, cancel)).isEmpty();
    }

    @Test
    void anOrderTabsButtonStateIsOfferedAsTheCheckMenuRecordsIt() {
        // the Check menu offers one-step checks only: Save receipt… enabled is buttonIsEnabled; a disabled Cancel order
        // and its reason are read by buttonIsDisabled in a do step, which no single check the menu records can be
        JsonNode enabled = json("{'action':'expect','target':{'window':'main','scope':['tab:ORD-1639'],'role':'button',"
                + "'name':'Save receipt…'},'state':['!disabled']}");
        JsonNode disabled = json("{'action':'expect','target':{'window':'main','scope':['tab:ORD-1636'],'role':'button',"
                + "'name':'Cancel order'},'state':['disabled']}");

        assertThat(offered(Collections.singletonList(enabled))).contains("buttonIsEnabled{button=Save receipt…}");
        assertThat(offered(Collections.singletonList(disabled))).noneMatch(c -> c.startsWith("buttonIsDisabled"));
        assertThat(step("buttonIsDisabled").matchAlso()).isEmpty();
    }

    @Test
    void anOrderTabsChecksThatCannotBePinnedDownOnlySuggestThemselves() {
        for (String id : Arrays.asList("saveReceipt", "orderItemsAre", "orderDeliveryIs", "receiptSaveErrorIs")) {
            assertThat(step(id).autoFold()).as(id).isFalse();
        }
        for (String id : Arrays.asList("keepExistingReceipt", "cancelOrder", "keepOrder", "orderStatusIs",
                "orderFactsAre", "orderItemIs", "orderItemsSummaryIs", "orderInvoiceIs")) {
            assertThat(step(id).autoFold()).as(id).isTrue();
        }
    }

    // ---- helpers

    /** Every step that matches the whole run {@code raw}, the most specific first (as a recording weighs them). */
    private List<Matcher.Choice> choices(JsonNode... raw) {
        List<Matcher.Candidate> vocabulary = new ArrayList<Matcher.Candidate>();
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            vocabulary.add(new Matcher.Candidate(c.step().id(), c.patterns(), c.step().autoFold()));
        }
        List<Matcher.Item> items = new ArrayList<Matcher.Item>();
        for (int i = 0; i < raw.length; i++) items.add(Matcher.Item.raw(i, "person", raw[i], false, true));
        return Matcher.matches(items, vocabulary, System.nanoTime() + 5_000_000_000L);
    }

    private static final List<String> CART = Arrays.asList("Product", "Qty", "Price", "Line total");
    private static final List<String> PRODUCTS = Arrays.asList("SKU", "Product", "Price", "Stock");
    private static final List<String> NEW_ORDER = Collections.singletonList("tab:New order · Ada Shop Ltd");

    /**
     * What the Check menu offers of the plugin's checks for {@code forms} (one element's checks, as
     * {@link CheckMenuForms} makes them), as {@code UiMcpSession.domainChecks} matches them: the checks whose pattern is
     * one step, each with its values ("orderIs{column=Status, order=ORD-1639, value=Placed}").
     */
    private List<String> offered(List<JsonNode> forms) {
        List<Matcher.Candidate> vocabulary = new ArrayList<Matcher.Candidate>();
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            if (c.step().kind() != DomainStep.Kind.CHECK) continue;
            List<Pattern> one = new ArrayList<Pattern>();
            for (Pattern p : c.patterns()) if (Matcher.oneStep(p)) one.add(p);
            if (!one.isEmpty()) vocabulary.add(new Matcher.Candidate(c.step().id(), one, false));
        }
        List<String> out = new ArrayList<String>();
        for (JsonNode form : forms) {
            List<Matcher.Choice> choices = Matcher.matches(Collections.singletonList(Matcher.Item.raw(0, "person", form,
                    false, false)), vocabulary, System.nanoTime() + 5_000_000_000L);
            for (Matcher.Choice c : choices) {
                String one = c.step + new java.util.TreeMap<String, String>(c.params);
                if (!out.contains(one)) out.add(one);
            }
        }
        return out;
    }

    /** A table as the app shows one: these columns, these rows of text. */
    @SafeVarargs
    private static JTable table(List<String> columns, List<String>... rows) {
        DefaultTableModel model = new DefaultTableModel(columns.toArray(), 0);
        for (List<String> r : rows) model.addRow(r.toArray());
        return new JTable(model);
    }

    private static List<String> line(String... cells) {
        return Arrays.asList(cells);
    }

    /** The values of the first of step {@code id}'s patterns (its match, then each matchAlso) that {@code step} fits; null: none. */
    private Map<String, String> matchAny(String id, JsonNode step) {
        for (Pattern p : patterns(id)) {
            Pattern.Match m = p.match(Collections.singletonList(step));
            if (m != null) return m.params();
        }
        return null;
    }

    /**
     * The steps a recording of {@code recorded} folds into on its own, in order, as the agent's session folds a person's
     * steps while they are recorded: each step in turn, the act steps only (a check is the person's, never a fold), a
     * {@link #CHECK} a barrier between them.
     */
    private List<String> folds(Object... recorded) {
        List<Matcher.Candidate> vocabulary = new ArrayList<Matcher.Candidate>();
        for (PatternCompiler.Compiled c : PatternCompiler.compile(steps, OrderDeskVocabularyTest::trace)) {
            if (c.step().kind() != DomainStep.Kind.ACT || c.patterns().isEmpty()) continue;
            vocabulary.add(new Matcher.Candidate(c.step().id(), c.patterns(), c.step().autoFold()));
        }
        List<Matcher.Item> items = new ArrayList<Matcher.Item>();
        for (int i = 0; i < recorded.length; i++) {
            items.add(recorded[i] == CHECK ? Matcher.Item.check(i, "person")
                    : Matcher.Item.raw(i, "person", (JsonNode) recorded[i], false, true));
        }
        List<String> out = new ArrayList<String>();
        for (Matcher.Fold f : Matcher.fold(items, vocabulary, System.nanoTime() + 5_000_000_000L).folds) out.add(f.choice.step);
        return out;
    }

    /** The step the run {@code raw} folds into first; null for none. */
    private String first(JsonNode... raw) {
        List<Matcher.Choice> c = choices(raw);
        return c.isEmpty() ? null : c.get(0).step;
    }

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
     * new order ({@link #app}), the checkout's on {@link CheckoutFake}, the Orders list's and the main window's on
     * {@link OrdersFake} ({@link #mainWindow}).
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
        if ("openNewOrderFromRecent".equals(s.id())) {
            RecentFake recent = new RecentFake("Ada Shop Ltd");
            body.run(recent.client());
            return recent.steps();
        }
        if (CHECKOUT_ON.containsKey(s.id())) {
            CheckoutFake checkout = new CheckoutFake(CHECKOUT_ON.get(s.id()));
            if (CREDIT_WARNING_STEPS.contains(s.id())) checkout.creditWarning();
            body.run(checkout.client());
            return checkout.steps();
        }
        if (ORDERS_LIST.contains(s.id()) || MAIN_WINDOW.contains(s.id())) {
            FakeApp app = mainWindow(s.id());
            body.run(app.client());
            return app.steps();
        }
        if (ORDER_TAB.contains(s.id())) {
            FakeApp app = orderTab(s.id());
            body.run(app.client());
            return app.steps();
        }
        FakeApp app = app();
        body.run(app.client());
        return app.steps();
    }

    /** The main window as step {@code id} needs it, with every value its sample check expects. */
    private static FakeApp mainWindow(String id) {
        OrdersFake app = ON_A_CART.contains(id) ? OrderDeskMainStepsTest.onNewOrder("Stapler") : new OrdersFake();
        if ("sortOrders".equals(id)) app.sortable();
        if ("clearOrderSearch".equals(id)) app.searchHolds("ada");
        app.answer("tab 'ORD-1639'", FakeApp.node("tab", "ORD-1639"));
        app.answer("popup menuitem 'Close tab'", FakeApp.withState(FakeApp.node("menuitem", "Close tab"), "disabled"));
        app.answer("dialog:Exit label", FakeApp.label("OptionPane.label", "Discard the open new orders?"));
        app.answer("dialog:About Order Desk label", FakeApp.label("OptionPane.label", "Order Desk"),
                FakeApp.label("OptionPane.label", "A small, fictional order-entry app."));
        app.answer("dialog:Cancel order* label", FakeApp.label("OptionPane.label", "Cancel order ORD-1639?"));
        app.menu("View > Theme", "Light", "Dark", "Lagoon");
        return app;
    }

    /**
     * ORD-1639's own tab as step {@code id} needs it: the file it saves to exists (so the question is answered), or the
     * app cannot write it; the Delivery or Invoice tab showing for a check of it (a check of another tab clicks it first).
     */
    private static FakeApp orderTab(String id) {
        OrderTabFake app = new OrderTabFake();
        if ("saveReceipt".equals(id) || "keepExistingReceipt".equals(id)) app.withFile("ORD-1639.txt");
        if ("receiptSaveErrorIs".equals(id)) app.failing("/nope/x.txt");
        if ("orderDeliveryIs".equals(id)) app.onSubTab("Delivery");
        if ("orderInvoiceIs".equals(id)) app.onSubTab("Invoice");
        return app;
    }

    /** A new order with every value the sample checks expect, so each check passes. */
    private static FakeApp app() {
        FakeApp app = FakeApp.withNewOrder("Stapler", "Gel pen, black");
        app.answer("tab 'Orders'", FakeApp.label("Orders", "New order · Ada Shop Ltd"));
        app.answer("element #customerTerms", FakeApp.label("customerTerms", "Card only"));
        app.answer("button 'New order for a recent customer'", FakeApp.node("button", "New order for a recent customer"));
        app.answer("popup menuitem", FakeApp.node("menuitem", "Ada Shop Ltd"));
        app.answer("tab 'New order · Ada Shop Ltd'", FakeApp.node("tab", "New order · Ada Shop Ltd"));
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
     * A pattern entry as the body's step writes it: a cell check as the Check menu records it (the table, then
     * {@code cell}: row, column, expected) is the body's expect of that cell's text (the target with the row and column).
     */
    private static JsonNode asBodyStep(JsonNode entry) {
        if (!entry.has("cell")) return entry;
        com.fasterxml.jackson.databind.node.ObjectNode step = entry.deepCopy();
        com.fasterxml.jackson.databind.node.ObjectNode target = (com.fasterxml.jackson.databind.node.ObjectNode) step.get("target");
        JsonNode cell = step.remove("cell");
        target.set("row", cell.get("row"));
        target.set("column", cell.get("column"));
        step.set("text", cell.get("expected"));
        return step;
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
