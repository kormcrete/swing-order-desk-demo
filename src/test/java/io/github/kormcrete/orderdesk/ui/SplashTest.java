package io.github.kormcrete.orderdesk.ui;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/** The k that builds itself on the splash, with the website hero's timing; and when the main window may show. */
class SplashTest {

    private static final int NAVY = 0xff1a4a6b;
    private static final int CYAN = 0xff76d8ff;

    @Test
    void theKIsNotThereYetAtTheStart() {
        BufferedImage frame = paint(0);
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                assertThat(frame.getRGB(x, y) >>> 24).as("alpha at (%d, %d)", x, y).isZero();
            }
        }
    }

    @Test
    void theWholeKAtTheEnd() {
        BufferedImage frame = paint(Splash.MOTION_MS);
        assertThat(at(frame, 16, 30)).as("stem").isEqualTo(NAVY);
        assertThat(at(frame, 46, 22)).as("upper arm").isEqualTo(NAVY);
        assertThat(at(frame, 48, 52)).as("lower arm").isEqualTo(NAVY);
        assertThat(at(frame, 31, 37)).as("block").isEqualTo(CYAN);
        assertThat(at(frame, 23.5, 37) >>> 24).as("the gap between stem and block").isZero();
        assertThat(at(frame, 32, 1) >>> 24).as("above the k").isZero();
    }

    @Test
    void theLowerArmIsStillOnItsWayAt600Ms() {
        assertThat(at(paint(600), 48, 52)).isNotEqualTo(NAVY);
    }

    @Test
    void theKDipsWhileItSettles() {
        assertThat(at(paint(1300), 16, 5) >>> 24).as("the stem's top is lower at 1.3 s").isZero();
        assertThat(at(paint(Splash.MOTION_MS), 16, 5)).isEqualTo(NAVY);
    }

    @Test
    void theAnimationIsTheMotionAndAShortHold() {
        assertThat(Splash.MOTION_MS).isEqualTo(1550);
        assertThat(Splash.HOLD_MS).isEqualTo(300);
    }

    @Test
    void headlessMeansNoSplash() {
        assertThat(Splash.showing()).isFalse();   // unit tests run headless
    }

    @Test
    void withoutASplashTheWindowShowsAtOnce() {
        AtomicBoolean shown = new AtomicBoolean();
        Splash.play();
        Splash.whenDone(() -> shown.set(true));
        assertThat(shown).isTrue();
    }

    private static BufferedImage paint(int ms) {
        BufferedImage frame = new BufferedImage(Splash.WIDTH, Splash.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();
        Splash.paintK(g, ms);
        g.dispose();
        return frame;
    }

    /** The pixel at a point of the k's 64-unit grid. */
    private static int at(BufferedImage frame, double x, double y) {
        return frame.getRGB((int) (Splash.K_X + Splash.K_SCALE * x), (int) (Splash.K_Y + Splash.K_SCALE * y));
    }
}
