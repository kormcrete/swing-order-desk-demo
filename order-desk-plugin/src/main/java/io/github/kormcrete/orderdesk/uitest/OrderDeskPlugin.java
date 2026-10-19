package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.junit.Client;
import io.github.kormcrete.swingui.junit.ClientApp;
import io.github.kormcrete.swingui.spi.PluginContext;
import io.github.kormcrete.swingui.spi.PluginParts;
import io.github.kormcrete.swingui.spi.UiPlugin;

/**
 * Order Desk's swing-ui-agent plugin: the app plugin ({@link OrderDeskApp}: a session reset closes every order tab and
 * shows the Orders list, so tests run one after another on one app), what {@code describe_ui} reports
 * ({@link OrderDeskInventory}) and the vocabulary ({@link OrderDeskVocabulary}).
 * Start the app with
 * {@code -javaagent:target/swing-ui-agent-agent.jar=port=8770,plugins=order-desk-plugin/target/classes}.
 */
public final class OrderDeskPlugin implements UiPlugin {

    @Override
    public PluginParts start(PluginContext context) {
        Client live = new Client(context.driver().verbs(), new ReadsOnly(), "order-desk-plugin");
        OrderDeskApp app = new OrderDeskApp(context.driver().verbs());
        OrderDeskInventory inventory = new OrderDeskInventory(context.driver().windows()::main);
        return new PluginParts(app, inventory, app, null, null, null, new OrderDeskVocabulary(live));
    }

    /** The choices only read the app: no app steps, logins or hooks. */
    private static final class ReadsOnly implements ClientApp {
        @Override public void navigate(String target) { throw refused("navigate"); }
        @Override public void closeActiveTab() { throw refused("close"); }
        @Override public void loginAs(String alias) { throw refused("loginAs"); }
        @Override public void loginAs(String user, String password) { throw refused("loginAs"); }
        @Override public void relogin() { throw refused("relogin"); }
        @Override public <T> T hooks(Class<T> type) { throw refused("hooks"); }

        private static UnsupportedOperationException refused(String what) {
            return new UnsupportedOperationException("the plugin's choices only read the app: no " + what);
        }
    }
}
