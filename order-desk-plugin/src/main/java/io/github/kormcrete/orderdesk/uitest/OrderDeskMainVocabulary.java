package io.github.kormcrete.orderdesk.uitest;

import io.github.kormcrete.swingui.spi.DomainStep;
import io.github.kormcrete.swingui.spi.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.ABOUT_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CANCEL_HINT;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.CLOSE_TAB_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.DISCARD_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.EXIT_ITEM;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.FILE_MENU;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.HELP_MENU;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.OK_ANSWER;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.OPEN_ORDERS_LABEL;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.STATUS_MESSAGE_ID;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.THEME_MENU;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskSteps.VIEW_MENU;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.json;
import static io.github.kormcrete.orderdesk.uitest.OrderDeskVocabulary.text;

/**
 * The main window's steps and checks: its tabs, its menus' items, the status bar, the questions File ▸ Close tab and
 * File ▸ Exit ask, Help ▸ About Order Desk, and View ▸ Theme. Each body calls {@link OrderDeskMainSteps}. No step ends the app.
 *
 * <p>Every body looks at the app first, so each says its {@code match}. A tab click or a tab check also fits an order's
 * own Items, Delivery and Invoice, which the bodies take too; a click on one of two tabs with the same title cannot be
 * told apart in a recording (the step takes the newest), so it only suggests itself, as do the checks a recording holds
 * as one item where the step reads several, and the menu and message checks a person's clicks cannot pin down.
 */
final class OrderDeskMainVocabulary {

    private static final String STEPS = OrderDeskMainSteps.class.getName();
    private static final String NEEDS_MAIN = "Needs: the main window, no dialog open.";
    private static final String NEEDS_CART = "Needs: a new order with something in its cart showing (without one, File ▸"
            + " Exit would end the app: the step then fails and chooses nothing).";

    private final Supplier<List<String>> tabs;
    private final Supplier<List<String>> menuItems;

    OrderDeskMainVocabulary(Supplier<List<String>> tabs, Supplier<List<String>> menuItems) {
        this.tabs = tabs;
        this.menuItems = menuItems;
    }

