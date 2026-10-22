package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_HINT;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ORDER_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ORDER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.DELIVERY_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.HINT_DASH;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.INVOICE_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ITEMS_SUMMARY_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ITEMS_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.KEEP_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.OK_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_FACTS_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_NUMBER_PREFIX;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ORDER_STATUS_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.RECEIPT_NOT_SAVED;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.REPLACE_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SAVE_RECEIPT_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SKU_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.json;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.key;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.prefix;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.text;

/**
 * An order's own tab's steps and checks: Save receipt… (saved, kept over a file that exists, or the error when the app
 * cannot write it), Cancel order (confirmed or kept), the status, the customer, date and total beside it, and the
 * Items, Delivery and Invoice tabs. Switching between those three is the main window's {@code selectTab}; Cancel
 * order's state and reason are read by the new order's {@code buttonIsDisabled} and {@code buttonIsEnabled}, which
 * take an order tab's buttons too. Each body calls {@link OrderDeskOrderTabSteps}.
 *
 * <p>Every body looks at the app first, so each says its {@code match}, scoped to the order's tab ({@code tab:{order}}
 * captures its number) and, for a check of Items, Delivery or Invoice, that tab inside it. A check of a list (the
 * lines), of the Delivery tab (its caption and address lines are labels a pattern cannot tell from its captioned
 * values) and the save-error check, which a recording cannot pin down, only suggest themselves.
 */
final class OrderDeskOrderTabVocabulary {

    private static final String STEPS = OrderDeskOrderTabSteps.class.getName();
    /** The order's tab, as a scope: the capture takes its title, the number. */
    private static final String ORDER_SCOPE = json("['tab:{order}']");
    /** A click on the order's tab, which every act makes first; a person may not have to. */
    private static final String TO_ORDER = json("{'optional':true,'action':'click','target':{'role':'tab','name':'{order}'}}");
    private static final String NEEDS_TAB = "Needs: the order's own tab open (Open order …; it selects the tab itself),"
            + " no dialog open.";
    private static final String NEEDS_SELECTED = "Needs: the order's own tab selected (Switch to the tab …, Open order …).";
    private static final String ORDER = "{order}: its number (ORD-1640) or its digits (1640)";

    private final Supplier<List<String>> orderTabs;
    private final Supplier<List<String>> orderItems;

    OrderDeskOrderTabVocabulary(Supplier<List<String>> orderTabs, Supplier<List<String>> orderItems) {
        this.orderTabs = orderTabs;
        this.orderItems = orderItems;
    }

