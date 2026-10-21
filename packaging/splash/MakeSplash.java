import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes the splash's still image: white, the website's hairline around it and the website's wordmark ("kormcrete",
 * "Solutions" under it) in the bottom-right corner. Order Desk paints the k building itself on it while it starts.
 * Run by make-splash.sh; not part of the app.
 * Usage: MakeSplash <wordmark dir> <resources dir>: reads wordmark.png and wordmark@2x.png, writes splash.png
 * (500 x 400) and splash@2x.png.
 */
public final class MakeSplash {

    static final int WIDTH = 500;
    static final int HEIGHT = 400;
    /** The website's hairline (--color-hairline): the light line around its cards and menus. */
    static final int HAIRLINE = 0xd5dee6;
    /** The wordmark's distance from the right and bottom edges. */
    static final int MARGIN_RIGHT = 20;
    static final int MARGIN_BOTTOM = 18;

    public static void main(String[] args) throws IOException {
        Path wordmarks = Paths.get(args[0]);
        Path resources = Files.createDirectories(Paths.get(args[1]));
        write(still(1, ImageIO.read(wordmarks.resolve("wordmark.png").toFile())), resources.resolve("splash.png"));
        write(still(2, ImageIO.read(wordmarks.resolve("wordmark@2x.png").toFile())), resources.resolve("splash@2x.png"));
    }

    static BufferedImage still(int scale, BufferedImage wordmark) {
        int width = WIDTH * scale;
        int height = HEIGHT * scale;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(wordmark, width - MARGIN_RIGHT * scale - wordmark.getWidth(),
                height - MARGIN_BOTTOM * scale - wordmark.getHeight(), null);
        g.setColor(new Color(HAIRLINE));
        g.fillRect(0, 0, width, scale);
        g.fillRect(0, height - scale, width, scale);
        g.fillRect(0, 0, scale, height);
        g.fillRect(width - scale, 0, scale, height);
        g.dispose();
        return image;
    }

    static void write(BufferedImage image, Path file) throws IOException {
        ImageIO.write(image, "png", file.toFile());
    }
}
