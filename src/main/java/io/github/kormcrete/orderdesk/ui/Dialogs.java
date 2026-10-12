package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;

/** Modal questions and messages with buttons that say what they do. */
final class Dialogs {

    private Dialogs() {
    }

    /** The index of the chosen option, or -1 when the dialog was closed. The first option is the default. */
    static int choose(Component parent, String title, String message, String detail, int messageType, String... options) {
        return JOptionPane.showOptionDialog(parent, content(message, detail), title, JOptionPane.DEFAULT_OPTION, messageType, null,
                options, options[0]);
    }

    /** The message, and under it the detail in smaller grey text, as in the mock. */
    static Object content(String message, String detail) {
        if (detail == null) return message;
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 0, wrap 1", "[]", "[]4[]"));
        panel.setOpaque(false);
        panel.add(new JLabel(message));
        JLabel sub = new JLabel(detail);
        sub.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $OrderDesk.muted");
        panel.add(sub);
        return panel;
    }

    static boolean confirm(Component parent, String title, String message, String detail, String yes, String no) {
        return choose(parent, title, message, detail, JOptionPane.QUESTION_MESSAGE, yes, no) == 0;
    }

    static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
