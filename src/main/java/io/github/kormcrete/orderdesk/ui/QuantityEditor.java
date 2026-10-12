package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.AbstractCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.EventObject;

/**
 * Edits Qty with a spinner; an arrow click or a committed value ends the edit, so the cart updates at once. Typing a
 * digit on a selected Qty cell starts the edit with that digit, as in a text cell; an edit started any other way (a
 * click, F2) selects the quantity, so typing replaces it.
 */
final class QuantityEditor extends AbstractCellEditor implements TableCellEditor {

    /** The cart's spinners keep FlatLaf's compact padding, not the roomier one of the form fields. */
    static final String COMPACT = "padding: 2,6,2,6";

    private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1)) {
        /**
         * The table hands the keys that start an edit to the editor component, which is this spinner, not its text
         * field; without this they would be lost.
         */
        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            if (e != null && e.getID() == KeyEvent.KEY_TYPED && !field().isFocusOwner() && typed(e.getKeyChar())) {
                return true;
            }
            return super.processKeyBinding(ks, e, condition, pressed);
        }
    };
    private boolean loading;
    private boolean committing;
    private boolean startedByKey;
    private int edit;

    QuantityEditor() {
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));   // digits only, whatever the machine's locale
        spinner.putClientProperty(FlatClientProperties.STYLE, COMPACT);
        spinner.addChangeListener(e -> {
            if (loading || committing) return;
            int queuedFor = edit;
            SwingUtilities.invokeLater(() -> {
                if (queuedFor == edit) stopCellEditing();   // never ends a later edit
            });
        });
    }

    JSpinner component() {
        return spinner;
    }

    private JFormattedTextField field() {
        return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        startedByKey = e instanceof KeyEvent;
        // started by a digit: FlatLaf must not select all when the field gets the focus, or the next digit replaces
        // the first; otherwise select all, also when a click gives the field the focus
        JFormattedTextField field = field();
        field.putClientProperty(FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, startedByKey
                ? FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY_NEVER : FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY_ALWAYS);
        field.putClientProperty(FlatClientProperties.SELECT_ALL_ON_MOUSE_CLICK, !startedByKey);
        return true;
    }

    /**
     * A key typed while the table still has the focus: a digit goes into the text field (the first one replaces the
     * old quantity) and the field takes the focus for the rest. Anything else is not ours.
     */
    boolean typed(char c) {
        if (c < '0' || c > '9') return false;
        JFormattedTextField field = field();
        if (startedByKey) {
            field.setText("");
            startedByKey = false;
        }
        field.replaceSelection(String.valueOf(c));
        field.requestFocusInWindow();
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        edit++;
        loading = true;
        try {
            spinner.setValue(value);
        } finally {
            loading = false;
        }
        if (!startedByKey) field().selectAll();
        return spinner;
    }

    @Override
    public Object getCellEditorValue() {
        committing = true;
        try {
            spinner.commitEdit();
        } catch (ParseException e) {
            // keep the last valid value
        } finally {
            committing = false;
        }
        return spinner.getValue();
    }
}
