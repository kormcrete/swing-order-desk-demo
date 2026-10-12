package io.github.kormcrete.orderdesk.model;

/** How a customer may pay: on invoice within a credit limit, or by card only. */
public final class PaymentTerms {

    private final boolean invoiceAllowed;
    private final Money creditLimit;
    private final boolean poRequired;

    private PaymentTerms(boolean invoiceAllowed, Money creditLimit, boolean poRequired) {
        this.invoiceAllowed = invoiceAllowed;
        this.creditLimit = creditLimit;
        this.poRequired = poRequired;
    }

    public static PaymentTerms invoice(Money creditLimit, boolean poRequired) {
        return new PaymentTerms(true, creditLimit, poRequired);
    }

    public static PaymentTerms cardOnly() {
        return new PaymentTerms(false, Money.ZERO, false);
    }

    public boolean invoiceAllowed() {
        return invoiceAllowed;
    }

    public Money creditLimit() {
        return creditLimit;
    }

    /** The customer wants their purchase order number on every invoice. */
    public boolean poRequired() {
        return poRequired;
    }
}
