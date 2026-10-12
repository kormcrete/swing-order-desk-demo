package io.github.kormcrete.orderdesk.model;

import java.util.Locale;

/** A customer of the shop. What they owe on open invoices changes as orders are placed and cancelled. */
public final class Customer {

    private final String name;
    private final Address address;
    private final PaymentTerms terms;
    private Money owes;

    public Customer(String name, Address address, PaymentTerms terms, Money owes) {
        this.name = name;
        this.address = address;
        this.terms = terms;
        this.owes = owes;
    }

    public String name() {
        return name;
    }

    /** The contact address; checkout starts from it. */
    public Address address() {
        return address;
    }

    public PaymentTerms terms() {
        return terms;
    }

    public Money owes() {
        return owes;
    }

    void setOwes(Money owes) {
        this.owes = owes;
    }

    /** The card next to the Customer field. */
    public String termsSummary() {
        if (!terms.invoiceAllowed()) return "Card only";
        return "Account · Invoice 30 days · Credit limit " + terms.creditLimit().format() + " · Owes " + owes.format()
                + (terms.poRequired() ? " · PO number required" : "");
    }

    /** The right-hand text of a suggestion: "Berlin · Account". */
    public String suggestionDetail() {
        return address.city() + " · " + (terms.invoiceAllowed() ? "Account" : "Card only");
    }

    /** Whether a suggestion search finds this customer: by name or city, any case. */
    public boolean matches(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return q.isEmpty()
                || name.toLowerCase(Locale.ROOT).contains(q)
                || address.city().toLowerCase(Locale.ROOT).contains(q);
    }

    @Override
    public String toString() {
        return name;
    }
}
