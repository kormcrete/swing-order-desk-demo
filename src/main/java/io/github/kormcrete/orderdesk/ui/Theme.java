package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import io.github.kormcrete.flatlaf.lagoon.FlatLagoonLaf;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.Dimension;

/**
 * FlatLaf Light, Dark or Kormcrete's Lagoon, switched live from View ▸ Theme. {@code FlatLaf.setup} resets every
 * default, so the Order Desk values below are put again on each switch: the app's own colour keys
 * ({@code OrderDesk.*}) that styles refer to as {@code $OrderDesk.muted}, and in every theme roomier tables and
 * controls, and tables with lines between the rows only. Light and Dark also get a grey header and tab strip and a
 * line around an order's tabs; under Lagoon the tabbed pane draws that line itself (its full border).
 */
final class Theme {

    /** Grey secondary text: captions, hints, the status bar. */
    static final String MUTED = "OrderDesk.muted";
    /** The customer's terms card. */
    static final String CARD_BACKGROUND = "OrderDesk.cardBackground";
    /** A soft line: the terms card, the status bar divider, the wizard's step rule. */
    static final String SOFT_BORDER = "OrderDesk.softBorder";
    /** True when an order's tabs get a line around them; under Lagoon their tabbed pane draws its full border. */
    static final String BOXED_TABS = "OrderDesk.boxedTabs";

    private Theme() {
    }

    static void light() {
        apply(new FlatLightLaf());
    }

    static void dark() {
        apply(new FlatDarkLaf());
    }

    /** Not {@code FlatLagoonLaf.setup()}: that puts Lagoon's font into UIManager, where it would outlive a switch. */
    static void lagoon() {
        FlatLaf.setup(new FlatLagoonLaf());
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        colors(defaults, false);
        sizes(defaults);
        FlatLaf.updateUI();
    }

    /**
     * Puts go into the look and feel's own defaults, which the next {@code FlatLaf.setup} replaces: a plain
     * {@code UIManager.put} would outlive the switch and carry light values into the dark theme.
     */
    private static void apply(FlatLaf laf) {
        FlatLaf.setup(laf);
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        boolean dark = laf.isDark();
        colors(defaults, dark);

        sizes(defaults);

        // tables: a grey header with a full-strength line under it
        defaults.put("TableHeader.bottomSeparatorColor", UIManager.getColor("Component.borderColor"));
        if (!dark) defaults.put("TableHeader.background", UIManager.getColor("Panel.background"));   // dark: as the mock

        // tab strip: grey titles, the selected one in text colour over the accent underline
        defaults.put("TabbedPane.inactiveUnderlineColor", UIManager.getColor("TabbedPane.underlineColor"));
        defaults.put("TabbedPane.foreground", UIManager.getColor(MUTED));
        defaults.put("TabbedPane.selectedForeground", UIManager.getColor("Label.foreground"));
        defaults.put(BOXED_TABS, true);
        FlatLaf.updateUI();
    }

    /**
     * Order Desk's room, the same in every theme: 26 px table rows and header, 28 px controls, 24 px tree rows; table
     * lines between the rows only.
     */
    private static void sizes(UIDefaults defaults) {
        defaults.put("Table.showHorizontalLines", true);
        defaults.put("Table.showVerticalLines", false);
        defaults.put("Table.intercellSpacing", new Dimension(0, 1));
        defaults.put("Table.rowHeight", 26);
        defaults.put("Table.cellMargins", new InsetsUIResource(2, 8, 2, 8));
        defaults.put("TableHeader.cellMargins", new InsetsUIResource(2, 8, 2, 8));
        defaults.put("TableHeader.height", 26);

        defaults.put("Button.margin", new InsetsUIResource(5, 14, 5, 14));
        for (String key : new String[] {"TextField.margin", "FormattedTextField.margin", "PasswordField.margin"}) {
            defaults.put(key, new InsetsUIResource(5, 8, 5, 8));
        }
        defaults.put("ComboBox.padding", new InsetsUIResource(5, 8, 5, 8));
        defaults.put("Spinner.padding", new InsetsUIResource(5, 8, 5, 8));

        defaults.put("TabbedPane.tabAreaInsets", new InsetsUIResource(0, 6, 0, 6));
        defaults.put("Tree.rowHeight", 24);
        defaults.put("Tree.border", new FlatEmptyBorder(4, 1, 4, 1));
    }

    private static void colors(UIDefaults defaults, boolean dark) {
        color(defaults, MUTED, dark ? 0x8a8a8a : 0x6e6e6e);
        color(defaults, CARD_BACKGROUND, dark ? 0x404345 : 0xfafafa);
        color(defaults, SOFT_BORDER, dark ? 0x4e5152 : 0xdadada);
    }

    private static void color(UIDefaults defaults, String key, int rgb) {
        defaults.put(key, new ColorUIResource(rgb));
    }
}