    List<DomainStep> steps() {
        String save = json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}", ORDER_SCOPE,
                text(SAVE_RECEIPT_BUTTON));
        String choose = json("{'action':'chooseFile','path':'{file}'}");
        String cancel = json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}", ORDER_SCOPE,
                text(CANCEL_ORDER_BUTTON));
        return Arrays.asList(
                // ---- acts
                DomainStep.act("saveReceipt", "Save the receipt of order {order} as {file}")
                        .about(NEEDS_TAB + " Ends: on the order's tab, the receipt saved, the status line \"Receipt saved"
                                + " to <path>\". Save receipt…, then {file} in its file chooser: a path, absolute or in the"
                                + " folder the chooser opens in (the user's home); the app adds .txt when it is missing. A"
                                + " file that exists is replaced (answers Replace to \"ORD-1640.txt already exists.\"). The"
                                + " outcome: where it was saved (path) and whether a file was replaced (replaced). When the"
                                + " app cannot write it, it fails with the app's message (\"" + RECEIPT_NOT_SAVED + ": …\","
                                + " answered OK). " + ORDER)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("file", ParamType.TEXT)
                        .codeWithOutcome((ui, p) -> OrderDeskOrderTabSteps.saveReceipt(ui, p.text("order"), p.text("file")))
                        .match(TO_ORDER, save, choose,
                                json("{'optional':true,'action':'answer','button':%s}", text(REPLACE_ANSWER)))
                        .java(STEPS, "saveReceipt(ui, {order}, {file})")
                        .build(),
                DomainStep.act("keepExistingReceipt", "Ask to save the receipt of order {order} over {file}, then keep the file")
                        .about(NEEDS_TAB + " {file} existing. Ends: on the order's tab, the file unchanged, after answering"
                                + " Cancel to \"ORD-1640.txt already exists. Do you want to replace it?\". A file that does"
                                + " not exist is saved at once (the app asks nothing): the step then fails saying so. " + ORDER)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("file", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.keepExistingReceipt(ui, p.text("order"), p.text("file")))
                        .match(TO_ORDER, save, choose, json("{'action':'answer','button':%s}", text(CANCEL_ANSWER)))
                        .java(STEPS, "keepExistingReceipt(ui, {order}, {file})")
                        .build(),
                DomainStep.act("cancelOrder", "Cancel order {order} from its tab")
                        .about(NEEDS_TAB + " Ends: on the order's tab, its status Cancelled (the app restores its stock and"
                                + " takes an invoice total off what the customer owes), the status line \"Order ORD-1640"
                                + " cancelled\". Cancel order, then Cancel order to \"Cancel order ORD-1640?\". A shipped or"
                                + " cancelled order fails before anything is clicked on its tab, with the app's reason (\""
                                + CANCEL_HINT + HINT_DASH + "this one is shipped\"). From the Orders list: Cancel the orders"
                                + " …. " + ORDER)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .code((ui, p) -> OrderDeskOrderTabSteps.cancelOrder(ui, p.text("order")))
                        .match(TO_ORDER, cancel, json("{'action':'answer','button':%s}", text(CANCEL_ORDER_ANSWER)))
                        .java(STEPS, "cancelOrder(ui, {order})")
                        .build(),
                DomainStep.act("keepOrder", "Ask to cancel order {order} from its tab, then keep it")
                        .about(NEEDS_TAB + " Ends: on the order's tab, the order unchanged, after answering Keep to"
                                + " \"Cancel order ORD-1640?\". A shipped or cancelled order fails first, with the app's"
                                + " reason. " + ORDER)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .code((ui, p) -> OrderDeskOrderTabSteps.keepOrder(ui, p.text("order")))
                        .match(TO_ORDER, cancel, json("{'action':'answer','button':%s}", text(KEEP_ANSWER)))
                        .java(STEPS, "keepOrder(ui, {order})")
                        .build(),

                // ---- checks
                DomainStep.check("receiptSaveErrorIs", "Saving the receipt of order {order} as {file} fails with {message}")
                        .about("Reads the error Save receipt… shows when the app cannot write {file} (a folder that does"
                                + " not exist, a file it may not write): \"" + RECEIPT_NOT_SAVED + ": <the reason>\" (give the"
                                + " whole text, the sentence before the colon, or the reason after it); answers it OK. A file"
                                + " the app can write is saved instead, and the check fails. " + NEEDS_TAB)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("file", ParamType.TEXT)
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.receiptSaveErrorIs(ui, p.text("order"), p.text("file"),
                                p.text("message")))
                        .match(TO_ORDER, save, choose,
                                json("{'action':'expect','target':{'window':'dialog','role':'label'},'text':'{message}'}"),
                                json("{'action':'answer','button':%s}", text(OK_ANSWER)))
                        .autoFold(false)
                        .java(STEPS, "receiptSaveErrorIs(ui, {order}, {file}, {message})")
                        .build(),
                DomainStep.check("orderStatusIs", "The status on order {order}'s tab is {status}")
                        .about("Reads the status beside the order's number on its own tab: Placed, Shipped or Cancelled,"
                                + " any case. " + NEEDS_SELECTED + " From the Orders list: The Status of order … is …")
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("status", ParamType.TEXT).choices(() -> OrderDeskOrderTabSteps.STATUSES)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderStatusIs(ui, p.text("order"), p.text("status")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{status}'}", ORDER_SCOPE,
                                text(ORDER_STATUS_ID)))
                        .java(STEPS, "orderStatusIs(ui, {order}, {status})")
                        .build(),
                DomainStep.check("orderFactsAre", "The customer, date and total on order {order}'s tab read {facts}")
                        .about("Reads the line beside the order's number and status: \"Ada Shop Ltd · 9 Oct 2026 ·"
                                + " €95.52\", or one of its parts (\"Ada Shop Ltd\", \"€95.52\"). The number is the tab's"
                                + " title (The selected tab is …). " + NEEDS_SELECTED)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("facts", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderFactsAre(ui, p.text("order"), p.text("facts")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{facts}'}", ORDER_SCOPE,
                                text(ORDER_FACTS_ID)))
                        .java(STEPS, "orderFactsAre(ui, {order}, {facts})")
                        .build(),
                DomainStep.check("orderItemsAre", "The lines on order {order}'s Items tab are {lines}")
                        .about("Reads the order's lines, top to bottom, by product name or its start, any case, parted by"
                                + " ; (\"Gel pen, black; A4 paper, 500 sheets\"); shows the Items tab for the read, then the"
                                + " tab that showed before. " + NEEDS_SELECTED)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("lines", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderItemsAre(ui, p.text("order"), p.text("lines")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table','row':{%s:'{lines}'}},'shown':true}",
                                itemsScope(), key(PRODUCT_COLUMN)))
                        .autoFold(false)
                        .java(STEPS, "orderItemsAre(ui, {order}, {lines})")
                        .build(),
                DomainStep.check("orderItemIs", "The {column} of {product} on order {order}'s Items tab is {value}")
                        .about("Reads one value of a line on the order's Items tab: its SKU, Qty, Price or Line total, e.g."
                                + " the Line total of Gel pen, black is €60.00. {product}: the name, its start or its SKU"
                                + " (\"PEN-GEL-BLK\"). Shows the Items tab for the read, then the tab that showed before. "
                                + NEEDS_SELECTED)
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskOrderTabSteps.ITEM_COLUMNS)
                        .param("product", ParamType.TEXT).choices(orderItems)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderItemIs(ui, p.text("order"), p.text("product"),
                                p.text("column"), p.text("value")))
                        // as the Check menu records a cell check: the Items table's lines by their key, the first
                        // column (SKU)
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table'},"
                                + "'cell':{'row':{%s:'{product}'},'column':'{column}','expected':'{value}'}}",
                                itemsScope(), key(SKU_COLUMN)))
                        // as the agent and a step's body record it: the cell as the target, then its text
                        .matchAlso(json("{'action':'expect','target':{'scope':%s,'role':'table','row':{%s:'*',%s:'{product}'},"
                                + "'column':'{column}'},'text':'{value}'}", itemsScope(), key(SKU_COLUMN), key(PRODUCT_COLUMN)))
                        .java(STEPS, "orderItemIs(ui, {order}, {product}, {column}, {value})")
                        .build(),
                DomainStep.check("orderItemsSummaryIs", "The units on order {order}'s Items tab read {summary}")
                        .about("Reads the count under the order's lines: \"55 units in 3 lines\", or one of its parts"
                                + " (\"55 units\", \"3 lines\"). " + NEEDS_SELECTED)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("summary", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderItemsSummaryIs(ui, p.text("order"), p.text("summary")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{summary}'}", itemsScope(),
                                text(ITEMS_SUMMARY_ID)))
                        .java(STEPS, "orderItemsSummaryIs(ui, {order}, {summary})")
                        .build(),
                DomainStep.check("orderDeliveryIs", "The {detail} on order {order}'s Delivery tab is {value}")
                        .about("Reads the order's Delivery tab: the Delivery address (its lines parted by ;, \"Ada"
                                + " Lindqvist; Torstraße 118; 10119 Berlin; Germany\", or one of them, \"10119 Berlin\"), the"
                                + " Shipping method (\"Standard · 3–5 business days · €4.90\", \"… · Free\") or the Delivery"
                                + " note (\"Ring twice.\"; \"—\" or none when there is none). {detail}: address, shipping"
                                + " method or note. Shows the Delivery tab for the read, then the tab that showed before. "
                                + NEEDS_SELECTED)
                        .param("detail", ParamType.TEXT).choices(() -> OrderDeskOrderTabSteps.DELIVERY_DETAILS)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderDeliveryIs(ui, p.text("order"), p.text("detail"), p.text("value")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':'{detail}'},'text':'{value}'}",
                                subTabScope(DELIVERY_TAB)))
                        .autoFold(false)
                        .java(STEPS, "orderDeliveryIs(ui, {order}, {detail}, {value})")
                        .build(),
                DomainStep.check("orderInvoiceIs", "The {detail} on order {order}'s Invoice tab is {value}")
                        .about("Reads the order's Invoice tab: the Payment (\"Invoice (30 days) · PO PO-2026-131\", \"Card"
                                + " ending 4242\"), the Order date (\"9 Oct 2026\"), or an amount: Subtotal, Volume discount"
                                + " (none while it does not show), Shipping (\"Free\"), VAT (20%) or Total. Shows the Invoice"
                                + " tab for the read, then the tab that showed before. " + NEEDS_SELECTED)
                        .param("detail", ParamType.TEXT).choices(() -> OrderDeskOrderTabSteps.INVOICE_DETAILS)
                        .param("order", ParamType.TEXT).choices(orderTabs)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskOrderTabSteps.orderInvoiceIs(ui, p.text("order"), p.text("detail"), p.text("value")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':'{detail}'},'text':'{value}'}",
                                subTabScope(INVOICE_TAB)))
                        .java(STEPS, "orderInvoiceIs(ui, {order}, {detail}, {value})")
                        .build());
    }

    /** An order's tab and its Items tab, as a scope. */
    private static String itemsScope() {
        return subTabScope(ITEMS_TAB);
    }

    /** An order's tab and the tab {@code sub} inside it, as a scope. */
    private static String subTabScope(String sub) {
        return json("['tab:{order}',%s]", text("tab:" + sub));
    }

    /** Any order's own tab, as a scope (no capture: for the new order's button checks, which take no order). */
    static String anyOrderScope() {
        return "[" + prefix("tab:" + ORDER_NUMBER_PREFIX) + "]";
    }
}
