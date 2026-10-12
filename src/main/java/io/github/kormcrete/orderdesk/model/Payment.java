package io.github.kormcrete.orderdesk.model;

/** How an order is paid: on invoice (with an optional PO number) or by card (only the last four digits are kept). */
public final class Payment {

    private final boolean invoice;
    private final String poNumber;
    private final String cardLast4;

    private Payment(boolean invoice, String poNumber, String cardLast4) {
        this.invoice = invoice;
        this.poNumber = poNumber;
        this.cardLast4 = cardLast4;
    }

    public static Payment invoice(String poNumber) {
        return new Payment(true, poNumber == null ? "" : poNumber.trim(), "");
    }

    /** From the number as typed. */
    public static Payment card(String cardNumber) {
        String digits = CardRules.digits(cardNumber);
        return new Payment(false, "", digits.length() <= 4 ? digits : digits.substring(digits.length() - 4));
    }

    public boolean isInvoice() {
        return invoice;
    }

    public String poNumber() {
        return poNumber;
    }

    public String cardLast4() {
        return cardLast4;
    }

    /** "Invoice (30 days) · PO PO-2026-131" or "Card ending 4242". */
    public String describe() {
        if (invoice) return poNumber.isEmpty() ? "Invoice (30 days)" : "Invoice (30 days) · PO " + poNumber;
        return "Card ending " + cardLast4;
    }
}
