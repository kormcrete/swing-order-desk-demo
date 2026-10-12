package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PricingTest {

    private final Category all = new Category("all", "All products", null);
    private final Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", all, Money.of("4.90"), 120);
    private final Product pen = new Product("PEN-GEL-BLK", "Gel pen, black", all, Money.of("1.20"), 800);
    private final Product hundred = new Product("X-100", "Hundred", all, Money.of("100.00"), 10);

    private static PricedLine line(Product p, int qty) {
        Cart cart = new Cart();
        cart.add(p, qty);
        return cart.lines().get(0);
    }

    @Test
    void theWorkedExampleFromTheSpec() {
        Totals t = Pricing.of(Arrays.asList(line(a4, 3), line(pen, 10)), Shipping.STANDARD);
        assertThat(t.subtotal()).isEqualTo(Money.of("26.70"));
        assertThat(t.discount()).isEqualTo(Money.of("0.60"));
        assertThat(t.discounted()).isEqualTo(Money.of("26.10"));
        assertThat(t.shipping()).isEqualTo(Money.of("4.90"));
        assertThat(t.vat()).isEqualTo(Money.of("6.20"));
        assertThat(t.total()).isEqualTo(Money.of("37.20"));
    }

    @Test
    void volumeDiscountStartsAtTen() {
        assertThat(Pricing.lineDiscount(line(pen, 9))).isEqualTo(Money.ZERO);
        assertThat(Pricing.lineDiscount(line(pen, 10))).isEqualTo(Money.of("0.60"));
        assertThat(Pricing.lineTotal(line(pen, 10))).isEqualTo(Money.of("12.00"));
    }

    @Test
    void standardShippingIsFreeFromOneHundredAfterDiscount() {
        assertThat(Pricing.of(Collections.singletonList(line(hundred, 1)), Shipping.STANDARD).shipping()).isEqualTo(Money.ZERO);
        assertThat(Shipping.STANDARD.cost(Money.of("99.99"))).isEqualTo(Money.of("4.90"));
        assertThat(Shipping.EXPRESS.cost(Money.of("500.00"))).isEqualTo(Money.of("14.90"));
    }

    @Test
    void beforeCheckoutThereIsNoShippingAndVatIsOnGoods() {
        Totals t = Pricing.of(Arrays.asList(line(a4, 3), line(pen, 10)), null);
        assertThat(t.shipping()).isEqualTo(Money.ZERO);
        assertThat(t.vat()).isEqualTo(Money.of("5.22"));
        assertThat(t.total()).isEqualTo(Money.of("31.32"));
    }

    @Test
    void expressOnlyToGermanyAndAustria() {
        assertThat(Shipping.EXPRESS.allowedFor(Country.GERMANY)).isTrue();
        assertThat(Shipping.EXPRESS.allowedFor(Country.AUSTRIA)).isTrue();
        assertThat(Shipping.EXPRESS.allowedFor(Country.NETHERLANDS)).isFalse();
        assertThat(Shipping.STANDARD.allowedFor(Country.FRANCE)).isTrue();
        assertThat(Shipping.STANDARD.describe()).isEqualTo("Standard · 3–5 business days");
        assertThat(Shipping.EXPRESS.describe()).isEqualTo("Express · next business day");
    }
}
