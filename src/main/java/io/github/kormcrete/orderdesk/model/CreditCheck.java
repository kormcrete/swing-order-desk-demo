package io.github.kormcrete.orderdesk.model;

/** The warning before an invoice order that takes a customer over their credit limit. */
public final class CreditCheck {

    private CreditCheck() {
    }

    public static boolean exceeds(Customer customer, Money orderTotal) {
        return customer.terms().invoiceAllowed()
                && customer.owes().plus(orderTotal).isGreaterThan(customer.terms().creditLimit());
    }

    /** "This order takes Birch & Co to €1,068.40 of its €1,000.00 credit limit." */
    public static String message(Customer customer, Money orderTotal) {
        return "This order takes " + customer.name() + " to " + customer.owes().plus(orderTotal).format()
                + " of its " + customer.terms().creditLimit().format() + " credit limit.";
    }

    /** "Owes €940.00 now, this order adds €128.40." */
    public static String detail(Customer customer, Money orderTotal) {
        return "Owes " + customer.owes().format() + " now, this order adds " + orderTotal.format() + ".";
    }
}
