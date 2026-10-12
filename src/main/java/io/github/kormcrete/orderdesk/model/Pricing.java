package io.github.kormcrete.orderdesk.model;

import java.util.List;

/**
 * The price rules: 10 or more of one product take 5% off that line; VAT is 20% of the discounted subtotal plus
 * shipping, rounded once; everything rounds half up to the cent.
 */
public final class Pricing {

    public static final int VAT_PERCENT = 20;
    static final int VOLUME_QUANTITY = 10;
    static final int VOLUME_PERCENT = 5;

    private Pricing() {
    }

    /** Quantity times price, before any discount. */
    public static Money lineTotal(PricedLine line) {
        return line.price().times(line.quantity());
    }

    public static Money lineDiscount(PricedLine line) {
        return line.quantity() >= VOLUME_QUANTITY ? lineTotal(line).percent(VOLUME_PERCENT) : Money.ZERO;
    }

    /** {@code shipping} is null before checkout: then there is no shipping and VAT is on the goods only. */
    public static Totals of(List<? extends PricedLine> lines, Shipping shipping) {
        Money subtotal = Money.ZERO;
        Money discount = Money.ZERO;
        for (PricedLine line : lines) {
            subtotal = subtotal.plus(lineTotal(line));
            discount = discount.plus(lineDiscount(line));
        }
        Money discounted = subtotal.minus(discount);
        Money shippingCost = shipping == null ? Money.ZERO : shipping.cost(discounted);
        Money vat = discounted.plus(shippingCost).percent(VAT_PERCENT);
        return new Totals(subtotal, discount, shippingCost, vat);
    }
}
