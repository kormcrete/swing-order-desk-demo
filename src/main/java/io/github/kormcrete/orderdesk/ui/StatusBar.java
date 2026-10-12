package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** The message on the left ("Ready", "Order ORD-1640 placed"), the open orders on the right behind a divider. */
final class StatusBar extends JPanel {

    private static final String STYLE = "font: -1; foreground: $OrderDesk.muted";

    private final JLabel message = new JLabel("Ready");
    private final JLabel openOrders = new JLabel("") {
        @Override
        public void updateUI() {
            super.updateUI();
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor(Theme.SOFT_BORDER)),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        }
    };

    StatusBar() {
        super(new MigLayout("insets 3 10 3 0", "[grow]16[]"));
        message.putClientProperty(FlatClientProperties.STYLE, STYLE);
        openOrders.putClientProperty(FlatClientProperties.STYLE, STYLE);
        add(message, "growx, wmin 0");
        add(openOrders, "growy");
    }

    void setMessage(String text) {
        message.setText(text);
    }

    void setOpenOrders(int count) {
        openOrders.setText(count == 1 ? "1 open order" : count + " open orders");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));
    }

    // for tests

    String message() {
        return message.getText();
    }

    String openOrders() {
        return openOrders.getText();
    }
}
