package io.github.kormcrete.orderdesk.model;

/** Shipping methods. Standard is free from €100.00 (after the volume discount); Express goes to DE and AT only. */
public enum Shipping {
    STANDARD("Standard", "3–5 business days", Money.of("4.90")),
    EXPRESS("Express", "next business day", Money.of("14.90"));

    public static final Money FREE_FROM = Money.of("100.00");

    private final String label;
    private final String timing;
    private final Money price;

    Shipping(String label, String timing, Money price) {
        this.label = label;
        this.timing = timing;
        this.price = price;
    }

    public String label() {
        return label;
    }

    public String timing() {
        return timing;
    }

    /** "Standard · 3–5 business days". */
    public String describe() {
        return label + " · " + timing;
    }

    public Money cost(Money discountedSubtotal) {
        return this == STANDARD && !discountedSubtotal.isLessThan(FREE_FROM) ? Money.ZERO : price;
    }

    public boolean allowedFor(Country country) {
        return this == STANDARD || country == Country.GERMANY || country == Country.AUSTRIA;
    }

    @Override
    public String toString() {
        return label;
    }
}
