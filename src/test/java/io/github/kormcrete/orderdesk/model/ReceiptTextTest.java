package io.github.kormcrete.orderdesk.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptTextTest {

    @Test
    void listsTheOrderLikeTheOrderTab() {
        Category all = new Category("all", "All products", null);
        Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", all, Money.of("4.90"), 120);
        Product pen = new Product("PEN-GEL-BLK", "Gel pen, black", all, Money.of("1.20"), 800);
        Address berlin = new Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "");
        Customer ada = new Customer("Ada Shop Ltd", berlin, PaymentTerms.invoice(Money.of("2000.00"), true), Money.ZERO);
        Order order = new Order("ORD-1640", LocalDate.of(2026, 10, 12), ada,
                Arrays.asList(new OrderLine(a4, a4.price(), 3), new OrderLine(pen, pen.price(), 10)),
                berlin, Shipping.STANDARD, "Ring twice.", Payment.invoice("PO-2026-131"), OrderStatus.PLACED);

        String text = ReceiptText.of(order);

        assertThat(text).startsWith("Order Desk — receipt\nORD-1640 · 12 Oct 2026 · Placed\n");
        assertThat(text).contains("Customer:  Ada Shop Ltd\n", "  10119 Berlin\n", "Shipping:  Standard · 3–5 business days\n",
                "Note:      Ring twice.\n", "Payment:   Invoice (30 days) · PO PO-2026-131\n");
        assertThat(text).containsPattern("\\n   3 × A4 paper, 500 sheets +€4\\.90 +€14\\.70\\n");
        assertThat(text).containsPattern("\\nVolume discount +−€0\\.60\\n");
        assertThat(text).containsPattern("\\nTotal +€37\\.20\\n$");
    }

    @Test
    void totalsAmountsEndInTheSameColumnAsTheLineTotals() {
        Category all = new Category("all", "All products", null);
        Product a4 = new Product("PAP-A4-500", "A4 paper, 500 sheets", all, Money.of("4.90"), 120);
        Address berlin = new Address("Ada Lindqvist", "Torstraße 118", "10119", "Berlin", Country.GERMANY, "");
        Customer ada = new Customer("Ada Shop Ltd", berlin, PaymentTerms.invoice(Money.of("2000.00"), true), Money.ZERO);
        Order order = new Order("ORD-1640", LocalDate.of(2026, 10, 12), ada,
                Arrays.asList(new OrderLine(a4, a4.price(), 3)),
                berlin, Shipping.STANDARD, "", Payment.invoice("PO-2026-131"), OrderStatus.PLACED);

        String[] rows = ReceiptText.of(order).split("\n");
        int lineRow = -1;
        for (String row : rows) if (row.contains(" × ")) lineRow = row.length();
        assertThat(lineRow).isPositive();
        for (String row : rows) {
            if (row.startsWith("Subtotal") || row.startsWith("Shipping ") || row.startsWith("VAT") || row.startsWith("Total")) {
                assertThat(row).hasSize(lineRow);
            }
        }
    }
}
