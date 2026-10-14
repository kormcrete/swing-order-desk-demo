package io.github.kormcrete.orderdesk.ui;

import org.junit.jupiter.api.Test;

import java.awt.Image;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppIconTest {

    @Test
    void theTileInEverySize() {
        List<Image> images = AppIcon.images();
        assertThat(images).hasSize(AppIcon.SIZES.length);
        for (int i = 0; i < images.size(); i++) {
            assertThat(images.get(i).getWidth(null)).isEqualTo(AppIcon.SIZES[i]);
            assertThat(images.get(i).getHeight(null)).isEqualTo(AppIcon.SIZES[i]);
        }
    }
}
