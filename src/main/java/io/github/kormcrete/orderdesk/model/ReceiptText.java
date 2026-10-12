package io.github.kormcrete.orderdesk.model;

import java.util.Locale;

/** The plain-text receipt that Save receipt… writes. */
public final class ReceiptText {

    private ReceiptText() {
    }

    public static String of(Order order) {
        StringBuilder out = new StringBuilder();
        line(out, "Order Desk — receipt");
        line(out, order.number() + " · " + Dates.format(order.date()) + " · " + order.status().label());
        line(out, "");
        line(out, "Customer:  " + order.customer().name());
        line(out, "Deliver to:");
        for (String addressLine : order.address().lines()) line(out, "  " + addressLine);
        line(out, "Shipping:  " + order.shipping().describe());
        if (!order.note().isEmpty()) line(out, "Note:      " + order.note());
        line(out, "Payment:   " + order.payment().describe());
        line(out, "");
        for (OrderLine l : order.lines()) {
            line(out, String.format(Locale.US, "%4d × %-32s %10s %11s",
                    l.quantity(), l.name(), l.price().format(), Pricing.lineTotal(l).format()));
        }
        line(out, "");
        Totals t = order.totals();
        total(out, "Subtotal", t.subtotal());
        if (t.discount().isPositive()) total(out, "Volume discount", t.discount().negate());
        total(out, "Shipping", t.shipping());
        total(out, "VAT (" + Pricing.VAT_PERCENT + "%)", t.vat());
        total(out, "Total", t.total());
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void total(StringBuilder out, String label, Money amount) {
        line(out, String.format(Locale.US, "%-50s %11s", label, amount.format()));
    }
}
