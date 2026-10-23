package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_HINT;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ORDERS_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ORDER_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.COPY_ORDER_NUMBER_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.KEEP_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDERS_EMPTY_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDERS_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SEARCH_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.STATUS_COMBO;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.json;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.key;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.text;

/**
 * The Orders list's steps and checks: search, the status filter, sorting, the orders listed and their values, the empty
 * list's text, selecting orders, and the row menu (cancel, keep, copy the numbers). {@code openOrder} stays in
 * {@link OrderDeskVocabulary} (the bundled recorded tests know it). Each body calls {@link OrderDeskOrdersSteps}.
 *
 * <p>Every body looks at the app first, so each says its {@code match}, scoped to the Orders tab: no other screen has
 * it. A match that captures one order where the step takes several, or that cannot tell what the person's clicks did
 * (a header click sorts one way or the other; a click with Cmd or Ctrl held adds to a selection), only suggests its step
 * ({@code autoFold(false)}).
 */
final class OrderDeskOrdersVocabulary {

    private static final String STEPS = OrderDeskOrdersSteps.class.getName();
    /** The Orders tab, as a scope. */
    static final String ORDERS_SCOPE = "[" + text("tab:" + ORDERS_TAB) + "]";
    /** A click on the Orders tab, which every act makes first; a person may not have to. */
    static final String TO_ORDERS = json("{'optional':true,'action':'click','target':{'role':'tab','name':%s}}", text(ORDERS_TAB));
    private static final String ORDERS = "{orders}: numbers (ORD-1640, or the digits 1640) or a customer's name or its"
            + " start when that customer has one order listed, several parted by ;";
    private static final String NEEDS_MAIN = "Needs: the main window, no dialog open (it goes to the Orders tab itself).";
    /** What an act that changes what the list shows says it returns. */
    private static final String LISTED = " The outcome (\"outcome\": true) says what the list then shows: count, and orders,"
            + " each order's Order, Date, Customer, Items, Total and Status, top to bottom (empty: the text shown instead)";

    private final Supplier<List<String>> orders;

    OrderDeskOrdersVocabulary(Supplier<List<String>> orders) {
        this.orders = orders;
    }

