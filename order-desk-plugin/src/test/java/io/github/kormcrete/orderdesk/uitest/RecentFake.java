package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.driver.VerbOutcome;
import io.github.kormcrete.swingui.driver.model.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The main window with the toolbar's ▾ offering {@code names}; a pick opens a new order's tab for that customer and
 * selects it (unless {@link #opensNothing}).
 */
class RecentFake extends OrdersFake {
    private boolean opens = true;
    private final List<String> open = new ArrayList<String>(Collections.singletonList("Orders"));

    RecentFake(String... names) {
        answer("button 'New order for a recent customer'", node("button", "New order for a recent customer"));
        Node[] offered = new Node[names.length];
        for (int i = 0; i < names.length; i++) offered[i] = node("menuitem", names[i]);
        answer("popup menuitem", offered);
    }

    RecentFake opensNothing() {
        opens = false;
        return this;
    }

    @Override
    OrdersFake tabs(String selected, String... titles) {
        if (open != null) {
            open.clear();
            open.addAll(Arrays.asList(titles));
        }
        return super.tabs(selected, titles);
    }

    @Override
    public VerbOutcome pick(List<String> path, Long timeout) {
        VerbOutcome out = super.pick(path, timeout);
        if (opens) {
            List<String> now = new ArrayList<String>(open);
            now.add("New order · " + path.get(0));
            tabs(now.get(now.size() - 1), now.toArray(new String[0]));
        }
        return out;
    }
}

