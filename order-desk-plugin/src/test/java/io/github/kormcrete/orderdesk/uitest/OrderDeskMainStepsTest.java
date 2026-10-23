package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.model.Node;
import org.junit.jupiter.api.Test;

import static io.github.kormcrete.orderdesk.uitest.FakeApp.label;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.node;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.withState;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The main window's steps and checks on a stand-in app ({@link OrdersFake}, with a new order where one is needed): the
 * steps each takes, how a tab or a menu item is found by the tester's words, that no step chooses File ▸ Exit unless the
 * app will ask first, and how each fails, in words, with what was expected and what the app shows.
 */
class OrderDeskMainStepsTest {

    private static final String NEW_ORDER = "New order · Ada Shop Ltd";
    private static final String CLOSE_TAB = "{'action':'menu','path':['File','Close tab']}";
    private static final String EXIT = "{'action':'menu','path':['File','Exit']}";

    /** The main window with a new order for Ada selected, holding {@code cart} lines (none: an empty cart). */
    static OrdersFake onNewOrder(String... cart) {
        OrdersFake app = new OrdersFake();
        app.tabs(NEW_ORDER, "Orders", NEW_ORDER);
        app.answer("text 'Customer'", node("text", "Customer"));
        if (cart.length > 0) {
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<java.util.List<String>>();
            for (String line : cart) rows.add(java.util.Arrays.asList(line, "1", "€1.00", "€1.00"));
            app.answer("table col=Line total", FakeApp.table(java.util.Arrays.asList("Product", "Qty", "Price", "Line total"),
                    rows.size(), rows));
        }
        return app;
    }

    // ---- tabs

