package io.github.kormcrete.orderdesk.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kormcrete.swingui.client.TracingVerbs;
import io.github.kormcrete.swingui.driver.DriverException;
import io.github.kormcrete.swingui.driver.Expectation;
import io.github.kormcrete.swingui.driver.FileChooserState;
import io.github.kormcrete.swingui.driver.Reason;
import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.Verbs;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;
import io.github.kormcrete.swingui.driver.platform.CapabilityReport;
import io.github.kormcrete.swingui.driver.platform.Watch;
import io.github.kormcrete.swingui.junit.Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stand-in for the running app: each action is kept as the step JSON the agent would run (as {@link TracingVerbs}
 * keeps it), and each read gets the node the test gave for that element, or NOT_FOUND. An {@code expect} is kept as a
 * step and judged on the same nodes, at once (no wait). Elements are named by {@link #key}: role, then {@code #id} or
 * {@code 'name'}, then {@code row=} and {@code col=}, e.g. {@code label #productCount} or
 * {@code table row=Stapler col=Line total}.
 */
class FakeApp implements Verbs {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TracingVerbs trace = new TracingVerbs();
    private final Map<String, List<Node>> nodes = new LinkedHashMap<String, List<Node>>();
    private final List<String> reads = new ArrayList<String>();

    /**
     * An app showing a new order: its Customer field, a products table (A4 paper, two gel pens, the Stapler with 4
     * left, the Desk lamp out of stock), an enabled Add to cart, a cart holding {@code cartLines} and a Total.
     */
    static FakeApp withNewOrder(String... cartLines) {
        FakeApp app = new FakeApp();
        app.answer("text 'Customer'", node("text", "Customer"));
        app.answer("popup list", items("list", Arrays.asList("Ada Shop Ltd Berlin · Account"), 0));
        List<List<String>> products = Arrays.asList(
                Arrays.asList("PAP-A4-500", "A4 paper, 500 sheets", "€4.90", "120"),
                Arrays.asList("PEN-GEL-BLK", "Gel pen, black", "€1.20", "800"),
                Arrays.asList("PEN-GEL-BLU", "Gel pen, blue", "€1.20", "650"),
                Arrays.asList("DSK-STAPLER", "Stapler", "€12.50", "4"),
                Arrays.asList("DSK-LAMP", "Desk lamp", "€39.00", "Out of stock"));
        app.answer("table col=SKU", table(Arrays.asList("SKU", "Product", "Price", "Stock"), products.size(), products));
        app.answer("button 'Add to cart'", node("button", "Add to cart"));
        List<List<String>> lines = new ArrayList<List<String>>();
        for (String line : cartLines) lines.add(Arrays.asList(line, "1", "€1.00", "€1.00"));
        if (!lines.isEmpty()) {
            app.answer("table col=Line total", table(Arrays.asList("Product", "Qty", "Price", "Line total"), lines.size(), lines));
        }
        app.answer("label 'Total'", label("Total", "€31.32"));
        return app;
    }

    /** An app showing the checkout's Review step: its dialog, Cancel and Place order (no step heading on Review). */
    static FakeApp onCheckoutReview() {
        FakeApp app = new FakeApp();
        app.answer("dialog:Checkout — * button 'Cancel'", node("button", "Cancel"));
        app.answer("dialog:Checkout — * button 'Place order'", node("button", "Place order"));
        return app;
    }

    /** What a read of the element {@code key} answers; several nodes for a readAll. Replaces an earlier answer. */
    FakeApp answer(String key, Node... answer) {
        nodes.put(key, new ArrayList<Node>(Arrays.asList(answer)));
        return this;
    }

    FakeApp forget(String key) {
        nodes.remove(key);
        return this;
    }

    Client client() {
        return new Client(this, trace.app(), "fake");
    }

    /** The actions and expects so far, as step JSON (read back from text, so a long timeout compares as a number). */
    JsonNode steps() {
        try {
            return JSON.readTree(JSON.writeValueAsString(trace.steps()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The keys read so far, in order (a read that found nothing too). */
    List<String> reads() {
        return reads;
    }

    // ---- nodes

    /** A label with its text as its name (no code name, no caption). */
    static Node label(String text) {
        return new Node("label", text);
    }

    /** A label whose code name or caption names it, showing {@code value}. */
    static Node label(String name, String value) {
        Node n = new Node("label", name);
        n.value = value;
        return n;
    }

    static Node cell(String column, String value) {
        Node n = new Node("cell", column);
        n.value = value;
        return n;
    }

    static Node withTooltip(Node n, String tooltip) {
        n.tooltip = tooltip;
        return n;
    }

    static Node withState(Node n, String... states) {
        n.state = new ArrayList<String>(Arrays.asList(states));
        return n;
    }

    static Node node(String role, String name) {
        return new Node(role, name);
    }

    /** A table node: its columns and rows; {@code rowCount} may say more rows than given (a read stops at 20). */
    static Node table(List<String> columns, int rowCount, List<List<String>> rows) {
        Node n = new Node("table", null);
        n.columns = columns;
        n.rows = rows;
        n.rowCount = rowCount;
        return n;
    }

    /** A list or tree node with its items, the first ones selected as given. */
    static Node items(String role, List<String> items, Integer... selected) {
        Node n = new Node(role, null);
        n.items = items;
        n.selected = Arrays.asList(selected);
        return n;
    }

    /** The element's key: role, {@code #id} or {@code 'name'}, {@code row=} values, {@code col=}, {@code item=}. */
    static String key(Locator l) {
        StringBuilder b = new StringBuilder();
        if (l.window != null && !"main".equals(l.window)) b.append(l.window).append(' ');
        b.append(l.role == null ? "element" : l.role);
        if (l.id != null) b.append(" #").append(l.id);
        if (l.name != null) b.append(" '").append(l.name).append('\'');
        if (l.row != null) {
            List<String> values = new ArrayList<String>();
            for (Map.Entry<String, Object> e : l.row.entrySet()) {
                if (!"*".equals(e.getValue())) values.add(String.valueOf(e.getValue()));
            }
            b.append(" row=").append(String.join(",", values));
        }
        if (l.column != null) b.append(" col=").append(l.column);
        if (l.item != null) b.append(" item=").append(String.join(" > ", l.item));
        if (l.scope != null) {
            // a section or an id scope tells apart elements of one window that look alike (a review section's labels,
            // the totals' Shipping); a tab scope does not (the tests answer one tab at a time), except for a tab inside
            // a tab (an order's own Items, Delivery and Invoice)
            List<String> inside = new ArrayList<String>();
            for (String step : l.scope) {
                if (step.startsWith("section:") || step.startsWith("id:") || "tab".equals(l.role) && step.startsWith("tab:")) {
                    inside.add(step);
                }
            }
            if (!inside.isEmpty()) b.append(" in ").append(String.join(",", inside));
        }
        return b.toString();
    }

    // ---- reads

    private final Map<String, List<String>> menus = new LinkedHashMap<String, List<String>>();

    /** The items of the menu bar's menu {@code menu}, as a read of them answers. */
    FakeApp menu(String menu, String... items) {
        menus.put(menu, Arrays.asList(items));
        return this;
    }

    @Override public List<String> menuItems(List<String> path) {
        List<String> items = menus.get(String.join(" > ", path));
        if (items == null) throw new DriverException(Reason.NOT_FOUND, "no menu " + path);
        return items;
    }

    private FileChooserState chooser;

    /** A file chooser shows, in {@code folder}, its file-name field holding {@code name}; null: none shows. */
    FakeApp chooser(String folder, String name) {
        chooser = folder == null ? null : new FileChooserState(folder, name, false, "FlatLaf Light");
        return this;
    }

    boolean chooserShowing() {
        return chooser != null;
    }

    @Override public FileChooserState fileChooser(Long timeout) {
        if (chooser == null) throw new DriverException(Reason.NOT_FOUND, "no file chooser is showing");
        return chooser;
    }

    @Override public CapabilityReport capabilities() { throw Verbs.missing("capabilities"); }
    @Override public Watch watch() { throw Verbs.missing("watch"); }
    private Node windows;

    /** What a snapshot of the windows answers (the app node, with one child per window): for describeModal. */
    FakeApp windows(Node app) {
        windows = app;
        return this;
    }

    /** An app node whose one window is the modal dialog titled {@code title}, its first label {@code label}. */
    static Node modal(String title, String label) {
        Node dialog = new Node("dialog", title);
        dialog.modal = Boolean.TRUE;
        dialog.children = new ArrayList<Node>(Arrays.asList(label(label)));
        Node app = new Node("app", null);
        app.children = new ArrayList<Node>(Arrays.asList(dialog));
        return app;
    }

    @Override public Node snapshot(String window, String ref, Integer maxNodes, Integer maxRows) {
        if (windows == null) throw Verbs.missing("snapshot");
        return windows;
    }

    @Override public Node read(Locator target, Integer maxRows) {
        List<Node> found = find(target);
        if (found.size() > 1) throw new DriverException(Reason.AMBIGUOUS, found.size() + " elements match " + key(target));
        return found.get(0);
    }

    @Override public List<Node> readAll(Locator target, Integer maxRows) {
        String key = key(target);
        reads.add(key);
        List<Node> found = nodes.get(key);
        return found == null ? new ArrayList<Node>() : found;
    }

    @Override public String remember(Locator target, boolean selected, Long timeout) {
        Node n = read(target);
        // a field shows its content, never its caption
        String text = n.value != null ? n.value : "text".equals(n.role) ? null : n.name;
        if (text == null || text.isEmpty()) throw new DriverException(Reason.EXPECTATION, "nothing to remember: " + key(target));
        return text;
    }

    private Node read(Locator target) {
        return read(target, null);
    }

    private List<Node> find(Locator target) {
        String key = key(target);
        reads.add(key);
        List<Node> found = nodes.get(key);
        if (found == null || found.isEmpty()) throw new DriverException(Reason.NOT_FOUND, "nothing matches " + key);
        return found;
    }

    // ---- actions: kept as steps

    @Override public VerbOutcome click(Locator target, String button, Integer count, List<String> modifiers, Long timeout) {
        return trace.click(target, button, count, modifiers, timeout);
    }

    @Override public VerbOutcome type(Locator target, String text, Boolean replace, Boolean commit, Long timeout) {
        return trace.type(target, text, replace, commit, timeout);
    }

    @Override public VerbOutcome press(String keys, Locator target, Long timeout) { return trace.press(keys, target, timeout); }
    @Override public VerbOutcome select(Locator target, List<String> items, Long timeout) { return trace.select(target, items, timeout); }
    @Override public VerbOutcome pick(List<String> path, Long timeout) { return trace.pick(path, timeout); }
    @Override public VerbOutcome menu(List<String> path, Long timeout) { return trace.menu(path, timeout); }
    @Override public VerbOutcome contextMenu(Locator target, List<String> path, Long timeout) {
        return trace.contextMenu(target, path, timeout);
    }
    @Override public VerbOutcome answer(String button, Long timeout) { return trace.answer(button, timeout); }
    @Override public VerbOutcome chooseFile(String path, Boolean approve, Long timeout) {
        chooser = null;
        return trace.chooseFile(path, approve, timeout);
    }

    /** Kept as a step, then judged at once on the given nodes. */
    @Override public VerbOutcome expect(Locator target, Expectation e, Long timeout) {
        VerbOutcome out = trace.expect(target, e, timeout);
        String key = key(target);
        reads.add(key);
        List<Node> found = nodes.get(key);
        Node n = found == null || found.isEmpty() ? null : found.get(0);
        if (e.absent != null) {
            if (n != null) throw new DriverException(Reason.EXPECTATION, "expected " + key + " to be absent but it is still there");
            return out;
        }
        if (n == null) throw new DriverException(Reason.NOT_FOUND, "nothing matches " + key);
        String text = n.value != null ? n.value : n.name;
        if (e.text != null && !e.text.equals(text)) {
            throw new DriverException(Reason.EXPECTATION, "expected text '" + e.text + "' but was '" + text + "'");
        }
        if (e.tooltip != null && !e.tooltip.equals(n.tooltip == null ? "" : n.tooltip)) {
            throw new DriverException(Reason.EXPECTATION, "expected tooltip '" + e.tooltip + "' but was '" + n.tooltip + "'");
        }
        if (e.state != null) {
            for (String s : e.state) {
                boolean not = s.startsWith("!");
                boolean has = n.state != null && n.state.contains(not ? s.substring(1) : s);
                if (has == not) throw new DriverException(Reason.EXPECTATION, "expected state " + s + " but the states were " + n.state);
            }
        }
        return out;
    }
}
