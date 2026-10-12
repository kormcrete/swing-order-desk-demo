package io.github.kormcrete.orderdesk.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The app's 16 px icons (Lucide, ISC license), recoloured by FlatLaf for the dark theme. */
final class Icons {

    static final String SEARCH = "search";
    static final String PLUS = "plus";
    static final String CHEVRON_DOWN = "chevron-down";
    static final String CART = "shopping-cart";
    static final String X = "x";
    static final String SAVE = "save";
    static final String BAN = "ban";
    static final String WARNING = "triangle-alert";
    static final List<String> ALL = Arrays.asList(SEARCH, PLUS, CHEVRON_DOWN, CART, X, SAVE, BAN, WARNING);

    private static final Map<String, Icon> CACHE = new HashMap<>();

    private Icons() {
    }

    static Icon get(String name) {
        return CACHE.computeIfAbsent(name, n -> new FlatSVGIcon("io/github/kormcrete/orderdesk/icons/" + n + ".svg", 16, 16));
    }
}
