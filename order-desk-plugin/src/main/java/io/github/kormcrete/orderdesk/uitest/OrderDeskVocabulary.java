package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.Target;
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
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.FILE_MENU;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDERS_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PLACE_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.QTY_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.RECENT_CUSTOMER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SKU_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.TOTAL_LABEL;

/**
 * Order Desk's domain steps: what an agent runs as one {@code do} step ("Add Gel pen, black to the cart"), and what a
 * person's recorded clicks fold into. Each body calls {@link OrderDeskSteps}, the same method a Java test calls.
 *
 * <p>A step without {@code match} gets its pattern from its body (the agent traces it when the plugin becomes ready).
 * The others say theirs: every step of a new order's tab first looks at the app (the new order it needs, the product,
 * cart line or customer a name means), which a trace cannot follow; {@code addToCart} matches a click anywhere on a
 * product's row, not only its SKU cell; {@code addQtyToCart} is {@code addToCart} then {@code setQuantity};
 * {@code openOrder} matches with or without the click on the Orders tab; {@code placeOrder} reads the new number. The
 * rest of a new order's steps and checks are in {@link OrderDeskNewOrderVocabulary}. The patterns take the app's names
 * from {@link OrderDeskSteps}' constants, so a rename is one change there.
 */
public final class OrderDeskVocabulary implements Vocabulary {

