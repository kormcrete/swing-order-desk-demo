package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.Store;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.BiConsumer;

/** The Order Desk window: menus, toolbar, workspace tabs (Orders is pinned) and the status bar. */
public final class MainWindow extends JFrame implements Workspace {

    private final Store store;
    private final JTabbedPane tabs = new JTabbedPane();
    private final StatusBar statusBar = new StatusBar();
    private final JMenuItem checkoutItem = new JMenuItem("Checkout…");
    private final JMenuItem removeItem = new JMenuItem("Remove from cart");
    private final JMenuItem closeTabItem = new JMenuItem("Close tab");

    private MainWindow(Store store) {
        super("Order Desk");
        setIconImages(AppIcon.images());
        this.store = store;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        setJMenuBar(menuBar());

        OrdersPanel orders = new OrdersPanel(store, this);
        orders.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, false);
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close tab");
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (pane, index) -> closeTab(index));
        tabs.addTab("Orders", orders);
        tabs.addChangeListener(e -> updateActions());

        JPanel content = new JPanel(new BorderLayout());
        content.add(toolbar(), BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setContentPane(content);

        store.addListener(this::refreshCounts);
        refreshCounts();
        updateActions();
        setMinimumSize(new Dimension(1024, 680));
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    /**
     * Light theme, then the window, shown once the k has built itself on the splash (at once without a splash). Call
     * on the event dispatch thread.
     */
    public static void show(Store store) {
        Splash.play();
        Theme.light();
        MainWindow window = new MainWindow(store);
        Splash.whenDone(() -> window.setVisible(true));
    }

    private JComponent toolbar() {
        JButton newOrder = new JButton("New order", Icons.get(Icons.PLUS));
        newOrder.addActionListener(e -> newOrder(null));
        JButton recent = new JButton(Icons.get(Icons.CHEVRON_DOWN));
        recent.setToolTipText("New order for a recent customer");
        recent.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (Customer customer : store.orders().recentCustomers(4)) {
                JMenuItem item = new JMenuItem(customer.name());
                item.addActionListener(ev -> newOrder(customer));
                menu.add(item);
            }
            menu.show(recent, 0, recent.getHeight());
        });
        JPanel bar = new JPanel(new MigLayout("insets 6 8 6 8, gap 0"));
        bar.add(newOrder);
        bar.add(recent, "gapleft 1");
        return bar;
    }

    private JMenuBar menuBar() {
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(item(new JMenuItem("New order"), KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcut), () -> newOrder(null)));
        file.add(item(closeTabItem, KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcut), this::closeSelectedTab));
        file.addSeparator();
        file.add(item(new JMenuItem("Exit"), null, this::exit));

        JMenu order = new JMenu("Order");
        order.setMnemonic(KeyEvent.VK_O);
        order.add(item(checkoutItem, null, () -> currentNewOrder().ifPresent(NewOrderPanel::checkout)));
        order.add(item(removeItem, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), () -> currentNewOrder().ifPresent(NewOrderPanel::removeSelectedLinesFromMenu)));

        // the accelerator path is kept fresh by actionsChanged(); opening the menu refreshes it too
        order.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                updateActions();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        JMenu theme = new JMenu("Theme");
        JRadioButtonMenuItem light = new JRadioButtonMenuItem("Light", true);
        JRadioButtonMenuItem dark = new JRadioButtonMenuItem("Dark");
        JRadioButtonMenuItem lagoon = new JRadioButtonMenuItem("Lagoon");
        ButtonGroup themes = new ButtonGroup();
        themes.add(light);
        themes.add(dark);
        themes.add(lagoon);
        light.addActionListener(e -> Theme.light());
        dark.addActionListener(e -> Theme.dark());
        lagoon.addActionListener(e -> Theme.lagoon());
        theme.add(light);
        theme.add(dark);
        theme.add(lagoon);
        view.add(theme);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        help.add(item(new JMenuItem("About Order Desk"), null, () -> Dialogs.info(this, "About Order Desk",
                "Order Desk\nA small, fictional order-entry app: the demo for swing-ui-agent.\n"
                        + "Built with Java 8, FlatLaf, the Lagoon theme and MigLayout. All customers, products and orders are invented.")));

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(order);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    private static JMenuItem item(JMenuItem item, KeyStroke accelerator, Runnable action) {
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(e -> action.run());
        return item;
    }

    private Optional<NewOrderPanel> currentNewOrder() {
        Component selected = tabs.getSelectedComponent();
        return selected instanceof NewOrderPanel ? Optional.of((NewOrderPanel) selected) : Optional.empty();
    }

    private void updateActions() {
        Optional<NewOrderPanel> current = currentNewOrder();
        checkoutItem.setEnabled(current.map(NewOrderPanel::canCheckout).orElse(false));
        removeItem.setEnabled(current.map(NewOrderPanel::hasSelectedLines).orElse(false));
        closeTabItem.setEnabled(tabs.getSelectedIndex() > 0);
    }

    private void refreshCounts() {
        statusBar.setOpenOrders(store.orders().openCount());
    }

    @Override
    public void setStatus(String message) {
        statusBar.setMessage(message);
    }

    @Override
    public void retitle(Component tab, String title) {
        int index = tabs.indexOfComponent(tab);
        if (index >= 0) tabs.setTitleAt(index, title);
    }

    @Override
    public void actionsChanged() {
        updateActions();
    }

    /** Opens a New order tab; {@code customer} may be null. */
    private void newOrder(Customer customer) {
        NewOrderPanel panel = new NewOrderPanel(store, this);
        tabs.addTab("New order", panel);
        tabs.setSelectedComponent(panel);
        if (customer != null) panel.chooseCustomer(customer);
        panel.focusFirstField();
        updateActions();
    }

    @Override
    public void openOrder(Order order) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component tab = tabs.getComponentAt(i);
            if (tab instanceof OrderPanel && ((OrderPanel) tab).order() == order) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
        OrderPanel panel = new OrderPanel(store, this, order);
        tabs.addTab(order.number(), panel);
        tabs.setSelectedComponent(panel);
    }

    @Override
    public void orderPlaced(Component newOrderTab, Order order) {
        int index = tabs.indexOfComponent(newOrderTab);
        if (index < 0) return;
        tabs.setComponentAt(index, new OrderPanel(store, this, order));
        tabs.setTitleAt(index, order.number());
        tabs.setSelectedIndex(index);
        setStatus("Order " + order.number() + " placed");
        updateActions();
    }

    /** The Orders tab (index 0) stays; a New order tab with something in its cart asks first. */
    private void closeTab(int index) {
        if (index <= 0) return;
        if (asksBeforeClosing(tabs.getComponentAt(index))
                && !Dialogs.confirm(this, "Close tab", "Discard this new order?", null, "Discard", "Cancel")) {
            return;
        }
        tabs.removeTabAt(index);
        updateActions();
    }

    /** Only a New order tab whose cart is not empty has something to lose. */
    static boolean asksBeforeClosing(Component tab) {
        return tab instanceof NewOrderPanel && ((NewOrderPanel) tab).hasUnsavedCart();
    }

    private void closeSelectedTab() {
        closeTab(tabs.getSelectedIndex());
    }

    /** Asks only when a New order tab has something in its cart. */
    private void exit() {
        boolean unsaved = false;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component tab = tabs.getComponentAt(i);
            if (asksBeforeClosing(tab)) unsaved = true;
        }
        if (unsaved && !Dialogs.confirm(this, "Exit", "Discard the open new orders?", null, "Discard and exit", "Cancel")) {
            return;
        }
        dispose();
        System.exit(0);
    }
}
