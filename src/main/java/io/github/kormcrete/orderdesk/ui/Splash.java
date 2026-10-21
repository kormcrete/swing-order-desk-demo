package io.github.kormcrete.orderdesk.ui;

import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.SplashScreen;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

/**
 * The splash: the JVM shows {@link #IMAGE} (the jar manifest's SplashScreen-Image) as soon as it starts, white with
 * the website's wordmark bottom-right, made by packaging/splash/make-splash.sh. Order Desk then paints the kormcrete k
 * building itself on it, frame by frame, as on the website's home page (Mark.astro and the build-* keyframes in its
 * global.css), and shows the main window when the k is complete. The JVM's own GIF animation runs at 10 frames a
 * second at most, too slow for this.
 */
final class Splash {

    static final int WIDTH = 500;
    static final int HEIGHT = 400;
    /** On the class path; {@code splash@2x.png} beside it for 2x screens. */
    static final String IMAGE = "io/github/kormcrete/orderdesk/splash.png";
    /** The last move (the settle) ends at 1.55 s; the finished k then stays 0.3 s before the window opens. */
    static final int MOTION_MS = 1550;
    static final int HOLD_MS = 300;
    /** The k's 64-unit grid: 200 px, its letter centred left to right and a little above the middle. */
    static final double K_SCALE = 3.125;
    static final double K_X = 144;
    static final double K_Y = 90;

    private static final int NAVY = 0x1a4a6b;
    private static final int CYAN = 0x76d8ff;
    private static final int FRAME_MS = 16;
    private static final Bezier PIECE = new Bezier(0.2, 0.7, 0.2, 1);
    private static final Bezier POP = new Bezier(0.3, 1.6, 0.5, 1);
    private static final Bezier EASE_OUT = new Bezier(0, 0, 0.58, 1);

    private static boolean playing;
    private static Runnable whenDone;

    private Splash() {
    }

