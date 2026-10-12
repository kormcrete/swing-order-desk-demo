package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Product;
import io.github.kormcrete.orderdesk.model.Store;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.List;

/** A New order tab: Customer field with its terms, the catalog and the cart. */
final class NewOrderPanel extends JPanel {

    private final Store store;
    private final Workspace workspace;
    private final Cart cart = new Cart();
    private final CustomerField customerField;
    private final JLabel terms = termsCard();
    private final ProductsPanel products;
    private final CartPanel cartPanel;
    private final Runnable storeListener = this::storeChanged;
    private Customer customer;

    NewOrderPanel(Store store, Workspace workspace) {
        // fixed preferred widths: a hint appearing under a pane does not shift the panes
        // the mock's 200 px tree + minmax(380px, 1fr) products | minmax(430px, 1.05fr) cart, 10 px apart
        super(new MigLayout("insets 10 12 12 12, fill", "[590:715:,grow 100,fill]10[430:531:,grow 105,fill]",
                "[]10[grow,fill]"));
        this.store = store;
        this.workspace = workspace;
        customerField = new CustomerField(store, this::customerChosen);
        products = new ProductsPanel(store.catalog(), this::addToCart);
        cartPanel = new CartPanel(cart, this::cartChanged, this::checkout);

        JPanel top = new JPanel(new MigLayout("insets 0", "[]12[320!]12[]"));
        top.add(Ui.labelFor("Customer", customerField));
        top.add(customerField, "growx");
        top.add(terms, "h 28!");
        add(top, "span, wrap");
        add(products, "grow");
        add(cartPanel, "grow");

        cartChanged();
    }

    /** The customer's terms on a soft, rounded card; hidden while no customer is chosen. */
    private static JLabel termsCard() {
        JLabel card = new JLabel() {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(new FlatLineBorder(new Insets(0, 10, 0, 10), UIManager.getColor(Theme.SOFT_BORDER), 1, 10));
            }
        };
        card.setOpaque(true);
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc: 10; background: $OrderDesk.cardBackground; foreground: $OrderDesk.muted");
        card.setVisible(false);
        return card;
    }

    private void showTerms() {
        terms.setText(customer == null ? "" : customer.termsSummary());
        terms.setVisible(customer != null);
    }

    void chooseCustomer(Customer customer) {
        customerField.choose(customer);
    }

    private void customerChosen(Customer chosen) {
        customer = chosen;
        showTerms();
        workspace.retitle(this, chosen == null ? "New order" : "New order · " + chosen.name());
        cartChanged();
    }

    void addToCart(List<Product> toAdd) {
        for (Product product : toAdd) {
            if (product.inStock()) cart.add(product, 1);
        }
        cartPanel.refresh();
        cartChanged();
    }

    private void cartChanged() {
        cartPanel.setCheckoutBlockedReason(checkoutBlockReason());
        workspace.actionsChanged();
    }

    /** Stock or what a customer owes changed elsewhere. */
    private void storeChanged() {
        products.refresh();
        cartPanel.refresh();
        showTerms();
        cartChanged();
    }

    /** Why Checkout… is disabled, or null. */
    String checkoutBlockReason() {
        if (customer == null) return "Choose a customer first";
        if (cart.isEmpty()) return "The cart is empty";
        if (!cart.stockProblems().isEmpty()) return "Fix quantities above stock";
        return null;
    }

    boolean canCheckout() {
        return checkoutBlockReason() == null;
    }

    boolean hasSelectedLines() {
        return cartPanel.hasSelection();
    }

    /** True when the focus is inside the cart panel (a null focus is outside). */
    static boolean focusInCart(Component focus, CartPanel cartPanel) {
        return focus != null && SwingUtilities.isDescendingFrom(focus, cartPanel);
    }

    /**
     * Remove from cart. A key press (the Backspace accelerator is window-wide) acts only while the focus is in
     * the cart; a menu click always acts.
     */
    void removeSelectedLinesFromMenu() {
        AWTEvent event = EventQueue.getCurrentEvent();
        if (event instanceof KeyEvent && !focusInCart(
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner(), cartPanel)) {
            return;
        }
        removeSelectedLines();
    }

    void removeSelectedLines() {
        cartPanel.removeSelected();
    }

    boolean hasUnsavedCart() {
        return !cart.isEmpty();
    }

    void checkout() {
        if (!canCheckout()) return;
        CheckoutDialog.show(this, store, customer, cart).ifPresent(order -> workspace.orderPlaced(this, order));
    }

    void focusFirstField() {
        customerField.requestFocusInWindow();
    }

    /** Follows the store while the tab is shown, and catches up with what changed while it was not. */
    @Override
    public void addNotify() {
        super.addNotify();
        store.addListener(storeListener);
        storeChanged();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        store.removeListener(storeListener);
    }

    // for tests

    String termsText() {
        return terms.getText();
    }

    CartPanel cartPanel() {
        return cartPanel;
    }

    ProductsPanel productsPanel() {
        return products;
    }

    Cart cart() {
        return cart;
    }
}
