package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Pricing;
import io.github.kormcrete.orderdesk.model.Totals;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Subtotal, Volume discount (only when there is one), Shipping, VAT (20%), Total. */
final class TotalsPanel extends JPanel {

    private static final String VAT = "VAT (" + Pricing.VAT_PERCENT + "%)";

    private final Map<String, JLabel> values = new LinkedHashMap<>();
    private final JLabel discountCaption;

    TotalsPanel() {
        super(new MigLayout("insets 0, wrap 2, hidemode 3, alignx right", "[right]24[right,90::]", "[]3[]3[]3[]5[]5[]"));
        row("Subtotal");
        discountCaption = row("Volume discount");
        row("Shipping");
        row(VAT);
        JSeparator rule = new JSeparator();   // the rule above Total
        rule.putClientProperty(FlatClientProperties.STYLE, "foreground: $Component.borderColor");
        add(rule, "span 2, growx, hmin pref");   // never squeezed away in a tight cell
        JLabel totalCaption = row("Total");
        totalCaption.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        values.get("Total").putClientProperty(FlatClientProperties.STYLE, "font: +1 bold");
    }

    private JLabel row(String caption) {
        JLabel label = Ui.hint(caption);
        JLabel value = new JLabel();
        label.setLabelFor(value);
        add(label);
        add(value);
        values.put(caption, value);
        return label;
    }

    /** {@code shippingText} replaces the shipping amount, e.g. "chosen at checkout" in the cart. */
    void show(Totals totals, String shippingText) {
        values.get("Subtotal").setText(totals.subtotal().format());
        boolean discount = totals.discount().isPositive();
        discountCaption.setVisible(discount);
        values.get("Volume discount").setVisible(discount);
        values.get("Volume discount").setText(totals.discount().negate().format());
        values.get("Shipping").setText(shippingText != null ? shippingText
                : totals.shipping().isZero() ? "Free" : totals.shipping().format());
        values.get(VAT).setText(totals.vat().format());
        values.get("Total").setText(totals.total().format());
    }

    /** The value next to {@code caption}, or null while it is hidden. */
    String valueOf(String caption) {
        JLabel value = values.get(caption);
        return value.isVisible() ? value.getText() : null;
    }
}
