package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.model.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The main window with an order's own tab selected, as {@link FakeApp} answers reads: ORD-1639 for Ada Shop Ltd (its
 * status, its facts, Save receipt…, Cancel order and, when it is not Placed, the reason beside it), its Items (four
 * lines, two of them gel pens), Delivery (an address of five lines, Standard shipping, no note) and Invoice (an
 * invoice with a PO, a volume discount). A click on Items, Delivery or Invoice selects it; Cancel order asks, and the
 * answer Cancel order cancels it. Save receipt's file chooser saves into {@code /home/ada} unless the path is absolute:
 * a file given by {@link #withFile} exists (the app asks to replace it), and {@link #failing} makes the save fail with
 * the app's message. What an action makes show is in what it changed ({@code withChanges}), as the live app reads it.
 */
class OrderTabFake extends OrdersFake {

    static final List<String> ITEM_COLUMNS = Arrays.asList("SKU", "Product", "Qty", "Price", "Line total");
    static final List<List<String>> LINES = Collections.unmodifiableList(Arrays.asList(
            Arrays.asList("PEN-GEL-BLK", "Gel pen, black", "50", "€1.20", "€60.00"),
            Arrays.asList("PAP-A4-500", "A4 paper, 500 sheets", "3", "€4.90", "€14.70"),
            Arrays.asList("DSK-CLIPS", "Paper clips, box of 100", "2", "€1.50", "€3.00"),
            Arrays.asList("PEN-GEL-BLU", "Gel pen, blue", "1", "€1.20", "€1.20")));
    static final List<String> ADDRESS = Collections.unmodifiableList(Arrays.asList(
            "Ada Lindqvist", "Torstraße 118", "10119 Berlin", "Germany", "+49 30 5550 1188"));
    static final String HOME = "/home/ada";

    private final String number;
    private String status;
    private String sub = "Items";
    private List<String> open;
    private String selected;
    /** The message of the question or error showing; null when none. */
    private String dialog;
    private final Set<String> files = new LinkedHashSet<String>();
    private String failing;

    /** ORD-1639, Placed, its tab selected. */
    OrderTabFake() {
        this("ORD-1639", "Placed");
    }

    OrderTabFake(String number, String status) {
        this.number = number;
        this.status = status;
        showTabs(number, "Orders", number);
        answer("element #orderFacts", label("orderFacts", "Ada Shop Ltd · 9 Oct 2026 · €95.52"));
        answer("button 'Save receipt…'", node("button", "Save receipt…"));
        answer("table", table(ITEM_COLUMNS, LINES.size(), LINES));
        for (List<String> line : LINES) {
            for (int c = 0; c < ITEM_COLUMNS.size(); c++) {
                answer("table row=" + line.get(0) + "," + line.get(1) + " col=" + ITEM_COLUMNS.get(c), cell(ITEM_COLUMNS.get(c), line.get(c)));
            }
        }
        answer("element #itemsSummary", label("itemsSummary", "56 units in 4 lines"));
        Node[] address = new Node[ADDRESS.size()];
        for (int i = 0; i < address.length; i++) address[i] = label(ADDRESS.get(i));
        answer("label in id:deliveryAddress", address);
        answer("label 'Shipping method'", label("Shipping method", "Standard · 3–5 business days · €4.90"));
        answer("label 'Delivery note'", label("Delivery note", "—"));
        answer("label 'Payment'", label("Payment", "Invoice (30 days) · PO PO-2026-131"));
        answer("label 'Order date'", label("Order date", "9 Oct 2026"));
        answer("label 'Subtotal'", label("Subtotal", "€77.70"));
        answer("label 'Volume discount'", label("Volume discount", "−€3.00"));
        answer("label 'Shipping'", label("Shipping", "€4.90"));
        answer("label 'VAT (20%)'", label("VAT (20%)", "€15.92"));
        answer("label 'Total'", label("Total", "€95.52"));
        showOrder();
    }

    /** The open tabs, left to right, {@code selected} the selected one. */
    OrderTabFake showTabs(String selected, String... titles) {
        this.selected = selected;
        this.open = new ArrayList<String>(Arrays.asList(titles));
        tabs(selected, titles);
        return this;
    }

    /** Which of Items, Delivery and Invoice is selected. */
    OrderTabFake onSubTab(String sub) {
        this.sub = sub;
        showOrder();
        return this;
    }

    /** {@code path} (in {@link #HOME} unless absolute) exists: saving to it asks to replace it. */
    OrderTabFake withFile(String path) {
        files.add(abs(path));
        return this;
    }

    /** A save fails: the app says "The receipt could not be saved: {@code reason}". */
    OrderTabFake failing(String reason) {
        failing = reason;
        return this;
    }

    String status() {
        return status;
    }

    private void showOrder() {
        Node pill = label("orderStatus", status);
        answer("element #orderStatus", pill);
        Node cancel = node("button", "Cancel order");
        if (!"Placed".equals(status)) {
            cancel = withState(cancel, "disabled");
            answer("label 'Only placed orders can be cancelled'",
                    label("Only placed orders can be cancelled — this one is " + status.toLowerCase(java.util.Locale.ROOT)));
        } else {
            forget("label 'Only placed orders can be cancelled'");
        }
        answer("button 'Cancel order'", cancel);
        Node[] inner = new Node[3];
        int i = 0;
        for (String t : Arrays.asList("Items", "Delivery", "Invoice")) {
            Node tab = node("tab", t);
            tab.value = sub;   // a tab's remember(selected) reads its pane's selected title
            answer("tab '" + t + "' in tab:" + number, tab);
            inner[i++] = tab;
        }
        answer("tab in tab:" + number, inner);
    }

    @Override
    public VerbOutcome click(Locator target, String button, Integer count, List<String> modifiers, Long timeout) {
        VerbOutcome out = super.click(target, button, count, modifiers, timeout);
        if ("tab".equals(target.role) && target.scope != null && target.scope.contains("tab:" + number)) {
            sub = target.name;
            showOrder();
        } else if ("tab".equals(target.role) && target.scope == null && open.contains(target.name)) {
            showTabs(target.name, open.toArray(new String[0]));
        } else if ("button".equals(target.role) && "Cancel order".equals(target.name) && "Placed".equals(status)) {
            dialog = "Cancel order " + number + "?";
        }
        return out;
    }

    @Override
    public VerbOutcome chooseFile(String path, Boolean approve, Long timeout) {
        VerbOutcome out = super.chooseFile(path, approve, timeout);
        String file = abs(path);
        if (!file.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) file += ".txt";
        lastFile = file;
        if (failing != null) dialog = "The receipt could not be saved: " + failing;
        else if (files.contains(file)) dialog = file.substring(file.lastIndexOf('/') + 1) + " already exists.\nDo you want to replace it?";
        else save(file);
        return out;
    }

    @Override
    public VerbOutcome answer(String button, Long timeout) {
        VerbOutcome out = super.answer(button, timeout);
        String was = dialog;
        dialog = null;
        if (was == null) return out;
        if (was.startsWith("Cancel order ") && "Cancel order".equals(button)) {
            status = "Cancelled";
            answer("element #statusMessage", label("statusMessage", "Order " + number + " cancelled"));
            showOrder();
        } else if (was.contains(" already exists.") && "Replace".equals(button)) {
            save(lastFile);
        }
        return out;
    }

    /** What an action changed: the message of a question or error it made show, as the live reading gives it. */
    @Override
    public VerbOutcome withChanges(Supplier<VerbOutcome> action) {
        VerbOutcome out = action.get();
        if (out == null) out = new VerbOutcome();
        Map<String, Object> changed = new LinkedHashMap<String, Object>();
        if (dialog != null) changed.put("message", dialog.replace('\n', ' '));
        out.changed = changed;
        return out;
    }

    private String lastFile;

    private void save(String file) {
        lastFile = file;
        files.add(file);
        answer("element #statusMessage", label("statusMessage", "Receipt saved to " + file));
    }

    private static String abs(String path) {
        return path.startsWith("/") ? path : HOME + "/" + path;
    }

}
