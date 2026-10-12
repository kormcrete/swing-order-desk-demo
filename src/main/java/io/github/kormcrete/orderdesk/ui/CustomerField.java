package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Store;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Customer field: type a name or city, pick a suggestion with the mouse or the arrow keys and Enter. Editing the
 * text after choosing forgets the customer until one is picked again.
 */
final class CustomerField extends JTextField {

    private final Store store;
    private final Consumer<Customer> onChoose;
    private final DefaultListModel<Customer> matches = new DefaultListModel<>();
    private final JList<Customer> list = new JList<>(matches);
    private final JScrollPane listScroll = new JScrollPane(list);
    private final JLabel noMatch = Ui.hint("No customer matches");
    private final JPopupMenu popup = new JPopupMenu();
    private Customer chosen;
    private boolean setting;

    CustomerField(Store store, Consumer<Customer> onChoose) {
        super(22);
        this.store = store;
        this.onChoose = onChoose;
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Type a name or city");
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new SuggestionRenderer());
        list.setVisibleRowCount(6);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        noMatch.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        popup.setFocusable(false);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) choose(matches.get(index));
            }
        });

        Ui.onTextChange(this, this::textChanged);
        addActionListener(e -> chooseHighlighted());
        bind(KeyEvent.VK_DOWN, "suggestion-next", () -> move(1));
        bind(KeyEvent.VK_UP, "suggestion-previous", () -> move(-1));
        bind(KeyEvent.VK_ESCAPE, "suggestions-hide", () -> popup.setVisible(false));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });
    }

    private void bind(int key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, Ui.action(action));
    }

    void choose(Customer customer) {
        chosen = customer;
        setting = true;
        try {
            setText(customer == null ? "" : customer.name());
        } finally {
            setting = false;
        }
        matches.clear();
        popup.setVisible(false);
        onChoose.accept(customer);
    }

    Customer chosen() {
        return chosen;
    }

    List<Customer> suggestions() {
        return Collections.list(matches.elements());
    }

    private void textChanged() {
        if (setting) return;
        if (chosen != null) {
            chosen = null;
            onChoose.accept(null);
        }
        String query = getText().trim();
        if (query.isEmpty()) {
            matches.clear();
            popup.setVisible(false);
            return;
        }
        suggest(query);
    }

    private void suggest(String query) {
        matches.clear();
        for (Customer customer : store.findCustomers(query)) matches.addElement(customer);
        if (!matches.isEmpty()) list.setSelectedIndex(0);
        popup.removeAll();
        popup.add(matches.isEmpty() ? noMatch : listScroll);
        if (!isShowing()) return;
        popup.pack();
        popup.setPopupSize(Math.max(getWidth(), UIScale.scale(320)), popup.getPreferredSize().height);
        popup.show(this, 0, getHeight());
    }

    /** Down opens all suggestions when none are showing. */
    private void move(int delta) {
        if (!popup.isVisible()) {
            suggest(getText().trim());
            return;
        }
        if (matches.isEmpty()) return;
        int index = Math.max(0, Math.min(matches.size() - 1, list.getSelectedIndex() + delta));
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    private void chooseHighlighted() {
        if (popup.isVisible() && list.getSelectedValue() != null) choose(list.getSelectedValue());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (popup != null) {
            SwingUtilities.updateComponentTreeUI(popup);
            SwingUtilities.updateComponentTreeUI(listScroll);
            SwingUtilities.updateComponentTreeUI(noMatch);
        }
    }

    /** Name on the left, "Berlin · Account" on the right. */
    private static final class SuggestionRenderer extends JPanel implements ListCellRenderer<Customer> {

        private final JLabel name = new JLabel();
        private final JLabel detail = new JLabel();

        SuggestionRenderer() {
            super(new BorderLayout(16, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            detail.putClientProperty(FlatClientProperties.STYLE, "font: -1");
            add(name, BorderLayout.WEST);
            add(detail, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Customer> list, Customer value, int index,
                                                      boolean selected, boolean focused) {
            name.setText(value.name());
            detail.setText(value.suggestionDetail());
            setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            Color text = selected ? list.getSelectionForeground() : list.getForeground();
            name.setForeground(text);
            detail.setForeground(selected ? text : UIManager.getColor(Theme.MUTED));
            return this;
        }
    }
}
