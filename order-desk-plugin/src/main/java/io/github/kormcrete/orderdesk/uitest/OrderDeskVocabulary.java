package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
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

/**
 * Order Desk's domain steps: what an agent runs as one {@code do} step ("Add Gel pen, black to the cart"), and what a
 * person's recorded clicks fold into. Each body calls {@link OrderDeskSteps}, the same method a Java test calls.
 *
 * <p>A step without {@code match} gets its pattern from its body (the agent traces it when the plugin becomes ready).
 * The others say theirs: {@code addToCart} matches a click anywhere on a product's row, not only its SKU cell;
 * {@code addQtyToCart} is {@code addToCart} then {@code setQuantity}; {@code openOrder} matches with or without the
 * click on the Orders tab; {@code placeOrder} and {@code cartTotalIs} read or check the app, which a trace cannot
 * follow. A written pattern only recognises recorded steps; a {@code do} step always runs its body, so a renamed
 * button is fixed in {@link OrderDeskSteps} for running, and in the pattern for recognising a person's clicks.
 */
public final class OrderDeskVocabulary implements Vocabulary {

    private static final String STEPS = OrderDeskSteps.class.getName();
    /** A choices read gives up soon: it runs while the agent looks a step up. */
    private static final Duration READ = Duration.ofSeconds(1);
    /** A product's row in the products table, by its name; the cart's lines have no SKU column. */
    private static final String PRODUCT_ROW =
            "'scope':['tab:New order*'],'role':'table','row':{'SKU':'*','Product':'{product}'}";
    private static final String ADD_BUTTON =
            "{'action':'click','target':{'scope':['tab:New order*'],'role':'button','name':'Add to cart'}}";

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
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':'New order'}}"),
                                json("{'action':'type','target':{'scope':['tab:New order'],'role':'text','name':'Customer'},"
                                        + "'text':'{customer}'}"),
                                json("{'action':'press','keys':'Enter'}"))
                        .matchAlso(json("{'action':'click','target':{'role':'button','name':'New order for a recent customer'}}"),
                                json("{'action':'pick','path':['{customer}']}"))
                        .java(STEPS, "openNewOrder(ui, {customer})")
                        .build(),
                DomainStep.act("addToCart", "Add {product} to the cart")
                        .about("Adds one of a product to the cart of the open new order")
                        .param("product", ParamType.TEXT).choices(this::productNames)
                        .code((ui, p) -> OrderDeskSteps.addToCart(ui, p.text("product")))
                        .match(json("{'action':'click','target':{" + PRODUCT_ROW + "}}"), json(ADD_BUTTON))
                        .matchAlso(json("{'action':'contextMenu','target':{" + PRODUCT_ROW + "},'path':['Add to cart']}"))
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
                DomainStep.act("checkoutWithPo", "Check out with PO {po}")
                        .about("Checks out the open new order, paid by PO number; ends on the Review step, before Place order")
                        .param("po", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.checkoutWithPo(ui, p.text("po")))
                        .java(STEPS, "checkoutWithPo(ui, {po})")
                        .build(),
                DomainStep.act("placeOrder", "Place the order")
                        .about("Places the order on the checkout's Review step and confirms it; ends on the new order's tab")
                        .code((ui, p) -> OrderDeskSteps.placeOrder(ui))
                        .match(json("{'action':'click','target':{'role':'button','name':'Place order'}}"),
                                json("{'action':'answer','button':'Place order'}"))
                        .java(STEPS, "placeOrder(ui)")
                        .build(),
                DomainStep.act("openOrder", "Open order {order}")
                        .about("Opens a placed order (e.g. ORD-1640) from the Orders list in a tab of its own")
                        .param("order", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.openOrder(ui, p.text("order")))
                        .match(json("{'optional':true,'action':'click','target':{'role':'tab','name':'Orders'}}"),
                                json("{'action':'click','target':{'scope':['tab:Orders'],'role':'table','row':{'Order':'{order}'}},"
                                        + "'count':2}"))
                        .java(STEPS, "openOrder(ui, {order})")
                        .build(),
                DomainStep.check("cartTotalIs", "The cart total is {total}")
                        .about("Checks the cart total of the open new order, e.g. €31.32")
                        .param("total", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskSteps.cartTotalIs(ui, p.text("total")))
                        .match(json("{'action':'expect','target':{'scope':['tab:New order*'],'role':'label','name':'Total'},"
                                + "'text':'{total}'}"))
                        .java(STEPS, "cartTotalIs(ui, {total})")
                        .build());
    }

    /** The products the open new order lists, e.g. "Gel pen, black". */
    private List<String> productNames() {
        try {
            return column(live.timeout(READ).tab("New order*").table().cell("SKU").rows(), "Product");
        } catch (RuntimeException e) {
            throw new IllegalStateException("the products are listed in a new order tab, and none is showing");
        }
    }

    /** The customers of the orders on the Orders tab, newest first: Order Desk's recent customers. */
    private List<String> recentCustomers() {
        try {
            return column(live.timeout(READ).tab("Orders").table().rows(), "Customer");
        } catch (RuntimeException e) {
            throw new IllegalStateException("the recent customers are listed on the Orders tab, which is not showing");
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

    /** Step JSON written with single quotes, as in the patterns above. */
    private static String json(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }
}