    /** Whether the JVM shows the splash (never in a headless JVM). */
    static boolean showing() {
        if (GraphicsEnvironment.isHeadless()) return false;
        try {
            return SplashScreen.getSplashScreen() != null;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    /** Starts building the k on the splash, on a thread of its own; does nothing without a splash. */
    static void play() {
        if (!showing()) return;
        SplashScreen splash = SplashScreen.getSplashScreen();
        synchronized (Splash.class) {
            if (playing) return;
            playing = true;
        }
        Thread player = new Thread(() -> animate(splash), "splash");
        player.setDaemon(true);
        player.start();
    }

    /** Runs show now when no splash plays, else on the event dispatch thread once the k is complete. */
    static void whenDone(Runnable show) {
        synchronized (Splash.class) {
            if (playing) {
                whenDone = show;
                return;
            }
        }
        show.run();
    }

    /**
     * Builds what the splash leads to. If that fails, the splash closes before the failure goes on: otherwise the
     * finished k would stay on screen with no app behind it, looking hung.
     */
    static <T> T building(Supplier<T> build) {
        try {
            return build.get();
        } catch (RuntimeException | Error e) {
            close();
            throw e;
        }
    }

    private static void close() {
        if (!showing()) return;
        try {
            SplashScreen.getSplashScreen().close();   // the painter's next update() then ends the animation
        } catch (IllegalStateException | NullPointerException e) {
            // closed in the meantime
        }
    }

    private static void animate(SplashScreen splash) {
        try {
            Graphics2D g = splash.createGraphics();
            double scale = splash.getSize().getWidth() / WIDTH;   // the overlay may be in device pixels
            g.scale(scale, scale);
            long start = System.nanoTime();
            for (int ms = 0; ; ms = (int) ((System.nanoTime() - start) / 1_000_000)) {
                int at = Math.min(ms, MOTION_MS);
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, WIDTH, HEIGHT);
                g.setComposite(AlphaComposite.SrcOver);
                paintK(g, at);
                splash.update();
                if (at == MOTION_MS) break;
                Thread.sleep(FRAME_MS);
            }
            Thread.sleep(HOLD_MS);
        } catch (IllegalStateException | InterruptedException e) {
            // the splash closed early (a window showed): open Order Desk's now
        } finally {
            Runnable show;
            synchronized (Splash.class) {
                playing = false;
                show = whenDone;
                whenDone = null;
            }
            if (show != null) SwingUtilities.invokeLater(show);
        }
    }

    /**
     * The k at ms into the animation, in splash coordinates: stem, block, upper arm, lower arm in the SVG's order (the
     * arms cover the block's corners). The pieces move in grid units, as the browser moves the SVG's parts, so they
     * come in from outside the splash, fading in.
     */
    static void paintK(Graphics2D g, int ms) {
        Graphics2D k = (Graphics2D) g.create();
        k.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        k.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        k.translate(K_X, K_Y);
        k.scale(K_SCALE, K_SCALE);
        k.translate(0, settle(ms));
        piece(k, new RoundRectangle2D.Double(10, 4, 12, 52, 6, 6), ms, 0, 0, -90);
        pop(k, new Rectangle2D.Double(25, 31, 12, 12), ms, 1050);
        piece(k, polygon(33, 31, 45, 18, 56, 18, 40, 35), ms, 350, 70, -40);
        piece(k, polygon(33, 43, 47, 56, 58, 56, 40, 39), ms, 500, 70, 40);
        k.dispose();
    }

    /** build-come: from (dx, dy) and transparent to its place, 0.5 s from delayMs. */
    private static void piece(Graphics2D g, Shape shape, int ms, int delayMs, double dx, double dy) {
        double e = PIECE.at(progress(ms, delayMs, 500));
        double rest = 1 - e;
        fill(g, AffineTransform.getTranslateInstance(rest * dx, rest * dy).createTransformedShape(shape), NAVY, e);
    }

    /** build-pop: the cyan block grows from its centre with an overshoot, 0.4 s from delayMs. */
    private static void pop(Graphics2D g, Rectangle2D block, int ms, int delayMs) {
        double e = POP.at(progress(ms, delayMs, 400));
        AffineTransform grow = AffineTransform.getTranslateInstance(block.getCenterX(), block.getCenterY());
        grow.scale(e, e);
        grow.translate(-block.getCenterX(), -block.getCenterY());
        fill(g, grow.createTransformedShape(block), CYAN, e);
    }

    /** build-k-settle: the whole k dips 3 units and comes back, ease-out per keyframe (0, 40, 100 %), 0.35 s from 1.2 s. */
    private static double settle(int ms) {
        double p = progress(ms, 1200, 350);
        return p <= 0.4 ? 3 * EASE_OUT.at(p / 0.4) : 3 * (1 - EASE_OUT.at((p - 0.4) / 0.6));
    }

    private static void fill(Graphics2D g, Shape shape, int rgb, double opacity) {
        float alpha = (float) Math.max(0, Math.min(1, opacity));
        if (alpha == 0) return;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(new Color(rgb));
        g.fill(shape);
    }

    private static double progress(int ms, int delayMs, int durationMs) {
        return Math.max(0, Math.min(1, (ms - delayMs) / (double) durationMs));
    }

    private static Shape polygon(double... xy) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        path.closePath();
        return path;
    }

    /** A CSS cubic-bezier timing function: y at the x given, the curve's parameter found by bisection. */
    private static final class Bezier {
        private final double x1, y1, x2, y2;

        Bezier(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        double at(double x) {
            if (x <= 0) return 0;
            if (x >= 1) return 1;
            double lo = 0, hi = 1, u = x;
            for (int i = 0; i < 40; i++) {
                u = (lo + hi) / 2;
                if (curve(u, x1, x2) < x) lo = u; else hi = u;
            }
            return curve(u, y1, y2);
        }

        private static double curve(double u, double p1, double p2) {
            double v = 1 - u;
            return 3 * v * v * u * p1 + 3 * v * u * u * p2 + u * u * u;
        }
    }
}
