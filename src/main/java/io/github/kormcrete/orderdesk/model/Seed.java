package io.github.kormcrete.orderdesk.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** The demo data the app starts with. All customers, products and orders are invented. */
public final class Seed {

    public static final int NEXT_ORDER_NUMBER = 1640;

    private Seed() {
    }

    public static Store store(Supplier<LocalDate> today) {
        Category all = new Category("all", "All products", null);
        Category office = new Category("office", "Office", all);
        Category paper = new Category("office/paper", "Paper", office);
        Category pens = new Category("office/pens", "Pens", office);
        Category desk = new Category("office/desk", "Desk", office);
        Category coffee = new Category("coffee", "Coffee", all);
        Category beans = new Category("coffee/beans", "Beans", coffee);
        Category machines = new Category("coffee/machines", "Machines", coffee);
        Category cleaning = new Category("cleaning", "Cleaning", all);

        Catalog catalog = new Catalog(all, Arrays.asList(
                new Product("PAP-A4-500", "A4 paper, 500 sheets", paper, Money.of("4.90"), 120),
                new Product("PAP-A3-250", "A3 paper, 250 sheets", paper, Money.of("7.80"), 40),
                new Product("PAP-NOTE-A5", "Notebook A5, ruled", paper, Money.of("2.60"), 150),
                new Product("PAP-STICKY", "Sticky notes, 3 pads", paper, Money.of("3.10"), 90),
                new Product("PEN-GEL-BLK", "Gel pen, black", pens, Money.of("1.20"), 800),
                new Product("PEN-GEL-BLU", "Gel pen, blue", pens, Money.of("1.20"), 650),
                new Product("PEN-MARK-SET", "Permanent markers, set of 4", pens, Money.of("5.40"), 75),
                new Product("PEN-HILITE", "Highlighter, yellow", pens, Money.of("0.90"), 300),
                new Product("DSK-STAPLER", "Stapler", desk, Money.of("12.50"), 4),
                new Product("DSK-LAMP", "Desk lamp", desk, Money.of("39.00"), 0),
                new Product("DSK-TRAY", "Letter tray", desk, Money.of("8.90"), 22),
                new Product("DSK-CLIPS", "Paper clips, box of 100", desk, Money.of("1.50"), 5),
                new Product("DSK-TAPE", "Tape dispenser", desk, Money.of("6.70"), 18),
                new Product("COF-BEANS-1K", "Coffee beans, 1 kg", beans, Money.of("18.90"), 60),
                new Product("COF-DECAF-500", "Decaf beans, 500 g", beans, Money.of("11.40"), 25),
                new Product("COF-FILTERS", "Paper filters, 100", beans, Money.of("2.90"), 110),
                new Product("COF-ESPRESSO", "Espresso machine", machines, Money.of("249.00"), 3),
                new Product("COF-GRINDER", "Burr grinder", machines, Money.of("89.00"), 7),
                new Product("COF-KETTLE", "Electric kettle", machines, Money.of("34.50"), 12),
                new Product("CLN-WIPES", "Cleaning wipes", cleaning, Money.of("3.40"), 200),
                new Product("CLN-SPRAY", "Glass cleaner, 500 ml", cleaning, Money.of("4.20"), 48),
                new Product("CLN-BAGS", "Bin bags, roll of 50", cleaning, Money.of("6.30"), 35)));

        Customer ada = new Customer("Ada Shop Ltd",
                new Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "+49 30 5550 1188"),
                PaymentTerms.invoice(Money.of("2000.00"), true), Money.of("450.00"));
        Customer birch = new Customer("Birch & Co",
                new Address("Jonas Birch", "Mariahilfer Straße 54", "1070", "Vienna", Country.AUSTRIA, ""),
                PaymentTerms.invoice(Money.of("1000.00"), false), Money.of("940.00"));
        Customer lumen = new Customer("Café Lumen",
                new Address("Noor de Vries", "Prinsengracht 263", "1016 GV", "Amsterdam", Country.NETHERLANDS, ""),
                PaymentTerms.cardOnly(), Money.ZERO);
        Customer durand = new Customer("Durand Bureau",
                new Address("Claire Durand", "14 Rue des Archives", "75004", "Paris", Country.FRANCE, ""),
                PaymentTerms.cardOnly(), Money.ZERO);
        Customer elm = new Customer("Elm Street Studio",
                new Address("Mia Hoffmann", "Elmstraße 7", "", "Leipzig", Country.GERMANY, ""),
                PaymentTerms.invoice(Money.of("5000.00"), false), Money.ZERO);
        Customer fjord = new Customer("Fjord Supplies",
                new Address("Lars Jensen", "Hafenstraße 21", "20359", "Hamburg", Country.GERMANY, ""),
                PaymentTerms.cardOnly(), Money.ZERO);