    @Test
    void selectTabClicksTheTabAWordOfItsTitleStarts() {
        OrdersFake app = onNewOrder();

        OrderDeskMainSteps.selectTab(app.client(), "ada");

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'role':'tab','name':'" + NEW_ORDER + "'}}]"));
    }

    @Test
    void selectTabOnTwoMatchesFailsNamingBoth() {
        OrdersFake app = new OrdersFake().tabs("Orders", "Orders", NEW_ORDER, "New order · Birch & Co");

        Throwable two = catchThrowable(() -> OrderDeskMainSteps.selectTab(app.client(), "new order"));

        assertThat(two).isInstanceOf(DriverException.class)
                .hasMessage("AMBIGUOUS: “new order” fits 2 tabs: " + NEW_ORDER + "; New order · Birch & Co. Give more of the name");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void selectTabOfTwoTabsWithOneTitleTakesTheNewest() {
        OrdersFake app = new OrdersFake().tabs("Orders", "Orders", NEW_ORDER, NEW_ORDER);

        OrderDeskMainSteps.selectTab(app.client(), "New order");

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'role':'tab','name':'" + NEW_ORDER + "','nth':1}}]"));
    }

    @Test
    void selectTabTakesTheItemsDeliveryOrInvoiceOfTheOrderShowing() {
        OrdersFake app = onOrder();

        OrderDeskMainSteps.selectTab(app.client(), "invoice");

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'scope':['tab:ORD-1639'],'role':'tab','name':'Invoice'}}]"));
    }

    @Test
    void selectTabFailsFastWhenItsScreenIsNotShowing() {
        FakeApp nothing = new FakeApp();
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskMainSteps.selectTab(nothing.client(), "Orders"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NO_MAIN);
        assertThat(nothing.steps()).isEmpty();
    }

    @Test
    void closeTabDiscardsANewOrderWithSomethingInItsCart() {
        OrdersFake full = onNewOrder("Stapler");
        OrdersFake empty = onNewOrder();
        OrdersFake order = onOrder();

        OrderDeskMainSteps.closeTab(full.client());
        OrderDeskMainSteps.closeTab(empty.client());
        OrderDeskMainSteps.closeTab(order.client());

        assertThat(full.steps()).isEqualTo(json("[" + CLOSE_TAB + ",{'action':'answer','button':'Discard'}]"));
        assertThat(empty.steps()).isEqualTo(json("[" + CLOSE_TAB + "]"));
        assertThat(order.steps()).isEqualTo(json("[" + CLOSE_TAB + "]"));
    }

    @Test
    void closeTabOnTheOrdersTabFailsSayingItNeverClosesAndChoosesNothing() {
        OrdersFake app = new OrdersFake();

        Throwable orders = catchThrowable(() -> OrderDeskMainSteps.closeTab(app.client()));

        assertThat(orders).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The Orders tab never closes"
                + " (File ▸ Close tab is disabled on it). Switch to the tab to close first with \"Switch to the tab <title>\"");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void keepTabAsksThenAnswersCancel() {
        OrdersFake app = onNewOrder("Stapler");

        OrderDeskMainSteps.keepTab(app.client());

        assertThat(app.steps()).isEqualTo(json("[" + CLOSE_TAB + ",{'action':'answer','button':'Cancel'}]"));
    }

    @Test
    void keepTabOnANewOrderThatWouldCloseAtOnceFailsAndChoosesNothing() {
        OrdersFake app = onNewOrder();

        Throwable empty = catchThrowable(() -> OrderDeskMainSteps.keepTab(app.client()));

        assertThat(empty).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Close tab asks only for a new"
                + " order with something in its cart; " + NEW_ORDER + " would close at once, so nothing was done");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void openTabsAreReadsTheTabsLeftToRightWithoutAnOrdersInnerTabs() {
        OrderDeskMainSteps.openTabsAre(onOrder().client(), "orders; ORD-1639");
    }

    @Test
    void openTabsAreFailsWithExpectedAndActual() {
        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.openTabsAre(onOrder().client(), "Orders"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The open tabs are Orders; ORD-1639, expected Orders");
    }

    @Test
    void selectedTabIsReadsTheTabAndAnOrdersInnerTab() {
        OrderDeskMainSteps.selectedTabIs(onOrder().client(), "ord-1639");
        OrderDeskMainSteps.selectedTabIs(onOrder().client(), "Items");
    }

    @Test
    void selectedTabIsFailsWithExpectedAndActual() {
        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.selectedTabIs(onOrder().client(), "Orders"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The selected tab is ORD-1639 (its Items), expected Orders");
    }

    // ---- menus

    @Test
    void menuItemIsOpensTheItemsMenuReadsItAndClosesIt() {
        OrdersFake app = new OrdersFake();
        app.answer("popup menuitem 'Close tab'", withState(node("menuitem", "Close tab"), "disabled"));

        OrderDeskMainSteps.menuItemIs(app.client(), "file > close tab", "disabled");

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'window':'main','role':'menu','name':'File'}},"
                + "{'action':'press','keys':'Escape'}]"));
    }

    @Test
    void menuItemIsFailsWithExpectedAndActualAndClosesTheMenu() {
        OrdersFake app = new OrdersFake();
        app.answer("popup menuitem 'Checkout…'", withState(node("menuitem", "Checkout…"), "disabled"));

        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.menuItemIs(app.client(), "checkout", "enabled"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("Order ▸ Checkout… is disabled, expected enabled");
        assertThat(app.steps().get(app.steps().size() - 1)).isEqualTo(json("{'action':'press','keys':'Escape'}"));
    }

    @Test
    void menuItemIsOnTwoMatchesFailsNamingThem() {
        OrdersFake app = new OrdersFake();

        Throwable two = catchThrowable(() -> OrderDeskMainSteps.menuItemIs(app.client(), "c", "enabled"));

        assertThat(two).isInstanceOf(DriverException.class).hasMessage("AMBIGUOUS: “c” fits 4 menu items: File > Close tab;"
                + " Order > Checkout…; the Orders list's row menu > Cancel orders…; the Orders list's row menu > Copy order"
                + " number. Give more of the name");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void menuItemIsReadsTheRowMenuOnTheSelectedOrdersWithTheCancelHint() {
        OrdersFake app = new OrdersFake().selecting(0, 2);
        app.answer("popup menuitem 'Cancel orders…'", withState(node("menuitem", "Cancel orders…"), "disabled"));
        app.answer("label 'Only placed orders can be cancelled'", label("Only placed orders can be cancelled"));

        OrderDeskMainSteps.menuItemIs(app.client(), "Cancel orders…", "disabled");

        assertThat(app.steps()).isEqualTo(json("[{'action':'click','target':{'scope':['tab:Orders'],'role':'table',"
                + "'row':{'Order':'ORD-1639'}},'button':'right','count':1},{'action':'press','keys':'Escape'}]"));
    }

    @Test
    void menuItemIsOfARowMenuFailsWithExpectedAndActual() {
        OrdersFake noHint = new OrdersFake().selecting(2);
        noHint.answer("popup menuitem 'Cancel orders…'", withState(node("menuitem", "Cancel orders…"), "disabled"));
        OrdersFake enabled = new OrdersFake().selecting(0);
        enabled.answer("popup menuitem 'Cancel orders…'", node("menuitem", "Cancel orders…"));

        Throwable hint = catchThrowable(() -> OrderDeskMainSteps.menuItemIs(noHint.client(), "cancel orders", "disabled"));
        Throwable on = catchThrowable(() -> OrderDeskMainSteps.menuItemIs(enabled.client(), "cancel orders", "disabled"));

        assertThat(hint).isInstanceOf(AssertionError.class).hasMessage("Cancel orders… is disabled for ORD-1636 but “Only"
                + " placed orders can be cancelled” is not shown beside the filter");
        assertThat(on).isInstanceOf(AssertionError.class).hasMessage("The row menu's Cancel orders… is enabled for"
                + " ORD-1639, expected disabled");
    }

    @Test
    void menuItemIsOfARowMenuWithNoOrderSelectedFailsAndOpensNothing() {
        OrdersFake app = new OrdersFake();

        Throwable none = catchThrowable(() -> OrderDeskMainSteps.menuItemIs(app.client(), "Open", "enabled"));

        assertThat(none).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: No order is selected: the row"
                + " menu acts on the selected orders. Select them first with \"Select the orders <orders>\"");
        assertThat(app.steps()).isEmpty();
    }

    // ---- the status bar

    @Test
    void statusLineIsFailsWithExpectedAndActual() {
        OrdersFake app = new OrdersFake();

        OrderDeskMainSteps.statusLineIs(app.client(), "Ready");
        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.statusLineIs(app.client(), "Order ORD-1639 cancelled"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The status line reads “Ready”, expected “Order"
                + " ORD-1639 cancelled”");
    }

    @Test
    void openOrdersCountIsTakesTheNumberAloneOrTheWords() {
        OrdersFake app = new OrdersFake();

        OrderDeskMainSteps.openOrdersCountIs(app.client(), "2");
        OrderDeskMainSteps.openOrdersCountIs(app.client(), "2 open orders");
        app.answer("label 'open order'", label("1 open order"));
        OrderDeskMainSteps.openOrdersCountIs(app.client(), "1");
    }

    @Test
    void openOrdersCountIsFailsWithExpectedAndActual() {
        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.openOrdersCountIs(new OrdersFake().client(), "3"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The open orders count reads “2 open orders”,"
                + " expected “3 open orders”");
    }

    @Test
    void statusLineIsFailsFastWhenItsScreenIsNotShowing() {
        FakeApp nothing = new FakeApp();
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskMainSteps.statusLineIs(nothing.client(), "Ready"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NO_MAIN);
    }

    // ---- exit and about

    @Test
    void cancelExitChoosesExitThenAnswersCancel() {
        OrdersFake app = onNewOrder("Stapler");

        OrderDeskMainSteps.cancelExit(app.client());

        assertThat(app.steps()).isEqualTo(json("[" + EXIT + ",{'action':'answer','button':'Cancel'}]"));
    }

    @Test
    void cancelExitNeverChoosesExitWhenTheAppWouldNotAsk() {
        // File ▸ Exit with no new order holding something in its cart ends the app at once
        for (OrdersFake app : new OrdersFake[] {new OrdersFake(), onNewOrder(), onOrder()}) {
            Throwable no = catchThrowable(() -> OrderDeskMainSteps.cancelExit(app.client()));
            Throwable question = catchThrowable(() -> OrderDeskMainSteps.exitQuestionIs(app.client(), "Discard the open new orders?"));

            assertThat(no).isInstanceOf(DriverException.class).hasMessageStartingWith("NOT_ACTIONABLE: File ▸ Exit asks"
                    + " \"Discard the open new orders?\" only while a new order has something in its cart; without one it"
                    + " ends the app, so the step chose nothing");
            assertThat(question).isInstanceOf(DriverException.class);
            assertThat(app.steps()).isEmpty();
        }
    }

    @Test
    void exitQuestionIsReadsTheQuestionThenAnswersCancel() {
        OrdersFake app = onNewOrder("Stapler");
        app.answer("dialog:Exit label", label("OptionPane.label", "Discard the open new orders?"));

        OrderDeskMainSteps.exitQuestionIs(app.client(), "Discard the open new orders?");

        assertThat(app.steps()).isEqualTo(json("[" + EXIT + ",{'action':'answer','button':'Cancel'}]"));
    }

    @Test
    void exitQuestionIsFailsWithExpectedAndActualAndStillAnswersCancel() {
        OrdersFake app = onNewOrder("Stapler");
        app.answer("dialog:Exit label", label("OptionPane.label", "Discard the open new orders?"));

        Throwable wrong = catchThrowable(() -> OrderDeskMainSteps.exitQuestionIs(app.client(), "Quit?"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The exit question reads “Discard the open new"
                + " orders?”, expected “Quit?”");
        assertThat(app.steps().get(1)).isEqualTo(json("{'action':'answer','button':'Cancel'}"));
    }

    // ---- the theme

    @Test
    void switchThemeChoosesItInViewTheme() {
        OrdersFake app = new OrdersFake();
        app.menu("View > Theme", "Light", "Dark", "Lagoon");

        OrderDeskMainSteps.switchTheme(app.client(), "dark");
        OrderDeskMainSteps.switchTheme(app.client(), "Light");

        assertThat(app.steps()).isEqualTo(json("[{'action':'menu','path':['View','Theme','Dark']},"
                + "{'action':'menu','path':['View','Theme','Light']}]"));
    }

    @Test
    void switchThemeThatFitsNoneNamesTheThemesAndChoosesNothing() {
        OrdersFake app = new OrdersFake();
        app.menu("View > Theme", "Light", "Dark", "Lagoon");

        Throwable none = catchThrowable(() -> OrderDeskMainSteps.switchTheme(app.client(), "Solarized"));

        assertThat(none).isInstanceOf(DriverException.class)
                .hasMessage("NOT_FOUND: No theme fits “Solarized”. View ▸ Theme offers: Light; Dark; Lagoon");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void aboutShowsReadsTheTextAsWholeWordsThenAnswersOk() {
        OrdersFake app = withAbout();

        OrderDeskMainSteps.aboutShows(app.client(), "A small, fictional order-entry app");
        OrderDeskMainSteps.aboutShows(app.client(), "all customers, products and orders are invented.");

        assertThat(app.steps().get(0)).isEqualTo(json("{'action':'menu','path':['Help','About Order Desk']}"));
        assertThat(app.steps().get(1)).isEqualTo(json("{'action':'answer','button':'OK'}"));
    }

    @Test
    void aboutShowsFailsWithExpectedAndActualAndStillAnswersOk() {
        OrdersFake app = withAbout();

        Throwable part = catchThrowable(() -> OrderDeskMainSteps.aboutShows(app.client(), "fictional app"));
        Throwable word = catchThrowable(() -> OrderDeskMainSteps.aboutShows(app.client(), "inven"));

        assertThat(part).isInstanceOf(AssertionError.class).hasMessage("About Order Desk reads “Order Desk A small, fictional"
                + " order-entry app: the demo. All customers, products and orders are invented.”, expected it to show “fictional app”");
        assertThat(word).isInstanceOf(AssertionError.class);
        assertThat(app.steps().get(1)).isEqualTo(json("{'action':'answer','button':'OK'}"));
    }

    // ---- stand-ins

    /** The main window with order ORD-1639's own tab selected, on its Items. */
    static OrdersFake onOrder() {
        OrdersFake app = new OrdersFake().tabs("ORD-1639", "Orders", "ORD-1639");
        // the tabs read in tree order: the order's inner tabs follow it
        app.answer("tab", node("tab", "Orders"), node("tab", "ORD-1639"), node("tab", "Items"), node("tab", "Delivery"),
                node("tab", "Invoice"));
        app.answer("tab in tab:ORD-1639", node("tab", "Items"), node("tab", "Delivery"), node("tab", "Invoice"));
        Node items = node("tab", "Items");
        items.value = "Items";
        app.answer("tab 'Items' in tab:ORD-1639", items);
        return app;
    }

    private static OrdersFake withAbout() {
        OrdersFake app = new OrdersFake();
        app.answer("dialog:About Order Desk label", label("OptionPane.label", "Order Desk"),
                label("OptionPane.label", "A small, fictional order-entry app: the demo."),
                label("OptionPane.label", "All customers,"), label("OptionPane.label", "products and orders are invented."));
        return app;
    }
}
