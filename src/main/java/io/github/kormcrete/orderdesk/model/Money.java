package io.github.kormcrete.orderdesk.model;

import java.math.BigDecimal;
import java.util.Locale;

/** An amount in euros, held as whole cents. Formats the same on every machine: {@code €1,234.50}. */
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /** {@code Money.of("4.90")} is €4.90; a fraction of a cent is an error. */
    public static Money of(String euros) {
        return new Money(new BigDecimal(euros).movePointRight(2).longValueExact());
    }

    public long cents() {
        return cents;
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }

    public Money minus(Money other) {
        return new Money(cents - other.cents);
    }

    public Money times(int factor) {
        return new Money(cents * factor);
    }

    public Money negate() {
        return new Money(-cents);
    }

    /** {@code percent} percent of this amount, rounded half up to a whole cent. */
    public Money percent(int percent) {
        return new Money(Math.floorDiv(cents * percent + 50, 100));
    }

    public boolean isZero() {
        return cents == 0;
    }

    public boolean isPositive() {
        return cents > 0;
    }

    public boolean isGreaterThan(Money other) {
        return cents > other.cents;
    }

    public boolean isLessThan(Money other) {
        return cents < other.cents;
    }

    /** {@code €1,234.50}; a negative amount starts with a minus sign: {@code −€0.60}. */
    public String format() {
        long abs = Math.abs(cents);
        return (cents < 0 ? "−" : "") + "€" + String.format(Locale.US, "%,d.%02d", abs / 100, abs % 100);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money && ((Money) o).cents == cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    @Override
    public String toString() {
        return format();
    }
}
