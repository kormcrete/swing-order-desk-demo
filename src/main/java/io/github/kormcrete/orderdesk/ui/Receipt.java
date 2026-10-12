package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.ReceiptText;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Consumer;

/** Save receipt…: a save file chooser, then the plain-text receipt. */
final class Receipt {

    private Receipt() {
    }

    static void save(Component parent, Order order, Consumer<String> status) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save receipt");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), order.number() + ".txt"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = withTxt(chooser.getSelectedFile());
        if (file.exists() && !Dialogs.confirm(parent, "Save receipt", file.getName() + " already exists.",
                "Do you want to replace it?", "Replace", "Cancel")) {
            return;
        }
        try {
            write(order, file);
            status.accept("Receipt saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error(parent, "Save receipt", "The receipt could not be saved: " + e.getMessage());
        }
    }

    static File withTxt(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".txt") ? file : new File(file.getPath() + ".txt");
    }

    static void write(Order order, File file) throws IOException {
        Files.write(file.toPath(), ReceiptText.of(order).getBytes(StandardCharsets.UTF_8));
    }
}
