package io.github.kormcrete.orderdesk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A node of the product tree; "All products" is the root. */
public final class Category {

    private final String id;
    private final String label;
    private final Category parent;
    private final List<Category> children = new ArrayList<>();

    public Category(String id, String label, Category parent) {
        this.id = id;
        this.label = label;
        this.parent = parent;
        if (parent != null) parent.children.add(this);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Category parent() {
        return parent;
    }

    public List<Category> children() {
        return Collections.unmodifiableList(children);
    }

    /** True for this category and every category below it. */
    public boolean contains(Category other) {
        for (Category c = other; c != null; c = c.parent) {
            if (c == this) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return label;
    }
}
