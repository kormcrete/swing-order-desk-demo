package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Store;

import java.awt.Component;
import java.util.List;
import java.util.Locale;

/** The cancel flow shared by the Orders tab and an order's own tab: ask, cancel in the store, say so in the status line. */
final class CancelOrders {

    static final String HINT = "Only placed orders can be cancelled";
    static final String DETAIL = "Stock is restored and invoice totals are taken off what the customer owes.";

    private CancelOrders() {
    }

    /** The reason line next to a disabled cancel action; empty when the order can be cancelled. */
    static String hintFor(Order order) {
        return order.cancellable() ? "" : HINT + " \u2014 this one is " + order.status().label().toLowerCase(Locale.ROOT);
    }

    /** Does nothing unless every order is cancellable and the user confirms. */
    static void run(Component parent, Store store, Workspace workspace, List<Order> orders) {
        if (orders.isEmpty() || !orders.stream().allMatch(Order::cancellable)) return;
        boolean one = orders.size() == 1;
        String message = one ? "Cancel order " + orders.get(0).number() + "?" : "Cancel " + orders.size() + " orders?";
        if (!Dialogs.confirm(parent, one ? "Cancel order" : "Cancel orders", message, DETAIL,
                one ? "Cancel order" : "Cancel orders", "Keep")) {
            return;
        }
        store.cancel(orders);
        workspace.setStatus(one ? "Order " + orders.get(0).number() + " cancelled" : orders.size() + " orders cancelled");
    }
}
