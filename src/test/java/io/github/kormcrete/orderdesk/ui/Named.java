package io.github.kormcrete.orderdesk.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds a component by its code name ({@code setName}), the way a plugin's {@code id} locator reaches it: exactly one
 * component by that name, or the test fails naming how many there were.
 */
final class Named {

    private Named() {
    }

    /** The one component in {@code root}'s tree named {@code name}, shown or not. */
    static <T extends Component> T find(Container root, String name, Class<T> type) {
        return only(all(root, name, false), name, type);
    }

    /** The one component named {@code name} that is visible up to {@code root} (a hidden card or tab page is not). */
    static <T extends Component> T shown(Container root, String name, Class<T> type) {
        return only(all(root, name, true), name, type);
    }

    /** How many components named {@code name} are visible up to {@code root}. */
    static int countShown(Container root, String name) {
        return all(root, name, true).size();
    }

    private static <T extends Component> T only(List<Component> found, String name, Class<T> type) {
        if (found.size() != 1) throw new AssertionError(found.size() + " components named '" + name + "'");
        return type.cast(found.get(0));
    }

    private static List<Component> all(Container root, String name, boolean visibleOnly) {
        List<Component> out = new ArrayList<>();
        collect(root, name, visibleOnly, out);
        return out;
    }

    private static void collect(Container parent, String name, boolean visibleOnly, List<Component> out) {
        for (Component c : parent.getComponents()) {
            if (visibleOnly && !c.isVisible()) continue;
            if (name.equals(c.getName())) out.add(c);
            if (c instanceof Container) collect((Container) c, name, visibleOnly, out);
        }
    }
}
