package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import org.junit.jupiter.api.Test;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.kormcrete.orderdesk.uitest.FakeApp.label;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.modal;
import static io.github.kormcrete.orderdesk.uitest.FakeApp.node;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskStepsTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The Orders list's steps and checks on a stand-in app ({@link OrdersFake}): the steps each takes, how it finds an order
 * by the tester's words (its number, its digits, its customer), and how it fails, in words, when the main window or the
 * Orders tab is not showing, an order cannot be cancelled, or what the list shows is not what was expected.
 */
class OrderDeskOrdersStepsTest {

    private static final String O = "'scope':['tab:Orders']";
    private static final String TO_ORDERS = "{'action':'click','target':{'role':'tab','name':'Orders'}}";

    private static String row(String number) {
        return "{" + O + ",'role':'table','row':{'Order':'" + number + "'}}";
    }

    // ---- open

    @Test
    void openOrderGoesToTheOrdersTabThenDoubleClicksTheOrdersRow() {
        OrdersFake app = new OrdersFake();
        app.answer("tab 'ORD-1639'", node("tab", "ORD-1639"));

        OrderDeskSteps.openOrder(app.client(), "ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + ",{'action':'click','target':" + row("ORD-1639") + ",'count':2}]"));
    }

    @Test
    void partialOrderNameResolvesOrNamesChoices() {
        // the digits of a number, a customer with one order listed (any case, the start of the name)
        OrdersFake app = new OrdersFake();
        app.answer("tab 'ORD-1638'", node("tab", "ORD-1638"));
        app.answer("tab 'ORD-1634'", node("tab", "ORD-1634"));

        OrderDeskOrdersSteps.openOrder(app.client(), "1638");
        OrderDeskOrdersSteps.openOrder(app.client(), "birch");

        assertThat(app.steps().get(1).get("target").get("row").get("Order").asText()).isEqualTo("ORD-1638");
        assertThat(app.steps().get(3).get("target").get("row").get("Order").asText()).isEqualTo("ORD-1634");
    }

