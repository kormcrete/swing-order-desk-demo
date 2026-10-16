package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.UiActionFailed;
import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;
import io.github.kormcrete.swingui.spi.Vocabulary;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ADD_TO_CART_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ADD_TO_CART_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CUSTOMER_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CUSTOMER_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDERS_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.RECENT_CUSTOMER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SKU_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.TOTAL_LABEL;

/**
 * Order Desk's domain steps: what an agent runs as one {@code do} step ("Add Gel pen, black to the cart"), and what a
 * person's recorded clicks fold into. Each body calls {@link OrderDeskSteps}, the same method a Java test calls.
 *
 * <p>A step without {@code match} gets its pattern from its body (the agent traces it when the plugin becomes ready).
 * The others say theirs: {@code addToCart} matches a click anywhere on a product's row, not only its SKU cell;
 * {@code addQtyToCart} is {@code addToCart} then {@code setQuantity}; {@code openOrder} matches with or without the
 * click on the Orders tab; {@code placeOrder} and {@code cartTotalIs} read or check the app, which a trace cannot
 * follow. The patterns take the app's names from {@link OrderDeskSteps}' constants, so a rename is one change there.
 */
public final class OrderDeskVocabulary implements Vocabulary {

    private static final String STEPS = OrderDeskSteps.class.getName();
    /** A choices read gives up soon: it runs while the agent looks a step up. */
    private static final Duration READ = Duration.ofSeconds(1);
    /** Any open new order's tab, as a scope: "New order", "New order · Ada Shop Ltd". */
    private static final String NEW_ORDER_SCOPE = "[" + prefix("tab:" + NEW_ORDER_TAB) + "]";
    /** A product's row in the products table, by its name; the cart's lines have no SKU column. */
    private static final String PRODUCT_ROW = json("'scope':%s,'role':'table','row':{%s:'*',%s:'{product}'}",
            NEW_ORDER_SCOPE, key(SKU_COLUMN), key(PRODUCT_COLUMN));
    private static final String ADD_BUTTON = json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}",
            NEW_ORDER_SCOPE, text(ADD_TO_CART_BUTTON));

    private final Client live;

    /** {@code live}: a Client on the running app, for the choices (product names, recent customers); null for none. */
    public OrderDeskVocabulary(Client live) {
        this.live = live;
    }

    @Override
    public List<DomainStep> steps() {
        return Arrays.asList(
                DomainStep.act("openNewOrder", "Open a new order for {customer}")
                        .about("Opens a new order tab and picks its customer by name, or by the start of it (\"Ada\")")
                        .param("customer", ParamType.TEXT).choices(this::recentCustomers)
                        .code((ui, p) -> OrderDeskSteps.openNewOrder(ui, p.text("customer")))
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':%s}}", text(NEW_ORDER_BUTTON)),
                                json("{'action':'type','target':{'scope':[%s],'role':'text','name':%s},'text':'{customer}'}",
                                        text("tab:" + NEW_ORDER_TAB), text(CUSTOMER_FIELD)),
                                json("{'action':'press','keys':'Enter'}"))
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':%s}}", text(RECENT_CUSTOMER_BUTTON)),
                                json("{'action':'pick','path':['{customer}']}"))
                        .java(STEPS, "openNewOrder(ui, {customer})")
                        .build(),
                DomainStep.act("addToCart", "Add {product} to the cart")
                        .about("Adds one of a product to the cart of the open new order")
                        .param("product", ParamType.TEXT).choices(this::productNames)
                        .code((ui, p) -> OrderDeskSteps.addToCart(ui, p.text("product")))
                        .match(json("{'action':'click','target':{%s}}", PRODUCT_ROW), ADD_BUTTON)
                        .matchAlso(json("{'action':'contextMenu','target':{%s},'path':[%s]}", PRODUCT_ROW, text(ADD_TO_CART_ITEM)))
                        .java(STEPS, "addToCart(ui, {product})")
                        .build(),
                DomainStep.act("setQuantity", "Set the quantity of {product} to {qty}")
                        .about("Sets the quantity of a product already in the cart of the open new order")
                        .param("product", ParamType.TEXT).choices(this::productNames)
                        .param("qty", ParamType.INT)
                        .code((ui, p) -> OrderDeskSteps.setQuantity(ui, p.text("product"), p.integer("qty")))
                        .java(STEPS, "setQuantity(ui, {product}, {qty})")
                        .build(),
                DomainStep.act("addQtyToCart", "Add {qty} × {product} to the cart")
                        .about("Adds a product to the cart of the open new order and sets its quantity")
                        .param("product", ParamType.TEXT).choices(this::productNames)
                        .param("qty", ParamType.INT)
                        .code((ui, p) -> OrderDeskSteps.addQtyToCart(ui, p.text("product"), p.integer("qty")))
                        .match(json("{'use':'addToCart'}"), json("{'use':'setQuantity'}"))
                        .java(STEPS, "addQtyToCart(ui, {product}, {qty})")
                        .build(),
                DomainStep.act("checkoutWithPo", "Check out with PO number {po}")
                        .about("Checks out the open new order, paid by PO number; ends on the Review step, before Place order")
                        .param("po", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.checkoutWithPo(ui, p.text("po")))
                        .java(STEPS, "checkoutWithPo(ui, {po})")
                        .build(),
                DomainStep.act("placeOrder", "Place the order")
                        .about("Places the order on the checkout's Review step and confirms it; ends on the new order's tab")
                        .code((ui, p) -> OrderDeskSteps.placeOrder(ui))
                        .match(json("{'action':'click','target':{'role':'button','name':%s}}", text(PLACE_ORDER_BUTTON)),
                                json("{'action':'answer','button':%s}", text(PLACE_ORDER_ANSWER)))
                        .java(STEPS, "placeOrder(ui)")
                        .build(),
                DomainStep.act("openOrder", "Open order {order}")
                        .about("Opens a placed order (e.g. ORD-1640) from the Orders list in a tab of its own")
                        .param("order", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.openOrder(ui, p.text("order")))
                        .match(json("{'optional':true,'action':'click','target':{'role':'tab','name':%s}}", text(ORDERS_TAB)),
                                json("{'action':'click','target':{'scope':[%s],'role':'table','row':{%s:'{order}'}},'count':2}",
                                        text("tab:" + ORDERS_TAB), key(ORDER_COLUMN)))
                        .java(STEPS, "openOrder(ui, {order})")
                        .build(),
                DomainStep.check("cartTotalIs", "The cart total is {total}")
                        .about("Checks the cart total of the open new order, e.g. €31.32")
                        .param("total", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.cartTotalIs(ui, p.text("total")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':%s},'text':'{total}'}",
                                NEW_ORDER_SCOPE, text(TOTAL_LABEL)))
                        .java(STEPS, "cartTotalIs(ui, {total})")
                        .build());
    }

    /** The products the open new order lists, e.g. "Gel pen, black". */
    private List<String> productNames() {
        try {
            return column(live.timeout(READ).tab(NEW_ORDER_TAB + "*").table().cell(SKU_COLUMN).rows(), PRODUCT_COLUMN);
        } catch (UiActionFailed e) {
            throw notShowing(e, "the products are listed in a new order tab, and none is showing");
        }
    }

    /** The customers of the orders on the Orders tab, newest first: Order Desk's recent customers. */
    private List<String> recentCustomers() {
        try {
            return column(live.timeout(READ).tab(ORDERS_TAB).table().rows(), CUSTOMER_COLUMN);
        } catch (UiActionFailed e) {
            throw notShowing(e, "the recent customers are listed on the Orders tab, which is not showing");
        }
    }

    private static List<String> column(List<Map<String, String>> rows, String column) {
        Set<String> out = new LinkedHashSet<String>();
        for (Map<String, String> row : rows) {
            String v = row.get(column);
            if (v != null && !v.isEmpty()) out.add(v);
        }
        return new ArrayList<String>(out);
    }

    /** A table that is not there says where it would be; any other failure goes on as it is. */
    private static RuntimeException notShowing(UiActionFailed e, String where) {
        if (e.reason() != Reason.NOT_FOUND && e.reason() != Reason.TIMEOUT) return e;
        return new IllegalStateException(where, e);
    }

    /**
     * Step JSON written with single quotes; each {@code %s} takes the next piece as it is: a {@link #text}, a
     * {@link #key} or a JSON part built from them.
     */
    private static String json(String singleQuoted, String... pieces) {
        return String.format(singleQuoted.replace('\'', '"'), (Object[]) pieces);
    }

    /** {@code name} as a JSON string of plain pattern text: a backslash keeps a *, an opening brace or a backslash plain. */
    static String text(String name) {
        return quote(plain(name));
    }

    /** {@code name} plain, then {@code *}: a scope step that matches every title starting with it. */
    static String prefix(String name) {
        return quote(plain(name) + "*");
    }

    /** {@code name} as a JSON object key (a row's column name). */
    static String key(String name) {
        return quote(name);
    }

    private static String plain(String name) {
        StringBuilder b = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == '*' || c == '{' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.append('"').toString();
    }
}
