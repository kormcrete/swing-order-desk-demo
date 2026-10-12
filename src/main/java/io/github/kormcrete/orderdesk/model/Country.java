package io.github.kormcrete.orderdesk.model;

import java.util.regex.Pattern;

/** The countries Order Desk delivers to, with their postal code rule. */
public enum Country {
    GERMANY("Germany", "\\d{5}", "Germany uses a 5-digit postal code"),
    AUSTRIA("Austria", "\\d{4}", "Austria uses a 4-digit postal code"),
    NETHERLANDS("Netherlands", "\\d{4} [A-Za-z]{2}", "The Netherlands use the form 1234 AB"),
    FRANCE("France", "\\d{5}", "France uses a 5-digit postal code");

    private final String label;
    private final Pattern postalCode;
    private final String postalCodeRule;

    Country(String label, String postalCode, String postalCodeRule) {
        this.label = label;
        this.postalCode = Pattern.compile(postalCode);
        this.postalCodeRule = postalCodeRule;
    }

    public String label() {
        return label;
    }

    /** Surrounding spaces do not count; letters may be either case. */
    public boolean acceptsPostalCode(String code) {
        return postalCode.matcher(code.trim()).matches();
    }

    /** What the Address step says under a postal code that does not fit. */
    public String postalCodeRule() {
        return postalCodeRule;
    }

    @Override
    public String toString() {
        return label;
    }
}
