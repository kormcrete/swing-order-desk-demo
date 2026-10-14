import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the app icons from the 1024 px kormcrete tile (run by make-icons.sh; not part of the app).
 * Usage: MakeIcons <tile.png> <macos.iconset dir> <windows .ico file> <window icon PNG dir>
 */
public final class MakeIcons {

    public static void main(String[] args) throws IOException {
        BufferedImage tile = ImageIO.read(new File(args[0]));
        if (tile == null || tile.getWidth() != 1024 || tile.getHeight() != 1024) {
            throw new IllegalArgumentException(args[0] + ": needs a 1024 x 1024 PNG");
        }
        BufferedImage rounded = rounded(tile);
        Path iconset = Files.createDirectories(Paths.get(args[1]));
        for (int size : new int[] {16, 32, 128, 256, 512}) {
            write(scale(rounded, size), iconset.resolve("icon_" + size + "x" + size + ".png"));
            write(scale(rounded, 2 * size), iconset.resolve("icon_" + size + "x" + size + "@2x.png"));
        }
        writeIco(tile, new int[] {16, 24, 32, 48, 64, 256}, Paths.get(args[2]));
        Path pngs = Files.createDirectories(Paths.get(args[3]));
        for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
            write(scale(tile, size), pngs.resolve("kormcrete-order-desk-" + size + ".png"));
        }
    }

    /** The macOS app icon grid: an 824 px rounded tile (corner radius 185) with a 100 px margin on 1024. */
    static BufferedImage rounded(BufferedImage tile) {
        BufferedImage out = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(100, 100, 824, 824, 370, 370));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(scale(tile, 824), 100, 100, null);
        g.dispose();
        return out;
    }

    /** Halves with bilinear filtering until the size is reached, so small icons stay crisp. */
    static BufferedImage scale(BufferedImage image, int size) {
        BufferedImage current = image;
        int width = image.getWidth();
        while (width > size) {
            width = Math.max(size, width / 2);
            BufferedImage next = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, width, width, null);
            g.dispose();
            current = next;
        }
        return current;
    }

    static void write(BufferedImage image, Path file) throws IOException {
        ImageIO.write(image, "png", file.toFile());
    }

    /** ICO: 32-bit DIB entries below 256 px (what every Windows resource tool reads), a PNG entry at 256. */
    static void writeIco(BufferedImage tile, int[] sizes, Path file) throws IOException {
        List<byte[]> entries = new ArrayList<>();
        for (int size : sizes) {
            BufferedImage image = scale(tile, size);
            entries.add(size >= 256 ? png(image) : dib(image));
        }
        int length = 6 + 16 * sizes.length;
        for (byte[] entry : entries) length += entry.length;
        ByteBuffer ico = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int offset = 6 + 16 * sizes.length;
        for (int i = 0; i < sizes.length; i++) {
            int side = sizes[i] >= 256 ? 0 : sizes[i];
            ico.put((byte) side).put((byte) side).put((byte) 0).put((byte) 0);
            ico.putShort((short) 1).putShort((short) 32).putInt(entries.get(i).length).putInt(offset);
            offset += entries.get(i).length;
        }
        for (byte[] entry : entries) ico.put(entry);
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, ico.array());
    }

    static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** BITMAPINFOHEADER (height doubled for the mask), BGRA rows bottom-up, then an all-zero AND mask. */
    static byte[] dib(BufferedImage image) {
        int size = image.getWidth();
        int maskRow = (size + 31) / 32 * 4;
        int pixels = size * size * 4;
        ByteBuffer dib = ByteBuffer.allocate(40 + pixels + maskRow * size).order(ByteOrder.LITTLE_ENDIAN);
        dib.putInt(40).putInt(size).putInt(2 * size).putShort((short) 1).putShort((short) 32)
                .putInt(0).putInt(pixels + maskRow * size).putInt(0).putInt(0).putInt(0).putInt(0);
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                int argb = image.getRGB(x, y);
                dib.put((byte) argb).put((byte) (argb >> 8)).put((byte) (argb >> 16)).put((byte) (argb >>> 24));
            }
        }
        return dib.array();
    }
}
