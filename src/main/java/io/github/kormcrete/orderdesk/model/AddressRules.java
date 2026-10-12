package io.github.kormcrete.orderdesk.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Checks a delivery address; the texts are what the Address step shows under each field. */
public final class AddressRules {

    private static final Pattern PHONE = Pattern.compile("[+\\d ]{6,20}");

    private AddressRules() {
    }

    /** The problem per field; empty when the address can be used. */
    public static Map<AddressField, String> validate(Address address) {
        Map<AddressField, String> errors = new EnumMap<>(AddressField.class);
        if (address.name().trim().isEmpty()) errors.put(AddressField.NAME, "Name is required");
        if (address.street().trim().isEmpty()) errors.put(AddressField.STREET, "Street is required");
        String postalCode = address.postalCode().trim();
        if (postalCode.isEmpty()) {
            errors.put(AddressField.POSTAL_CODE, "Postal code is required");
        } else if (!address.country().acceptsPostalCode(postalCode)) {
            errors.put(AddressField.POSTAL_CODE, address.country().postalCodeRule());
        }
        if (address.city().trim().isEmpty()) errors.put(AddressField.CITY, "City is required");
        String phone = address.phone().trim();
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            errors.put(AddressField.PHONE, "Only +, digits and spaces, 6–20 characters");
        }
        return errors;
    }
}