    @Test
    void openOrderOnTwoMatchesFailsNamingTheCustomersOrders() {
        // Ada Shop Ltd has two orders listed: "Ada" names no single one
        OrdersFake app = new OrdersFake();

        Throwable two = catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ada"));

        assertThat(two).isInstanceOf(DriverException.class)
                .hasMessage("AMBIGUOUS: Ada Shop Ltd has 2 orders listed: ORD-1639; ORD-1636. Give the order number");
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "]"));
    }

    @Test
    void openOrderThatFitsNoneNamesTheOrdersListedAndWhatFiltersThem() {
        OrdersFake app = new OrdersFake().listing("", "ORD-1639", "ORD-1636").searchHolds("ada").statusFilterOn("Placed");

        Throwable none = catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1638"));

        assertThat(none).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: No order fits “ORD-1638”. The Orders"
                + " list shows: ORD-1639; ORD-1636. Search holds “ada”: \"Clear the order search\" lists every order. The"
                + " list shows Placed orders only: \"Filter the orders by status All\" lists every order");
    }

    @Test
    void anOrdersStepThatMissesSelectsTheTabThatWasSelectedAgain() {
        OrdersFake app = new OrdersFake().tabs("New order · Ada Shop Ltd", "Orders", "New order · Ada Shop Ltd");

        catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "zz"));

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':{'role':'tab','name':'New order · Ada Shop Ltd'}}]"));
    }

    @Test
    void openOrderFailsWhenItsTabDoesNotOpen() {
        OrdersFake app = new OrdersFake();

        Throwable shut = catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1639"));

        assertThat(shut).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The tab of ORD-1639 did not open");
    }

    @Test
    void openOrderThatFailsClosesTheTabsItOpenedAndGivesBackTheSelectionAndTheTab() {
        // of two orders only ORD-1639's tab opens: it is closed again, ORD-1634 is selected again as it was, and the
        // new order shows again
        OrdersFake app = new OrdersFake().tabs("New order · Ada Shop Ltd", "Orders", "New order · Ada Shop Ltd")
                .selecting(3).opening("ORD-1639");

        Throwable shut = catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1639; ORD-1638"));

        assertThat(shut).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The tab of ORD-1638 did not open");
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'click','target':" + row("ORD-1638") + ",'modifiers':['Mod']}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Open']}"
                + ",{'action':'click','target':{'role':'tab','name':'ORD-1639'}}"
                + ",{'action':'menu','path':['File','Close tab']}"
                + ",{'action':'click','target':" + row("ORD-1634") + "}"
                + ",{'action':'click','target':{'role':'tab','name':'New order · Ada Shop Ltd'}}]"));
    }

    @Test
    void openOrderThatFailsWithNothingSelectedBeforeUnselectsItsRow() {
        OrdersFake app = new OrdersFake();

        catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1639"));

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + ",'count':2}"
                + ",{'action':'click','target':" + row("ORD-1639") + ",'modifiers':['Mod']}]"));
    }

    @Test
    void openSeveralOrdersSelectsThemThenOpensThemFromTheRowMenu() {
        OrdersFake app = new OrdersFake();
        app.answer("tab 'ORD-1639'", node("tab", "ORD-1639"));
        app.answer("tab 'ORD-1638'", node("tab", "ORD-1638"));

        OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1639; 1638");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'click','target':" + row("ORD-1638") + ",'modifiers':['Mod']}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Open']}]"));
    }

    // ---- search, filter, sort

    @Test
    void searchOrdersTypesIntoTheOrdersTabsSearch() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.searchOrders(app.client(), "ada");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'type','target':{" + O + ",'role':'text','name':'Search'},'text':'ada'}]"));
    }

    @Test
    void clearOrderSearchClicksItsXOnlyWhenItHoldsText() {
        OrdersFake empty = new OrdersFake();
        OrdersFake full = new OrdersFake().searchHolds("ada");

        OrderDeskOrdersSteps.clearOrderSearch(empty.client());
        OrderDeskOrdersSteps.clearOrderSearch(full.client());

        assertThat(empty.steps()).isEqualTo(json("[" + TO_ORDERS + "]"));
        assertThat(full.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':{'scope':['tab:Orders','text:Search'],'id':'clear'}}]"));
    }

    @Test
    void filterOrdersTakesTheTestersWordForAStatus() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.filterOrders(app.client(), "cancel");
        OrderDeskOrdersSteps.filterOrders(app.client(), "placed");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'select','target':{" + O + ",'role':'combo','name':'Status'},'items':['Cancelled']}," + TO_ORDERS
                + ",{'action':'select','target':{" + O + ",'role':'combo','name':'Status'},'items':['Placed']}]"));
    }

    @Test
    void filterOrdersWithAStatusThatFitsNoneNamesTheStatusesAndClicksNothing() {
        OrdersFake app = new OrdersFake();

        Throwable none = catchThrowable(() -> OrderDeskOrdersSteps.filterOrders(app.client(), "open"));

        assertThat(none).isInstanceOf(DriverException.class)
                .hasMessage("NOT_FOUND: No status fits “open”. The Status filter offers: All; Placed; Shipped; Cancelled");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void sortOrdersClicksTheHeaderUntilTheOrdersReadInTheAskedDirection() {
        // a first click on Total sorts it ascending; descending takes a second
        OrdersFake app = new OrdersFake().sortable();

        Map<String, Object> left = OrderDeskOrdersSteps.sortOrders(app.client(), "total", "descending");

        String header = "{'action':'click','target':{" + O + ",'role':'table','column':'Total'}}";
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "," + header + "," + header + "]"));
        assertThat(left).containsEntry("column", "Total").containsEntry("direction", "descending");
        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1634", "ORD-1638", "ORD-1639", "ORD-1636");
    }

    @Test
    void sortOrdersTakesTheTestersWordsForColumnAndDirection() {
        OrdersFake app = new OrdersFake().sortable();

        Map<String, Object> oldest = OrderDeskOrdersSteps.sortOrders(app.client(), "date", "oldest first");

        assertThat(app.steps()).hasSize(2);
        assertThat(oldest).containsEntry("column", "Date").containsEntry("direction", "ascending");
        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1634", "ORD-1636", "ORD-1638", "ORD-1639");
    }

    @Test
    void sortOrdersBackToTheStartOrderClicksTwice() {
        // the list starts on Order, descending: the first click reverses it, the second brings it back
        OrdersFake app = new OrdersFake().sortable();

        Map<String, Object> left = OrderDeskOrdersSteps.sortOrders(app.client(), "Order", "newest first");

        assertThat(app.steps()).hasSize(3);
        assertThat(left).containsEntry("direction", "descending");
        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1639", "ORD-1638", "ORD-1636", "ORD-1634");
    }

    @Test
    void sortOrdersReadsTheOrderNumbersAndNamesAsTheAppsSorterSortsThem() {
        // the app's table sorter compares a String column (Order, Customer) with Collator.getInstance(), in the app's
        // locale: ORD-1000 sorts before ORD-999, and in Swedish Ärlig AB after Zeta AB
        Locale was = Locale.getDefault();
        Locale.setDefault(new Locale("sv", "SE"));
        try {
            assertThat(OrderDeskOrdersSteps.directionOf(sortedByTheApp("ORD-999", "ORD-1640", "ORD-1000"),
                    OrderDeskOrdersSteps.comparator("Order"))).isEqualTo(1);
            assertThat(OrderDeskOrdersSteps.directionOf(sortedByTheApp("Zeta AB", "Ärlig AB", "Ada Shop Ltd"),
                    OrderDeskOrdersSteps.comparator("Customer"))).isEqualTo(1);
        } finally {
            Locale.setDefault(was);
        }
    }

    /** {@code values} in one String column, as the app's TableRowSorter shows them sorted ascending. */
    private static List<String> sortedByTheApp(String... values) {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Column"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };
        for (String v : values) model.addRow(new Object[] {v});
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        List<String> shown = new ArrayList<String>();
        for (int i = 0; i < sorter.getViewRowCount(); i++) shown.add((String) model.getValueAt(sorter.convertRowIndexToModel(i), 0));
        return shown;
    }

    @Test
    void sortOrdersByAStatusFollowsTheFiltersOrder() {
        OrdersFake app = new OrdersFake().sortable();

        OrderDeskOrdersSteps.sortOrders(app.client(), "status", "descending");

        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1634", "ORD-1636", "ORD-1639", "ORD-1638");
    }

    @Test
    void sortOrdersWhereEveryValueIsTheSameSaysEither() {
        OrdersFake app = new OrdersFake().listing("", "ORD-1639", "ORD-1636").sortable();

        Map<String, Object> left = OrderDeskOrdersSteps.sortOrders(app.client(), "Customer", "descending");

        assertThat(app.steps()).hasSize(2);
        assertThat(left).containsEntry("column", "Customer").containsEntry("direction", "either");
    }

    @Test
    void sortOrdersWithADirectionThatFitsNoneFailsBeforeClicking() {
        OrdersFake app = new OrdersFake().sortable();

        Throwable bad = catchThrowable(() -> OrderDeskOrdersSteps.sortOrders(app.client(), "Total", "sideways"));

        assertThat(bad).isInstanceOf(DriverException.class).hasMessage("BAD_INPUT: No sort direction fits “sideways”."
                + " Give ascending or descending (newest first, oldest first)");
        assertThat(app.steps()).isEmpty();
    }

    @Test
    void sortOrdersFailsWhenTheOrdersNeverReadSorted() {
        // a header click that sorts nothing (the stand-in is not sortable): three clicks, then the reason
        OrdersFake app = new OrdersFake().listing("", "ORD-1638", "ORD-1639", "ORD-1634");

        Throwable unsorted = catchThrowable(() -> OrderDeskOrdersSteps.sortOrders(app.client(), "Total", "ascending"));

        assertThat(unsorted).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The orders do not read sorted"
                + " by Total, ascending after 3 clicks on its header: Total reads €181.44; €95.52; €542.58");
    }

    @Test
    void sortOrdersThatFailsBringsBackTheOrderAndTheTabItFound() {
        // the stand-in sorts Total as text, so it never reads sorted by amount: the step fails, clicks the Order header
        // (the list read newest first by Order when it began) until the orders read as they did, and shows the new
        // order again
        OrdersFake app = new OrdersFake().tabs("New order · Ada Shop Ltd", "Orders", "New order · Ada Shop Ltd")
                .sortableAsText();

        Throwable unsorted = catchThrowable(() -> OrderDeskOrdersSteps.sortOrders(app.client(), "Total", "ascending"));

        assertThat(unsorted).isInstanceOf(DriverException.class).hasMessageStartingWith("NOT_ACTIONABLE: The orders do not"
                + " read sorted by Total, ascending after 3 clicks on its header");
        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1639", "ORD-1638", "ORD-1636", "ORD-1634");
        String header = "{'action':'click','target':{" + O + ",'role':'table','column':'%s'}}";
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "," + String.format(header, "Total") + ","
                + String.format(header, "Total") + "," + String.format(header, "Total") + ","
                + String.format(header, "Order") + "," + String.format(header, "Order")
                + ",{'action':'click','target':{'role':'tab','name':'New order · Ada Shop Ltd'}}]"));
    }

    // ---- select, cancel, keep, copy    // ---- select, cancel, keep, copy

    @Test
    void selectOrdersClicksTheFirstThenTheOthersWithModHeld() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.selectOrders(app.client(), "ORD-1639; 1636; ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'click','target':" + row("ORD-1636") + ",'modifiers':['Mod']}]"));
    }

    @Test
    void cancelOrdersSelectsThemOpensCancelOrdersAndConfirms() {
        OrdersFake one = new OrdersFake();
        OrdersFake two = new OrdersFake();

        OrderDeskOrdersSteps.cancelOrders(one.client(), "ORD-1639");
        OrderDeskOrdersSteps.cancelOrders(two.client(), "ORD-1639; ORD-1638");

        assertThat(one.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Cancel orders…']}"
                + ",{'action':'answer','button':'Cancel order'}]"));
        assertThat(two.steps().get(4)).isEqualTo(json("{'action':'answer','button':'Cancel orders'}"));
    }

    @Test
    void cancelOrdersOfAnOrderThatIsNotPlacedFailsWithTheAppsReasonBeforeClickingTheList() {
        OrdersFake app = new OrdersFake();

        Throwable shipped = catchThrowable(() -> OrderDeskOrdersSteps.cancelOrders(app.client(), "ORD-1639; ORD-1636; 1634"));

        assertThat(shipped).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: Only placed orders can be"
                + " cancelled: ORD-1636 is shipped; ORD-1634 is cancelled");
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "]"));
    }

    @Test
    void cancelOrdersOnTwoMatchesFailsNamingThem() {
        OrdersFake app = new OrdersFake();

        Throwable two = catchThrowable(() -> OrderDeskOrdersSteps.cancelOrders(app.client(), "Ada Shop Ltd"));

        assertThat(two).isInstanceOf(DriverException.class)
                .hasMessage("AMBIGUOUS: Ada Shop Ltd has 2 orders listed: ORD-1639; ORD-1636. Give the order number");
    }

    @Test
    void keepOrdersAsksThenAnswersKeep() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.keepOrders(app.client(), "ORD-1638");

        assertThat(app.steps().get(2)).isEqualTo(json("{'action':'contextMenu','target':" + row("ORD-1638")
                + ",'path':['Cancel orders…']}"));
        assertThat(app.steps().get(3)).isEqualTo(json("{'action':'answer','button':'Keep'}"));
    }

    @Test
    void copyOrderNumbersSelectsThemThenCopies() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.copyOrderNumbers(app.client(), "ORD-1639; ORD-1636");

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'click','target':" + row("ORD-1636") + ",'modifiers':['Mod']}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Copy order number']}]"));
    }

    // ---- the screen it needs

    @Test
    void anOrdersCheckFailsFastWhenAnotherTabIsSelected() {
        FakeApp app = new FakeApp();
        app.answer("tab 'Orders'", node("tab", "Orders"));
        long start = System.currentTimeMillis();

        Throwable elsewhere = catchThrowable(() -> OrderDeskOrdersSteps.ordersListedAre(app.client(), "ORD-1639"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(elsewhere).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NOT_ON_ORDERS);
    }

    @Test
    void searchOrdersFailsFastWhenItsScreenIsNotShowing() {
        FakeApp nothing = new FakeApp();
        long start = System.currentTimeMillis();

        Throwable gone = catchThrowable(() -> OrderDeskOrdersSteps.searchOrders(nothing.client(), "ada"));

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
        assertThat(gone).isInstanceOf(DriverException.class).hasMessage("NOT_FOUND: " + OrderDeskOrdersSteps.NO_MAIN);
        assertThat(nothing.steps()).isEmpty();
    }

    @Test
    void anOrdersActFailsFastWhileADialogIsOpen() {
        OrdersFake app = new OrdersFake();
        app.windows(modal("Checkout — Ada Shop Ltd", "Address"));

        Throwable blocked = catchThrowable(() -> OrderDeskOrdersSteps.cancelOrders(app.client(), "ORD-1639"));

        assertThat(blocked).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: A dialog is open over the main"
                + " window (Checkout — Ada Shop Ltd: Address): answer or close it first");
        assertThat(app.steps()).isEmpty();
    }

    // ---- checks

    @Test
    void orderIsReadsOneValueAndAStatusInAnyCase() {
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.orderIs(app.client(), "ORD-1639", "Total", "€95.52");
        OrderDeskOrdersSteps.orderIs(app.client(), "1636", "status", "shipped");
        OrderDeskOrdersSteps.orderIs(app.client(), "birch", "Customer", "Birch & Co");
    }

    @Test
    void orderIsReadsTheOrderCellThatNamesTheRow() {
        // the palette's Check menu offers a check of the Order cell too ("The Order of order ORD-1639 is ORD-1639")
        OrdersFake app = new OrdersFake();

        OrderDeskOrdersSteps.orderIs(app.client(), "ORD-1639", "Order", "ORD-1639");

        assertThat(app.steps()).isEqualTo(json("[{'action':'expect','target':{" + O + ",'role':'table','row':{'Order':"
                + "'ORD-1639'},'column':'Order'},'text':'ORD-1639','timeout':1000}]"));
    }

    @Test
    void orderIsFailsWithExpectedAndActual() {
        OrdersFake app = new OrdersFake();

        Throwable total = catchThrowable(() -> OrderDeskOrdersSteps.orderIs(app.client(), "ORD-1639", "Total", "€9.99"));
        Throwable status = catchThrowable(() -> OrderDeskOrdersSteps.orderIs(app.client(), "ORD-1639", "Status", "cancelled"));

        assertThat(total).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's Total reads “€95.52”, expected “€9.99”");
        assertThat(status).isInstanceOf(AssertionError.class).hasMessage("ORD-1639's Status reads “Placed”, expected “cancelled”");
    }

    @Test
    void ordersListedArePassesInOrderAndForNone() {
        OrderDeskOrdersSteps.ordersListedAre(new OrdersFake().client(), "ORD-1639; 1638; ORD-1636; ORD-1634");
        OrderDeskOrdersSteps.ordersListedAre(new OrdersFake().listing("No orders match ‘zz’").client(), "none");
    }

    @Test
    void ordersListedAreFailsWithExpectedAndActual() {
        Throwable order = catchThrowable(() -> OrderDeskOrdersSteps.ordersListedAre(new OrdersFake().client(),
                "ORD-1638; ORD-1639; ORD-1636; ORD-1634"));
        Throwable none = catchThrowable(() -> OrderDeskOrdersSteps.ordersListedAre(
                new OrdersFake().listing("", "ORD-1639").client(), "none"));

        assertThat(order).isInstanceOf(AssertionError.class).hasMessage("The Orders list shows 4 orders: ORD-1639;"
                + " ORD-1638; ORD-1636; ORD-1634, expected ORD-1638; ORD-1639; ORD-1636; ORD-1634");
        assertThat(none).isInstanceOf(AssertionError.class).hasMessage("The Orders list shows 1 order: ORD-1639, expected none");
    }

    @Test
    void ordersEmptyMessageIsReadsTheTextThatReplacesTheOrders() {
        OrdersFake empty = new OrdersFake().listing("No cancelled orders");

        OrderDeskOrdersSteps.ordersEmptyMessageIs(empty.client(), "No cancelled orders");
        OrderDeskOrdersSteps.ordersEmptyMessageIs(new OrdersFake().client(), "none");
    }

    @Test
    void ordersEmptyMessageIsFailsWithExpectedAndActual() {
        Throwable wrong = catchThrowable(() -> OrderDeskOrdersSteps.ordersEmptyMessageIs(
                new OrdersFake().listing("No orders match ‘zz’").client(), "No orders"));
        Throwable listed = catchThrowable(() -> OrderDeskOrdersSteps.ordersEmptyMessageIs(new OrdersFake().client(),
                "No orders"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("The Orders list reads “No orders match ‘zz’”,"
                + " expected “No orders”");
        assertThat(listed).isInstanceOf(AssertionError.class).hasMessage("The Orders list is not shown, expected “No orders”");
    }

    @Test
    void selectedOrdersAreComparesInAnyOrder() {
        OrdersFake app = new OrdersFake().selecting(0, 2);

        OrderDeskOrdersSteps.selectedOrdersAre(app.client(), "1636; ORD-1639");
        OrderDeskOrdersSteps.selectedOrdersAre(new OrdersFake().client(), "none");
    }

    @Test
    void selectedOrdersAreFailsWithExpectedAndActual() {
        OrdersFake app = new OrdersFake().selecting(0, 2);

        Throwable wrong = catchThrowable(() -> OrderDeskOrdersSteps.selectedOrdersAre(app.client(), "ORD-1639"));

        assertThat(wrong).isInstanceOf(AssertionError.class)
                .hasMessage("The selected orders are ORD-1636; ORD-1639, expected ORD-1639");
    }

    @Test
    void theStatusLineAfterCopyingIsReadByItsCodeName() {
        OrdersFake app = new OrdersFake();
        app.answer("element #statusMessage", label("statusMessage", "Copied ORD-1639, ORD-1636"));

        OrderDeskMainSteps.statusLineIs(app.client(), "Copied ORD-1639, ORD-1636");
    }
}
