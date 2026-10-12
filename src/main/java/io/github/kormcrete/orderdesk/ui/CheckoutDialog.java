package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Store;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/** The modal "Checkout — <customer>" dialog around {@link CheckoutPanel}. Enter = Next, Esc = Cancel. */
final class CheckoutDialog extends JDialog {

    private Order placed;

    private CheckoutDialog(Window owner, Store store, Customer customer, Cart cart) {
        super(owner, "Checkout — " + customer.name(), ModalityType.APPLICATION_MODAL);
        CheckoutPanel panel = new CheckoutPanel(store, customer, cart,
                (title, message, detail, type, options) -> Dialogs.choose(this, title, message, detail, type, options),
                order -> {
                    placed = order;
                    dispose();
                },
                this::dispose,
                CheckoutPanel.PLACE_ORDER_MILLIS);
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.nextButton());
        getRootPane().registerKeyboardAction(e -> panel.cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.cancel();
            }
        });
        setMinimumSize(new Dimension(620, 540));
        setSize(700, 600);
        setLocationRelativeTo(owner);
    }

    /** Shows the wizard and returns the placed order, or nothing when it was cancelled. */
    static Optional<Order> show(Component parent, Store store, Customer customer, Cart cart) {
        CheckoutDialog dialog = new CheckoutDialog(SwingUtilities.getWindowAncestor(parent), store, customer, cart);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.placed);
    }
}