    List<DomainStep> steps() {
        String closeTab = json("{'action':'menu','path':[%s,%s]}", text(FILE_MENU), text(CLOSE_TAB_ITEM));
        String exit = json("{'action':'menu','path':[%s,%s]}", text(FILE_MENU), text(EXIT_ITEM));
        String cancel = json("{'action':'answer','button':%s}", text(CANCEL_ANSWER));
        return Arrays.asList(
                // ---- tabs
                DomainStep.act("selectTab", "Switch to the tab {tab}")
                        .about(NEEDS_MAIN + " Ends: on that tab. {tab}: its title or the start of a word of it, any case"
                                + " (\"Orders\", \"ORD-1640\", \"Ada\" for \"New order · Ada Shop Ltd\"), or while an order's"
                                + " own tab shows its Items, Delivery or Invoice. Two new orders for one customer share a"
                                + " title: it takes the newest (the rightmost)")
                        .param("tab", ParamType.TEXT).choices(tabs)
                        .code((ui, p) -> OrderDeskMainSteps.selectTab(ui, p.text("tab")))
                        .match(json("{'action':'click','target':{'role':'tab','name':'{tab}'}}"))
                        .autoFold(false)
                        .java(STEPS, "selectTab(ui, {tab})")
                        .build(),
                DomainStep.act("closeTab", "Close the selected tab")
                        .about(NEEDS_MAIN + " Ends: the tab gone (another one selected). File ▸ Close tab; a new order with"
                                + " something in its cart asks \"Discard this new order?\", answered Discard. The Orders tab"
                                + " never closes: it fails saying so")
                        .code((ui, p) -> OrderDeskMainSteps.closeTab(ui))
                        .match(closeTab, json("{'optional':true,'action':'answer','button':%s}", text(DISCARD_ANSWER)))
                        .java(STEPS, "closeTab(ui)")
                        .build(),
                DomainStep.act("keepTab", "Ask to close the selected tab, then cancel")
                        .about("Needs: a new order with something in its cart showing, no dialog open. Ends: on it as it"
                                + " was, after answering Cancel to File ▸ Close tab's \"Discard this new order?\"")
                        .code((ui, p) -> OrderDeskMainSteps.keepTab(ui))
                        .match(closeTab, cancel)
                        .java(STEPS, "keepTab(ui)")
                        .build(),
                DomainStep.check("openTabsAre", "The open tabs are {tabs}")
                        .about("Reads the open tabs' titles, left to right, parted by ; (\"Orders; New order · Ada Shop"
                                + " Ltd; ORD-1640\"), any case")
                        .param("tabs", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskMainSteps.openTabsAre(ui, p.text("tabs")))
                        .match(json("{'action':'expect','target':{'role':'tab','name':'{tabs}'},'shown':true}"))
                        .autoFold(false)
                        .java(STEPS, "openTabsAre(ui, {tabs})")
                        .build(),
                DomainStep.check("selectedTabIs", "The selected tab is {tab}")
                        .about("Reads which tab is selected, by its whole title, any case (\"Orders\", \"ORD-1640\"); an"
                                + " order's own Items, Delivery or Invoice counts while that order's tab is selected")
                        .param("tab", ParamType.TEXT).choices(tabs)
                        .code((ui, p) -> OrderDeskMainSteps.selectedTabIs(ui, p.text("tab")))
                        .match(json("{'action':'expect','target':{'role':'tab','name':'{tab}'},'state':['selected']}"))
                        .java(STEPS, "selectedTabIs(ui, {tab})")
                        .build(),

                // ---- menus and the status bar
                DomainStep.check("menuItemIs", "The menu item {item} is {state}")
                        .about("Reads whether a menu item is enabled or disabled. The menu bar's (\"Close tab\" or \"File >"
                                + " Close tab\", \"Checkout…\", \"Remove from cart\", \"About Order Desk\"): opens its menu"
                                + " and closes it again; Close tab is disabled on the Orders tab, Checkout… and Remove from"
                                + " cart while the selected new order cannot check out or has no cart line selected. The"
                                + " Orders list's row menu (Open, Cancel orders…, Copy order number): read on the selected"
                                + " orders (Needs: orders selected on the Orders tab); Cancel orders… is disabled unless"
                                + " every selected order is Placed, and then \"" + CANCEL_HINT + "\" shows beside the filter"
                                + " (checked too). {state}: enabled or disabled. Needs: no dialog open")
                        .param("item", ParamType.TEXT).choices(menuItems)
                        .param("state", ParamType.TEXT).choices(() -> OrderDeskMainSteps.STATES)
                        .code((ui, p) -> OrderDeskMainSteps.menuItemIs(ui, p.text("item"), p.text("state")))
                        .match(json("{'action':'expect','target':{'role':'menuitem','name':'{item}'},'state':['{state}']}"))
                        .autoFold(false)
                        .java(STEPS, "menuItemIs(ui, {item}, {state})")
                        .build(),
                DomainStep.check("statusLineIs", "The status line reads {message}")
                        .about("Reads the status line on the left of the status bar: \"Ready\", \"Order ORD-1640 placed\","
                                + " \"Order ORD-1635 cancelled\", \"2 orders cancelled\", \"Copied ORD-1633, ORD-1634\","
                                + " \"Receipt saved to …\"")
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskMainSteps.statusLineIs(ui, p.text("message")))
                        .match(json("{'action':'expect','target':{'id':%s},'text':'{message}'}", text(STATUS_MESSAGE_ID)))
                        .java(STEPS, "statusLineIs(ui, {message})")
                        .build(),
                DomainStep.check("openOrdersCountIs", "The open orders count reads {count}")
                        .about("Reads the open orders on the right of the status bar: \"4 open orders\", \"1 open order\","
                                + " or the number alone (\"4\"). It moves as orders are placed and cancelled: give the"
                                + " count you expect now")
                        .param("count", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskMainSteps.openOrdersCountIs(ui, p.text("count")))
                        .match(json("{'action':'expect','target':{'role':'label','name':%s},'text':'{count}'}",
                                OrderDeskVocabulary.quote("*" + OrderDeskVocabulary.plain(" " + OPEN_ORDERS_LABEL) + "*")))
                        .java(STEPS, "openOrdersCountIs(ui, {count})")
                        .build(),

                // ---- exit and about
                DomainStep.act("cancelExit", "Ask to exit, then cancel")
                        .about(NEEDS_CART + " Ends: on it, the app and its tabs as they were, after answering Cancel to"
                                + " File ▸ Exit's \"Discard the open new orders?\". No step exits the app")
                        .code((ui, p) -> OrderDeskMainSteps.cancelExit(ui))
                        .match(exit, cancel)
                        .autoFold(false)
                        .java(STEPS, "cancelExit(ui)")
                        .build(),
                DomainStep.check("exitQuestionIs", "The exit question reads {message}")
                        .about("Reads the question File ▸ Exit asks (\"Discard the open new orders?\"): chooses File ▸"
                                + " Exit, reads it, answers Cancel. " + NEEDS_CART)
                        .param("message", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskMainSteps.exitQuestionIs(ui, p.text("message")))
                        .match(exit, json("{'action':'expect','target':{'role':'label'},'text':'{message}'}"), cancel)
                        .autoFold(false)
                        .java(STEPS, "exitQuestionIs(ui, {message})")
                        .build(),
                DomainStep.check("aboutShows", "About Order Desk shows {text}")
                        .about("Reads Help ▸ About Order Desk's text: opens it, reads, closes it (OK). Passes when the text"
                                + " shows {text} as whole words, e.g. \"A small, fictional order-entry app\" or \"All"
                                + " customers, products and orders are invented.\". Needs: no dialog open")
                        .param("text", ParamType.TEXT)
                        .code((ui, p) -> OrderDeskMainSteps.aboutShows(ui, p.text("text")))
                        .match(json("{'action':'menu','path':[%s,%s]}", text(HELP_MENU), text(ABOUT_ITEM)),
                                json("{'action':'expect','target':{'role':'label'},'text':'{text}'}"),
                                json("{'action':'answer','button':%s}", text(OK_ANSWER)))
                        .autoFold(false)
                        .java(STEPS, "aboutShows(ui, {text})")
                        .build(),

                // ---- the theme
                DomainStep.act("switchTheme", "Switch the theme to {theme}")
                        .about(NEEDS_MAIN + " Ends: the whole app in that theme (View ▸ Theme), every tab and value as it"
                                + " was: only the look changes (a screenshot shows it). {theme}: Light (the theme at start),"
                                + " Dark or Lagoon, any case or its start")
                        .param("theme", ParamType.TEXT).choices(() -> OrderDeskMainSteps.THEMES)
                        .code((ui, p) -> OrderDeskMainSteps.switchTheme(ui, p.text("theme")))
                        .match(json("{'action':'menu','path':[%s,%s,'{theme}']}", text(VIEW_MENU), text(THEME_MENU)))
                        .java(STEPS, "switchTheme(ui, {theme})")
                        .build());
    }
}
