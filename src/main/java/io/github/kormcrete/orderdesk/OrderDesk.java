package io.github.kormcrete.orderdesk;

import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import io.github.kormcrete.orderdesk.model.Today;
import io.github.kormcrete.orderdesk.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.function.Supplier;

/** Starts Order Desk. {@code -Dorderdesk.today=2026-10-12} fixes today's date. */
public final class OrderDesk {

    private OrderDesk() {
    }

    public static void main(String[] args) {
        // macOS: Quit (Cmd+Q) closes the window the normal way, so an unsaved cart still asks first
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        Supplier<LocalDate> today;
        try {
            today = Today.fromSystemProperty();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        Store store = Seed.store(today);
        SwingUtilities.invokeLater(() -> MainWindow.show(store));
    }
}
