package io.github.kormcrete.orderdesk.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusBarTest {

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    @Test
    void messageAndOpenOrders() {
        Edt.run(() -> {
            StatusBar bar = new StatusBar();
            assertThat(bar.message()).isEqualTo("Ready");
            bar.setMessage("Order ORD-1640 placed");
            bar.setOpenOrders(1);
            assertThat(bar.message()).isEqualTo("Order ORD-1640 placed");
            assertThat(bar.openOrders()).isEqualTo("1 open order");
            bar.setOpenOrders(4);
            assertThat(bar.openOrders()).isEqualTo("4 open orders");
        });
    }
}
