package io.github.kormcrete.orderdesk.ui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** The kormcrete tile as the window icon: Windows shows it in the taskbar and title bar (macOS uses the Dock icon). */
final class AppIcon {

    static final int[] SIZES = {16, 32, 48, 64, 128, 256};

    private AppIcon() {
    }

    static List<Image> images() {
        List<Image> images = new ArrayList<>();
        for (int size : SIZES) {
            String name = "/io/github/kormcrete/orderdesk/icons/app/kormcrete-order-desk-" + size + ".png";
            try (InputStream in = AppIcon.class.getResourceAsStream(name)) {
                if (in == null) throw new IllegalStateException("missing " + name);
                images.add(ImageIO.read(in));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return images;
    }
}
