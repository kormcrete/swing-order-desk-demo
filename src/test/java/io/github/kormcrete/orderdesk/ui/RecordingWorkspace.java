package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A Workspace that only remembers what the panels asked of it. */
final class RecordingWorkspace implements Workspace {

    final List<String> statuses = new ArrayList<>();
    final Map<Component, String> titles = new HashMap<>();
    final List<Order> opened = new ArrayList<>();
    int actionsChanged;

    @Override
    public void setStatus(String message) {
        statuses.add(message);
    }

    @Override
    public void retitle(Component tab, String title) {
        titles.put(tab, title);
    }

    @Override
    public void actionsChanged() {
        actionsChanged++;
    }

    @Override
    public void openOrder(Order order) {
        opened.add(order);
    }

    @Override
    public void orderPlaced(Component newOrderTab, Order order) {
    }
}