    List<DomainStep> steps() {
        return Arrays.asList(
                DomainStep.act("searchOrders", "Search the orders for {text}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, listing the orders whose number or customer holds"
                                + " the text, any case (\"No orders match ‘…’\" when none does)." + LISTED)
                        .param("text", ParamType.TEXT)
                        .codeWithOutcome((ui, p) -> OrderDeskOrdersSteps.searchOrders(ui, p.text("text")))
                        .match(TO_ORDERS, json("{'action':'type','target':{'scope':%s,'role':'text','name':%s},'text':'{text}'}",
                                ORDERS_SCOPE, text(SEARCH_FIELD)))
                        .java(STEPS, "searchOrders(ui, {text})")
                        .build(),
                DomainStep.act("clearOrderSearch", "Clear the order search")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, its Search emptied with its X, every order of the"
                                + " status filter's choice listed." + LISTED)
                        .codeWithOutcome((ui, p) -> OrderDeskOrdersSteps.clearOrderSearch(ui))
                        .match(TO_ORDERS, json("{'action':'click','target':{'scope':[%s,%s],'id':'clear'}}",
                                text("tab:" + ORDERS_TAB), text("text:" + SEARCH_FIELD)))
                        .java(STEPS, "clearOrderSearch(ui)")
                        .build(),
                DomainStep.act("filterOrders", "Filter the orders by status {status}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, its Status filter on that status, listing those"
                                + " orders only. {status}: All, Placed, Shipped or Cancelled, any case or its start"
                                + " (\"placed\", \"cancel\")." + LISTED)
                        .param("status", ParamType.TEXT).choices(() -> OrderDeskOrdersSteps.STATUSES)
                        .codeWithOutcome((ui, p) -> OrderDeskOrdersSteps.filterOrders(ui, p.text("status")))
                        .match(TO_ORDERS, json("{'action':'select','target':{'scope':%s,'role':'combo','name':%s},'items':['{status}']}",
                                ORDERS_SCOPE, text(STATUS_COMBO)))
                        .java(STEPS, "filterOrders(ui, {status})")
                        .build(),
                DomainStep.act("sortOrders", "Sort the orders by {column}, {direction}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, sorted by that column in that direction: it clicks"
                                + " the header, and again until the orders read in that direction (the app never unsorts:"
                                + " a click reverses; at start the list is by Order, descending, newest first)."
                                + " {column}: Order, Date, Customer, Items, Total or Status (by the filter's order: Placed,"
                                + " Shipped, Cancelled). {direction}: ascending or descending (\"newest first\", \"oldest"
                                + " first\"); left out: ascending. The outcome says the column and the direction it is"
                                + " left in (either: every order listed has the same value there), and the orders in their"
                                + " new order: count, and orders, each order's Order, Date, Customer, Items, Total and Status,"
                                + " top to bottom (the first is the lowest of an ascending sort)")
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskOrdersSteps.COLUMNS)
                        .param("direction", ParamType.TEXT).optional(OrderDeskOrdersSteps.ASCENDING)
                        .choices(() -> Arrays.asList(OrderDeskOrdersSteps.ASCENDING, OrderDeskOrdersSteps.DESCENDING))
                        .codeWithOutcome((ui, p) -> OrderDeskOrdersSteps.sortOrders(ui, p.text("column"), p.text("direction")))
                        .match(TO_ORDERS, json("{'action':'click','target':{'scope':%s,'role':'table','column':'{column}'}}",
                                ORDERS_SCOPE))
                        .autoFold(false)
                        .java(STEPS, "sortOrders(ui, {column}, {direction})")
                        .build(),
                DomainStep.act("selectOrders", "Select the orders {orders}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, those orders selected (a click on the first, Cmd"
                                + " or Ctrl held for the others). " + ORDERS)
                        .param("orders", ParamType.TEXT).choices(orders)
                        .code((ui, p) -> OrderDeskOrdersSteps.selectOrders(ui, p.text("orders")))
                        .match(TO_ORDERS, json("{'action':'click','target':{%s}}", orderRow("orders")))
                        .autoFold(false)
                        .java(STEPS, "selectOrders(ui, {orders})")
                        .build(),
                DomainStep.act("cancelOrders", "Cancel the orders {orders}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, those orders Cancelled (the app restores their"
                                + " stock and takes invoice totals off what the customer owes), the status line \"Order"
                                + " ORD-1635 cancelled\" or \"2 orders cancelled\". As a person: selects them, the row"
                                + " menu's Cancel orders…, confirms (\"Cancel order ORD-1635?\"). An order not Placed fails"
                                + " before anything is clicked: \"" + CANCEL_HINT + "\". " + ORDERS)
                        .param("orders", ParamType.TEXT).choices(orders)
                        .code((ui, p) -> OrderDeskOrdersSteps.cancelOrders(ui, p.text("orders")))
                        .match(TO_ORDERS, optionalRowClick(),
                                json("{'action':'contextMenu','target':{%s},'path':[%s]}", orderRow("orders"), text(CANCEL_ORDERS_ITEM)),
                                json("{'action':'answer','button':%s}", text(CANCEL_ORDER_ANSWER)))
                        .java(STEPS, "cancelOrders(ui, {orders})")
                        .build(),
                DomainStep.act("keepOrders", "Ask to cancel the orders {orders}, then keep them")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, those orders selected and unchanged, after"
                                + " answering Keep to \"Cancel order ORD-1635?\" (\"Cancel 2 orders?\"). An order not Placed"
                                + " fails first: \"" + CANCEL_HINT + "\". " + ORDERS)
                        .param("orders", ParamType.TEXT).choices(orders)
                        .code((ui, p) -> OrderDeskOrdersSteps.keepOrders(ui, p.text("orders")))
                        .match(TO_ORDERS, optionalRowClick(),
                                json("{'action':'contextMenu','target':{%s},'path':[%s]}", orderRow("orders"), text(CANCEL_ORDERS_ITEM)),
                                json("{'action':'answer','button':%s}", text(KEEP_ANSWER)))
                        .autoFold(false)
                        .java(STEPS, "keepOrders(ui, {orders})")
                        .build(),
                DomainStep.act("copyOrderNumbers", "Copy the order numbers of {orders}")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, those orders selected, their numbers on the"
                                + " clipboard, the status line \"Copied ORD-1633, ORD-1634\" (the clipboard itself is not"
                                + " checked). " + ORDERS)
                        .param("orders", ParamType.TEXT).choices(orders)
                        .code((ui, p) -> OrderDeskOrdersSteps.copyOrderNumbers(ui, p.text("orders")))
                        .match(TO_ORDERS, optionalRowClick(),
                                json("{'action':'contextMenu','target':{%s},'path':[%s]}", orderRow("orders"), text(COPY_ORDER_NUMBER_ITEM)))
                        .autoFold(false)
                        .java(STEPS, "copyOrderNumbers(ui, {orders})")
                        .build(),
                DomainStep.act("showOrders", "Show the Orders list")
                        .about(NEEDS_MAIN + " Ends: on the Orders tab, its search, filter and sort as they were. To read"
                                + " what the list shows without changing it, e.g. before and after something that must not"
                                + " change the orders." + LISTED)
                        .codeWithOutcome((ui, p) -> OrderDeskOrdersSteps.showOrders(ui))
                        // no pattern: a person's click on the Orders tab reads nothing
                        .autoFold(false)
                        .java(STEPS, "showOrders(ui)")
                        .build(),

                // ---- checks (the Orders tab showing)
                DomainStep.check("orderIs", "The {column} of order {order} is {value}")
                        .about("Reads one value of a listed order on the Orders tab: its Date (\"9 Oct 2026\"), Customer,"
                                + " Items, Total (\"€95.52\") or Status (Placed, Shipped, Cancelled, any case), e.g. the"
                                + " Status of order ORD-1639 is placed. {order}: its number, digits or its customer when"
                                + " that customer has one order listed")
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskOrdersSteps.CHECK_COLUMNS)
                        .param("order", ParamType.TEXT).choices(orders)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrdersSteps.orderIs(ui, p.text("order"), p.text("column"), p.text("value")))
                        // as the Check menu records a cell check: the table, then the cell by its row's key (the
                        // first column, Order) and its column
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table'},"
                                + "'cell':{'row':{%s:'{order}'},'column':'{column}','expected':'{value}'}}",
                                ORDERS_SCOPE, key(ORDER_COLUMN)))
                        // as the agent and a step's body record it: the cell as the target, then its text
                        .matchAlso(json("{'action':'expect','target':{%s,'column':'{column}'},'text':'{value}'}", orderRow("order")))
                        .java(STEPS, "orderIs(ui, {order}, {column}, {value})")
                        .build(),
                DomainStep.check("ordersListedAre", "The orders listed are {orders}")
                        .about("Reads the orders the Orders tab lists, in order, by number parted by ; (\"ORD-1639;"
                                + " ORD-1638\"); none: no order listed (see The orders list reads …)")
                        .param("orders", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrdersSteps.ordersListedAre(ui, p.text("orders")))
                        .match(json("{'action':'expect','target':{%s},'shown':true}", orderRow("orders")))
                        .autoFold(false)
                        .java(STEPS, "ordersListedAre(ui, {orders})")
                        .build(),
                DomainStep.check("ordersCountIs", "The number of orders listed is {count}")
                        .about("Reads how many orders the Orders tab lists (after a search or a filter: how many are"
                                + " left), e.g. 2; 0 while the list's empty text shows instead")
                        .param("count", ParamType.INT)
                        .code((ui, p) -> OrderDeskOrdersSteps.ordersCountIs(ui, p.integer("count")))
                        // as the agent records a row count of the Orders table
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table'},'rows':{'op':'eq','value':'{count}'}}",
                                ORDERS_SCOPE))
                        .java(STEPS, "ordersCountIs(ui, {count})")
                        .build(),
                DomainStep.check("ordersEmptyMessageIs", "The orders list reads {message}")
                        .about("Reads the text that replaces the orders on the Orders tab when none is listed: \"No orders"
                                + " match ‘zzz’\", \"No orders\", \"No cancelled orders\"; none: orders are listed")
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrdersSteps.ordersEmptyMessageIs(ui, p.text("message")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{message}'}",
                                ORDERS_SCOPE, text(ORDERS_EMPTY_ID)))
                        .java(STEPS, "ordersEmptyMessageIs(ui, {message})")
                        .build(),
                DomainStep.check("selectedOrdersAre", "The selected orders are {orders}")
                        .about("Reads which orders are selected on the Orders tab, by number parted by ;, in any order;"
                                + " none: no order selected")
                        .param("orders", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrdersSteps.selectedOrdersAre(ui, p.text("orders")))
                        .match(json("{'action':'expect','target':{%s},'state':['selected']}", orderRow("orders")))
                        .autoFold(false)
                        .java(STEPS, "selectedOrdersAre(ui, {orders})")
                        .build(),
                DomainStep.check("cancelQuestionIs", "Asking to cancel the orders {orders} reads {message}")
                        .about("Reads the question the Orders list's row menu Cancel orders… asks (\"Cancel order"
                                + " ORD-1635?\" or \"Cancel 2 orders?\", then \"Stock is restored and invoice totals are"
                                + " taken off what the customer owes.\"): selects the orders, Cancel orders…, reads, answers"
                                + " Keep, so nothing is cancelled. {message}: the whole text or one of its lines. An order"
                                + " not Placed fails first: \"" + CANCEL_HINT + "\". Needs: the main window, no dialog open. "
                                + ORDERS)
                        .param("orders", ParamType.TEXT).choices(orders)
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrdersSteps.cancelQuestionIs(ui, p.text("orders"), p.text("message")))
                        // no pattern: a check of several steps (the row menu, the read, Keep); the Check menu records
                        // one step, and an agent runs it as a do step
                        .autoFold(false)
                        .java(STEPS, "cancelQuestionIs(ui, {orders}, {message})")
                        .build());
    }

    /** An order's row in the Orders list, by the param's value in its Order column. */
    static String orderRow(String param) {
        return json("'scope':%s,'role':'table','row':{%s:%s}", ORDERS_SCOPE, key(ORDER_COLUMN),
                OrderDeskVocabulary.quote("{" + param + "}"));
    }

    /** A click on the order's row before its row menu: a right-click selects an order not selected yet itself. */
    private static String optionalRowClick() {
        return json("{'optional':true,'action':'click','target':{%s}}", orderRow("orders"));
    }
}
