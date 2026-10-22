package io.github.kormcrete.swingui.driver.inspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.kormcrete.swingui.driver.model.Locator;
import io.github.kormcrete.swingui.driver.widgets.Cells;
import io.github.kormcrete.swingui.driver.widgets.TableWidget;
import io.github.kormcrete.swingui.flow.FlowJson;
import io.github.kormcrete.swingui.vocab.Matcher;

import javax.swing.JTable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The checks a person's Check menu records for one table cell, in the form the agent's session matches them with a
 * plugin's checks: the library's own {@code CheckMenu.choices} for the cell (as {@code Inspector.cell} addresses it: the
 * row by its key, {@code TableWidget.rowKey}), then {@link Matcher#matchForm} with the cell as it was seen, as
 * {@code UiMcpSession.domainChecks} does. In the library's package because {@code CheckMenu} and {@code Target} are not
 * API: a plugin test that builds these shapes by hand can drift from what the palette really records.
 */
public final class CheckMenuForms {

    private static final ObjectMapper JSON = FlowJson.registerDriverTypes(new ObjectMapper());

    private CheckMenuForms() {
    }

    /**
     * The Check menu's recordable checks of cell ({@code row}, {@code column}) (view indices) of {@code table}, which
     * shows in {@code window} inside {@code scope} (its tabs and titled sections, root first), each as a pattern
     * matches it.
     */
    public static List<JsonNode> cell(JTable table, int row, int column, String window, List<String> scope) {
        List<String> headers = TableWidget.headers(table);
        Locator recorded = new Locator();
        recorded.window = window;
        recorded.role = "table";
        recorded.row = TableWidget.rowKey(table, row);
        recorded.column = headers.get(column);
        Locator seen = new Locator();
        seen.window = window;
        seen.scope = scope == null || scope.isEmpty() ? null : new ArrayList<String>(scope);
        seen.role = "table";
        Map<String, Object> cells = new LinkedHashMap<String, Object>();
        for (int c = 0; c < headers.size(); c++) {
            if (cells.containsKey(headers.get(c))) continue;
            String text = Cells.table(table, row, c).text;
            cells.put(headers.get(c), text == null ? "" : text);
        }
        seen.row = cells;
        seen.column = recorded.column;
        Target t = new Target(recorded, false, "cell", "cell", "cell", table, null, row, column, seen);
        ObjectNode seenJson = JSON.createObjectNode();
        seenJson.set("target", stripNulls(JSON.valueToTree(seen)));
        List<JsonNode> out = new ArrayList<JsonNode>();
        for (Map<String, Object> step : CheckMenu.steps(CheckMenu.choices(t))) {
            Map<String, Object> plain = new LinkedHashMap<String, Object>(step);
            plain.remove(Inspector.ROW_INDEX);
            plain.remove(Inspector.SEEN);
            out.add(Matcher.matchForm(stripNulls(JSON.valueToTree(plain)), seenJson, false));
        }
        return out;
    }

    private static JsonNode stripNulls(JsonNode n) {
        if (n.isObject()) {
            ObjectNode o = (ObjectNode) n;
            List<String> empty = new ArrayList<String>();
            o.fieldNames().forEachRemaining(f -> { if (o.get(f).isNull()) empty.add(f); else stripNulls(o.get(f)); });
            o.remove(empty);
        } else if (n.isArray()) {
            for (JsonNode c : n) stripNulls(c);
        }
        return n;
    }
}