    private static final String STEPS = OrderDeskSteps.class.getName();
    /** A choices read gives up soon: it runs while the agent looks a step up. */
    private static final Duration READ = Duration.ofSeconds(1);
    /** Any open new order's tab, as a scope: "New order", "New order · Ada Shop Ltd". */
    static final String NEW_ORDER_SCOPE = "[" + prefix("tab:" + NEW_ORDER_TAB) + "]";
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
        List<DomainStep> steps = new ArrayList<DomainStep>(firstSteps());
        steps.addAll(new OrderDeskNewOrderVocabulary(this::knownCustomers, this::productNames, this::cartLines,
                this::categories).steps());
        return steps;
    }

    /** The first eight steps, which the bundled recorded tests use. */
    private List<DomainStep> firstSteps() {
        return Arrays.asList(
                DomainStep.act("openNewOrder", "Open a new order for {customer}")
                        .about("Needs: the main window, no dialog open. Ends: on a new order for that customer (its tab"
                                + " \"New order · Ada Shop Ltd\"). {customer}: the name, its start or the city (\"Ada\");"
                                + " one that fits several customers or none fails naming those that fit")
                        .param("customer", ParamType.TEXT).choices(this::knownCustomers)
                        .code((ui, p) -> OrderDeskSteps.openNewOrder(ui, p.text("customer")))
                        .match(json("{'action':'menu','path':[%s,%s]}", text(FILE_MENU), text(NEW_ORDER_ITEM)),
                                json("{'action':'type','target':{'scope':[%s],'role':'text','name':%s},'text':'{customer}'}",
                                        text("tab:" + NEW_ORDER_TAB), text(CUSTOMER_FIELD)),
                                json("{'action':'press','keys':'Enter'}"))
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':%s}}", text(NEW_ORDER_BUTTON)),
                                json("{'action':'type','target':{'scope':[%s],'role':'text','name':%s},'text':'{customer}'}",
                                        text("tab:" + NEW_ORDER_TAB), text(CUSTOMER_FIELD)),
                                json("{'action':'press','keys':'Enter'}"))
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':%s}}", text(RECENT_CUSTOMER_BUTTON)),
                                json("{'action':'pick','path':['{customer}']}"))
                        .java(STEPS, "openNewOrder(ui, {customer})")
                        .build(),
                DomainStep.act("addToCart", "Add {product} to the cart")
                        .about("Needs: a new order showing, the product listed (see Browse category, Search the products)."
                                + " Ends: on it, one more in the product's cart line. {product}: the name or its start"
                                + " (\"stapler\"); one that fits several or none fails naming them; a product Add to cart"
                                + " refuses (out of stock) fails with the app's reason")
                        .param("product", ParamType.TEXT).choices(this::productNames)
                        .code((ui, p) -> OrderDeskSteps.addToCart(ui, p.text("product")))
                        .match(json("{'action':'click','target':{%s}}", PRODUCT_ROW), ADD_BUTTON)
                        .matchAlso(json("{'action':'contextMenu','target':{%s},'path':[%s]}", PRODUCT_ROW, text(ADD_TO_CART_ITEM)))
                        .java(STEPS, "addToCart(ui, {product})")
                        .build(),
                DomainStep.act("setQuantity", "Set the quantity of {product} to {qty}")
                        .about("Needs: a new order with the product in its cart. Ends: on it, the line at that quantity."
                                + " {product}: the name or its start. Above stock the app keeps it and the cart warns (The"
                                + " cart warns …); below 1 it keeps the line as it was")
                        .param("product", ParamType.TEXT).choices(this::cartLines)
                        .param("qty", ParamType.INT)
                        .code((ui, p) -> OrderDeskSteps.setQuantity(ui, p.text("product"), p.integer("qty")))
                        .match(json("{'action':'type','target':{'scope':%s,'role':'table','row':{%s:'{product}'},'column':%s},"
                                + "'text':'{qty}','commit':true}", NEW_ORDER_SCOPE, key(PRODUCT_COLUMN), text(QTY_COLUMN)))
                        .java(STEPS, "setQuantity(ui, {product}, {qty})")
                        .build(),
                DomainStep.act("addQtyToCart", "Add {qty} × {product} to the cart")
                        .about("Needs: a new order showing, the product listed. Ends: on it, the product's cart line at"
                                + " that quantity (above stock the app keeps it and the cart warns)")
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
                        .about("Reads the cart total under the cart, e.g. €31.32")
                        .param("total", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.cartTotalIs(ui, p.text("total")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':%s},'text':'{total}'}",
                                NEW_ORDER_SCOPE, text(TOTAL_LABEL)))
                        .java(STEPS, "cartTotalIs(ui, {total})")
                        .build());
    }

    /** The products the showing new order lists, e.g. "Gel pen, black". */
    private List<String> productNames() {
        Client ui = needsNewOrder("the products are listed in a new order tab, and none is showing");
        return OrderDeskNewOrderSteps.listedProducts(ui);
    }

    /** The products in the showing new order's cart. */
    private List<String> cartLines() {
        Client ui = needsNewOrder("the cart lines are in a new order tab, and none is showing");
        return OrderDeskNewOrderSteps.cartLines(ui);
    }

    /** The categories of the showing new order: "All products", "Office", "Office > Paper", ... */
    private List<String> categories() {
        Client ui = needsNewOrder("the categories are in a new order tab, and none is showing");
        return OrderDeskNewOrderSteps.categories(ui);
    }

    /**
     * The customers a tester knows: those of the orders on the Orders tab (every customer with an order), or, while a new
     * order's Customer field shows suggestions, those. No screen lists every customer without acting (Down in an empty
     * Customer field does), so a name outside these still works: the step resolves it against the app's own suggestions.
     */
    private List<String> knownCustomers() {
        Client ui = live.timeout(READ);
        try {
            Target orders = ui.tab(ORDERS_TAB).table();
            List<String> names = column(orders.rows(), CUSTOMER_COLUMN);
            int count = orders.rowCount();
            Set<String> all = new LinkedHashSet<String>(names);
            for (int i = names.size(); i < count; i++) all.add(orders.rowAt(i).cell(CUSTOMER_COLUMN).shownText());
            return new ArrayList<String>(all);
        } catch (UiActionFailed e) {
            if (e.reason() != Reason.NOT_FOUND && e.reason() != Reason.TIMEOUT) throw e;
        }
        try {
            List<String> names = new ArrayList<String>();
            for (String item : ui.popup().list(null).items()) names.add(item);
            return names;
        } catch (UiActionFailed e) {
            throw notShowing(e, "the customers are listed on the Orders tab, or in a new order's Customer suggestions;"
                    + " neither is showing");
        }
    }

    /** A Client on the app, once a new order shows; else why the choices cannot be read. */
    private Client needsNewOrder(String where) {
        Client ui = live.timeout(READ);
        try {
            OrderDeskNewOrderSteps.requireNewOrder(ui);
        } catch (DriverException e) {
            throw new IllegalStateException(where, e);
        }
        return ui;
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
    static String json(String singleQuoted, String... pieces) {
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

    static String plain(String name) {
        StringBuilder b = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == '*' || c == '{' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.append('"').toString();
    }
}
