package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import io.github.kormcrete.orderdesk.model.Address;
import io.github.kormcrete.orderdesk.model.Payment;
import io.github.kormcrete.orderdesk.model.PricedLine;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Totals;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/** Step 4: everything read-only, with an Edit link per section, the lines and the totals. */
final class ReviewStep extends JPanel {

    private final Map<String, JPanel> sections = new LinkedHashMap<>();
    private final LinesTableModel lines = new LinesTableModel();
    private final TotalsPanel totals = new TotalsPanel();

    ReviewStep(IntConsumer goTo) {
        // as in the mock: one row per section (caption, text, Edit at the right), stacked, then lines and totals
        super(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[top]14[top]14[top]14[]10[]"));
        add(section("Delivery address", () -> goTo.accept(0)));
        add(section("Shipping", () -> goTo.accept(1)));
        add(section("Payment", () -> goTo.accept(2)));
        JTable table = Ui.table(lines);
        Ui.widths(table, 0, 60, 90, 100);
        add(Ui.scroll(table), "hmax 170");
        add(totals, "right");
    }

    /**
     * A section's panel carries its caption as a {@link SectionBorder}: a titled border that paints the caption in the
     * caption column, so each Edit stays inside its own titled section ({@code section:Shipping}).
     */
    private JPanel section(String title, Runnable edit) {
        JPanel content = Ui.textColumn(Collections.<String>emptyList());
        sections.put(title, content);
        JButton editButton = new JButton("Edit");
        editButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        editButton.putClientProperty(FlatClientProperties.STYLE,
                "foreground: $Component.linkColor; margin: 0,2,0,2; font: -1");
        editButton.addActionListener(e -> edit.run());
        JPanel section = new JPanel(new MigLayout("insets 0, gap 14 0", "[grow,fill][]", "[top]"));
        section.setBorder(new SectionBorder(title));
        section.add(content);
        section.add(editButton, "top");
        return section;
    }

    void show(Address address, Shipping shipping, String note, Payment payment, List<? extends PricedLine> orderLines,
              Totals orderTotals) {
        Ui.setLines(sections.get("Delivery address"), address.lines());
        List<String> shippingLines = new ArrayList<>();
        shippingLines.add(Ui.shippingText(shipping, orderTotals));
        if (!note.isEmpty()) shippingLines.add("Note: " + note);
        Ui.setLines(sections.get("Shipping"), shippingLines);
        Ui.setLines(sections.get("Payment"), Collections.singletonList(payment.describe()));
        lines.setLines(orderLines);
        totals.show(orderTotals, null);
    }

    // for tests

    List<String> sectionLines(String title) {
        List<String> out = new ArrayList<>();
        for (Component c : sections.get(title).getComponents()) out.add(((JLabel) c).getText());
        return out;
    }

    TotalsPanel totals() {
        return totals;
    }

    /**
     * A titled border whose caption sits in a 110 px column left of the content, top-aligned with its first line,
     * in the muted caption colour, instead of on a box line.
     */
    static final class SectionBorder extends TitledBorder {

        private static final int CAPTION_WIDTH = 110;
        private static final int GAP = 14;

        SectionBorder(String title) {
            super(title);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(0, UIScale.scale(CAPTION_WIDTH + GAP), 0, 0);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Font font = UIManager.getFont("Label.font");
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setFont(font);
                g2.setColor(UIManager.getColor(Theme.MUTED));
                FlatUIUtils.drawString((JComponent) c, g2, getTitle(), x, y + g2.getFontMetrics().getAscent());
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Dimension getMinimumSize(Component c) {
            return new Dimension(UIScale.scale(CAPTION_WIDTH + GAP), 0);
        }
    }
}
