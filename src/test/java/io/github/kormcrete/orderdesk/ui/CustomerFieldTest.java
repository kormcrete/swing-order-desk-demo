package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Customer;
import io.github.kormcrete.orderdesk.model.Seed;
import io.github.kormcrete.orderdesk.model.Store;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerFieldTest {

    private final Store store = Seed.store(() -> LocalDate.of(2026, 10, 12));
    private final List<Customer> told = new ArrayList<>();
    private final CustomerField field = Edt.call(() -> new CustomerField(store, told::add));

    @BeforeAll
    static void theme() {
        Edt.run(Theme::light);
    }

    private List<String> suggestionNames() {
        return field.suggestions().stream().map(Customer::name).collect(Collectors.toList());
    }

    @Test
    void suggestsByNameOrCity() {
        Edt.run(() -> {
            field.setText("a");
            assertThat(suggestionNames()).containsExactly("Ada Shop Ltd", "Birch & Co", "Café Lumen", "Durand Bureau", "Fjord Supplies");
            field.setText("hamb");
            assertThat(suggestionNames()).containsExactly("Fjord Supplies");
            field.setText("zzz");
            assertThat(suggestionNames()).isEmpty();
            assertThat(told).isEmpty();
        });
    }

    @Test
    void choosingSetsTheTextAndEditingForgetsIt() {
        Edt.run(() -> {
            Customer ada = store.findCustomers("Ada").get(0);
            field.choose(ada);
            assertThat(field.getText()).isEqualTo("Ada Shop Ltd");
            assertThat(field.chosen()).isSameAs(ada);
            field.setText("Ada Shop");
            assertThat(field.chosen()).isNull();
            assertThat(told).containsExactly(ada, null);
        });
    }

    @Test
    void clearingForgetsTheCustomer() {
        Edt.run(() -> {
            field.choose(store.findCustomers("Birch").get(0));
            field.setText("");
            assertThat(field.chosen()).isNull();
            assertThat(field.suggestions()).isEmpty();
        });
    }
}
