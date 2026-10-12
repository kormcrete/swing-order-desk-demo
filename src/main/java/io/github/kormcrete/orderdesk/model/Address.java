package io.github.kormcrete.orderdesk.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A delivery or contact address. Values are kept as typed; {@link AddressRules} checks them. */
public final class Address {

    private final String name;
    private final String street;
    private final String postalCode;
    private final String city;
    private final Country country;
    private final String phone;

    public Address(String name, String street, String postalCode, String city, Country country, String phone) {
        this.name = name == null ? "" : name;
        this.street = street == null ? "" : street;
        this.postalCode = postalCode == null ? "" : postalCode;
        this.city = city == null ? "" : city;
        this.country = country;
        this.phone = phone == null ? "" : phone;
    }

    public String name() {
        return name;
    }

    public String street() {
        return street;
    }

    public String postalCode() {
        return postalCode;
    }

    public String city() {
        return city;
    }

    public Country country() {
        return country;
    }

    public String phone() {
        return phone;
    }

    public Address withPostalCode(String code) {
        return new Address(name, street, code, city, country, phone);
    }

    /** As on a label: name, street, "10119 Berlin", country, then the phone if there is one. */
    public List<String> lines() {
        List<String> lines = new ArrayList<>(Arrays.asList(name.trim(), street.trim(),
                (postalCode.trim() + " " + city.trim()).trim(), country.label()));
        if (!phone.trim().isEmpty()) lines.add(phone.trim());
        return lines;
    }
}
