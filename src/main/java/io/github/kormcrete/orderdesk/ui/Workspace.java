package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;

import java.awt.Component;

/** What a tab may ask of the main window. */
interface Workspace {

    /** The status bar's message. */
    void setStatus(String message);

    void retitle(Component tab, String title);

    /** Something changed that the Order menu depends on. */
    void actionsChanged();

    /** Opens the order's tab, or selects it when it is open. */
    void openOrder(Order order);

    /** The New order tab turns into the placed order's tab. */
    void orderPlaced(Component newOrderTab, Order order);
}
