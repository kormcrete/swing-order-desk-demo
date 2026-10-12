package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Product;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CartPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final Cart cart = new Cart();
    private final AtomicInteger changes = new AtomicInteger();
    private final AtomicInteger checkouts = new AtomicInteger();
    private final CartPanel panel = Edt.call(() -> new CartPanel(cart, changes::incrementAndGet, checkouts::incrementAndGet));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private Product product(String sku) {
        return store.catalog().find(sku).get();
    }

    @Test
    void emptyCartShowsAHint() {
        Edt.run(() -> assertThat(panel.emptyMessage()).isEqualTo("Select products and press Add to cart."));
    }

    @Test
    void cartTableIgnoresQuantityBelowOne() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 1);
            panel.refresh();
            panel.table().setValueAt(0, 0, 1);
            panel.table().setValueAt(-3, 0, 1);
            panel.table().setValueAt("", 0, 1);
            panel.table().setValueAt(null, 0, 1);
            assertThat(cart.lines().get(0).quantity()).isEqualTo(1);
            assertThat(changes.get()).isZero();
            panel.table().setValueAt(3, 0, 1);
            assertThat(cart.lines().get(0).quantity()).isEqualTo(3);
            assertThat(changes.get()).isEqualTo(1);
        });
    }

    @Test
    void stockMessageAndTotals() {
        Edt.run(() -> {
            cart.add(product("DSK-STAPLER"), 1);
            panel.refresh();
            panel.table().setValueAt(5, 0, 1);
            assertThat(panel.stockMessage()).isEqualTo("Only 4 Staplers in stock");
            assertThat(panel.totals().valueOf("Subtotal")).isEqualTo("€62.50");
            assertThat(panel.totals().valueOf("Shipping")).isEqualTo("chosen at checkout");
            panel.table().setValueAt(4, 0, 1);
            assertThat(panel.stockMessage()).isEmpty();
        });
    }

    @Test
    void removingOneLineDoesNotAsk() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 1);
            cart.add(product("CLN-WIPES"), 2);
            panel.refresh();
            panel.table().setRowSelectionInterval(0, 0);
            assertThat(panel.hasSelection()).isTrue();
            int before = changes.get(); // selecting a line reports too (the Order menu follows it)
            assertThat(before).isPositive();
            panel.removeSelected();
            assertThat(cart.lines()).extracting(l -> l.product().sku()).containsExactly("CLN-WIPES");
            assertThat(changes.get()).isGreaterThan(before);
        });
    }

    @Test
    void checkoutButtonAndItsReason() {
        Edt.run(() -> {
            panel.setCheckoutBlockedReason("The cart is empty");
            assertThat(panel.checkoutButton().isEnabled()).isFalse();
            assertThat(panel.checkoutHint()).isEqualTo("The cart is empty");
            panel.setCheckoutBlockedReason(null);
            assertThat(panel.checkoutButton().isEnabled()).isTrue();
            assertThat(panel.checkoutHint()).isEmpty();
            panel.checkoutButton().doClick();
            assertThat(checkouts.get()).isEqualTo(1);
        });
    }

    private static void type(JTable table, String text) {
        JSpinner spinner = ((QuantityEditor) table.getCellEditor()).component();
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setText(text);
    }

    @Test
    void quantitiesHaveNoGroupingInAnyLocale() {
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            Edt.run(() -> {
                Cart big = new Cart();
                big.add(product("PAP-A4-500"), 1200);
                CartPanel german = new CartPanel(big, () -> { }, () -> { });
                JTable table = german.table();
                JSpinner shown = (JSpinner) table.prepareRenderer(table.getCellRenderer(0, 1), 0, 1);
                assertThat(((JSpinner.DefaultEditor) shown.getEditor()).getTextField().getText()).isEqualTo("1200");
                table.editCellAt(0, 1);
                JSpinner editing = ((QuantityEditor) table.getCellEditor()).component();
                assertThat(((JSpinner.DefaultEditor) editing.getEditor()).getTextField().getText()).isEqualTo("1200");
                table.getCellEditor().cancelCellEditing();
            });
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void aQueuedStopDoesNotEndTheNextCellsEdit() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 1);
            cart.add(product("CLN-WIPES"), 1);
            panel.refresh();
            JTable table = panel.table();
            table.editCellAt(0, 1);
            type(table, "5");
            table.editCellAt(1, 1);
        });
        Edt.run(() -> {
            assertThat(panel.table().isEditing()).isTrue();
            assertThat(panel.table().getEditingRow()).isEqualTo(1);
            assertThat(cart.lines().get(0).quantity()).isEqualTo(5);
        });
    }

    @Test
    void anArrowClickStillEndsTheEdit() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 1);
            panel.refresh();
            panel.table().editCellAt(0, 1);
            ((QuantityEditor) panel.table().getCellEditor()).component().setValue(2);
        });
        Edt.run(() -> {
            assertThat(panel.table().isEditing()).isFalse();
            assertThat(cart.lines().get(0).quantity()).isEqualTo(2);
        });
    }

    @Test
    void backspaceStaysWithTheTextComponents() {
        // Order > Remove from cart has a bare Backspace accelerator; menu accelerators are tried after the
        // focused component's own bindings, so text fields and the spinner's editor field keep editing.
        Edt.run(() -> {
            KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
            assertThat(new JTextField().getInputMap().get(backspace)).isEqualTo("delete-previous");
            JSpinner spinner = new JSpinner();
            JTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            assertThat(field.getInputMap().get(backspace)).isEqualTo("delete-previous");
        });
    }

    @Test
    void removeSaysWhyItIsDisabled() {
        Edt.run(() -> {
            assertThat(panel.removeHint()).isEmpty();   // nothing in the cart: the empty cart says what to do
            cart.add(product("PAP-A4-500"), 1);
            panel.refresh();
            assertThat(panel.removeButton().isEnabled()).isFalse();
            assertThat(panel.removeHint()).isEqualTo("Select a cart line first");
            panel.table().setRowSelectionInterval(0, 0);
            assertThat(panel.removeButton().isEnabled()).isTrue();
            assertThat(panel.removeHint()).isEmpty();
        });
    }

    /** Runs a key through the spinner's key bindings, the way the table hands it a key that starts an edit. */
    private static boolean keyTyped(JSpinner spinner, char c) {
        KeyEvent typed = new KeyEvent(spinner, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c);
        return SwingUtilities.processKeyBindings(typed);   // public; calls processKeyBinding(.., WHEN_FOCUSED, ..)
    }

    private static JFormattedTextField textField(JSpinner spinner) {
        return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
    }

    @Test
    void aDigitTypedOnTheQtyCellStartsTheEditWithIt() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 1);
            panel.refresh();
            QuantityEditor editor = (QuantityEditor) panel.table().getColumnModel().getColumn(1).getCellEditor();
            KeyEvent key = new KeyEvent(panel.table(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_3, '3');
            assertThat(panel.table().editCellAt(0, 1, key)).isTrue();
            assertThat(keyTyped(editor.component(), '3')).isTrue();   // the first digit replaces the old quantity
            assertThat(keyTyped(editor.component(), '2')).isTrue();
            assertThat(editor.typed('x')).isFalse();
            assertThat(editor.getCellEditorValue()).isEqualTo(32);
            editor.stopCellEditing();
            assertThat(cart.lines().get(0).quantity()).isEqualTo(32);
        });
    }

    @Test
    void aClickOnTheQtyCellSelectsTheQuantitySoTypingReplacesIt() {
        Edt.run(() -> {
            cart.add(product("PAP-A4-500"), 3);
            panel.refresh();
            QuantityEditor editor = (QuantityEditor) panel.table().getColumnModel().getColumn(1).getCellEditor();
            MouseEvent click = new MouseEvent(panel.table(), MouseEvent.MOUSE_PRESSED, 0, 0, 10, 10, 2, false);
            assertThat(panel.table().editCellAt(0, 1, click)).isTrue();
            JFormattedTextField field = textField(editor.component());
            assertThat(field.getSelectedText()).isEqualTo("3");
            assertThat(field.getClientProperty(FlatClientProperties.SELECT_ALL_ON_MOUSE_CLICK)).isEqualTo(true);
            field.replaceSelection("7");
            assertThat(editor.getCellEditorValue()).isEqualTo(7);
            editor.stopCellEditing();
            assertThat(cart.lines().get(0).quantity()).isEqualTo(7);
        });
    }
}
