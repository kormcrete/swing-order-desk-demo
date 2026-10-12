package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.CardField;
import io.github.kormcrete.orderdesk.model.CardRules;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Payment;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Step 3: on invoice (account customers; PO number when they require one) or by card. Card fields format as you
 * type; their errors show once a field was left.
 */
final class PaymentStep extends JPanel {

    private final Customer customer;
    private final Supplier<LocalDate> today;
    private final Runnable onChange;
    private final JRadioButton invoice = new JRadioButton("Invoice (30 days)");
    private final JRadioButton card = new JRadioButton("Card");
    private final JLabel invoiceNote = Ui.hint("");
    private final JTextField po = new JTextField();
    private final JTextField number = new JTextField();
    private final JTextField expiry = new JTextField();
    private final JTextField nameOnCard = new JTextField();
    private final Map<JTextField, JLabel> errors = new HashMap<>();
    private final Set<JTextField> left = new HashSet<>();
    private boolean formatting;

    PaymentStep(Customer customer, Supplier<LocalDate> today, Runnable onChange) {
        super(new MigLayout("insets 0, hidemode 3", "[26!][grow,fill]12[grow,fill]", ""));
        this.customer = customer;
        this.today = today;
        this.onChange = onChange;

        ButtonGroup group = new ButtonGroup();
        group.add(invoice);
        group.add(card);
        boolean invoiceAllowed = customer.terms().invoiceAllowed();
        invoice.setEnabled(invoiceAllowed);
        (invoiceAllowed ? invoice : card).setSelected(true);
        invoiceNote.setText(invoiceAllowed
                ? "Credit limit " + customer.terms().creditLimit().format() + " · owes " + customer.owes().format()
                : customer.name() + " pays by card");
        number.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "1234 5678 9012 3456");
        expiry.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "MM/YY");
        for (JTextField field : new JTextField[] {po, number, expiry, nameOnCard}) errors.put(field, Ui.fieldError());

        add(Ui.heading("Payment"), "span, wrap 10");
        add(invoice, "span, wrap 0");
        add(invoiceNote, "skip 1, span, wrap " + (invoiceAllowed ? 6 : 16));
        JLabel poLabel = Ui.labelFor(customer.terms().poRequired() ? "PO number" : "PO number (optional)", po);
        add(poLabel, "skip 1, span, wrap 2");
        add(po, "skip 1, wrap 0");
        add(errors.get(po), "skip 1, span, wrap 16");
        if (!invoiceAllowed) {   // card-only customers have no PO number to give
            poLabel.setVisible(false);
            po.setVisible(false);
            errors.get(po).setVisible(false);
        }
        add(card, "span, wrap 0");
        add(Ui.hint("Debit or credit card"), "skip 1, span, wrap 6");
        add(Ui.labelFor("Card number", number), "skip 1, span, wrap 2");
        add(number, "skip 1, wrap 0");
        add(errors.get(number), "skip 1, span, wrap 6");
        add(Ui.labelFor("Expiry", expiry), "skip 1");
        add(Ui.labelFor("Name on card", nameOnCard), "wrap 2");
        add(expiry, "skip 1");
        add(nameOnCard, "wrap 0");
        add(errors.get(expiry), "skip 1");
        add(errors.get(nameOnCard), "wrap");

        invoice.addActionListener(e -> changed());
        card.addActionListener(e -> changed());
        for (JTextField field : errors.keySet()) {
            Ui.onTextChange(field, () -> edited(field));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    leave(field);
                }
            });
        }
        updateEnabled();
        showErrors();
    }

    private void edited(JTextField field) {
        if (formatting) return;
        if (field == number || field == expiry) SwingUtilities.invokeLater(() -> reformat(field));
        changed();
    }

    /** Card number in groups of four, expiry as MM/YY; runs after the typing event, never inside it. */
    private void reformat(JTextField field) {
        String formatted = field == number ? CardRules.format(field.getText()) : CardRules.formatExpiry(field.getText());
        if (formatted.equals(field.getText())) return;
        formatting = true;
        try {
            field.setText(formatted);
        } finally {
            formatting = false;
        }
        changed();
    }

    /** A field was left: from now on its error shows. */
    void leave(JTextField field) {
        left.add(field);
        showErrors();
    }

    private void changed() {
        updateEnabled();
        showErrors();
        onChange.run();
    }

    private void updateEnabled() {
        boolean byInvoice = invoice.isSelected();
        po.setEnabled(byInvoice);
        number.setEnabled(!byInvoice);
        expiry.setEnabled(!byInvoice);
        nameOnCard.setEnabled(!byInvoice);
    }

    private void showErrors() {
        String poProblem = invoice.isSelected() && customer.terms().poRequired() && po.getText().trim().isEmpty()
                ? "PO number is required" : null;
        show(po, poProblem);
        Map<CardField, String> problems = card.isSelected()
                ? CardRules.validate(number.getText(), expiry.getText(), nameOnCard.getText(), today.get())
                : Collections.<CardField, String>emptyMap();
        show(number, problems.get(CardField.NUMBER));
        show(expiry, problems.get(CardField.EXPIRY));
        show(nameOnCard, problems.get(CardField.NAME));
    }

    private void show(JTextField field, String problem) {
        Ui.showError(field, errors.get(field), left.contains(field) ? problem : null);
    }

    Payment payment() {
        return invoice.isSelected() ? Payment.invoice(po.getText()) : Payment.card(number.getText());
    }

    String problem() {
        if (invoice.isSelected()) {
            return customer.terms().poRequired() && po.getText().trim().isEmpty() ? "Enter the PO number" : null;
        }
        return CardRules.validate(number.getText(), expiry.getText(), nameOnCard.getText(), today.get()).isEmpty()
                ? null : "Complete the card details";
    }

    /** After the credit warning's "Pay by card instead". */
    void selectCard() {
        card.setSelected(true);
        changed();
        number.requestFocusInWindow();
    }

    void focusFirst() {
        (invoice.isSelected() ? po : number).requestFocusInWindow();
    }

    // for tests

    JRadioButton invoiceButton() {
        return invoice;
    }

    JRadioButton cardButton() {
        return card;
    }

    JTextField poField() {
        return po;
    }

    JTextField cardNumberField() {
        return number;
    }

    JTextField expiryField() {
        return expiry;
    }

    JTextField nameOnCardField() {
        return nameOnCard;
    }

    String invoiceNote() {
        return invoiceNote.getText();
    }

    String errorText(JTextField field) {
        return errors.get(field).getText().trim();
    }
}
