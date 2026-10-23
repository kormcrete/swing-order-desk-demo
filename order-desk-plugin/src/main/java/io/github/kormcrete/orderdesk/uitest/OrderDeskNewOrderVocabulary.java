package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ADD_TO_CART_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_HINT;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CUSTOMER_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CUSTOMER_TERMS_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.HINT_DASH;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.IN_STOCK_WARNING;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.KEEP_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.LINE_TOTAL_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NEW_ORDER_TAB;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.NO_CUSTOMER_MATCHES;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.PRODUCT_COUNT_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.RECENT_CUSTOMER_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.REMOVE_BUTTON;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SEARCH_FIELD;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SHOW_IN_CATEGORY_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.SKU_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.STOCK_COLUMN;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.TITLE_SEPARATOR;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.NEW_ORDER_SCOPE;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.json;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.key;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.text;

/**
 * The steps and checks of a new order's tab beyond the first eight ({@link OrderDeskVocabulary}): the customer, the
 * categories and search, several products at once, the cart's lines and amounts, and the reasons beside a disabled
 * button. Each body calls {@link OrderDeskNewOrderSteps}. Every body looks at the app (the new order it needs, the names
 * it resolves), so each says its {@code match}; a match that is looser than the step (it takes one name where the step
 * takes several, or any label for a caption) only suggests the step ({@code autoFold(false)}).
 */
final class OrderDeskNewOrderVocabulary {

    private static final String STEPS = OrderDeskNewOrderSteps.class.getName();
    /** A param that names several things: how the about says to part them. */
    private static final String SEVERAL = "parted by ;";

    private final Supplier<List<String>> customers;
    private final Supplier<List<String>> products;
    private final Supplier<List<String>> cartLines;
    private final Supplier<List<String>> categories;
    private final Supplier<List<String>> recentCustomers;

    OrderDeskNewOrderVocabulary(Supplier<List<String>> customers, Supplier<List<String>> products,
                                Supplier<List<String>> cartLines, Supplier<List<String>> categories,
                                Supplier<List<String>> recentCustomers) {
        this.recentCustomers = recentCustomers;
        this.customers = customers;
        this.products = products;
        this.cartLines = cartLines;
        this.categories = categories;
    }

