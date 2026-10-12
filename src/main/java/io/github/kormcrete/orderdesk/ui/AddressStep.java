package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Address;
import io.github.kormcrete.orderdesk.model.AddressField;
import io.github.kormcrete.orderdesk.model.AddressRules;
import io.github.kormcrete.orderdesk.model.Country;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Step 1: the delivery address, filled in from the customer. A field's error shows at once when its prefilled value is
 * invalid, and for the other fields once the field was left; from then on it updates as you type.
 */
final class AddressStep extends JPanel {

    private final JTextField name = new JTextField();
    private final JTextField street = new JTextField();
    private final JTextField postalCode = new JTextField();
    private final JTextField city = new JTextField();
    private final JComboBox<Country> country = new JComboBox<>(Country.values());
    private final JTextField phone = new JTextField();
    private final Map<AddressField, JTextField> fields = new EnumMap<>(AddressField.class);
    private final Map<AddressField, JLabel> errors = new EnumMap<>(AddressField.class);
    private final Set<AddressField> shown = EnumSet.noneOf(AddressField.class);
    private final Runnable onChange;

    AddressStep(Address initial, Runnable onChange) {
        super(new MigLayout("insets 0, wrap 2", "[grow,fill,sg]16[grow,fill,sg]", ""));
        this.onChange = onChange;
        name.setText(initial.name());
        street.setText(initial.street());
        postalCode.setText(initial.postalCode());
        city.setText(initial.city());
        country.setSelectedItem(initial.country());
        phone.setText(initial.phone());

        // as in the mock: captions above the fields; Name and Street full width, then two fields per row
        add(Ui.heading("Delivery address"), "span, wrap 10");
        wide("Name", name, AddressField.NAME);
        wide("Street", street, AddressField.STREET);
        JLabel noError = Ui.fieldError();
        pair(Ui.labelFor("Postal code", postalCode), postalCode, error(AddressField.POSTAL_CODE, postalCode),
                Ui.labelFor("City", city), city, error(AddressField.CITY, city));
        pair(Ui.labelFor("Country", country), country, noError,
                Ui.labelFor("Phone (optional)", phone), phone, error(AddressField.PHONE, phone));

        shown.addAll(AddressRules.validate(address()).keySet());
        for (Map.Entry<AddressField, JTextField> entry : fields.entrySet()) {
            AddressField key = entry.getKey();
            Ui.onTextChange(entry.getValue(), this::changed);
            entry.getValue().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    leave(key);
                }
            });
        }
        country.addActionListener(e -> {
            shown.add(AddressField.POSTAL_CODE);   // a new country re-checks the postal code
            changed();
        });
        showErrors();
    }

    private JLabel error(AddressField key, JTextField field) {
        JLabel error = Ui.fieldError();
        fields.put(key, field);
        errors.put(key, error);
        return error;
    }

    /** Caption, field and error across both columns. */
    private void wide(String label, JTextField field, AddressField key) {
        JLabel error = error(key, field);
        add(Ui.labelFor(label, field), "span, wrap 2");
        add(field, "span, wrap 0");
        add(error, "span, wrap 6");
    }

    /** Two captions, two fields, two errors side by side. */
    private void pair(JLabel leftLabel, JComponent left, JLabel leftError, JLabel rightLabel, JComponent right,
                      JLabel rightError) {
        add(leftLabel);
        add(rightLabel, "wrap 2");
        add(left);
        add(right, "wrap 0");
        add(leftError);
        add(rightError, "wrap 6");
    }

    /** A field was left: from now on its error shows (and the reason under Next may change with it). */
    void leave(AddressField key) {
        if (!shown.add(key)) return;
        showErrors();
        onChange.run();
    }

    private void changed() {
        showErrors();
        onChange.run();
    }

    private void showErrors() {
        Map<AddressField, String> problems = AddressRules.validate(address());
        for (AddressField key : fields.keySet()) {
            Ui.showError(fields.get(key), errors.get(key), shown.contains(key) ? problems.get(key) : null);
        }
    }

    Address address() {
        return new Address(name.getText(), street.getText(), postalCode.getText(), city.getText(),
                (Country) country.getSelectedItem(), phone.getText());
    }

    /**
     * Why Next is disabled, or null: "Fix the fields marked in red" while an error is shown, otherwise the first
     * error's own text (a field changed but not left yet shows no red).
     */
    String problem() {
        Map<AddressField, String> problems = AddressRules.validate(address());
        if (problems.isEmpty()) return null;
        for (AddressField key : problems.keySet()) {
            if (shown.contains(key)) return "Fix the fields marked in red";
        }
        return problems.values().iterator().next();
    }

    void focusFirst() {
        Map<AddressField, String> problems = AddressRules.validate(address());
        fields.get(problems.isEmpty() ? AddressField.NAME : problems.keySet().iterator().next()).requestFocusInWindow();
    }

    // for tests

    JTextField field(AddressField key) {
        return fields.get(key);
    }

    JComboBox<Country> countryBox() {
        return country;
    }

    String errorText(AddressField key) {
        return errors.get(key).getText().trim();
    }
}
