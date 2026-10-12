package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderBookTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 12);

    private final Category all = new Category("all", "All products", null);
    private final Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", all, Money.of("4.90"), 120);
    private final Product pen = new Product("PEN-GEL-BLK", "Gel pen, black", all, Money.of("1.20"), 800);
    private final Product stapler = new Product("DSK-STAPLER", "Stapler", all, Money.of("12.50"), 4);
    private final Catalog catalog = new Catalog(all, Arrays.asList(a4, pen, stapler));
    private final OrderBook book = new OrderBook(catalog, 1640);

    private final Address berlin = new Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "");
    private final Address amsterdam = new Address("Noor de Vries", "Prinsengracht 263", "1016 GV", "Amsterdam", Country.NETHERLANDS, "");
    private final Customer ada = new Customer("Ada Shop Ltd", berlin, PaymentTerms.invoice(Money.of("2000.00"), true), Money.of("450.00"));
    private final Customer lumen = new Customer("Café Lumen", amsterdam, PaymentTerms.cardOnly(), Money.ZERO);

    private Cart cart(Object... productQty) {
        Cart cart = new Cart();
        for (int i = 0; i < productQty.length; i += 2) cart.add((Product) productQty[i], (Integer) productQty[i + 1]);
        return cart;
    }

    @Test
    void placingTakesTheNextNumberStockAndCredit() {
        Order order = book.place(ada, cart(a4, 3, pen, 10), berlin, Shipping.STANDARD, "", Payment.invoice("PO-1"), DAY);
        assertThat(order.number()).isEqualTo("ORD-1640");
        assertThat(order.date()).isEqualTo(DAY);
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.units()).isEqualTo(13);
        assertThat(order.totals().total()).isEqualTo(Money.of("37.20"));
        assertThat(a4.stock()).isEqualTo(117);
        assertThat(pen.stock()).isEqualTo(790);
        assertThat(ada.owes()).isEqualTo(Money.of("487.20"));
        assertThat(book.nextNumber()).isEqualTo("ORD-1641");
        assertThat(book.find("ORD-1640")).contains(order);
        assertThat(book.openCount()).isEqualTo(1);
    }

    @Test
    void orderLinesKeepThePriceTheyWerePlacedAt() {
        Order order = book.place(lumen, cart(a4, 1), amsterdam, Shipping.STANDARD, "Ring twice.", Payment.card("4242424242424242"), DAY);
        assertThat(order.lines().get(0).price()).isEqualTo(Money.of("4.90"));
        assertThat(order.lines().get(0).sku()).isEqualTo("PAP-A4-500");
        assertThat(order.note()).isEqualTo("Ring twice.");
        assertThat(lumen.owes()).isEqualTo(Money.ZERO);
    }

    @Test
    void cancellingRestoresStockAndCredit() {
        Order order = book.place(ada, cart(stapler, 4), berlin, Shipping.STANDARD, "", Payment.invoice("PO-1"), DAY);
        book.cancel(order);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellable()).isFalse();
        assertThat(stapler.stock()).isEqualTo(4);
        assertThat(ada.owes()).isEqualTo(Money.of("450.00"));
        assertThatThrownBy(() -> book.cancel(order)).hasMessage("Only placed orders can be cancelled: ORD-1640");
    }

    @Test
    void placingRefusesWhatTheRulesForbid() {
        assertThatThrownBy(() -> book.place(ada, new Cart(), berlin, Shipping.STANDARD, "", Payment.invoice("x"), DAY))
                .hasMessage("The cart is empty");
        Cart tooMany = cart(stapler, 1);
        tooMany.setQuantity(stapler, 5);
        assertThatThrownBy(() -> book.place(ada, tooMany, berlin, Shipping.STANDARD, "", Payment.invoice("x"), DAY))
                .hasMessage("Only 4 Staplers in stock");
        assertThatThrownBy(() -> book.place(lumen, cart(a4, 1), amsterdam, Shipping.EXPRESS, "", Payment.card("4242424242424242"), DAY))
                .hasMessage("Express delivers to Germany and Austria only");
        assertThatThrownBy(() -> book.place(lumen, cart(a4, 1), amsterdam, Shipping.STANDARD, "", Payment.invoice(""), DAY))
                .hasMessage("Café Lumen pays by card");
        assertThat(stapler.stock()).isEqualTo(4);
        assertThat(book.orders()).isEmpty();
        assertThat(book.nextNumber()).isEqualTo("ORD-1640");
    }

    @Test
    void recentCustomersByLatestOrder() {
        book.addExisting(new Order("ORD-1001", LocalDate.of(2026, 9, 1), lumen, Collections.singletonList(new OrderLine(a4, a4.price(), 1)),
                amsterdam, Shipping.STANDARD, "", Payment.card("1"), OrderStatus.SHIPPED));
        book.addExisting(new Order("ORD-1002", LocalDate.of(2026, 9, 2), ada, Collections.singletonList(new OrderLine(a4, a4.price(), 1)),
                berlin, Shipping.STANDARD, "", Payment.invoice(""), OrderStatus.SHIPPED));
        book.addExisting(new Order("ORD-1003", LocalDate.of(2026, 9, 3), ada, Collections.singletonList(new OrderLine(a4, a4.price(), 1)),
                berlin, Shipping.STANDARD, "", Payment.invoice(""), OrderStatus.PLACED));
        assertThat(book.recentCustomers(4)).containsExactly(ada, lumen);
        assertThat(book.recentCustomers(1)).containsExactly(ada);
        assertThat(a4.stock()).isEqualTo(120);   // existing orders do not move stock
    }
}