        OrderBook book = new OrderBook(catalog, NEXT_ORDER_NUMBER);
        order(book, catalog, 1628, "2026-09-01", elm, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.invoice(""), "",
                "PAP-A4-500", 20, "PEN-GEL-BLU", 12);
        order(book, catalog, 1629, "2026-09-05", fjord, OrderStatus.SHIPPED, Shipping.EXPRESS, Payment.card("1881"), "",
                "CLN-WIPES", 6, "CLN-SPRAY", 4, "CLN-BAGS", 2);
        order(book, catalog, 1630, "2026-09-10", durand, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("0005"), "",
                "COF-GRINDER", 1, "COF-BEANS-1K", 4);
        order(book, catalog, 1631, "2026-09-15", birch, OrderStatus.SHIPPED, Shipping.EXPRESS, Payment.invoice(""),
                "Deliver to the back entrance, please.", "COF-ESPRESSO", 1, "COF-BEANS-1K", 10, "COF-FILTERS", 3);
        order(book, catalog, 1632, "2026-09-18", lumen, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("4242"), "",
                "COF-BEANS-1K", 12, "COF-DECAF-500", 4);
        order(book, catalog, 1633, "2026-09-22", ada, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.invoice("PO-2026-118"), "",
                "PAP-A4-500", 10, "DSK-TRAY", 2, "PEN-MARK-SET", 3);
        order(book, catalog, 1634, "2026-09-25", elm, OrderStatus.CANCELLED, Shipping.STANDARD, Payment.invoice(""), "",
                "DSK-TAPE", 2, "PAP-STICKY", 5);
        order(book, catalog, 1635, "2026-09-29", durand, OrderStatus.PLACED, Shipping.STANDARD, Payment.card("0005"), "",
                "PAP-NOTE-A5", 30, "PEN-HILITE", 20);
        order(book, catalog, 1636, "2026-10-02", fjord, OrderStatus.SHIPPED, Shipping.STANDARD, Payment.card("1881"), "",
                "CLN-BAGS", 10, "CLN-WIPES", 24);
        order(book, catalog, 1637, "2026-10-05", birch, OrderStatus.PLACED, Shipping.EXPRESS, Payment.invoice(""), "",
                "COF-DECAF-500", 6, "COF-KETTLE", 1);
        order(book, catalog, 1638, "2026-10-07", lumen, OrderStatus.PLACED, Shipping.STANDARD, Payment.card("4242"),
                "Ring twice.", "COF-BEANS-1K", 8);
        order(book, catalog, 1639, "2026-10-09", ada, OrderStatus.PLACED, Shipping.STANDARD, Payment.invoice("PO-2026-131"), "",
                "PEN-GEL-BLK", 50, "PAP-A4-500", 3, "DSK-CLIPS", 2);

        return new Store(catalog, Arrays.asList(ada, birch, lumen, durand, elm, fjord), book, today);
    }

    /**
     * {@code skuQuantity}: SKU, quantity, SKU, quantity, … Elm Street Studio has no postal code on file; both of its
     * seed orders were delivered to 04109.
     */
    private static void order(OrderBook book, Catalog catalog, int number, String date, Customer customer,
                              OrderStatus status, Shipping shipping, Payment payment, String note, Object... skuQuantity) {
        List<OrderLine> lines = new ArrayList<>();
        for (int i = 0; i < skuQuantity.length; i += 2) {
            Product product = catalog.find((String) skuQuantity[i]).orElseThrow(IllegalStateException::new);
            lines.add(new OrderLine(product, product.price(), (Integer) skuQuantity[i + 1]));
        }
        Address address = customer.address().postalCode().isEmpty()
                ? customer.address().withPostalCode("04109") : customer.address();
        book.addExisting(new Order("ORD-" + number, LocalDate.parse(date), customer, lines, address, shipping, note,
                payment, status));
    }
}
