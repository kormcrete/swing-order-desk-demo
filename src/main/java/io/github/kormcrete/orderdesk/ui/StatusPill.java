package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatLaf;
import io.github.kormcrete.orderdesk.model.OrderStatus;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** An order status as a coloured pill: Placed (blue), Shipped (green), Cancelled (grey). */
class StatusPill extends JLabel {

    private OrderStatus status;
    private boolean onSelection;

    StatusPill() {
        this(14, 6);
    }

    /** {@code left} and {@code right} are the label's insets; the pill reaches 8 px into each. */
    StatusPill(int left, int right) {
        setBorder(BorderFactory.createEmptyBorder(0, left, 0, right));
    }

    /** The mock's 11.5 px pill text next to 13 px body text. */
    static Font pillFont(Font base) {
        return base.deriveFont(base.getSize2D() - 1.5f);
    }

    void setStatus(OrderStatus status, boolean onSelection) {
        this.status = status;
        this.onSelection = onSelection;
        setText(status == null ? "" : status.label());
        if (status != null) setForeground(colors(status, onSelection)[1]);
    }

    OrderStatus status() {
        return status;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setFont(pillFont(UIManager.getFont("Label.font")));
        if (status != null) setForeground(colors(status, onSelection)[1]);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (status != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int height = fm.getHeight() + 4;
            int width = fm.stringWidth(getText()) + 16;
            g2.setColor(colors(status, onSelection)[0]);
            g2.fillRoundRect(getInsets().left - 8, (getHeight() - height) / 2, width, height, height, height);
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /** {background, text}. On a selected row the pill is a white tint. */
    static Color[] colors(OrderStatus status, boolean onSelection) {
        if (onSelection) return new Color[] {new Color(255, 255, 255, 60), Color.WHITE};
        boolean dark = FlatLaf.isLafDark();
        switch (status) {
            case PLACED:
                return dark ? pair(0x2f4466, 0xa9c7f0) : pair(0xdbe8f6, 0x1f5fa8);
            case SHIPPED:
                return dark ? pair(0x2e4a35, 0x9fd4aa) : pair(0xdcf0e1, 0x2a7a3b);
            default:
                return dark ? pair(0x4a4c4e, 0xa0a0a0) : pair(0xe8e8e8, 0x6b6b6b);
        }
    }

    private static Color[] pair(int background, int text) {
        return new Color[] {new Color(background), new Color(text)};
    }
}
