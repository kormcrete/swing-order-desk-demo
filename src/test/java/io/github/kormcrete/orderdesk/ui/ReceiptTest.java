package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.ReceiptText;
import io.github.kormcrete.orderdesk.model.Seed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptTest {

    @TempDir
    Path dir;

    @Test
    void addsTxtWhenMissing() {
        assertThat(Receipt.withTxt(new File("/tmp/ORD-1640")).getName()).isEqualTo("ORD-1640.txt");
        assertThat(Receipt.withTxt(new File("/tmp/ORD-1640.TXT")).getName()).isEqualTo("ORD-1640.TXT");
    }

    @Test
    void writesTheReceiptText() throws Exception {
        Order order = Seed.store(() -> LocalDate.of(2026, 10, 12)).orders().find("ORD-1639").get();
        File file = dir.resolve("ORD-1639.txt").toFile();
        Receipt.write(order, file);
        assertThat(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).isEqualTo(ReceiptText.of(order));
    }
}
