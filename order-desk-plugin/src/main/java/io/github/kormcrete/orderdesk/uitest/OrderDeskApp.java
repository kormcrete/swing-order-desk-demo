package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.PendingDialog;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.Verbs;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;
import io.github.kormcrete.swingui.spi.AppHooks;
import io.github.kormcrete.swingui.spi.AppInfo;
import io.github.kormcrete.swingui.spi.ResetSpec;
import io.github.kormcrete.swingui.spi.StepHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Order Desk as the app plugin: its reset brings every test to the same start, the Orders list with no order tab
 * open, so recorded tests run one after another on one app. A New order tab with something in its cart asks "Discard
 * this new order?" before it closes; the reset answers Discard. Order Desk has no login and nothing to navigate to by
 * name: tests open what they need with the menu or a {@code do} step. Placed orders stay (they are the app's data,
 * and each test creates its own).
 */
final class OrderDeskApp implements StepHost, AppHooks {

    static final String CLOSE_TAB_ITEM = "Close tab";
    static final String DISCARD_ANSWER = "Discard";
    /** More than any test opens: a reset that has not closed them by then is stuck, and says on which tab. */
    private static final int MAX_CLOSES = 200;
    private static final long TIMEOUT_MS = 5_000L;

    private final Verbs verbs;

    OrderDeskApp(Verbs verbs) {
        this.verbs = verbs;
    }

    @Override public Verbs verbs() {
        return verbs;
    }

    /** Every tab but Orders closed, last first (new orders discarded), then the Orders list shown. */
    @Override
    public void reset(ResetSpec spec) {
        for (int i = 0; i < MAX_CLOSES; i++) {
            List<Node> tabs = tabs();
            if (tabs.size() <= 1) break;
            int last = tabs.size() - 1;
            if (!tabs.get(last).hasState("selected")) select(tabs, last);
            String title = tabs.get(last).name;
            closeActiveTab();
            if (tabs().size() >= tabs.size()) {
                throw new DriverException(Reason.APP_ERROR, "reset: the tab '" + title + "' would not close");
            }
        }
        List<Node> tabs = tabs();
        if (tabs.size() > 1) {
            throw new DriverException(Reason.APP_ERROR, "reset: tabs still open after " + MAX_CLOSES + " closes");
        }
        if (!tabs.get(0).hasState("selected")) select(tabs, 0);
    }

    /** File ▸ Close tab on the selected tab; a new order's "Discard this new order?" is answered Discard. */
    @Override
    public void closeActiveTab() {
        VerbOutcome closed = verbs.menu(Arrays.asList(OrderDeskSteps.FILE_MENU, CLOSE_TAB_ITEM), TIMEOUT_MS);
        if (closed != null && closed.pending != null) verbs.answer(DISCARD_ANSWER, TIMEOUT_MS);
    }

    /** Nobody logs in to Order Desk: nothing to do. */
    @Override
    public void login(ResetSpec who, boolean force) {
    }

    @Override
    public void loginAs(String alias) {
        throw new DriverException(Reason.CAPABILITY_GAP, "Order Desk has no login: leave loginAs out of its tests");
    }

    @Override
    public void navigate(String target) {
        throw new DriverException(Reason.CAPABILITY_GAP, "Order Desk names no navigate targets: open screens with the menu "
                + "(File ▸ New order) or a do step (Open a new order for …)");
    }

    /** The app steps here never wait on a dialog of their own: the reset and close answer what they open. */
    @Override
    public PendingDialog openDialog() {
        return null;
    }

    @Override
    public AppInfo info() {
        return new AppInfo("Order Desk", OrderDeskApp.class.getPackage().getImplementationVersion());
    }

    /** The main window's tabs, Orders first; a fresh read each time. */
    private List<Node> tabs() {
        Node strip = strip(verbs.snapshot("main", null, null, 4, 400, 0));
        if (strip == null) throw new DriverException(Reason.NOT_FOUND, "reset: no Orders tab in the main window");
        List<Node> tabs = new ArrayList<Node>();
        for (Node c : strip.children) if ("tab".equals(c.role)) tabs.add(c);
        return tabs;
    }

    /** The tab strip that holds the Orders tab (an order's own tabs, Invoice and the like, sit inside it). */
    private static Node strip(Node n) {
        if (n == null) return null;
        if ("tabs".equals(n.role) && n.children != null) {
            for (Node c : n.children) if ("tab".equals(c.role) && OrderDeskSteps.ORDERS_TAB.equals(c.name)) return n;
        }
        if (n.children != null) {
            for (Node c : n.children) {
                Node found = strip(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Clicks tab {@code index}: by its title and, as titles repeat ("New order · Ada Shop Ltd"), which of them it is. */
    private void select(List<Node> tabs, int index) {
        String title = tabs.get(index).name;
        int nth = 0;
        for (int i = 0; i < index; i++) if (title.equals(tabs.get(i).name)) nth++;
        Locator tab = Locator.of("tab", title);
        tab.window = "main";
        tab.nth = nth;
        verbs.click(tab, null, null, null, TIMEOUT_MS);
    }
}
