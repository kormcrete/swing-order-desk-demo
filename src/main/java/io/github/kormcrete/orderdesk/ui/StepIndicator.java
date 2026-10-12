package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** 1 Address · 2 Shipping · 3 Payment · 4 Review: done steps get a check, the current one is filled and bold. */
final class StepIndicator extends JPanel {

    private final JLabel[] steps;

    StepIndicator(String... titles) {
        super(new MigLayout("insets 0, gap 0", "", "[center]"));
        steps = new JLabel[titles.length];
        for (int i = 0; i < titles.length; i++) {
            steps[i] = new JLabel(titles[i]);
            steps[i].setIconTextGap(UIScale.scale(6));
            add(steps[i]);
            if (i < titles.length - 1) add(new JSeparator(), "growx, pushx, gapleft 10, gapright 10");
        }
        setCurrent(0);
    }

    /** The soft rule under the steps, as in the mock; its colour follows the theme. */
    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(Theme.SOFT_BORDER)),
                BorderFactory.createEmptyBorder(0, 0, 12, 0)));
    }

    void setCurrent(int current) {
        for (int i = 0; i < steps.length; i++) {
            int state = i < current ? StepIcon.DONE : i == current ? StepIcon.CURRENT : StepIcon.TODO;
            steps[i].setIcon(new StepIcon(i + 1, state));
            steps[i].putClientProperty(FlatClientProperties.STYLE,
                    state == StepIcon.CURRENT ? "font: bold" : "foreground: $OrderDesk.muted");
        }
    }

    private static final class StepIcon implements Icon {

        static final int TODO = 0;
        static final int CURRENT = 1;
        static final int DONE = 2;

        private final int number;
        private final int state;

        StepIcon(int number, int state) {
            this.number = number;
            this.state = state;
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(20);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(20);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int size = getIconWidth();
            g2.setStroke(new BasicStroke(UIScale.scale(1.5f)));
            if (state == CURRENT) {
                g2.setColor(UIManager.getColor("Component.accentColor"));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                drawNumber(g2, c, x, y, size);
            } else if (state == DONE) {
                Color green = UIManager.getColor("Actions.Green");
                g2.setColor(green != null ? green : new Color(0x59A869));
                g2.drawOval(x + 1, y + 1, size - 3, size - 3);
                Path2D check = new Path2D.Float();
                check.moveTo(x + size * 0.30, y + size * 0.52);
                check.lineTo(x + size * 0.45, y + size * 0.66);
                check.lineTo(x + size * 0.72, y + size * 0.38);
                g2.draw(check);
            } else {
                g2.setColor(UIManager.getColor("Label.disabledForeground"));
                g2.drawOval(x + 1, y + 1, size - 3, size - 3);
                drawNumber(g2, c, x, y, size);
            }
            g2.dispose();
        }

        private void drawNumber(Graphics2D g2, Component c, int x, int y, int size) {
            g2.setFont(c.getFont().deriveFont(c.getFont().getSize2D() - 1f));
            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(number);
            g2.drawString(text, x + (size - fm.stringWidth(text)) / 2f, y + (size - fm.getHeight()) / 2f + fm.getAscent());
        }
    }
}
