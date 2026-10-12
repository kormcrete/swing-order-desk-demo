package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPanelTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final RecordingWorkspace workspace = new RecordingWorkspace();

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private OrderPanel panel(String number) {
        Order order = store.orders().find(number).get();
        return Edt.call(() -> new OrderPanel(store, workspace, order));
    }

    @Test
    void aShippedOrder() {
        OrderPanel panel = panel("ORD-1636");
        Edt.run(() -> {
            assertThat(panel.statusText()).isEqualTo("Shipped");
            assertThat(panel.factsText()).isEqualTo("Fjord Supplies · 2 Oct 2026 · €164.84");
            assertThat(panel.cancelButton().isEnabled()).isFalse();
            assertThat(panel.cancelHint()).isEqualTo("Only placed orders can be cancelled \u2014 this one is shipped");
            assertThat(panel.details().getTitleAt(0)).isEqualTo("Items");
            assertThat(panel.details().getTitleAt(1)).isEqualTo("Delivery");
            assertThat(panel.details().getTitleAt(2)).isEqualTo("Invoice");
            assertThat(panel.itemsTable().getRowCount()).isEqualTo(2);
        });
    }

    @Test
    void followsTheStoreWhileShown() {
        OrderPanel panel = panel("ORD-1639");
        Edt.run(() -> {
            panel.addNotify();   // what adding the tab to the window does
            assertThat(panel.cancelButton().isEnabled()).isTrue();
            assertThat(panel.cancelHint()).isEmpty();
            store.cancel(Collections.singletonList(panel.order()));
            assertThat(panel.statusText()).isEqualTo("Cancelled");
            assertThat(panel.cancelButton().isEnabled()).isFalse();
            assertThat(panel.cancelHint()).isEqualTo("Only placed orders can be cancelled \u2014 this one is cancelled");
            panel.removeNotify();
        });
    }

    @Test
    void stopsFollowingWhenClosedAndCatchesUpWhenShownAgain() {
        OrderPanel panel = panel("ORD-1638");
        Edt.run(() -> {
            panel.addNotify();
            panel.removeNotify();
            store.cancel(Collections.singletonList(panel.order()));
            assertThat(panel.statusText()).isEqualTo("Placed");
            panel.addNotify();
            assertThat(panel.statusText()).isEqualTo("Cancelled");
            panel.removeNotify();
        });
    }
}
