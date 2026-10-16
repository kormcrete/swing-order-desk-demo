package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.agent.BodyTrace;
import io.github.kormcrete.swingui.client.TracingVerbs;
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

/** The vocabulary as the agent reads it when the plugin becomes ready, and how a person's steps fold into it. */
class OrderDeskVocabularyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<DomainStep> steps = new OrderDeskVocabulary(null).steps();

    @Test
    void theEightStepsWithTheirSentences() {
        List<String> sentences = new ArrayList<String>();
        for (DomainStep s : steps) sentences.add(s.id() + ": " + s.sentence());

        assertThat(sentences).containsExactly(
                "openNewOrder: Open a new order for {customer}",
                "addToCart: Add {product} to the cart",
                "setQuantity: Set the quantity of {product} to {qty}",
                "addQtyToCart: Add {qty} × {product} to the cart",
                "checkoutWithPo: Check out with PO number {po}",
                "placeOrder: Place the order",
                "openOrder: Open order {order}",
                "cartTotalIs: The cart total is {total}");
        assertThat(step("cartTotalIs").kind()).isEqualTo(DomainStep.Kind.CHECK);
        for (DomainStep s : steps) {
            if (!s.id().equals("cartTotalIs")) assertThat(s.kind()).as(s.id()).isEqualTo(DomainStep.Kind.ACT);
            assertThat(s.about()).as(s.id()).isNotBlank();
        }
        assertThat(step("setQuantity").params().get(1).type()).isEqualTo(ParamType.INT);
        assertThat(step("addQtyToCart").params().get(1).type()).isEqualTo(ParamType.INT);
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
    void everyStepNamesItsOrderDeskStepsMethod() throws Exception {
        for (DomainStep s : steps) {
            assertThat(s.javaClass()).as(s.id()).isEqualTo(OrderDeskSteps.class.getName());
            String call = s.javaCall();
            String name = call.substring(0, call.indexOf('('));
            int args = call.split(",").length;
            Method m = method(name);
            assertThat(m).as(call).isNotNull();
            assertThat(Modifier.isStatic(m.getModifiers())).as(call).isTrue();
            assertThat(m.getParameterCount()).as(call).isEqualTo(args);
        }
        assertThat(step("placeOrder").javaCall()).isEqualTo("placeOrder(ui)");
        assertThat(method("placeOrder").getReturnType()).isEqualTo(String.class);
    }

    @Test
    void eachBodyTakesTheStepsOfItsOrderDeskStepsMethod() {
        assertThat(run("openNewOrder", "customer", "Ada")).isEqualTo(traced(ui -> OrderDeskSteps.openNewOrder(ui, "Ada")));
        assertThat(run("addToCart", "product", "Gel pen, black"))
                .isEqualTo(traced(ui -> OrderDeskSteps.addToCart(ui, "Gel pen, black")));
        assertThat(run("setQuantity", "product", "Gel pen, black", "qty", "10"))
                .isEqualTo(traced(ui -> OrderDeskSteps.setQuantity(ui, "Gel pen, black", 10)));
        assertThat(run("addQtyToCart", "product", "A4 paper, 500 sheets", "qty", "3"))
                .isEqualTo(traced(ui -> OrderDeskSteps.addQtyToCart(ui, "A4 paper, 500 sheets", 3)));
        assertThat(run("checkoutWithPo", "po", "PO-7781"))
                .isEqualTo(traced(ui -> OrderDeskSteps.checkoutWithPo(ui, "PO-7781")));
        assertThat(run("placeOrder")).isEqualTo(traced(OrderDeskSteps::placeOrder));
        assertThat(run("openOrder", "order", "ORD-1640")).isEqualTo(traced(ui -> OrderDeskSteps.openOrder(ui, "ORD-1640")));
        assertThat(run("cartTotalIs", "total", "€31.32")).isEqualTo(traced(ui -> OrderDeskSteps.cartTotalIs(ui, "€31.32")));
    }

    @Test
    void everyWrittenPatternNamesWhatItsBodyClicks() throws IOException {
        // a written pattern (match) and the body take their names from OrderDeskSteps' constants; this pins that a
        // rename there reaches both: each entry's fixed parts equal the body's step at the same place
        int checked = 0;
        for (DomainStep s : steps) {
            if (s.match().isEmpty() || s.match().get(0).contains("\"use\"")) continue;
            Map<String, String> values = new HashMap<String, String>();
            for (DomainStep.Param p : s.params()) values.put(p.name(), "{" + p.name() + "}");
            JsonNode body = run(s.id(), values);
            assertThat(body.size()).as(s.id() + " steps").isGreaterThanOrEqualTo(s.match().size());
            for (int i = 0; i < s.match().size(); i++) {
                JsonNode entry = JSON.readTree(s.match().get(i));
                assertThat(fixedParts(entry, body.get(i))).as(s.id() + " match " + (i + 1) + " vs " + body.get(i)).isEmpty();
                checked++;
            }
        }
        assertThat(checked).isEqualTo(7);   // addToCart 2, placeOrder 2, openOrder 2, cartTotalIs 1
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

    private static Method method(String name) {
        for (Method m : OrderDeskSteps.class.getMethods()) if (m.getName().equals(name)) return m;
        return null;
    }

    /** The steps {@code id}'s body takes with these values (name, value, ...), on a Client that reaches no app. */
    private JsonNode run(String id, String... values) {
        Map<String, String> v = new HashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) v.put(values[i], values[i + 1]);
        return run(id, v);
    }

    private JsonNode run(String id, final Map<String, String> v) {
        Params params = new Params() {
            @Override public String text(String name) { return v.get(name); }
            @Override public int integer(String name) { return Integer.parseInt(v.get(name)); }
        };
        return traced(ui -> step(id).body().run(ui, params));
    }

    private static JsonNode traced(Body body) {
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
