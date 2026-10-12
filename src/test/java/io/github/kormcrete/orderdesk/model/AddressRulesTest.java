package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import static io.github.kormcrete.orderdesk.model.AddressField.CITY;
import static io.github.kormcrete.orderdesk.model.AddressField.NAME;
import static io.github.kormcrete.orderdesk.model.AddressField.PHONE;
import static io.github.kormcrete.orderdesk.model.AddressField.POSTAL_CODE;
import static io.github.kormcrete.orderdesk.model.AddressField.STREET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class AddressRulesTest {

    private static Address berlin(String postalCode, String phone) {
        return new Address("Ada Lindqvist", "Torstraße 118", postalCode, "Berlin", Country.GERMANY, phone);
    }

    @Test
    void aCompleteAddressHasNoErrors() {
        assertThat(AddressRules.validate(berlin("10119", "+49 30 5550 1188"))).isEmpty();
        assertThat(AddressRules.validate(berlin("10119", ""))).isEmpty();
    }

    @Test
    void requiredFields() {
        Address empty = new Address(" ", "", "", "", Country.GERMANY, "");
        assertThat(AddressRules.validate(empty)).containsOnly(
                entry(NAME, "Name is required"),
                entry(STREET, "Street is required"),
                entry(POSTAL_CODE, "Postal code is required"),
                entry(CITY, "City is required"));
    }

    @Test
    void postalCodePerCountry() {
        assertThat(Country.GERMANY.acceptsPostalCode("10119")).isTrue();
        assertThat(Country.GERMANY.acceptsPostalCode("1011")).isFalse();
        assertThat(Country.FRANCE.acceptsPostalCode("75004")).isTrue();
        assertThat(Country.AUSTRIA.acceptsPostalCode("1070")).isTrue();
        assertThat(Country.AUSTRIA.acceptsPostalCode("10700")).isFalse();
        assertThat(Country.NETHERLANDS.acceptsPostalCode("1016 GV")).isTrue();
        assertThat(Country.NETHERLANDS.acceptsPostalCode("1016GV")).isFalse();
        Address vienna = new Address("Jonas Birch", "Mariahilfer Straße 54", "10700", "Vienna", Country.AUSTRIA, "");
        assertThat(AddressRules.validate(vienna)).containsOnly(entry(POSTAL_CODE, "Austria uses a 4-digit postal code"));
        assertThat(Country.NETHERLANDS.postalCodeRule()).isEqualTo("The Netherlands use the form 1234 AB");
    }

    @Test
    void postalCodeIgnoresCaseAndSpaces() {
        assertThat(Country.NETHERLANDS.acceptsPostalCode(" 1016 gv ")).isTrue();
        assertThat(Country.GERMANY.acceptsPostalCode(" 10119 ")).isTrue();
    }

    @Test
    void phoneIsOptionalButChecked() {
        assertThat(AddressRules.validate(berlin("10119", "12345"))).containsOnly(entry(PHONE, "Only +, digits and spaces, 6–20 characters"));
        assertThat(AddressRules.validate(berlin("10119", "call me"))).containsKey(PHONE);
        assertThat(AddressRules.validate(berlin("10119", " +49 30 1 "))).isEmpty();
    }

    @Test
    void addressLinesAndCopies() {
        assertThat(berlin("10119", "").lines()).containsExactly("Ada Lindqvist", "Torstraße 118", "10119 Berlin", "Germany");
        assertThat(berlin("10119", "+49 30 5550 1188").lines()).endsWith("+49 30 5550 1188");
        assertThat(berlin("", "").withPostalCode("04109").postalCode()).isEqualTo("04109");
        assertThat(Country.AUSTRIA.toString()).isEqualTo("Austria");
    }
}
