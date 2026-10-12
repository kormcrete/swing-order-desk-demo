package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {

    private final Category all = new Category("all", "All products", null);
    private final Category office = new Category("office", "Office", all);
    private final Category paper = new Category("office/paper", "Paper", office);
    private final Category desk = new Category("office/desk", "Desk", office);
    private final Category cleaning = new Category("cleaning", "Cleaning", all);

    private final Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", paper, Money.of("4.90"), 120);
    private final Product stapler = new Product("DSK-STAPLER", "Stapler", desk, Money.of("12.50"), 4);
    private final Product lamp = new Product("DSK-LAMP", "Desk lamp", desk, Money.of("39.00"), 0);
    private final Product bags = new Product("CLN-BAGS", "Bin bags, roll of 50", cleaning, Money.of("6.30"), 35);
    private final Catalog catalog = new Catalog(all, Arrays.asList(a4, stapler, lamp, bags));

    @Test
    void categoriesContainTheirDescendants() {
        assertThat(all.contains(paper)).isTrue();
        assertThat(office.contains(paper)).isTrue();
        assertThat(office.contains(office)).isTrue();
        assertThat(paper.contains(office)).isFalse();
        assertThat(office.contains(cleaning)).isFalse();
        assertThat(all.children()).containsExactly(office, cleaning);
        assertThat(office.children()).containsExactly(paper, desk);
        assertThat(paper.parent()).isSameAs(office);
        assertThat(paper.toString()).isEqualTo("Paper");   // what the tree shows
    }

    @Test
    void findsProductsBySku() {
        assertThat(catalog.find("DSK-STAPLER")).contains(stapler);
        assertThat(catalog.find("NOPE")).isEmpty();
    }

    @Test
    void stockFlags() {
        assertThat(lamp.inStock()).isFalse();
        assertThat(stapler.lowStock()).isTrue();
        assertThat(a4.lowStock()).isFalse();
        assertThat(lamp.lowStock()).isFalse();
    }

    @Test
    void takingAndGivingBackStock() {
        catalog.take(stapler, 3);
        assertThat(stapler.stock()).isEqualTo(1);
        catalog.giveBack(stapler, 3);
        assertThat(stapler.stock()).isEqualTo(4);
        assertThatThrownBy(() -> catalog.take(stapler, 5)).hasMessage("Only 4 Staplers in stock");
        assertThat(stapler.stock()).isEqualTo(4);
    }

    @Test
    void stockMessagesReadNaturally() {
        assertThat(stapler.onlyInStockMessage()).isEqualTo("Only 4 Staplers in stock");
        assertThat(bags.onlyInStockMessage()).isEqualTo("Only 35 × Bin bags, roll of 50 in stock");
        Product wipes = new Product("CLN-WIPES", "Cleaning wipes", cleaning, Money.of("3.40"), 2);
        assertThat(wipes.onlyInStockMessage()).isEqualTo("Only 2 Cleaning wipes in stock");
    }

    @Test
    void stockMessagesForOneAndNoneLeft() {
        Product oneStapler = new Product("DSK-STAPLER", "Stapler", desk, Money.of("12.50"), 1);
        Product oneBagRoll = new Product("CLN-BAGS", "Bin bags, roll of 50", cleaning, Money.of("6.30"), 1);
        Product noStapler = new Product("DSK-STAPLER", "Stapler", desk, Money.of("12.50"), 0);
        Product noBagRolls = new Product("CLN-BAGS", "Bin bags, roll of 50", cleaning, Money.of("6.30"), 0);
        assertThat(oneStapler.onlyInStockMessage()).isEqualTo("Only 1 Stapler in stock");
        assertThat(oneBagRoll.onlyInStockMessage()).isEqualTo("Only 1 × Bin bags, roll of 50 in stock");
        assertThat(noStapler.onlyInStockMessage()).isEqualTo("No Staplers in stock");
        assertThat(noBagRolls.onlyInStockMessage()).isEqualTo("No Bin bags, roll of 50 in stock");
    }
}
