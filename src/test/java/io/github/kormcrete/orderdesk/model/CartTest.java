package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final Category all = new Category("all", "All products", null);
    private final Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", all, Money.of("4.90"), 120);
    private final Product stapler = new Product("DSK-STAPLER", "Stapler", all, Money.of("12.50"), 4);
    private final Product lamp = new Product("DSK-LAMP", "Desk lamp", all, Money.of("39.00"), 0);
    private final Cart cart = new Cart();

    @Test
    void addingTheSameProductAddsToItsLine() {
        cart.add(a4, 1);
        cart.add(stapler, 1);
        cart.add(a4, 2);
        assertThat(cart.lines()).extracting(CartLine::name).containsExactly("A4 paper, 500 sheets", "Stapler");
        assertThat(cart.lines().get(0).quantity()).isEqualTo(3);
        assertThat(cart.units()).isEqualTo(4);
    }

    @Test
    void outOfStockProductsCannotBeAdded() {
        assertThatThrownBy(() -> cart.add(lamp, 1)).hasMessage("Desk lamp is out of stock");
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void quantityBelowOneIsRefused() {
        cart.add(a4, 1);
        assertThatThrownBy(() -> cart.setQuantity(a4, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cart.setQuantity(a4, -2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cart.add(a4, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cart.lines().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void quantitiesAboveStockAreProblems() {
        cart.add(stapler, 1);
        cart.setQuantity(stapler, 5);
        assertThat(cart.lines().get(0).overStock()).isTrue();
        assertThat(cart.stockProblems()).containsExactly("Only 4 Staplers in stock");
        cart.setQuantity(stapler, 4);
        assertThat(cart.stockProblems()).isEmpty();
    }

    @Test
    void removingLines() {
        cart.add(a4, 1);
        cart.add(stapler, 1);
        cart.remove(Arrays.asList(a4, stapler));
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void cartTotalsHaveNoShipping() {
        cart.add(stapler, 2);
        assertThat(cart.totals().total()).isEqualTo(Money.of("30.00"));
    }
}
