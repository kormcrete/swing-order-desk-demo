package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Address;
import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.CreditCheck;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Payment;
import io.github.kormcrete.orderdesk.model.Pricing;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Store;
import io.github.kormcrete.orderdesk.model.Totals;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * The checkout wizard's content: step indicator, the four steps, Back / Next / Cancel and the reason Next is
 * disabled. Questions go through an {@link Asker}, so the whole flow runs in tests without a dialog.
 */
final class CheckoutPanel extends JPanel {

    static final long PLACE_ORDER_MILLIS = 2000;
    private static final String[] STEPS = {"Address", "Shipping", "Payment", "Review"};

    /** A modal question; returns the chosen option's index, or -1 when closed. */
    interface Asker {
        int choose(String title, String message, String detail, int messageType, String... options);
    }

    private final Store store;
    private final Customer customer;
    private final Cart cart;
    private final Asker asker;
    private final Consumer<Order> onPlaced;
    private final Runnable onCancel;
    private final long placeMillis;
    private final StepIndicator indicator = new StepIndicator(STEPS);
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final AddressStep address;
    private final ShippingStep shipping;
    private final PaymentStep payment;
    private final ReviewStep review;
    private final JButton back = new JButton("Back");
    private final JButton next = new JButton("Next");
    private final JButton cancel = new JButton("Cancel");
    private final JLabel reason = Ui.hint("");
    private final JLabel placingFor = Ui.hint("");
    private int step;
    private boolean placing;

    CheckoutPanel(Store store, Customer customer, Cart cart, Asker asker, Consumer<Order> onPlaced, Runnable onCancel,
                  long placeMillis) {
        super(new MigLayout("insets 12 20 16 20, fill", "[grow,fill]", "[]14[grow,fill]16[]"));
        this.store = store;
        this.customer = customer;
        this.cart = cart;
        this.asker = asker;
        this.onPlaced = onPlaced;
        this.onCancel = onCancel;
        this.placeMillis = placeMillis;
        address = new AddressStep(customer.address(), this::updateButtons);
        shipping = new ShippingStep(this::updateButtons);
        payment = new PaymentStep(customer, store::today, this::updateButtons);
        review = new ReviewStep(this::goTo);

        body.add(address, "0");
        body.add(shipping, "1");
        body.add(payment, "2");
        body.add(review, "3");
        body.add(busyPanel(), "busy");
        Ui.primary(next);
        back.addActionListener(e -> goTo(step - 1));
        next.addActionListener(e -> next());
        cancel.addActionListener(e -> cancel());

        add(indicator, "wrap");
        add(body, "wrap");
        add(reason, "split 4, growx, pushx");
        add(back);
        add(next);
        add(cancel);
        goTo(0);
    }

    /** "Placing order…", the moving bar and "Order for €37.20 · Ada Shop Ltd". */
    private JPanel busyPanel() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JPanel panel = new JPanel(new MigLayout("fill", "[center]", "push[]10[]10[]push"));
        panel.add(new JLabel("Placing order…"), "wrap");
        panel.add(bar, "w 260!, wrap");
        panel.add(placingFor);
        return panel;
    }

    void goTo(int target) {
        step = target;
        if (target == 1) shipping.enter(address.address().country(), cart.totals().discounted());
        if (target == 3) {
            review.show(address.address(), shipping.method(), shipping.note(), payment.payment(), cart.lines(),
                    Pricing.of(cart.lines(), shipping.method()));
        }
        cards.show(body, String.valueOf(target));
        indicator.setCurrent(target);
        back.setEnabled(target > 0);
        next.setText(target == 3 ? "Place order" : "Next");
        updateButtons();
        if (target == 0) address.focusFirst();
        else if (target == 1) shipping.focusFirst();
        else if (target == 2) payment.focusFirst();
        else next.requestFocusInWindow();
    }

    private void updateButtons() {
        String problem = step == 0 ? address.problem() : step == 2 ? payment.problem() : null;
        next.setEnabled(problem == null && !placing);
        reason.setText(problem == null ? "" : problem);
    }

    void next() {
        if (placing || !next.isEnabled()) return;
        if (step == 2 && !creditAccepted()) return;
        if (step < 3) goTo(step + 1);
        else placeOrder();
    }

    /** Invoice orders over the credit limit need a decision: pay by card instead, or continue. */
    private boolean creditAccepted() {
        if (!payment.payment().isInvoice()) return true;
        Money total = Pricing.of(cart.lines(), shipping.method()).total();
        if (!CreditCheck.exceeds(customer, total)) return true;
        int choice = asker.choose("Credit limit", CreditCheck.message(customer, total), CreditCheck.detail(customer, total),
                JOptionPane.WARNING_MESSAGE, "Pay by card instead", "Continue with invoice");
        if (choice == 0) payment.selectCard();
        return choice == 1;
    }

    private void placeOrder() {
        Totals totals = Pricing.of(cart.lines(), shipping.method());
        int units = cart.units();
        int choice = asker.choose("Place order", "Place order for " + totals.total().format() + "?",
                "Order for " + customer.name() + ", " + units + (units == 1 ? " item." : " items."),
                JOptionPane.QUESTION_MESSAGE, "Place order", "Cancel");
        if (choice != 0) return;
        Address deliverTo = address.address();
        Shipping method = shipping.method();
        String note = shipping.note();
        Payment paidBy = payment.payment();
        placing = true;
        back.setEnabled(false);
        next.setEnabled(false);
        cancel.setEnabled(false);
        reason.setText("");
        placingFor.setText("Order for " + totals.total().format() + " · " + customer.name());
        cards.show(body, "busy");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(placeMillis);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed(e.toString());
                    return;
                } catch (ExecutionException e) {
                    failed(String.valueOf(e.getCause()));
                    return;
                }
                Order order;
                try {
                    order = store.place(customer, cart, deliverTo, method, note, paidBy);
                } catch (RuntimeException e) {
                    failed(e.getMessage() != null ? e.getMessage() : e.toString());
                    return;
                }
                onPlaced.accept(order);
            }
        }.execute();
    }

    /** Placing failed: back to the review, usable again. */
    private void failed(String detail) {
        placing = false;
        cancel.setEnabled(true);
        asker.choose("Place order", "The order could not be placed.", detail, JOptionPane.ERROR_MESSAGE, "OK");
        goTo(3);
    }

    void cancel() {
        if (!placing) onCancel.run();
    }

    JButton nextButton() {
        return next;
    }

    // for tests

    JButton backButton() {
        return back;
    }

    int step() {
        return step;
    }

    String reason() {
        return reason.getText();
    }

    String placingText() {
        return placingFor.getText();
    }

    boolean isPlacing() {
        return placing;
    }

    AddressStep addressStep() {
        return address;
    }

    ShippingStep shippingStep() {
        return shipping;
    }

    PaymentStep paymentStep() {
        return payment;
    }

    ReviewStep reviewStep() {
        return review;
    }
}
