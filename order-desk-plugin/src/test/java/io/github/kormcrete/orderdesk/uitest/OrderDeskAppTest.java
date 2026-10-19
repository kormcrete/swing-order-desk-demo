package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.PendingDialog;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.Verbs;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;
import io.github.kormcrete.swingui.driver.platform.CapabilityReport;
import io.github.kormcrete.swingui.driver.platform.Watch;
import io.github.kormcrete.swingui.spi.ResetSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** Order Desk's reset and app steps, on a stand-in of its main window's tabs. */
class OrderDeskAppTest {

    private final Tabs app = new Tabs();
    private final OrderDeskApp orderDesk = new OrderDeskApp(app);

    @Test
    void resetClosesEveryOrderTabDiscardingNewOrdersAndShowsTheOrdersList() {
        app.open("ORD-1640", false);
        app.open("New order · Ada Shop Ltd", true);
        app.open("New order · Ada Shop Ltd", true);
        app.open("New order", false);
        app.selected = 2;

        orderDesk.reset(ResetSpec.alias(null));

        assertThat(app.titles).containsExactly("Orders");
        assertThat(app.selected).isEqualTo(0);
        // a new order with something in its cart asks first: Discard, twice; the others close without a question
        assertThat(app.answers).containsExactly("Discard", "Discard");
    }

    @Test
    void resetOnTheOrdersListAloneDoesNothingButShowIt() {
        orderDesk.reset(ResetSpec.alias(null));

        assertThat(app.titles).containsExactly("Orders");
        assertThat(app.log).doesNotContain("menu [File, Close tab]");
    }

    @Test
    void aTabThatWillNotCloseFailsTheResetNamingIt() {
        app.open("New order · Birch & Co", true);
        app.refuseDiscard = true;

        Throwable t = catchThrowable(() -> orderDesk.reset(ResetSpec.alias(null)));

        assertThat(t).isInstanceOf(DriverException.class).hasMessageContaining("New order · Birch & Co");
        assertThat(((DriverException) t).reason()).isEqualTo(Reason.APP_ERROR);
    }

    @Test
    void closeClosesTheActiveTabAnsweringDiscard() {
        app.open("New order · Ada Shop Ltd", true);

        orderDesk.closeActiveTab();

        assertThat(app.titles).containsExactly("Orders");
        assertThat(app.answers).containsExactly("Discard");
    }

    @Test
    void orderDeskHasNoLoginAndNoNavigateTargets() {
        orderDesk.login(ResetSpec.alias("default"), true);
        assertThat(app.log).isEmpty();

        Throwable login = catchThrowable(() -> orderDesk.loginAs("default"));
        assertThat(((DriverException) login).reason()).isEqualTo(Reason.CAPABILITY_GAP);
        assertThat(login).hasMessageContaining("no login");
        Throwable navigate = catchThrowable(() -> orderDesk.navigate("Orders"));
        assertThat(((DriverException) navigate).reason()).isEqualTo(Reason.CAPABILITY_GAP);
        assertThat(navigate).hasMessageContaining("menu");
        assertThat(orderDesk.info().name).isEqualTo("Order Desk");
    }

    /** The main window's tab strip as Order Desk keeps it: Orders first and never closed; File ▸ Close tab closes the selected one. */
    private static final class Tabs implements Verbs {
        final List<String> titles = new ArrayList<String>(Collections.singletonList("Orders"));
        final List<Boolean> unsaved = new ArrayList<Boolean>(Collections.singletonList(false));
        final List<String> answers = new ArrayList<String>();
        final List<String> log = new ArrayList<String>();
        int selected;
        boolean asking;
        boolean refuseDiscard;

        void open(String title, boolean withCart) {
            titles.add(title);
            unsaved.add(withCart);
            selected = titles.size() - 1;
        }

        @Override
        public Node snapshot(String window, String ref, Integer maxNodes, Integer maxRows) {
            return snapshot(window, ref, null, null, maxNodes, maxRows);
        }

        @Override
        public Node snapshot(String window, String ref, Locator target, Integer depth, Integer maxNodes, Integer maxRows) {
            Node strip = node("tabs", null);
            for (int i = 0; i < titles.size(); i++) {
                Node tab = node("tab", titles.get(i));
                if (i == selected) tab.state = new ArrayList<String>(Collections.singletonList("selected"));
                strip.addChild(tab);
            }
            Node main = node("window", "Order Desk");
            main.addChild(node("menubar", null));
            main.addChild(strip);
            Node root = node("app", null);
            root.addChild(main);
            return root;
        }

        @Override
        public Node read(Locator target, Integer maxRows) {
            throw new UnsupportedOperationException();
        }

        @Override
        public VerbOutcome click(Locator target, String button, Integer count, List<String> modifiers, Long timeout) {
            log.add("click " + target);
            assertThat(target.role).isEqualTo("tab");
            int seen = 0;
            for (int i = 0; i < titles.size(); i++) {
                if (!titles.get(i).equals(target.name)) continue;
                if (seen++ == (target.nth == null ? 0 : target.nth)) {
                    selected = i;
                    return new VerbOutcome();
                }
            }
            throw new DriverException(Reason.NOT_FOUND, "no tab " + target);
        }

        @Override
        public VerbOutcome menu(List<String> path, Long timeout) {
            log.add("menu " + path);
            assertThat(path).isEqualTo(Arrays.asList("File", "Close tab"));
            if (selected == 0) throw new DriverException(Reason.NOT_ACTIONABLE, "Close tab is disabled");
            VerbOutcome out = new VerbOutcome();
            if (unsaved.get(selected)) {
                asking = true;
                out.pending = new PendingDialog("Close tab", "Discard this new order?", Arrays.asList("Discard", "Cancel"),
                        Collections.<String>emptyList());
            } else {
                close();
            }
            return out;
        }

        @Override
        public VerbOutcome answer(String button, Long timeout) {
            log.add("answer " + button);
            if (!asking) throw new DriverException(Reason.NOT_FOUND, "no dialog");
            answers.add(button);
            asking = false;
            if (button.equals("Discard") && !refuseDiscard) close();
            return new VerbOutcome();
        }

        private void close() {
            titles.remove(selected);
            unsaved.remove(selected);
            // as JTabbedPane does: the tab after it takes its place, else the one before
            if (selected >= titles.size()) selected = titles.size() - 1;
        }

        private static Node node(String role, String name) {
            Node n = new Node();
            n.role = role;
            n.name = name;
            return n;
        }

        @Override public void resetPointer() { log.add("resetPointer"); }
        @Override public CapabilityReport capabilities() { return null; }
        @Override public Watch watch() { return null; }
    }
}
