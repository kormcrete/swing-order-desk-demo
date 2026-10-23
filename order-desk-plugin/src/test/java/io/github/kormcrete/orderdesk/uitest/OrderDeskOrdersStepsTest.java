package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.model.Node;
import org.junit.jupiter.api.Test;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;
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
        // of two orders only ORD-1639's tab opens: it is closed again (the app then shows the tab now last, not
        // Orders, so Orders is clicked before its rows are), ORD-1634 is selected again as it was, and the new order
        // shows again
        OrdersFake app = new OrdersFake().tabs("New order · Ada Shop Ltd", "Orders", "New order · Ada Shop Ltd")
                .selecting(3).opening("ORD-1639");

        Throwable shut = catchThrowable(() -> OrderDeskOrdersSteps.openOrder(app.client(), "ORD-1639; ORD-1638"));

        assertThat(shut).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The tab of ORD-1638 did not open");
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'click','target':" + row("ORD-1638") + ",'modifiers':['Mod']}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Open']}"
                + ",{'action':'click','target':{'role':'tab','name':'ORD-1639'}}"
                + ",{'action':'menu','path':['File','Close tab']}," + TO_ORDERS
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

    // ---- what the list shows, as an act's outcome

    @Test
    void showOrdersGoesToTheOrdersTabAndSaysWhatItLists() {
        OrdersFake app = new OrdersFake().tabs("ORD-1639", "Orders", "ORD-1639").listing("", "ORD-1639", "ORD-1634");

        Map<String, Object> shown = OrderDeskOrdersSteps.showOrders(app.client());

        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "]"));
        assertThat(shown).containsEntry("count", 2);
        assertThat(shown.get("orders")).isEqualTo(orders(
                order("ORD-1639", "9 Oct 2026", "Ada Shop Ltd", "55", "€95.52", "Placed"),
                order("ORD-1634", "1 Oct 2026", "Birch & Co", "3", "€542.58", "Cancelled")));
        // keyed by number, in the list's order
        assertThat(((Map<?, ?>) shown.get("orders")).keySet()).containsExactly("ORD-1639", "ORD-1634");
        assertThat(shown).doesNotContainKey("empty");
    }

    @Test
    void showOrdersReadsEveryOrderPastTheFirstRowsOfARead() {
        OrdersFake app = new OrdersFake().readsFirst(2);

        Map<String, Object> shown = OrderDeskOrdersSteps.showOrders(app.client());

        assertThat(shown).containsEntry("count", 4);
        assertThat(((Map<?, ?>) shown.get("orders")).get("ORD-1634"))
                .isEqualTo(order("ORD-1634", "1 Oct 2026", "Birch & Co", "3", "€542.58", "Cancelled").getValue());
    }

    @Test
    void showOrdersOfAnEmptyListSaysTheTextThatShowsInstead() {
        OrdersFake app = new OrdersFake().listing("No orders match ‘zz’");

        Map<String, Object> shown = OrderDeskOrdersSteps.showOrders(app.client());

        assertThat(shown).containsEntry("count", 0).containsEntry("orders", Collections.emptyMap())
                .containsEntry("empty", "No orders match ‘zz’");
    }

    @Test
    void searchFilterAndClearSayWhatTheListShowsAfterThem() {
        OrdersFake app = new OrdersFake().listing("", "ORD-1639", "ORD-1636");

        Map<String, Object> searched = OrderDeskOrdersSteps.searchOrders(app.client(), "ada");
        Map<String, Object> filtered = OrderDeskOrdersSteps.filterOrders(app.client(), "placed");
        Map<String, Object> cleared = OrderDeskOrdersSteps.clearOrderSearch(app.client());

        for (Map<String, Object> listed : Arrays.asList(searched, filtered, cleared)) {
            assertThat(listed).containsEntry("count", 2);
            assertThat(((Map<?, ?>) listed.get("orders")).keySet()).containsExactly("ORD-1639", "ORD-1636");
            assertThat(((Map<?, ?>) listed.get("orders")).get("ORD-1636"))
                    .isEqualTo(order("ORD-1636", "3 Oct 2026", "Ada Shop Ltd", "12", "€40.56", "Shipped").getValue());
        }
    }

    @Test
    void sortOrdersSaysTheOrdersInTheirNewOrder() {
        OrdersFake app = new OrdersFake().sortable();

        Map<String, Object> left = OrderDeskOrdersSteps.sortOrders(app.client(), "Total", "ascending");

        assertThat(left).containsEntry("column", "Total").containsEntry("direction", "ascending").containsEntry("count", 4);
        assertThat(((Map<?, ?>) left.get("orders")).keySet()).containsExactly("ORD-1636", "ORD-1639", "ORD-1638", "ORD-1634");
    }

    @Test
    void aListOutcomeOffersARecordingNoValueByItsPlaceInTheList() {
        // the recorder takes every value a domain step returns, at its path, as a source a later step may be linked
        // to (Outcomes.newValues): a path by position (".step.orders[3].Customer") would name another order once the
        // list changes; keyed by number, a value names its own order, and the number itself is no value
        OrdersFake app = new OrdersFake();
        Map<String, Object> outcome = new java.util.LinkedHashMap<String, Object>();
        outcome.put("step", OrderDeskOrdersSteps.showOrders(app.client()));

        Map<String, String> sources = io.github.kormcrete.swingui.driver.outcome.Outcomes.newValues(
                io.github.kormcrete.swingui.driver.outcome.UiReading.builder().build(), outcome);

        assertThat(sources).isNotEmpty();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            assertThat(e.getValue()).as(e.getKey()).doesNotMatch(".*\\[\\d+\\].*");
            assertThat(e.getKey()).as(e.getValue()).doesNotStartWith("ORD-");
        }
        assertThat(sources).containsEntry("Birch & Co", ".step.orders.ORD-1634.Customer");
    }

    /** One order of a list outcome: its number, and its other five values by column. */
    private static Map.Entry<String, Map<String, String>> order(String... cells) {
        Map<String, String> o = new java.util.LinkedHashMap<String, String>();
        for (int i = 1; i < cells.length; i++) o.put(OrdersFake.COLUMNS.get(i), cells[i]);
        return new java.util.AbstractMap.SimpleEntry<String, Map<String, String>>(cells[0], o);
    }

    @SafeVarargs
    private static Map<String, Map<String, String>> orders(Map.Entry<String, Map<String, String>>... entries) {
        Map<String, Map<String, String>> out = new java.util.LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> e : entries) out.put(e.getKey(), e.getValue());
        return out;
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

    @Test
    void sortOrdersThatFailsReadsEveryValueItNeedsToPutTheListBack() {
        // the list began sorted by Customer (Ada, Birch, Café) and a read gives its first row only: the rest are read
        // one by one, so the Customer of the others is read too before the list is put back
        OrdersFake app = new OrdersFake().listing("", "ORD-1639", "ORD-1634", "ORD-1638").sortableAsText().readsFirst(1);

        Throwable unsorted = catchThrowable(() -> OrderDeskOrdersSteps.sortOrders(app.client(), "Total", "ascending"));

        assertThat(unsorted).isInstanceOf(DriverException.class).hasMessage("NOT_ACTIONABLE: The orders do not read"
                + " sorted by Total, ascending after 3 clicks on its header: Total reads €181.44; €542.58; €95.52");
        assertThat(OrderDeskOrdersSteps.listedOrders(app.client())).containsExactly("ORD-1639", "ORD-1634", "ORD-1638");
    }

    // ---- select, cancel, keep, copy

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
    void cancelQuestionIsAsksReadsTheQuestionThenKeeps() {
        // the question's whole text, or one of its lines: "Cancel order ORD-1639?" / "Stock is restored …"
        OrdersFake app = new OrdersFake();
        cancelQuestion(app, "Cancel order ORD-1639?");

        OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1639", "Cancel order ORD-1639?");
        OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "1639", STOCK_RESTORED);
        OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1639", "Cancel order ORD-1639?  " + STOCK_RESTORED);

        // nothing was selected before: each run unselects the row it clicked again
        String once = TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Cancel orders…']}"
                + ",{'action':'answer','button':'Keep'}"
                + ",{'action':'click','target':" + row("ORD-1639") + ",'modifiers':['Mod']}";
        assertThat(app.steps()).isEqualTo(json("[" + once + "," + once + "," + once + "]"));
    }

    @Test
    void cancelQuestionIsLeavesTheTabAndTheSelectionAsItFoundThemWhenItPassesAndWhenItFails() {
        // started on a new order's tab with ORD-1634 selected on the Orders list
        String newOrder = "New order · Ada Shop Ltd";
        String expected = "[" + TO_ORDERS
                + ",{'action':'click','target':" + row("ORD-1639") + "}"
                + ",{'action':'contextMenu','target':" + row("ORD-1639") + ",'path':['Cancel orders…']}"
                + ",{'action':'answer','button':'Keep'}"
                + ",{'action':'click','target':" + row("ORD-1634") + "}"
                + ",{'action':'click','target':{'role':'tab','name':'" + newOrder + "'}}]";
        OrdersFake pass = new OrdersFake().tabs(newOrder, "Orders", newOrder).selecting(3);
        OrdersFake fail = new OrdersFake().tabs(newOrder, "Orders", newOrder).selecting(3);
        cancelQuestion(pass, "Cancel order ORD-1639?");
        cancelQuestion(fail, "Cancel order ORD-1639?");

        OrderDeskOrdersSteps.cancelQuestionIs(pass.client(), "ORD-1639", "Cancel order ORD-1639?");
        Throwable wrong = catchThrowable(() -> OrderDeskOrdersSteps.cancelQuestionIs(fail.client(), "ORD-1639", "x"));

        assertThat(pass.steps()).isEqualTo(json(expected));
        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessageStartingWith("Cancelling ORD-1639 asks");
        assertThat(fail.steps()).isEqualTo(json(expected));
    }

    @Test
    void cancelQuestionIsSelectsAgainTheOneOfTwoTabsWithOneTitleThatWasSelected() {
        // two new orders for Ada: the left one (the first of that title) was selected, not the newest
        final String ada = "New order · Ada Shop Ltd";
        OrdersFake app = new OrdersFake() {
            @Override
            public Node read(io.github.kormcrete.swingui.driver.model.Locator target, Integer maxRows) {
                if ("tab".equals(target.role) && ada.equals(target.name) && target.nth != null) {
                    Node tab = node("tab", ada);
                    if (target.nth == 0) tab.state = new ArrayList<String>(Collections.singletonList("selected"));
                    return tab;
                }
                return super.read(target, maxRows);
            }
        };
        app.tabs(ada, "Orders", ada, ada);
        cancelQuestion(app, "Cancel order ORD-1639?");

        OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1639", "Cancel order ORD-1639?");

        JsonNode steps = app.steps();
        assertThat(steps.get(steps.size() - 1)).isEqualTo(json("{'action':'click','target':{'role':'tab','name':'" + ada
                + "','nth':0}}"));
    }

    @Test
    void cancelQuestionIsThatSeesNoQuestionSaysSoAndLeavesTheScreenAsFound() {
        OrdersFake app = new OrdersFake();

        Throwable none = catchThrowable(() -> OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1639", "x"));

        assertThat(none).isInstanceOf(AssertionError.class).hasMessage("Cancel orders… asked nothing, expected “x”");
        JsonNode steps = app.steps();
        assertThat(steps.get(steps.size() - 1)).isEqualTo(json("{'action':'click','target':" + row("ORD-1639")
                + ",'modifiers':['Mod']}"));
    }

    @Test
    void cancelQuestionIsFailsWithExpectedAndActualAndStillKeeps() {
        OrdersFake app = new OrdersFake();
        cancelQuestion(app, "Cancel 2 orders?");

        Throwable wrong = catchThrowable(() -> OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1639; ORD-1638",
                "Cancel order ORD-1639?"));

        assertThat(wrong).isInstanceOf(AssertionError.class).hasMessage("Cancelling ORD-1639; ORD-1638 asks “Cancel 2"
                + " orders? " + STOCK_RESTORED + "”, expected “Cancel order ORD-1639?”");
        // Keep, then both rows unselected again (nothing was selected before)
        JsonNode steps = app.steps();
        assertThat(steps.get(4)).isEqualTo(json("{'action':'answer','button':'Keep'}"));
        assertThat(steps).hasSize(7);
    }

    @Test
    void cancelQuestionIsOfAnOrderThatIsNotPlacedFailsWithTheAppsReasonBeforeClickingTheList() {
        OrdersFake app = new OrdersFake();

        Throwable shipped = catchThrowable(() -> OrderDeskOrdersSteps.cancelQuestionIs(app.client(), "ORD-1636", "x"));

        assertThat(shipped).isInstanceOf(DriverException.class)
                .hasMessage("NOT_ACTIONABLE: Only placed orders can be cancelled: ORD-1636 is shipped");
        assertThat(app.steps()).isEqualTo(json("[" + TO_ORDERS + "]"));
    }

    private static final String STOCK_RESTORED = "Stock is restored and invoice totals are taken off what the customer owes.";

    /** The question the row menu's Cancel orders… asks, as its message pane's labels. */
    private static void cancelQuestion(OrdersFake app, String first) {
        app.answer("dialog:Cancel order* label", label("OptionPane.label", first), label("OptionPane.label", STOCK_RESTORED));
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
    void ordersCountIsCountsTheOrdersListed() {
        OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().client(), 4);
        OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().readsFirst(2).client(), 4);
        OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().listing("", "ORD-1638").client(), 1);
        OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().listing("No orders match ‘zz’").client(), 0);
    }

    @Test
    void ordersCountIsFailsWithExpectedAndActual() {
        Throwable four = catchThrowable(() -> OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().client(), 2));
        Throwable one = catchThrowable(() -> OrderDeskOrdersSteps.ordersCountIs(new OrdersFake().listing("", "ORD-1638").client(), 0));
        Throwable none = catchThrowable(() -> OrderDeskOrdersSteps.ordersCountIs(
                new OrdersFake().listing("No orders match ‘zz’").client(), 1));

        assertThat(four).isInstanceOf(AssertionError.class).hasMessage("The Orders list shows 4 orders, expected 2");
        assertThat(one).isInstanceOf(AssertionError.class).hasMessage("The Orders list shows 1 order, expected 0");
        assertThat(none).isInstanceOf(AssertionError.class)
                .hasMessage("The Orders list shows no orders (“No orders match ‘zz’”), expected 1");
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
