package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Country;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Shipping;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.KeyboardFocusManager;

/** Step 2: Standard or Express, and an optional delivery note. */
final class ShippingStep extends JPanel {

    private final JRadioButton standard = new JRadioButton(Shipping.STANDARD.describe());
    private final JRadioButton express = new JRadioButton(Shipping.EXPRESS.describe());
    private final JLabel standardPrice = new JLabel();
    private final JLabel expressPrice = new JLabel();
    private final JLabel expressReason = Ui.hint("");
    private final JTextArea note = new JTextArea(3, 30);

    ShippingStep(Runnable onChange) {
        super(new MigLayout("insets 0, hidemode 3", "[grow,fill][right]", ""));
        ButtonGroup group = new ButtonGroup();
        group.add(standard);
        group.add(express);
        standard.setSelected(true);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        // Tab and Shift+Tab move on, as in the other fields, instead of typing a tab
        note.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        note.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        add(Ui.heading("Shipping method"), "span, wrap 10");
        add(standard);
        add(standardPrice, "wrap 6");
        add(express);
        add(expressPrice, "wrap 0");
        add(expressReason, "gapleft 26, span, wrap 18");
        add(Ui.labelFor("Delivery note (optional)", note), "span, wrap 4");
        note.putClientProperty(FlatClientProperties.STYLE, "margin: 6,8,6,8");
        JScrollPane noteScroll = Ui.scroll(note);
        noteScroll.putClientProperty(FlatClientProperties.STYLE, "arc: 10");   // rounded like the fields
        add(noteScroll, "span, growx, hmin 64");

        standard.addActionListener(e -> onChange.run());
        express.addActionListener(e -> onChange.run());
    }

    /** Prices for this order and whether Express goes to {@code country}. */
    void enter(Country country, Money discountedSubtotal) {
        Money standardCost = Shipping.STANDARD.cost(discountedSubtotal);
        standardPrice.setText(standardCost.isZero() ? "Free" : standardCost.format());
        expressPrice.setText(Shipping.EXPRESS.cost(discountedSubtotal).format());
        boolean expressAllowed = Shipping.EXPRESS.allowedFor(country);
        express.setEnabled(expressAllowed);
        expressReason.setText(expressAllowed ? "" : "Express delivers to Germany and Austria only");
        if (!expressAllowed && express.isSelected()) standard.setSelected(true);
    }

    Shipping method() {
        return express.isSelected() ? Shipping.EXPRESS : Shipping.STANDARD;
    }

    /** One line: line breaks and runs of spaces become single spaces, so the review and the receipt stay one row. */
    String note() {
        return note.getText().replaceAll("\\s+", " ").trim();
    }

    void focusFirst() {
        (express.isSelected() ? express : standard).requestFocusInWindow();
    }

    // for tests

    JRadioButton expressButton() {
        return express;
    }

    String expressReason() {
        return expressReason.getText();
    }

    String standardPrice() {
        return standardPrice.getText();
    }

    JTextArea noteArea() {
        return note;
    }
}
