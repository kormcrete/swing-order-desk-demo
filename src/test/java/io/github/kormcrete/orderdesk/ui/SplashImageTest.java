package io.github.kormcrete.orderdesk.ui;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The splash's still image that packaging/splash/make-splash.sh writes: white, the website's hairline around it, the
 * website's wordmark bottom-right. The k is painted on it while Order Desk starts (SplashTest).
 */
class SplashImageTest {

    private static final String IMAGE_2X = Splash.IMAGE.replace(".png", "@2x.png");
    private static final int WHITE = 0xffffff;
    /** The website's hairline: the light line around its cards and menus. */
    private static final int HAIRLINE = 0xd5dee6;
    private static final int NAVY = 0x1a4a6b;

    @Test
    void bothSizesExist() throws IOException {
        assertThat(read(Splash.IMAGE).getWidth()).isEqualTo(Splash.WIDTH);
        assertThat(read(Splash.IMAGE).getHeight()).isEqualTo(Splash.HEIGHT);
        assertThat(read(IMAGE_2X).getWidth()).isEqualTo(2 * Splash.WIDTH);
        assertThat(read(IMAGE_2X).getHeight()).isEqualTo(2 * Splash.HEIGHT);
    }

    @Test
    void hasTheHairlineBorder() throws IOException {
        for (int scale = 1; scale <= 2; scale++) {
            BufferedImage image = read(scale == 1 ? Splash.IMAGE : IMAGE_2X);
            int right = image.getWidth() - 1;
            int bottom = image.getHeight() - 1;
            for (int line = 0; line < scale; line++) {   // 1 px at 1x, 2 px at 2x
                for (int x = 0; x <= right; x++) {
                    assertThat(rgb(image, x, line)).as("%dx top", scale).isEqualTo(HAIRLINE);
                    assertThat(rgb(image, x, bottom - line)).as("%dx bottom", scale).isEqualTo(HAIRLINE);
                }
                for (int y = 0; y <= bottom; y++) {
                    assertThat(rgb(image, line, y)).as("%dx left", scale).isEqualTo(HAIRLINE);
                    assertThat(rgb(image, right - line, y)).as("%dx right", scale).isEqualTo(HAIRLINE);
                }
            }
            assertThat(rgb(image, scale, scale)).as("%dx inside", scale).isEqualTo(WHITE);
        }
    }

    @Test
    void theWordmarkSitsBottomRightAndNothingElse() throws IOException {
        for (int scale = 1; scale <= 2; scale++) {
            BufferedImage image = read(scale == 1 ? Splash.IMAGE : IMAGE_2X);
            // the corner box the wordmark may use: 160 x 60 px inside the border
            int boxLeft = image.getWidth() - 160 * scale;
            int boxTop = image.getHeight() - 60 * scale;
            boolean navyInBox = false;
            for (int y = scale; y < image.getHeight() - scale; y++) {
                for (int x = scale; x < image.getWidth() - scale; x++) {
                    boolean inBox = x >= boxLeft && y >= boxTop;
                    if (inBox) {
                        navyInBox |= rgb(image, x, y) == NAVY;
                    } else {
                        assertThat(rgb(image, x, y)).as("%dx (%d, %d)", scale, x, y).isEqualTo(WHITE);
                    }
                }
            }
            assertThat(navyInBox).as("%dx: the wordmark's k", scale).isTrue();
        }
    }

    private static int rgb(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xffffff;
    }

    private static BufferedImage read(String resource) throws IOException {
        try (InputStream in = SplashImageTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return ImageIO.read(in);
        }
    }
}