    List<DomainStep> steps() {
        return Arrays.asList(
                // ---- the customer
                DomainStep.act("chooseCustomer", "Choose {customer} as the customer")
                        .about("Needs: a new order showing. Ends: on that new order, now for the customer (its tab"
                                + " \"New order · Ada Shop Ltd\"). {customer}: the name, its start or the city (\"Ada\","
                                + " \"Hamburg\"); one that fits several customers or none fails naming those that fit")
                        .param("customer", ParamType.TEXT).choices(customers)
                        .code((ui, p) -> OrderDeskNewOrderSteps.chooseCustomer(ui, p.text("customer")))
                        .match(json("{'action':'type','target':{'scope':%s,'role':'text','name':%s},'text':'{customer}'}",
                                NEW_ORDER_SCOPE, text(CUSTOMER_FIELD)), json("{'action':'press','keys':'Enter'}"))
                        .java(STEPS, "chooseCustomer(ui, {customer})")
                        .build(),
                DomainStep.act("typeCustomer", "Type {text} into Customer without picking a customer")
                        .about("Needs: a new order showing. Ends: on it, the Customer field's suggestions showing and no"
                                + " customer chosen: editing the name forgets a chosen customer (the tab reads \"New"
                                + " order\" again, Checkout… is disabled)")
                        .param("text", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.typeCustomer(ui, p.text("text")))
                        .match(json("{'action':'type','target':{'scope':%s,'role':'text','name':%s},'text':'{text}'}",
                                NEW_ORDER_SCOPE, text(CUSTOMER_FIELD)))
                        .autoFold(false)
                        .java(STEPS, "typeCustomer(ui, {text})")
                        .build(),
                DomainStep.check("customerSuggestionsAre", "The customer suggestions are {customers}")
                        .about("Reads the Customer field's suggestions (after Type … into Customer), in order, by the"
                                + " customers' names " + SEVERAL + ": \"Café Lumen; Fjord Supplies\"; none (or the text the"
                                + " list then shows, \"" + NO_CUSTOMER_MATCHES + "\"): nothing fits")
                        .param("customers", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.customerSuggestionsAre(ui, p.text("customers")))
                        .match(json("{'action':'expect','target':{'role':'list','item':['{customers}']},'shown':true}"))
                        .autoFold(false)
                        .java(STEPS, "customerSuggestionsAre(ui, {customers})")
                        .build(),
                DomainStep.check("newOrderIsFor", "The new order is for {customer}")
                        .about("Reads whose new order shows, by its tab's title \"New order · Ada Shop Ltd\": the"
                                + " customer's whole name, any case; none: no customer chosen yet (the tab reads \"New order\")")
                        .param("customer", ParamType.TEXT).choices(customers)
                        .code((ui, p) -> OrderDeskNewOrderSteps.newOrderIsFor(ui, p.text("customer")))
                        .match(json("{'action':'expect','target':{'role':'tab','name':%s},'state':['selected']}",
                                OrderDeskVocabulary.quote(OrderDeskVocabulary.plain(NEW_ORDER_TAB + TITLE_SEPARATOR) + "{customer}")))
                        .java(STEPS, "newOrderIsFor(ui, {customer})")
                        .build(),
                DomainStep.check("customerTermsAre", "The customer's terms are {terms}")
                        .about("Reads the terms beside the Customer field, e.g. \"Card only\" or \"Account · Invoice 30"
                                + " days · Credit limit €2,000.00 · Owes €450.00 · PO number required\": the whole text or one"
                                + " part between the dots (\"Account\", \"Owes €450.00\"); none: no customer chosen")
                        .param("terms", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.customerTermsAre(ui, p.text("terms")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{terms}'}",
                                NEW_ORDER_SCOPE, text(CUSTOMER_TERMS_ID)))
                        .java(STEPS, "customerTermsAre(ui, {terms})")
                        .build(),
                DomainStep.check("recentCustomersAre", "The recent customers are {customers}")
                        .about("Reads the customers the toolbar's ▾ (New order for a recent customer) offers, newest"
                                + " first, by name " + SEVERAL + ": opens its menu and closes it again. Needs: the main"
                                + " window, no dialog open")
                        .param("customers", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.recentCustomersAre(ui, p.text("customers")))
                        .match(json("{'action':'click','target':{'role':'button','name':%s}}", text(RECENT_CUSTOMER_BUTTON)),
                                json("{'action':'expect','target':{'role':'menuitem','name':'{customers}'},'shown':true}"))
                        .autoFold(false)
                        .java(STEPS, "recentCustomersAre(ui, {customers})")
                        .build(),
                DomainStep.act("openNewOrderFromRecent", "Open a new order for {customer} from the recent customers")
                        .about("Needs: the main window, no dialog open. Ends: on a new order for that customer (its tab"
                                + " \"New order · Ada Shop Ltd\"), opened the way the toolbar's ▾ (New order for a recent"
                                + " customer) does: opens its menu and picks the customer. {customer}: one of the recent"
                                + " customers (see The recent customers are …), the name or its start; another fails naming"
                                + " those it offers")
                        .param("customer", ParamType.TEXT).choices(recentCustomers)
                        .code((ui, p) -> OrderDeskNewOrderSteps.openNewOrderFromRecent(ui, p.text("customer")))
                        .match(json("{'action':'click','target':{'role':'button','name':%s}}", text(RECENT_CUSTOMER_BUTTON)),
                                json("{'action':'pick','path':['{customer}']}"))
                        .java(STEPS, "openNewOrderFromRecent(ui, {customer})")
                        .build(),

                // ---- products
                DomainStep.act("browseCategory", "Browse category {category}")
                        .about("Needs: a new order showing. Ends: on it, its products list showing that category's"
                                + " products only (All products: every product). {category}: a name (\"Desk\") or a path"
                                + " (\"Office > Desk\")")
                        .param("category", ParamType.TEXT).choices(categories)
                        .code((ui, p) -> OrderDeskNewOrderSteps.browseCategory(ui, p.text("category")))
                        .match(json("{'action':'click','target':{'scope':%s,'role':'tree','item':['*','{category}']}}", NEW_ORDER_SCOPE))
                        .matchAlso(json("{'action':'click','target':{'scope':%s,'role':'tree','item':['{category}']}}", NEW_ORDER_SCOPE))
                        .java(STEPS, "browseCategory(ui, {category})")
                        .build(),
                DomainStep.check("selectedCategoryIs", "The selected category is {category}")
                        .about("Reads the category selected in Categories, e.g. \"Desk\" (or its path \"Office > Desk\")")
                        .param("category", ParamType.TEXT).choices(categories)
                        .code((ui, p) -> OrderDeskNewOrderSteps.selectedCategoryIs(ui, p.text("category")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'tree','item':['*','{category}']},'state':['selected']}",
                                NEW_ORDER_SCOPE))
                        .matchAlso(json("{'action':'expect','target':{'scope':%s,'role':'tree','item':['{category}']},'state':['selected']}",
                                NEW_ORDER_SCOPE))
                        .autoFold(false)
                        .java(STEPS, "selectedCategoryIs(ui, {category})")
                        .build(),
                DomainStep.act("searchProducts", "Search the products for {text}")
                        .about("Needs: a new order showing. Ends: on it, listing the products whose SKU or name holds the"
                                + " text (\"No products match ‘…’\" when none does)")
                        .param("text", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.searchProducts(ui, p.text("text")))
                        .match(json("{'action':'type','target':{'scope':%s,'role':'text','name':%s},'text':'{text}'}",
                                NEW_ORDER_SCOPE, text(SEARCH_FIELD)))
                        .java(STEPS, "searchProducts(ui, {text})")
                        .build(),
                DomainStep.act("clearProductSearch", "Clear the product search")
                        .about("Needs: a new order showing. Ends: on it, its Search emptied with its X, every product of"
                                + " the selected category listed")
                        .code((ui, p) -> OrderDeskNewOrderSteps.clearProductSearch(ui))
                        .match(json("{'action':'click','target':{'scope':[%s,%s],'id':'clear'}}",
                                OrderDeskVocabulary.prefix("tab:" + NEW_ORDER_TAB), text("text:" + SEARCH_FIELD)))
                        .java(STEPS, "clearProductSearch(ui)")
                        .build(),
                DomainStep.act("addProductsToCart", "Add {products} to the cart together")
                        .about("Needs: a new order showing, the products listed. Ends: on it, one more of each in the cart"
                                + " (in the list's order). {products}: names or their starts " + SEVERAL
                                + " (\"Stapler; Gel pen, black\"; one named twice counts once). The app leaves out-of-stock"
                                + " products out: the outcome says, from the cart afterwards, which were added and which"
                                + " left out (leftOut); all out of stock fails with the app's reason")
                        .param("products", ParamType.TEXT)
                        .codeWithOutcome((ui, p) -> OrderDeskNewOrderSteps.addProductsToCart(ui, p.text("products")))
                        .match(json("{'action':'click','target':{%s}}", productRow("products")),
                                json("{'action':'click','target':{%s}}", productRow("*")),
                                json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}",
                                        NEW_ORDER_SCOPE, text(ADD_TO_CART_BUTTON)))
                        .autoFold(false)
                        .java(STEPS, "addProductsToCart(ui, {products})")
                        .build(),
                DomainStep.act("selectProducts", "Select {products} in the products list")
                        .about("Needs: a new order showing, the products listed. Ends: on it, those products selected and"
                                + " nothing added: Add to cart then shows whether it can add them (check it with \"<button> is disabled:"
                                + " <reason>\", e.g. \"The selected products are out of stock\"), the count reads \"… · 2"
                                + " selected\". {products}: names or their starts " + SEVERAL + " (the first clicked, the"
                                + " others with Cmd or Ctrl held)")
                        .param("products", ParamType.TEXT).choices(products)
                        .code((ui, p) -> OrderDeskNewOrderSteps.selectProducts(ui, p.text("products")))
                        .match(json("{'action':'click','target':{%s}}", productRow("products")))
                        // a click on a product row is also the first half of adding it: folded at once, it would take
                        // the click a following Add to cart needs (a recording folds a run as soon as it fits)
                        .autoFold(false)
                        .java(STEPS, "selectProducts(ui, {products})")
                        .build(),
                DomainStep.act("showInCategory", "Show {product} in its category")
                        .about("Needs: a new order showing, the product listed. Ends: on it, Categories on the product's"
                                + " category, the product selected")
                        .param("product", ParamType.TEXT).choices(products)
                        .code((ui, p) -> OrderDeskNewOrderSteps.showInCategory(ui, p.text("product")))
                        .match(json("{'action':'contextMenu','target':{%s},'path':[%s]}", productRow("product"),
                                text(SHOW_IN_CATEGORY_ITEM)))
                        .java(STEPS, "showInCategory(ui, {product})")
                        .build(),
                DomainStep.check("productsListedAre", "The products listed are {products}")
                        .about("Reads the products the list shows, in order, by name " + SEVERAL + "; none: nothing fits"
                                + " the search (\"No products match ‘zzz’\")")
                        .param("products", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.productsListedAre(ui, p.text("products")))
                        .match(json("{'action':'expect','target':{%s},'shown':true}", productRow("products")))
                        .autoFold(false)
                        .java(STEPS, "productsListedAre(ui, {products})")
                        .build(),
                DomainStep.check("productIs", "The {column} of {product} is {value}")
                        .about("Reads one value of a listed product: its SKU, Price or Stock (\"Out of stock\" at 0),"
                                + " e.g. the Price of Stapler is €12.50. {product}: its name, the start of it, or its SKU (\"DSK-STAPLER\")")
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskNewOrderSteps.PRODUCT_CHECK_COLUMNS)
                        .param("product", ParamType.TEXT).choices(products)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.productIs(ui, p.text("product"), p.text("column"), p.text("value")))
                        // as the Check menu records a cell check: the products table's rows by their key, the
                        // first column (SKU); the cart's are keyed by Product
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table'},"
                                + "'cell':{'row':{%s:'{product}'},'column':'{column}','expected':'{value}'}}",
                                NEW_ORDER_SCOPE, key(SKU_COLUMN)))
                        // as the agent and a step's body record it: the cell as the target, then its text
                        .matchAlso(json("{'action':'expect','target':{%s,'column':'{column}'},'text':'{value}'}", productRow("product")))
                        .java(STEPS, "productIs(ui, {product}, {column}, {value})")
                        .build(),
                DomainStep.check("lowStockWarningIs", "The low-stock warning of {product} is {warning}")
                        .about("Reads the warning a listed product's Stock shows on hover at 5 or fewer left, e.g."
                                + " \"Low stock: 4 left\"; none: no warning")
                        .param("product", ParamType.TEXT).choices(products)
                        .param("warning", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.lowStockWarningIs(ui, p.text("product"), p.text("warning")))
                        .match(json("{'action':'expect','target':{%s,'column':%s},'tooltip':'{warning}'}", productRow("product"),
                                text(STOCK_COLUMN)))
                        .java(STEPS, "lowStockWarningIs(ui, {product}, {warning})")
                        .build(),
                DomainStep.check("productCountIs", "The product count reads {count}")
                        .about("Reads the count under the products, e.g. \"22 products\" or \"22 products · 2 selected\"")
                        .param("count", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.productCountIs(ui, p.text("count")))
                        .match(json("{'action':'expect','target':{'scope':%s,'id':%s},'text':'{count}'}",
                                NEW_ORDER_SCOPE, text(PRODUCT_COUNT_ID)))
                        .java(STEPS, "productCountIs(ui, {count})")
                        .build(),

                // ---- the cart
                DomainStep.act("removeFromCart", "Remove {products} from the cart")
                        .about("Needs: a new order with those lines in its cart. Ends: on it, the lines gone. {products}:"
                                + " one line or several " + SEVERAL + " (names or their starts; one named twice counts"
                                + " once); several are asked about"
                                + " first (\"Remove 2 lines from the cart?\"), answered Remove lines")
                        .param("products", ParamType.TEXT).choices(cartLines)
                        .code((ui, p) -> OrderDeskNewOrderSteps.removeFromCart(ui, p.text("products")))
                        .match(json("{'action':'click','target':{%s}}", cartRow("products")),
                                json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}",
                                        NEW_ORDER_SCOPE, text(REMOVE_BUTTON)))
                        .matchAlso(json("{'action':'contextMenu','target':{%s},'path':[%s]}", cartRow("products"),
                                text(REMOVE_BUTTON)))
                        .java(STEPS, "removeFromCart(ui, {products})")
                        .build(),
                DomainStep.act("keepCartLines", "Ask to remove {products} from the cart, then keep them")
                        .about("Needs: a new order with those lines (two or more, " + SEVERAL + ") in its cart. Ends: on"
                                + " it, the cart as it was, after answering Keep to \"Remove 2 lines from the cart?\"")
                        .param("products", ParamType.TEXT).choices(cartLines)
                        .code((ui, p) -> OrderDeskNewOrderSteps.keepCartLines(ui, p.text("products")))
                        .match(json("{'action':'click','target':{%s}}", cartRow("products")),
                                json("{'action':'click','target':{%s}}", cartRow("*")),
                                json("{'action':'click','target':{'scope':%s,'role':'button','name':%s}}",
                                        NEW_ORDER_SCOPE, text(REMOVE_BUTTON)),
                                json("{'action':'answer','button':%s}", text(KEEP_ANSWER)))
                        .autoFold(false)
                        .java(STEPS, "keepCartLines(ui, {products})")
                        .build(),
                DomainStep.check("cartLinesAre", "The cart holds {lines}")
                        .about("Reads the cart's lines in the order they were added, by product name " + SEVERAL
                                + "; none: an empty cart (\"Select products and press Add to cart.\")")
                        .param("lines", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.cartLinesAre(ui, p.text("lines")))
                        .match(json("{'action':'expect','target':{%s},'shown':true}", cartRow("lines")))
                        .autoFold(false)
                        .java(STEPS, "cartLinesAre(ui, {lines})")
                        .build(),
                DomainStep.check("cartLineIs", "The {column} of {product} in the cart is {value}")
                        .about("Reads one value of a cart line: its Qty, Price or Line total, e.g. the Line total of A4"
                                + " paper, 500 sheets is €14.70")
                        .param("column", ParamType.TEXT).choices(() -> OrderDeskNewOrderSteps.CART_CHECK_COLUMNS)
                        .param("product", ParamType.TEXT).choices(cartLines)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.cartLineIs(ui, p.text("product"), p.text("column"), p.text("value")))
                        // as the Check menu records a cell check: the cart's lines by their key, the first column
                        // (Product); the row is the one the table was seen at, so it names Line total too, which the
                        // products table (keyed by SKU, with a Product column of its own) has not
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'table'},"
                                + "'cell':{'row':{%s:'{product}',%s:'*'},'column':'{column}','expected':'{value}'}}",
                                NEW_ORDER_SCOPE, key(PRODUCT_COLUMN), key(LINE_TOTAL_COLUMN)))
                        // as the agent and a step's body record it: the cell as the target, then its text
                        .matchAlso(json("{'action':'expect','target':{%s,'column':'{column}'},'text':'{value}'}", cartRow("product")))
                        .java(STEPS, "cartLineIs(ui, {product}, {column}, {value})")
                        .build(),
                DomainStep.check("cartAmountIs", "The cart's {amount} is {value}")
                        .about("Reads one amount under the cart: Subtotal, Volume discount (5% off a line of 10 or more;"
                                + " none while it does not show), Shipping (\"chosen at checkout\"), VAT (20%) or Total,"
                                + " e.g. the cart's Subtotal is €26.70")
                        .param("amount", ParamType.TEXT).choices(() -> OrderDeskNewOrderSteps.CART_AMOUNTS)
                        .param("value", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.cartAmountIs(ui, p.text("amount"), p.text("value")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':'{amount}'},'text':'{value}'}",
                                NEW_ORDER_SCOPE))
                        .autoFold(false)
                        .java(STEPS, "cartAmountIs(ui, {amount}, {value})")
                        .build(),
                DomainStep.check("cartStockWarningIs", "The cart warns {warning}")
                        .about("Reads the warning under the cart while a line asks for more than is in stock, e.g."
                                + " \"Only 4 Staplers in stock\" (one per line, joined by \"; \"); none: no warning")
                        .param("warning", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.cartStockWarningIs(ui, p.text("warning")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'label','name':%s},'text':'{warning}'}",
                                NEW_ORDER_SCOPE, OrderDeskVocabulary.quote("*" + OrderDeskVocabulary.plain(IN_STOCK_WARNING) + "*")))
                        .java(STEPS, "cartStockWarningIs(ui, {warning})")
                        .build(),
                DomainStep.check("buttonIsDisabled", "{button} is disabled: {reason}")
                        .about("Reads that Add to cart, Remove or Checkout… is disabled, and the reason beside it, e.g."
                                + " Checkout… \"The cart is empty\", Add to cart \"Select a product first\", Remove"
                                + " \"Select a cart line first\"; in the checkout Next (\"Fix the fields marked in red\","
                                + " \"Enter the PO number\", \"Complete the card details\"), Express (\"Express delivers to"
                                + " Germany and Austria only\") or Invoice (\"Café Lumen pays by card\"); on the selected"
                                + " order's own tab Cancel order (\"" + CANCEL_HINT + HINT_DASH + "this one is shipped\", or"
                                + " just \"this one is shipped\")")
                        .param("button", ParamType.TEXT).choices(OrderDeskNewOrderVocabulary::reasonButtons)
                        .param("reason", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskNewOrderSteps.buttonIsDisabled(ui, p.text("button"), p.text("reason")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'button','name':'{button}'},'state':['disabled']}",
                                NEW_ORDER_SCOPE),
                                json("{'action':'expect','target':{'scope':%s},'text':'{reason}'}", NEW_ORDER_SCOPE))
                        .autoFold(false)
                        .java(STEPS, "buttonIsDisabled(ui, {button}, {reason})")
                        .build(),
                DomainStep.check("buttonIsEnabled", "{button} is enabled")
                        .about("Reads that Add to cart, Remove or Checkout… (in the checkout: Next, Express or Invoice; on"
                                + " the selected order's own tab: Cancel order or Save receipt…) is enabled; when not, the"
                                + " failure gives the reason beside it")
                        .param("button", ParamType.TEXT).choices(OrderDeskNewOrderVocabulary::reasonButtons)
                        .code((ui, p) -> OrderDeskNewOrderSteps.buttonIsEnabled(ui, p.text("button")))
                        .match(json("{'action':'expect','target':{'scope':%s,'role':'button','name':'{button}'},'state':['!disabled']}",
                                NEW_ORDER_SCOPE))
                        .matchAlso(json("{'action':'expect','target':{'scope':%s,'role':'button','name':'{button}'},'state':['!disabled']}",
                                OrderDeskOrderTabVocabulary.anyOrderScope()))
                        .java(STEPS, "buttonIsEnabled(ui, {button})")
                        .build());
    }

    /** The buttons whose reason a check reads: a new order's, then the checkout's. */
    static List<String> reasonButtons() {
        List<String> all = new java.util.ArrayList<String>(OrderDeskNewOrderSteps.REASON_BUTTONS);
        all.addAll(OrderDeskCheckoutSteps.REASON_CONTROLS);
        all.addAll(OrderDeskOrderTabSteps.BUTTONS);
        return all;
    }

    /** A product's row in the products table, by the param (or {@code *}); the cart has no SKU column. */
    private static String productRow(String param) {
        return json("'scope':%s,'role':'table','row':{%s:'*',%s:%s}", NEW_ORDER_SCOPE, key(SKU_COLUMN), key(PRODUCT_COLUMN),
                capture(param));
    }

    /** A cart line by the param (or {@code *}); the products table has no Line total column. */
    private static String cartRow(String param) {
        return json("'scope':%s,'role':'table','row':{%s:%s,%s:'*'}", NEW_ORDER_SCOPE, key(PRODUCT_COLUMN), capture(param),
                key(LINE_TOTAL_COLUMN));
    }

    private static String capture(String param) {
        return OrderDeskVocabulary.quote("*".equals(param) ? "*" : "{" + param + "}");
    }
}
