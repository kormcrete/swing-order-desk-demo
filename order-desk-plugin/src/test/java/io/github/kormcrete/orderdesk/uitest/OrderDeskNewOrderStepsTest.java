package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.driver.model.Node;
import io.github.kormcrete.swingui.junit.Client;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.FakeApp.cell;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.items;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.label;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.node;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.table;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.withState;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.withTooltip;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The New order steps and checks on a stand-in app ({@link FakeApp}): the steps each takes, how it finds a product,
 * a cart line or a customer by the start of its name, and how it fails, in words, when the new order is not showing,
 * a name fits several or none, or what the app shows is not what was expected.
 */
class OrderDeskNewOrderStepsTest {

    private static final String NO = "'scope':['tab:New order*']";
    private static final List<String> PRODUCT_COLUMNS = Arrays.asList("SKU", "Product", "Price", "Stock");
    private static final List<String> CART_COLUMNS = Arrays.asList("Product", "Qty", "Price", "Line total");

    private final FakeApp app = new FakeApp();
    private final Client ui = app.client();

    // ---- the customer

    @Test
    void openNewOrderTypesTheNameAndTakesTheOnlySuggestion() {
        suggestions("Ada Shop Ltd Berlin · Account");

        OrderDeskNewOrderSteps.openNewOrder(ui, "Ada");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'menu','path':['File','New order']},"
                + "{'action':'type','target':{'scope':['tab:New order'],'role':'text','name':'Customer'},'text':'Ada'},"
                + "{'action':'press','keys':'Enter'}]"));
    }

    @Test
    void partialCustomerNameResolvesOrNamesChoices() {
        // the app matches anywhere in the name or the city: "am" fits Café Lumen (Amsterdam) and Fjord Supplies (Hamburg)
        suggestions("Café Lumen Amsterdam · Card only", "Fjord Supplies Hamburg · Card only");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.openNewOrder(ui, "am"));

        assertThat(two).isInstanceOf(DriverException.class).hasMessageContaining("“am” fits 2 customers")
                .hasMessageContaining("Café Lumen Amsterdam · Card only").hasMessageContaining("Fjord Supplies Hamburg · Card only");
        assertThat(((DriverException) two).reason()).isEqualTo(Reason.AMBIGUOUS);
        assertThat(app.steps().toString()).doesNotContain("Enter");
    }

    @Test
    void theStartOfOneCustomersNameTakesItWhenTheCityFitsAnother() {
        // "b": Birch & Co by its name, Ada Shop Ltd by Berlin, Durand Bureau by its name's second word
        suggestions("Ada Shop Ltd Berlin · Account", "Birch & Co Vienna · Account", "Durand Bureau Paris · Card only");
        newOrderShowing();

        OrderDeskNewOrderSteps.chooseCustomer(ui, "b");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'type','target':{" + NO + ",'role':'text','name':'Customer'},'text':'b'},"
                + "{'action':'click','target':{'window':'popup','role':'list','item':['Birch & Co Vienna · Account']}}]"));
    }

    @Test
    void chooseCustomerOnTwoMatchesFailsNamingBoth() {
        newOrderShowing();
        suggestions("Ada Shop Ltd Berlin · Account", "Ada Lindqvist Hamburg · Card only");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.chooseCustomer(ui, "Ada"));

        assertThat(two).hasMessageContaining("“Ada” fits 2 customers").hasMessageContaining("Ada Shop Ltd Berlin")
                .hasMessageContaining("Ada Lindqvist Hamburg");
    }

    @Test
    void aNameThatFitsNoCustomerFailsNamingTheCustomersTheAppOffers() {
        // the step clears the field and opens every suggestion (Down) to name them
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.chooseCustomer(uiThatFindsNoSuggestionFirst(), "zz"));

        assertThat(none).isInstanceOf(DriverException.class).hasMessageContaining("No customer fits “zz”")
                .hasMessageContaining("Ada Shop Ltd Berlin · Account; Birch & Co Vienna · Account");
        assertThat(((DriverException) none).reason()).isEqualTo(Reason.NOT_FOUND);
    }

    @Test
    void typeCustomerOnlyTypes() {
        newOrderShowing();

        OrderDeskNewOrderSteps.typeCustomer(ui, "Bi");

        assertThat(app.steps()).isEqualTo(json("[{'action':'type','target':{" + NO + ",'role':'text','name':'Customer'},'text':'Bi'}]"));
    }

    @Test
    void customerSuggestionsAreComparesTheNamesInOrder() {
        newOrderShowing();
        suggestions("Café Lumen Amsterdam · Card only", "Fjord Supplies Hamburg · Card only");

        OrderDeskNewOrderSteps.customerSuggestionsAre(ui, "Café Lumen; Fjord Supplies");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.customerSuggestionsAre(ui, "Fjord Supplies"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessageContaining("Café Lumen Amsterdam · Card only; Fjord Supplies Hamburg · Card only")
                .hasMessageContaining("expected Fjord Supplies");
    }

    @Test
    void customerSuggestionsAreNoneChecksTheNoMatchText() {
        newOrderShowing();
        app.answer("popup label 'No customer matches'", label("No customer matches"));

        OrderDeskNewOrderSteps.customerSuggestionsAre(ui, "none");

        assertThat(app.steps()).isEqualTo(json("[{'action':'expect','target':{'window':'popup','role':'label',"
                + "'name':'No customer matches'},'text':'No customer matches','timeout':1000}]"));
    }

    @Test
    void newOrderIsForReadsTheSelectedTabsTitle() {
        newOrderShowing();
        app.answer("tab 'Orders'", label("Orders", "New order · Ada Shop Ltd"));

        OrderDeskNewOrderSteps.newOrderIsFor(ui, "Ada Shop Ltd");
        OrderDeskNewOrderSteps.newOrderIsFor(ui, "ada shop ltd");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.newOrderIsFor(ui, "Birch & Co"));
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.newOrderIsFor(ui, "none"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("Ada Shop Ltd").hasMessageContaining("Birch & Co");
        assertThat(none).isInstanceOf(AssertionError.class).hasMessageContaining("Ada Shop Ltd").hasMessageContaining("no customer");
    }

    @Test
    void customerTermsAreFailsWithExpectedAndActual() {
        newOrderShowing();
        app.answer("element #customerTerms", label("customerTerms", "Card only"));

        OrderDeskNewOrderSteps.customerTermsAre(ui, "Card only");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.customerTermsAre(ui, "Account · Invoice 30 days"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("Card only")
                .hasMessageContaining("Account · Invoice 30 days");
    }

    @Test
    void recentCustomersAreOpensTheToolbarMenuReadsItAndClosesIt() {
        app.answer("button 'New order for a recent customer'", node("button", "New order for a recent customer"));
        app.answer("popup menuitem", node("menuitem", "Ada Shop Ltd"), node("menuitem", "Café Lumen"));

        OrderDeskNewOrderSteps.recentCustomersAre(ui, "Ada Shop Ltd; Café Lumen");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.recentCustomersAre(ui, "Café Lumen; Ada Shop Ltd"));

        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'click','target':{'role':'button','name':'New order for a recent customer'}}"));
        assertThat(app.steps().get(1)).isEqualTo(json("{'action':'press','keys':'Escape'}"));
        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("Ada Shop Ltd; Café Lumen")
                .hasMessageContaining("expected Café Lumen; Ada Shop Ltd");
    }

    @Test
    void recentCustomersAreFailsFastWhenItsScreenIsNotShowing() {
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskNewOrderSteps.recentCustomersAre(ui, "Ada Shop Ltd"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).isInstanceOf(DriverException.class).hasMessageContaining("New order for a recent customer (▾) is not showing");
        assertThat(app.steps()).isEmpty();
    }

    // ---- products

    @Test
    void addToCartFindsTheProductByTheStartOfItsName() {
        newOrderShowing();
        products();
        app.answer("button 'Add to cart'", node("button", "Add to cart"));

        OrderDeskNewOrderSteps.addToCart(ui, "stapl");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Add to cart'}}]"));
    }

    @Test
    void partialProductNameResolvesOrNamesChoices() {
        newOrderShowing();
        products();
        app.answer("button 'Add to cart'", node("button", "Add to cart"));

        OrderDeskNewOrderSteps.addToCart(ui, "A4 PAPER");
        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.addToCart(ui, "gel pen"));
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.addToCart(ui, "pencil"));

        assertThat(app.steps().get(0).toString()).contains("A4 paper, 500 sheets");
        assertThat(two).isInstanceOf(DriverException.class).hasMessageContaining("“gel pen” fits 2 products")
                .hasMessageContaining("Gel pen, black; Gel pen, blue");
        assertThat(((DriverException) two).reason()).isEqualTo(Reason.AMBIGUOUS);
        assertThat(none).isInstanceOf(DriverException.class).hasMessageContaining("No product fits “pencil”")
                .hasMessageContaining("A4 paper, 500 sheets; Gel pen, black; Gel pen, blue; Stapler; Desk lamp");
        assertThat(((DriverException) none).reason()).isEqualTo(Reason.NOT_FOUND);
    }

    @Test
    void aProductNotListedSaysTheListIsSearched() {
        newOrderShowing();
        products();
        app.answer("text 'Search'", label("Search", "pen"));

        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.addToCart(ui, "Espresso"));

        assertThat(none).hasMessageContaining("No product fits “Espresso”").hasMessageContaining("Search holds “pen”");
    }

    @Test
    void theProductsPastTheFirstTwentyRowsAreReadToo() {
        newOrderShowing();
        List<List<String>> rows = new ArrayList<List<String>>();
        for (int i = 0; i < 20; i++) rows.add(Arrays.asList("SKU-" + i, "Product " + i, "€1.00", "10"));
        app.answer("table col=SKU", table(PRODUCT_COLUMNS, 21, rows));
        app.answer("table row=20 col=SKU", cell("SKU", "CLN-BAGS"));
        app.answer("table row=CLN-BAGS col=Product", cell("Product", "Bin bags, roll of 50"));
        app.answer("button 'Add to cart'", node("button", "Add to cart"));

        OrderDeskNewOrderSteps.addToCart(ui, "bin bags");

        assertThat(app.steps().get(0).toString()).contains("Bin bags, roll of 50");
    }

    @Test
    void addToCartOfAnOutOfStockProductFailsWithTheAppsReason() {
        newOrderShowing();
        products();
        app.answer("button 'Add to cart'", withState(node("button", "Add to cart"), "disabled"));
        app.answer("element #addToCartReason", label("addToCartReason", "The selected products are out of stock"));

        Throwable lamp = catchThrowable(() -> OrderDeskNewOrderSteps.addToCart(ui, "Desk lamp"));

        assertThat(lamp).isInstanceOf(DriverException.class)
                .hasMessage("NOT_ACTIONABLE: Desk lamp cannot be added to the cart: The selected products are out of stock");
        // the person's view: the lamp selected, Add to cart never pressed
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Desk lamp'},'column':'SKU'}}]"));
    }

    @Test
    void addToCartFailsFastWhenItsScreenIsNotShowing() {
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskNewOrderSteps.addToCart(ui, "Stapler"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).isInstanceOf(DriverException.class).hasMessageContaining("No new order is showing")
                .hasMessageContaining("Open a new order for");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void addQtyToCartAddsThenTypesTheQuantityIntoItsCartLine() {
        newOrderShowing();
        products();
        cart("Stapler");
        app.answer("button 'Add to cart'", node("button", "Add to cart"));

        OrderDeskNewOrderSteps.addQtyToCart(ui, "stapler", 3);

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Add to cart'}},"
                + "{'action':'type','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'Qty'},"
                + "'text':'3','commit':true}]"));
    }

    @Test
    void addProductsToCartSelectsThemAllThenAddsOnceAndSaysWhichWereLeftOut() {
        CartFake app = new CartFake();

        Map<String, Object> made = OrderDeskNewOrderSteps.addProductsToCart(app.client(), "stapler; desk lamp; A4");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Desk lamp'},'column':'SKU'},'modifiers':['Mod']},"
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'A4 paper, 500 sheets'},'column':'SKU'},'modifiers':['Mod']},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Add to cart'}}]"));
        assertThat(made).containsEntry("added", Arrays.asList("Stapler", "A4 paper, 500 sheets"))
                .containsEntry("leftOut", Collections.singletonList("Desk lamp"));
    }

    @Test
    void browseCategoryClicksTheCategoryByItsName() {
        newOrderShowing();
        categories();

        OrderDeskNewOrderSteps.browseCategory(ui, "desk");
        OrderDeskNewOrderSteps.browseCategory(ui, "All products");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'tree','item':['Office','Desk']}},"
                + "{'action':'click','target':{" + NO + ",'role':'tree','item':['All products']}}]"));
    }

    @Test
    void selectedCategoryIsFailsWithExpectedAndActual() {
        newOrderShowing();
        app.answer("tree", label("tree", "Desk"));

        OrderDeskNewOrderSteps.selectedCategoryIs(ui, "Desk");
        OrderDeskNewOrderSteps.selectedCategoryIs(ui, "Office > Desk");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.selectedCategoryIs(ui, "Pens"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("Desk").hasMessageContaining("Pens");
    }

    @Test
    void searchProductsTypesIntoSearchAndClearUsesItsX() {
        newOrderShowing();
        app.answer("text 'Search'", label("Search", "pen"));

        OrderDeskNewOrderSteps.searchProducts(ui, "pen");
        OrderDeskNewOrderSteps.clearProductSearch(ui);
        app.answer("text 'Search'", label("Search", ""));
        OrderDeskNewOrderSteps.clearProductSearch(ui);   // already empty: nothing to do

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'type','target':{" + NO + ",'role':'text','name':'Search'},'text':'pen'},"
                + "{'action':'click','target':{'scope':['tab:New order*','text:Search'],'id':'clear'}}]"));
    }

    @Test
    void showInCategoryUsesTheProductsRowMenu() {
        newOrderShowing();
        products();

        OrderDeskNewOrderSteps.showInCategory(ui, "stapler");

        assertThat(app.steps()).isEqualTo(json("[{'action':'contextMenu','target':{" + NO + ",'role':'table',"
                + "'row':{'Product':'Stapler'},'column':'SKU'},'path':['Show in category']}]"));
    }

    @Test
    void productsListedAreFailsWithExpectedAndActual() {
        newOrderShowing();
        products();

        OrderDeskNewOrderSteps.productsListedAre(ui, "A4 paper, 500 sheets; Gel pen, black; Gel pen, blue; Stapler; Desk lamp");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.productsListedAre(ui, "Stapler; Desk lamp"));
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.productsListedAre(ui, "none"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("expected Stapler; Desk lamp")
                .hasMessageContaining("A4 paper, 500 sheets; Gel pen, black");
        assertThat(none).isInstanceOf(AssertionError.class).hasMessageContaining("5 products");
    }

    @Test
    void productIsReadsOneCellOfTheProductsRow() {
        newOrderShowing();
        products();
        app.answer("table row=DSK-LAMP,Desk lamp col=Stock", cell("Stock", "Out of stock"));

        OrderDeskNewOrderSteps.productIs(ui, "desk lamp", "stock", "Out of stock");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.productIs(ui, "desk lamp", "Stock", "3"));

        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'expect','target':{" + NO + ",'role':'table',"
                + "'row':{'SKU':'DSK-LAMP','Product':'Desk lamp'},'column':'Stock'},'text':'Out of stock','timeout':1000}"));
        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("Out of stock").hasMessageContaining("3");
    }

    @Test
    void lowStockWarningIsReadsTheStockCellsTooltip() {
        newOrderShowing();
        products();
        app.answer("table row=DSK-STAPLER,Stapler col=Stock", withTooltip(cell("Stock", "4"), "Low stock: 4 left"));
        app.answer("table row=PAP-A4-500,A4 paper, 500 sheets col=Stock", cell("Stock", "120"));

        OrderDeskNewOrderSteps.lowStockWarningIs(ui, "Stapler", "Low stock: 4 left");
        OrderDeskNewOrderSteps.lowStockWarningIs(ui, "A4", "none");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.lowStockWarningIs(ui, "A4", "Low stock: 4 left"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageContaining("A4 paper, 500 sheets")
                .hasMessageContaining("no warning").hasMessageContaining("Low stock: 4 left");
    }

    @Test
    void productCountIsFailsWithExpectedAndActual() {
        newOrderShowing();
        app.answer("element #productCount", label("productCount", "22 products · 1 selected"));

        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.productCountIs(ui, "22 products"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The product count reads “22 products · 1 selected”, expected “22 products”");
    }

    // ---- the cart

    @Test
    void setQuantityFindsTheCartLineByTheStartOfItsName() {
        newOrderShowing();
        cart("A4 paper, 500 sheets", "Gel pen, black");

        OrderDeskNewOrderSteps.setQuantity(ui, "a4", 3);

        assertThat(app.steps()).isEqualTo(json("[{'action':'type','target':{" + NO + ",'role':'table',"
                + "'row':{'Product':'A4 paper, 500 sheets'},'column':'Qty'},'text':'3','commit':true}]"));
    }

    @Test
    void setQuantityOnTwoMatchesFailsNamingBoth() {
        newOrderShowing();
        cart("Gel pen, black", "Gel pen, blue");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.setQuantity(ui, "gel", 3));

        assertThat(two).isInstanceOf(DriverException.class).hasMessageContaining("“gel” fits 2 cart lines")
                .hasMessageContaining("Gel pen, black; Gel pen, blue");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void setQuantityOfAProductNotInTheCartNamesTheLines() {
        newOrderShowing();
        cart("Gel pen, black");

        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.setQuantity(ui, "Stapler", 3));

        assertThat(none).hasMessageContaining("No cart line fits “Stapler”").hasMessageContaining("Gel pen, black");
    }

    @Test
    void removeFromCartOneLineAsksNothing() {
        newOrderShowing();
        cart("Stapler", "Gel pen, black");

        OrderDeskNewOrderSteps.removeFromCart(ui, "stapler");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'Line total'}},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Remove'}}]"));
    }

    @Test
    void removeFromCartSeveralLinesAnswersRemoveLines() {
        newOrderShowing();
        cart("Stapler", "Gel pen, black");

        OrderDeskNewOrderSteps.removeFromCart(ui, "Stapler; Gel pen");

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'Line total'}},"
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Gel pen, black'},'column':'Line total'},'modifiers':['Mod']},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Remove'}},"
                + "{'action':'answer','button':'Remove lines'}]"));
    }

    @Test
    void removeFromCartOnTwoMatchesFailsNamingBoth() {
        newOrderShowing();
        cart("Gel pen, black", "Gel pen, blue");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.removeFromCart(ui, "Gel pen"));

        assertThat(two).hasMessageContaining("“Gel pen” fits 2 cart lines");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void keepCartLinesAnswersKeepAndNeedsTwoLines() {
        newOrderShowing();
        cart("Stapler", "Gel pen, black");

        OrderDeskNewOrderSteps.keepCartLines(ui, "Stapler; Gel pen");
        Throwable one = catchThrowable(() -> OrderDeskNewOrderSteps.keepCartLines(ui, "Stapler"));

        assertThat(app.steps().get(3)).isEqualTo(json("{'action':'answer','button':'Keep'}"));
        assertThat(one).isInstanceOf(DriverException.class).hasMessageContaining("two or more");
    }

    @Test
    void cartLinesAreFailsWithExpectedAndActual() {
        newOrderShowing();
        cart("Stapler", "Gel pen, black");

        OrderDeskNewOrderSteps.cartLinesAre(ui, "Stapler; Gel pen, black");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.cartLinesAre(ui, "Gel pen, black; Stapler"));
        Throwable empty = catchThrowable(() -> OrderDeskNewOrderSteps.cartLinesAre(ui, "none"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The cart holds Stapler; Gel pen, black, expected Gel pen, black; Stapler");
        assertThat(empty).isInstanceOf(AssertionError.class).hasMessageContaining("Stapler; Gel pen, black")
                .hasMessageContaining("expected an empty cart");
    }

    @Test
    void anEmptyCartChecksItsText() {
        newOrderShowing();
        app.answer("label 'Select products and press Add to cart.'", label("Select products and press Add to cart."));

        OrderDeskNewOrderSteps.cartLinesAre(ui, "none");

        assertThat(app.steps()).isEqualTo(json("[{'action':'expect','target':{" + NO + ",'role':'label',"
                + "'name':'Select products and press Add to cart.'},'text':'Select products and press Add to cart.','timeout':1000}]"));
    }

    @Test
    void cartLineIsFailsWithExpectedAndActual() {
        newOrderShowing();
        cart("A4 paper, 500 sheets");
        app.answer("table row=A4 paper, 500 sheets col=Line total", cell("Line total", "€14.70"));

        OrderDeskNewOrderSteps.cartLineIs(ui, "A4", "line total", "€14.70");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.cartLineIs(ui, "A4", "Line total", "€9.80"));
        Throwable column = catchThrowable(() -> OrderDeskNewOrderSteps.cartLineIs(ui, "A4", "Colour", "red"));

        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'expect','target':{" + NO + ",'role':'table',"
                + "'row':{'Product':'A4 paper, 500 sheets'},'column':'Line total'},'text':'€14.70','timeout':1000}"));
        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("A4 paper, 500 sheets's Line total in the cart reads “€14.70”, expected “€9.80”");
        assertThat(column).isInstanceOf(DriverException.class).hasMessageContaining("Qty; Price; Line total");
    }

    @Test
    void cartLineIsOnTwoMatchesFailsNamingBoth() {
        newOrderShowing();
        cart("Gel pen, black", "Gel pen, blue");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.cartLineIs(ui, "gel pen", "Qty", "1"));

        assertThat(two).isInstanceOf(DriverException.class).hasMessageContaining("“gel pen” fits 2 cart lines")
                .hasMessageContaining("Gel pen, black; Gel pen, blue");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void browseCategoryOnTwoMatchesFailsNamingBoth() {
        newOrderShowing();
        categories();

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.browseCategory(ui, "p"));
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.browseCategory(ui, "Tea"));

        assertThat(two).hasMessageContaining("“p” fits 2 categories: Office > Paper; Office > Pens");
        assertThat(none).hasMessageContaining("No category fits “Tea”").hasMessageContaining("All products; Office");
    }

    @Test
    void cartAmountIsReadsTheValueCaptionedSoAndNoneMeansNotShown() {
        newOrderShowing();
        app.answer("label 'Subtotal'", label("Subtotal", "€26.70"));
        app.answer("label 'VAT (20%)'", label("VAT (20%)", "€5.22"));

        OrderDeskNewOrderSteps.cartAmountIs(ui, "subtotal", "€26.70");
        OrderDeskNewOrderSteps.cartAmountIs(ui, "VAT", "€5.22");
        OrderDeskNewOrderSteps.cartAmountIs(ui, "Volume discount", "none");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.cartAmountIs(ui, "Subtotal", "€20.00"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The cart's Subtotal reads “€26.70”, expected “€20.00”");
    }

    @Test
    void cartTotalIsFailsWithExpectedAndActual() {
        newOrderShowing();
        app.answer("label 'Total'", label("Total", "€31.32"));

        OrderDeskNewOrderSteps.cartTotalIs(ui, "€31.32");
        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.cartTotalIs(ui, "€30.00"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The cart total reads “€31.32”, expected “€30.00”");
    }

    @Test
    void cartStockWarningIsReadsTheWarningUnderTheCart() {
        newOrderShowing();
        app.answer("label 'in stock'", label("Only 4 Staplers in stock"));

        OrderDeskNewOrderSteps.cartStockWarningIs(ui, "Only 4 Staplers in stock");
        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.cartStockWarningIs(ui, "none"));

        assertThat(none).isInstanceOf(AssertionError.class).hasMessageContaining("Only 4 Staplers in stock");
    }

    @Test
    void buttonIsDisabledChecksTheStateAndTheReasonBesideIt() {
        newOrderShowing();
        app.answer("button 'Checkout…'", withState(node("button", "Checkout…"), "disabled"));
        app.answer("element #checkoutReason", label("checkoutReason", "Choose a customer first"));
        app.answer("button 'Remove'", withState(node("button", "Remove"), "disabled"));
        app.answer("label 'Select a cart line first'", label("Select a cart line first"));

        OrderDeskNewOrderSteps.buttonIsDisabled(ui, "checkout", "Choose a customer first");
        OrderDeskNewOrderSteps.buttonIsDisabled(ui, "Remove", "Select a cart line first");
        Throwable reason = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(ui, "Checkout…", "The cart is empty"));

        assertThat(reason).isInstanceOf(AssertionError.class)
                .hasMessage("Checkout…'s reason reads “Choose a customer first”, expected “The cart is empty”");
    }

    @Test
    void buttonIsEnabledFailsNamingTheReasonItShows() {
        newOrderShowing();
        app.answer("button 'Add to cart'", withState(node("button", "Add to cart"), "disabled"));
        app.answer("element #addToCartReason", label("addToCartReason", "Select a product first"));

        Throwable disabled = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsEnabled(ui, "add"));

        assertThat(disabled).isInstanceOf(AssertionError.class)
                .hasMessage("Add to cart is disabled (“Select a product first”), expected enabled");
    }

    // ---- after the review: failures leave the screen as found, names given twice, Remove's reason, whole names

    @Test
    void newOrderIsForTakesTheWholeNameOnly() {
        newOrderShowing();
        app.answer("tab 'Orders'", label("Orders", "New order · Ada Shop Ltd"));

        Throwable start = catchThrowable(() -> OrderDeskNewOrderSteps.newOrderIsFor(ui, "A"));

        assertThat(start).isInstanceOf(AssertionError.class)
                .hasMessage("The new order is for Ada Shop Ltd, expected A (only the start of its name: give the whole name)");
    }

    @Test
    void buttonIsDisabledRemoveFailsWithExpectedAndActual() {
        newOrderShowing();
        app.answer("button 'Remove'", withState(node("button", "Remove"), "disabled"));
        app.answer("label 'Select a cart line first'", label("Select a cart line first"));

        Throwable wrong = catchThrowable(() -> OrderDeskNewOrderSteps.buttonIsDisabled(ui, "Remove", "Nothing selected"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("Remove's reason reads “Select a cart line first”, expected “Nothing selected”");
    }

    @Test
    void addProductsToCartNamedTwiceSelectsItOnceAndReadsTheCartForWhatWasAdded() {
        CartFake app = new CartFake();

        Map<String, Object> made = OrderDeskNewOrderSteps.addProductsToCart(app.client(), "stapler; Stapler; desk lamp");

        // a second Mod-click on the Stapler would have unselected it
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'SKU'}},"
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Desk lamp'},'column':'SKU'},'modifiers':['Mod']},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Add to cart'}}]"));
        assertThat(made).containsEntry("added", Collections.singletonList("Stapler"))
                .containsEntry("leftOut", Collections.singletonList("Desk lamp"));
    }

    @Test
    void addProductsToCartCountsAProductAlreadyInTheCartWhenItsLineGrows() {
        CartFake app = new CartFake();
        app.inCart("Stapler", 1);

        Map<String, Object> made = OrderDeskNewOrderSteps.addProductsToCart(app.client(), "Stapler; A4");

        assertThat(made).containsEntry("added", Arrays.asList("Stapler", "A4 paper, 500 sheets"))
                .containsEntry("leftOut", Collections.emptyList());
    }

    @Test
    void removeFromCartNamedTwiceSelectsTheLineOnce() {
        newOrderShowing();
        cart("Stapler", "Gel pen, black");

        OrderDeskNewOrderSteps.removeFromCart(ui, "Stapler; stapler");
        Throwable one = catchThrowable(() -> OrderDeskNewOrderSteps.keepCartLines(ui, "Stapler; stapler"));

        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'Line total'}},"
                + "{'action':'click','target':{" + NO + ",'role':'button','name':'Remove'}},"
                + "{'action':'click','target':{" + NO + ",'role':'table','row':{'Product':'Stapler'},'column':'Line total'}}]"));
        assertThat(one).isInstanceOf(DriverException.class).hasMessageContaining("two or more");
    }

    @Test
    void chooseCustomerOnTwoMatchesClosesTheSuggestionsAndPicksTheCustomerAgain() {
        CustomerFake app = new CustomerFake("Ada Shop Ltd");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.chooseCustomer(app.client(), "am"));

        assertThat(two).hasMessageContaining("“am” fits 2 customers");
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'type','target':{" + NO + ",'role':'text','name':'Customer'},'text':'am'},"
                + "{'action':'press','keys':'Escape'},"
                + "{'action':'type','target':{" + NO + ",'role':'text','name':'Customer'},'text':'Ada Shop Ltd'},"
                + "{'action':'press','keys':'Enter'}]"));
    }

    @Test
    void chooseCustomerThatFitsNoneLeavesAnEmptyFieldEmpty() {
        CustomerFake app = new CustomerFake("");

        Throwable none = catchThrowable(() -> OrderDeskNewOrderSteps.chooseCustomer(app.client(), "zz"));

        assertThat(none).hasMessageContaining("No customer fits “zz”");
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'type','target':{" + NO + ",'role':'text','name':'Customer'},'text':'zz'},"
                + "{'action':'click','target':{'scope':['tab:New order*','text:Customer'],'id':'clear'}},"
                + "{'action':'press','keys':'Down','target':{" + NO + ",'role':'text','name':'Customer'}},"
                + "{'action':'press','keys':'Escape'}]"));
    }

    @Test
    void openNewOrderThatFitsNoOneClosesTheTabItOpened() {
        CustomerFake app = new CustomerFake("");

        Throwable two = catchThrowable(() -> OrderDeskNewOrderSteps.openNewOrder(app.client(), "am"));

        assertThat(two).hasMessageContaining("“am” fits 2 customers");
        assertThat(app.steps()).isEqualTo(json("["
                + "{'action':'menu','path':['File','New order']},"
                + "{'action':'type','target':{'scope':['tab:New order'],'role':'text','name':'Customer'},'text':'am'},"
                + "{'action':'press','keys':'Escape'},"
                + "{'action':'menu','path':['File','Close tab']}]"));
    }

    @Test
    void recentCustomersAreClosesTheMenuWhenItsReadFails() {
        FakeApp broken = new FakeApp() {
            @Override
            public List<Node> readAll(io.github.kormcrete.swingui.driver.model.Locator target, Integer maxRows) {
                throw new DriverException(Reason.APP_ERROR, "the menu went away");
            }
        };
        broken.answer("button 'New order for a recent customer'", node("button", "New order for a recent customer"));

        Throwable failed = catchThrowable(() -> OrderDeskNewOrderSteps.recentCustomersAre(broken.client(), "Ada Shop Ltd"));

        assertThat(failed).isNotNull();
        assertThat(broken.steps()).isEqualTo(json("["
                + "{'action':'click','target':{'role':'button','name':'New order for a recent customer'}},"
                + "{'action':'press','keys':'Escape'}]"));
    }

    // ---- helpers

    private void newOrderShowing() {
        app.answer("text 'Customer'", node("text", "Customer"));
    }

    private void suggestions(String... items) {
        app.answer("popup list", items("list", Arrays.asList(items), 0));
    }

    /** The products table: A4 paper, two gel pens, the Stapler (4 left) and the Desk lamp (out of stock). */
    private void products() {
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("PAP-A4-500", "A4 paper, 500 sheets", "€4.90", "120"),
                Arrays.asList("PEN-GEL-BLK", "Gel pen, black", "€1.20", "800"),
                Arrays.asList("PEN-GEL-BLU", "Gel pen, blue", "€1.20", "650"),
                Arrays.asList("DSK-STAPLER", "Stapler", "€12.50", "4"),
                Arrays.asList("DSK-LAMP", "Desk lamp", "€39.00", "Out of stock"));
        app.answer("table col=SKU", table(PRODUCT_COLUMNS, rows.size(), rows));
    }

    private void cart(String... products) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (String p : products) rows.add(Arrays.asList(p, "1", "€1.00", "€1.00"));
        app.answer("table col=Line total", table(CART_COLUMNS, rows.size(), rows));
    }

    private void categories() {
        Node tree = items("tree", Arrays.asList("All products", "Office", "Office > Paper", "Office > Pens", "Office > Desk",
                "Coffee", "Coffee > Beans", "Coffee > Machines", "Cleaning"), 0);
        app.answer("tree", tree);
    }

    /** The customer field's popup shows "No customer matches" for what was typed, then every customer after Down. */
    private Client uiThatFindsNoSuggestionFirst() {
        return new FakeApp() {
            private boolean listed;
            {
                answer("text 'Customer'", node("text", "Customer"));
                answer("popup label 'No customer matches'", label("No customer matches"));
            }

            @Override
            public io.github.kormcrete.swingui.driver.VerbOutcome press(String keys, io.github.kormcrete.swingui.driver.model.Locator target, Long timeout) {
                if ("Down".equals(keys) && !listed) {
                    listed = true;
                    forget("popup label 'No customer matches'");
                    answer("popup list", items("list", Arrays.asList("Ada Shop Ltd Berlin · Account", "Birch & Co Vienna · Account"), 0));
                }
                return super.press(keys, target, timeout);
            }
        }.client();
    }

    /** A new order whose cart grows when Add to cart is clicked: one more of each selected product in stock. */
    private static final class CartFake extends FakeApp {
        private final Map<String, Integer> cart = new java.util.LinkedHashMap<String, Integer>();
        private final List<String> selected = new ArrayList<String>();

        CartFake() {
            answer("text 'Customer'", node("text", "Customer"));
            List<List<String>> rows = Arrays.asList(
                    Arrays.asList("PAP-A4-500", "A4 paper, 500 sheets", "€4.90", "120"),
                    Arrays.asList("DSK-STAPLER", "Stapler", "€12.50", "4"),
                    Arrays.asList("DSK-LAMP", "Desk lamp", "€39.00", "Out of stock"));
            answer("table col=SKU", table(PRODUCT_COLUMNS, rows.size(), rows));
            answer("button 'Add to cart'", node("button", "Add to cart"));
        }

        void inCart(String product, int qty) {
            cart.put(product, qty);
            showCart();
        }

        @Override
        public io.github.kormcrete.swingui.driver.VerbOutcome click(io.github.kormcrete.swingui.driver.model.Locator target,
                String button, Integer count, List<String> modifiers, Long timeout) {
            if (target.row != null && "SKU".equals(target.column)) {
                if (modifiers == null) selected.clear();
                selected.add(String.valueOf(target.row.get("Product")));
            } else if ("Add to cart".equals(target.name)) {
                for (String p : selected) if (!"Desk lamp".equals(p)) cart.put(p, cart.containsKey(p) ? cart.get(p) + 1 : 1);
                showCart();
            }
            return super.click(target, button, count, modifiers, timeout);
        }

        private void showCart() {
            List<List<String>> lines = new ArrayList<List<String>>();
            for (Map.Entry<String, Integer> e : cart.entrySet()) lines.add(Arrays.asList(e.getKey(), String.valueOf(e.getValue()), "€1.00", "€1.00"));
            answer("table col=Line total", table(CART_COLUMNS, lines.size(), lines));
        }
    }

    /**
     * A new order's Customer field holding {@code text}: typing shows the suggestions for it (name or city holds it),
     * the X empties it, Down lists every customer, Escape closes the list.
     */
    private static final class CustomerFake extends FakeApp {
        private static final List<String> ALL = Arrays.asList("Ada Shop Ltd Berlin · Account", "Birch & Co Vienna · Account",
                "Café Lumen Amsterdam · Card only", "Fjord Supplies Hamburg · Card only");

        CustomerFake(String text) {
            field(text);
        }

        private void field(String text) {
            Node f = node("text", "Customer");
            f.value = text;
            answer("text 'Customer'", f);
        }

        private void suggest(List<String> items) {
            forget("popup list");
            forget("popup label 'No customer matches'");
            if (items == null) return;
            if (items.isEmpty()) answer("popup label 'No customer matches'", label("No customer matches"));
            else answer("popup list", items("list", items, 0));
        }

        @Override
        public io.github.kormcrete.swingui.driver.VerbOutcome type(io.github.kormcrete.swingui.driver.model.Locator target,
                String text, Boolean replace, Boolean commit, Long timeout) {
            field(text);
            List<String> fits = new ArrayList<String>();
            for (String c : ALL) if (c.toLowerCase().contains(text.toLowerCase())) fits.add(c);
            suggest(fits);
            return super.type(target, text, replace, commit, timeout);
        }

        @Override
        public io.github.kormcrete.swingui.driver.VerbOutcome click(io.github.kormcrete.swingui.driver.model.Locator target,
                String button, Integer count, List<String> modifiers, Long timeout) {
            if ("clear".equals(target.id)) {
                field("");
                suggest(null);
            }
            return super.click(target, button, count, modifiers, timeout);
        }

        @Override
        public io.github.kormcrete.swingui.driver.VerbOutcome press(String keys, io.github.kormcrete.swingui.driver.model.Locator target,
                Long timeout) {
            if ("Down".equals(keys)) suggest(ALL);
            if ("Escape".equals(keys) || "Enter".equals(keys)) suggest(null);
            return super.press(keys, target, timeout);
        }
    }
}
