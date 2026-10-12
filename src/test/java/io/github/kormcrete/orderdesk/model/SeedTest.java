package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SeedTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));

    @Test
    void sizes() {
        assertThat(store.catalog().products()).hasSize(22);
        assertThat(store.customers()).extracting(Customer::name).containsExactly(
                "Ada Shop Ltd", "Birch & Co", "Café Lumen", "Durand Bureau", "Elm Street Studio", "Fjord Supplies");
        assertThat(store.orders().orders()).hasSize(12);
        assertThat(store.orders().nextNumber()).isEqualTo("ORD-1640");
        assertThat(store.orders().openCount()).isEqualTo(4);
    }

    @Test
    void categoryTree() {
        Category all = store.catalog().root();
        assertThat(all.children()).extracting(Category::label).containsExactly("Office", "Coffee", "Cleaning");
        assertThat(productsIn(all)).isEqualTo(22);
        assertThat(productsIn(child(all, "Office"))).isEqualTo(13);
        assertThat(productsIn(child(child(all, "Office"), "Desk"))).isEqualTo(5);
        assertThat(productsIn(child(all, "Coffee"))).isEqualTo(6);
        assertThat(productsIn(child(all, "Cleaning"))).isEqualTo(3);
    }

    private long productsIn(Category category) {
        return store.catalog().products().stream().filter(p -> category.contains(p.category())).count();
    }

    private static Category child(Category parent, String label) {
        return parent.children().stream().filter(c -> c.label().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError(label));
    }

    @Test
    void seedOrderTotalsMatchTheMock() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ORD-1628", "128.14");
        expected.put("ORD-1629", "77.64");
        expected.put("ORD-1630", "197.52");
        expected.put("ORD-1631", "542.58");
        expected.put("ORD-1632", "313.27");
        expected.put("ORD-1633", "102.54");
        expected.put("ORD-1634", "40.56");
        expected.put("ORD-1635", "115.32");
        expected.put("ORD-1636", "164.84");
        expected.put("ORD-1637", "141.36");
        expected.put("ORD-1638", "181.44");
        expected.put("ORD-1639", "95.52");
        expected.forEach((number, total) ->
                assertThat(store.orders().find(number).get().totals().total()).as(number).isEqualTo(Money.of(total)));
        assertThat(store.orders().find("ORD-1639").get().units()).isEqualTo(55);
    }

    @Test
    void stockAndCreditAreAsListed() {
        assertThat(store.catalog().find("DSK-STAPLER").get().stock()).isEqualTo(4);
        assertThat(store.catalog().find("DSK-LAMP").get().stock()).isEqualTo(0);
        assertThat(store.catalog().find("COF-ESPRESSO").get().stock()).isEqualTo(3);
        Customer birch = store.findCustomers("Birch").get(0);
        assertThat(birch.owes()).isEqualTo(Money.of("940.00"));
        assertThat(birch.terms().creditLimit()).isEqualTo(Money.of("1000.00"));
    }

    @Test
    void elmStreetHasNoPostalCodeButItsOldOrderHasOne() {
        Customer elm = store.findCustomers("Elm").get(0);
        assertThat(elm.address().postalCode()).isEmpty();
        assertThat(store.orders().find("ORD-1634").get().address().postalCode()).isEqualTo("04109");
    }

    @Test
    void recentCustomers() {
        assertThat(store.orders().recentCustomers(4)).extracting(Customer::name)
                .containsExactly("Ada Shop Ltd", "Café Lumen", "Birch & Co", "Fjord Supplies");
    }
}
