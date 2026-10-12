package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Cart;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Payment;
import io.github.kormcrete.orderdesk.model.Product;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Shipping;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class NewOrderPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final RecordingWorkspace workspace = new RecordingWorkspace();
    private final NewOrderPanel panel = Edt.call(() -> new NewOrderPanel(store, workspace));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private Customer customer(String name) {
        return store.findCustomers(name).get(0);
    }

    private Product product(String sku) {
        return store.catalog().find(sku).get();
    }

    @Test
    void checkoutReasonsInOrder() {
        Edt.run(() -> {
            assertThat(panel.checkoutBlockReason()).isEqualTo("Choose a customer first");
            panel.chooseCustomer(customer("Ada"));
            assertThat(workspace.titles.get(panel)).isEqualTo("New order · Ada Shop Ltd");
            assertThat(panel.termsText()).isEqualTo("Account · Invoice 30 days · Credit limit €2,000.00 · Owes €450.00 · PO number required");
            assertThat(panel.checkoutBlockReason()).isEqualTo("The cart is empty");
            panel.addToCart(Collections.singletonList(product("DSK-STAPLER")));
            assertThat(panel.checkoutBlockReason()).isNull();
            assertThat(panel.canCheckout()).isTrue();
            panel.cartPanel().table().setValueAt(5, 0, 1);
            assertThat(panel.checkoutBlockReason()).isEqualTo("Fix quantities above stock");
            assertThat(panel.cartPanel().checkoutHint()).isEqualTo("Fix quantities above stock");
            assertThat(panel.hasUnsavedCart()).isTrue();
            panel.chooseCustomer(null);
            assertThat(workspace.titles.get(panel)).isEqualTo("New order");
            assertThat(panel.termsText()).isEmpty();
        });
    }

    @Test
    void addingTheSameProductTwiceAddsToTheLine() {
        Edt.run(() -> {
            panel.addToCart(Collections.singletonList(product("PAP-A4-500")));
            panel.addToCart(Collections.singletonList(product("PAP-A4-500")));
            assertThat(panel.cart().lines()).hasSize(1);
            assertThat(panel.cart().lines().get(0).quantity()).isEqualTo(2);
            assertThat(workspace.actionsChanged).isPositive();
        });
    }

    @Test
    void stockPlacedElsewhereShowsUpInTheCart() {
        Edt.run(() -> {
            panel.addNotify();   // what adding the tab to the window does
            panel.chooseCustomer(customer("Fjord"));
            panel.addToCart(Collections.singletonList(product("DSK-STAPLER")));
            panel.cartPanel().table().setValueAt(4, 0, 1);
            assertThat(panel.checkoutBlockReason()).isNull();

            Cart other = new Cart();
            other.add(product("DSK-STAPLER"), 1);
            Customer fjord = customer("Fjord");
            store.place(fjord, other, fjord.address(), Shipping.STANDARD, "", Payment.card("4242424242424242"));

            assertThat(panel.checkoutBlockReason()).isEqualTo("Fix quantities above stock");
            assertThat(panel.cartPanel().stockMessage()).isEqualTo("Only 3 Staplers in stock");
            panel.removeNotify();
        });
    }

    @Test
    void aClosedTabNoLongerFollowsTheStoreAndCatchesUpWhenShownAgain() {
        Edt.run(() -> {
            panel.addNotify();
            panel.removeNotify();
            panel.chooseCustomer(customer("Fjord"));
            panel.addToCart(Collections.singletonList(product("DSK-STAPLER")));
            panel.cartPanel().table().setValueAt(4, 0, 1);

            Cart other = new Cart();
            other.add(product("DSK-STAPLER"), 1);
            Customer fjord = customer("Fjord");
            store.place(fjord, other, fjord.address(), Shipping.STANDARD, "", Payment.card("4242424242424242"));
            assertThat(panel.cartPanel().stockMessage()).isNotEqualTo("Only 3 Staplers in stock");

            panel.addNotify();
            assertThat(panel.cartPanel().stockMessage()).isEqualTo("Only 3 Staplers in stock");
            assertThat(panel.checkoutBlockReason()).isEqualTo("Fix quantities above stock");
            panel.removeNotify();
        });
    }

    @Test
    void closingTheTabAsksOnlyWhenTheCartHasSomething() {
        Edt.run(() -> {
            assertThat(MainWindow.asksBeforeClosing(panel)).isFalse();
            panel.addToCart(Collections.singletonList(product("PAP-A4-500")));
            assertThat(MainWindow.asksBeforeClosing(panel)).isTrue();
            assertThat(MainWindow.asksBeforeClosing(new OrderPanel(store, workspace, store.orders().find("ORD-1639").get())))
                    .isFalse();
        });
    }

    @Test
    void selectingACartLineTellsTheWorkspace() {
        Edt.run(() -> {
            panel.addToCart(Collections.singletonList(product("PAP-A4-500")));
            assertThat(panel.hasSelectedLines()).isFalse();
            int before = workspace.actionsChanged;
            panel.cartPanel().table().setRowSelectionInterval(0, 0);
            assertThat(panel.hasSelectedLines()).isTrue();
            assertThat(workspace.actionsChanged).isGreaterThan(before);
            before = workspace.actionsChanged;
            panel.cartPanel().table().clearSelection();
            assertThat(panel.hasSelectedLines()).isFalse();
            assertThat(workspace.actionsChanged).isGreaterThan(before);
        });
    }

    @Test
    void backspaceAcceleratorActsOnlyWithFocusInTheCart() {
        Edt.run(() -> {
            panel.addToCart(Collections.singletonList(product("PAP-A4-500"))); // the table is shown once the cart has a line
            CartPanel cartPanel = panel.cartPanel();
            assertThat(NewOrderPanel.focusInCart(cartPanel.table(), cartPanel)).isTrue();
            assertThat(NewOrderPanel.focusInCart(panel.productsPanel().table(), cartPanel)).isFalse();
            assertThat(NewOrderPanel.focusInCart(new javax.swing.JTree(), cartPanel)).isFalse();
            assertThat(NewOrderPanel.focusInCart(null, cartPanel)).isFalse();
        });
    }
}
