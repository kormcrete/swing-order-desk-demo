package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatLaf;
import io.github.kormcrete.flatlaf.lagoon.FlatLagoonLaf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThemeTest {

    @AfterEach
    void backToLight() {
        Edt.run(Theme::light);
    }

    @Test
    void orderDeskKeysFollowEveryThemeSwitch() {
        Edt.run(() -> {
            Theme.light();
            assertThat(UIManager.getColor(Theme.MUTED)).isEqualTo(new Color(0x6e6e6e));
            assertThat(UIManager.getColor(Theme.CARD_BACKGROUND)).isEqualTo(new Color(0xfafafa));
            assertThat(UIManager.getColor("TableHeader.background")).isEqualTo(UIManager.getColor("Panel.background"));
            assertThat(UIManager.getInt("Table.rowHeight")).isEqualTo(26);

            Theme.dark();
            assertThat(UIManager.getColor(Theme.MUTED)).isEqualTo(new Color(0x8a8a8a));
            assertThat(UIManager.getColor(Theme.CARD_BACKGROUND)).isEqualTo(new Color(0x404345));
            // the light header colour must not outlive the switch
            assertThat(UIManager.getColor("TableHeader.background")).isNotEqualTo(new Color(0xf2f2f2));
            assertThat(UIManager.getInt("Table.rowHeight")).isEqualTo(26);

            Theme.light();
            assertThat(UIManager.getColor(Theme.MUTED)).isEqualTo(new Color(0x6e6e6e));
        });
    }

    @Test
    void shownComponentsTakeTheNewThemesColours() {
        Edt.run(() -> {
            Theme.light();
            JLabel hint = Ui.hint("Select a product first");
            JTable table = Ui.table(new DefaultTableModel(1, 2));
            JPanel panel = new JPanel();
            panel.add(hint);
            panel.add(Ui.scroll(table));
            assertThat(hint.getForeground()).isEqualTo(new Color(0x6e6e6e));   // $OrderDesk.muted resolves

            Theme.dark();
            SwingUtilities.updateComponentTreeUI(panel);   // what FlatLaf.updateUI does for every window
            assertThat(hint.getForeground()).isEqualTo(new Color(0x8a8a8a));
            assertThat(table.getTableHeader().getBackground()).isEqualTo(UIManager.getColor("TableHeader.background"));
            assertThat(table.getTableHeader().getBackground()).isNotEqualTo(new Color(0xf2f2f2));
        });
    }

    @Test
    void lagoonGetsOrderDesksSizesAndKeepsItsOwnStyle() {
        Edt.run(() -> {
            FlatLaf.setup(new FlatLagoonLaf());   // Lagoon as it comes
            Object intercellSpacing = UIManager.get("Table.intercellSpacing");
            Object tabForeground = UIManager.get("TabbedPane.foreground");
            Object headerBackground = UIManager.get("TableHeader.background");

            Theme.lagoon();
            assertThat(UIManager.getLookAndFeel()).isInstanceOf(FlatLagoonLaf.class);
            assertThat(UIManager.getColor(Theme.MUTED)).isEqualTo(new Color(0x6e6e6e));
            assertThat(UIManager.getColor(Theme.CARD_BACKGROUND)).isEqualTo(new Color(0xfafafa));
            assertThat(UIManager.getColor(Theme.SOFT_BORDER)).isEqualTo(new Color(0xdadada));
            // the sizes Light gets: 26 px rows and header, 28 px controls, 24 px tree rows
            assertThat(UIManager.getInt("Table.rowHeight")).isEqualTo(26);
            assertThat(UIManager.getInt("TableHeader.height")).isEqualTo(26);
            assertThat(UIManager.getInsets("Table.cellMargins")).isEqualTo(new Insets(2, 8, 2, 8));
            assertThat(UIManager.getInsets("TableHeader.cellMargins")).isEqualTo(new Insets(2, 8, 2, 8));
            assertThat(UIManager.getInsets("Button.margin")).isEqualTo(new Insets(5, 14, 5, 14));
            assertThat(UIManager.getInsets("TextField.margin")).isEqualTo(new Insets(5, 8, 5, 8));
            assertThat(UIManager.getInsets("ComboBox.padding")).isEqualTo(new Insets(5, 8, 5, 8));
            assertThat(UIManager.getInsets("Spinner.padding")).isEqualTo(new Insets(5, 8, 5, 8));
            assertThat(UIManager.getInt("Tree.rowHeight")).isEqualTo(24);
            assertThat(UIManager.getInsets("TabbedPane.tabAreaInsets")).isEqualTo(new Insets(0, 6, 0, 6));
            // Lagoon's style stays: its grid, tab colours and header colour
            assertThat(UIManager.get("Table.intercellSpacing")).isEqualTo(intercellSpacing);
            assertThat(UIManager.get("TabbedPane.foreground")).isEqualTo(tabForeground);
            assertThat(UIManager.get("TableHeader.background")).isEqualTo(headerBackground);
        });
    }

    @Test
    void lightAndDarkComeBackCleanAfterLagoon() {
        String[] keys = {"defaultFont", "Table.rowHeight", "Button.margin", "TabbedPane.foreground",
                "TabbedPane.tabAreaInsets", "Component.borderColor", "Panel.background", "TableHeader.background"};
        Edt.run(() -> {
            for (Runnable theme : new Runnable[] {Theme::light, Theme::dark}) {
                theme.run();
                Map<String, Object> before = values(keys);
                Theme.lagoon();
                theme.run();
                Theme.lagoon();
                theme.run();
                assertThat(values(keys)).isEqualTo(before);
            }
        });
    }

    @Test
    void eachReviewSectionKeepsItsTitledBorder() {
        Edt.run(() -> {
            ReviewStep review = new ReviewStep(step -> { });
            List<String> titles = new ArrayList<>();
            collectTitles(review, titles);
            // the driver scopes each Edit by its section: section:Delivery address, section:Shipping, section:Payment
            assertThat(titles).containsExactly("Delivery address", "Shipping", "Payment");
        });
    }

    @Test
    void aDialogDetailIsASecondSmallerGreyLine() {
        Edt.run(() -> {
            Theme.light();   // the muted colour exists only once a theme is installed
            assertThat(Dialogs.content("Discard this new order?", null)).isEqualTo("Discard this new order?");
            JPanel panel = (JPanel) Dialogs.content("Place order for €37.20?", "Order for Ada Shop Ltd, 13 items.");
            JLabel message = (JLabel) panel.getComponent(0);
            JLabel detail = (JLabel) panel.getComponent(1);
            assertThat(message.getText()).isEqualTo("Place order for €37.20?");
            assertThat(detail.getText()).isEqualTo("Order for Ada Shop Ltd, 13 items.");
            assertThat(detail.getForeground()).isEqualTo(UIManager.getColor(Theme.MUTED));
            assertThat(detail.getFont().getSize2D()).isLessThan(message.getFont().getSize2D());
        });
    }

    private static Map<String, Object> values(String... keys) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : keys) values.put(key, UIManager.get(key));
        return values;
    }

    private static void collectTitles(Container container, List<String> out) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel && ((JPanel) c).getBorder() instanceof TitledBorder) {
                out.add(((TitledBorder) ((JPanel) c).getBorder()).getTitle());
            }
            if (c instanceof Container) collectTitles((Container) c, out);
        }
    }
}
