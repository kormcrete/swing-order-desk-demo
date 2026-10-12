package io.github.kormcrete.orderdesk.ui;

import javax.swing.SwingUtilities;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/** Runs test code on the event dispatch thread, like the app does, and rethrows what it threw. */
final class Edt {

    private Edt() {
    }

    static <T> T call(Callable<T> task) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(task.call());
                } catch (Throwable t) {
                    failure.set(t);
                }
            });
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        Throwable t = failure.get();
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
        if (t != null) throw new AssertionError(t);
        return result.get();
    }

    static void run(Runnable task) {
        call(() -> {
            task.run();
            return null;
        });
    }
}
