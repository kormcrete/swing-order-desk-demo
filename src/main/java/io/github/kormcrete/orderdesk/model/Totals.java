package io.github.kormcrete.orderdesk.model;

/** The money of a cart or an order, as the totals block shows it. */
public final class Totals {

    private final Money subtotal;
    private final Money discount;
    private final Money shipping;
    private final Money vat;

    Totals(Money subtotal, Money discount, Money shipping, Money vat) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.shipping = shipping;
        this.vat = vat;
    }

    public Money subtotal() {
        return subtotal;
    }

    /** The volume discount, a positive amount. */
    public Money discount() {
        return discount;
    }

    public Money discounted() {
        return subtotal.minus(discount);
    }

    public Money shipping() {
        return shipping;
    }

    public Money vat() {
        return vat;
    }

    public Money total() {
        return discounted().plus(shipping).plus(vat);
    }
}
