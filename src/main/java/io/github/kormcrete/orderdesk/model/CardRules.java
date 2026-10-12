package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Card details as the Payment step checks and shows them. Any 16 digits are a card number: no checksum. */
public final class CardRules {

    private static final Pattern EXPIRY = Pattern.compile("(\\d{2})/(\\d{2})");

    private CardRules() {
    }

    /** The number without the spaces and dashes people type; any other character stays, and fails the check. */
    public static String digits(String number) {
        return number.replaceAll("[\\s-]", "");
    }

    /** Up to 16 digits in groups of four, as the field shows them: {@code 4242 4242 4242 4242}. */
    public static String format(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 16) digits = digits.substring(0, 16);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) out.append(' ');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    /** {@code MM/YY} from what was typed: digits only, a slash after the month. */
    public static String formatExpiry(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 4) digits = digits.substring(0, 4);
        return digits.length() <= 2 ? digits : digits.substring(0, 2) + "/" + digits.substring(2);
    }

    /** The problem per field; empty when the card can be used. A card is valid through its expiry month. */
    public static Map<CardField, String> validate(String number, String expiry, String name, LocalDate today) {
        Map<CardField, String> errors = new EnumMap<>(CardField.class);
        if (!digits(number).matches("\\d{16}")) errors.put(CardField.NUMBER, "Enter the 16 digits of the card");
        Matcher m = EXPIRY.matcher(expiry.trim());
        if (!m.matches()) {
            errors.put(CardField.EXPIRY, "Use MM/YY");
        } else {
            int month = Integer.parseInt(m.group(1));
            int year = 2000 + Integer.parseInt(m.group(2));
            if (month < 1 || month > 12) {
                errors.put(CardField.EXPIRY, "Month must be 01–12");
            } else if (year * 12 + month < today.getYear() * 12 + today.getMonthValue()) {
                errors.put(CardField.EXPIRY, "This card has expired");
            }
        }
        if (name.trim().isEmpty()) errors.put(CardField.NAME, "Name on card is required");
        return errors;
    }
}
