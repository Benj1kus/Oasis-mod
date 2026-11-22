import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Run during processResources. The source art is always read-only.
public class GenerateFeralTextures {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]), output = Path.of(args[1]);
        Files.createDirectories(output);
        for (String name : Files.readAllLines(Path.of(args[2]))) {
            generate(source, output, name);
        }
    }

    private static void generate(Path src, Path out, String name) throws Exception {
        Path original = src.resolve(name + ".png");
        Path custom = src.resolve("dry_" + name + ".png");
        BufferedImage healthy = ImageIO.read(original.toFile());
        int w = healthy.getWidth(), h = healthy.getHeight();
        boolean authored = Files.exists(custom);
        BufferedImage dry = authored ? ImageIO.read(custom.toFile()) : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (!authored) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = healthy.getRGB(x, y);
                    int l = (54 * channel(c,16) + 183 * channel(c,8) + 19 * channel(c,0)) / 256;
                    int r = Math.min(255, 28 + l), g = 17 + l * 4 / 5, b = 14 + l * 3 / 5;
                    dry.setRGB(x, y, (c & 0xff000000) | r << 16 | g << 8 | b);
                }
            }
            ImageIO.write(dry, "png", out.resolve(custom.getFileName()).toFile());
        }
        Path meta = src.resolve(name + ".png.mcmeta");
        if (Files.exists(meta) && !Files.exists(src.resolve("dry_" + name + ".png.mcmeta"))
                && dry.getHeight() * (long)w == h * (long)dry.getWidth()) {
            Files.copy(meta, out.resolve("dry_" + name + ".png.mcmeta"), StandardCopyOption.REPLACE_EXISTING);
        }
        for (int stage = 1; stage <= 2; stage++) {
            String prefix = "wilting" + stage + "_";
            BufferedImage mixed = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            // A static dry sprite can also be used with an animated healthy strip.
            boolean repeat = Files.exists(meta) && dry.getWidth() == dry.getHeight() && h > w;
            for (int y = 0; y < h; y++) {
                int sy = repeat ? (y % w) * dry.getHeight() / w : y * dry.getHeight() / h;
                for (int x = 0; x < w; x++) {
                    int a = healthy.getRGB(x,y), b = dry.getRGB(x * dry.getWidth() / w, sy);
                    mixed.setRGB(x,y, blend(a,b,stage));
                }
            }
            ImageIO.write(mixed,"png",out.resolve(prefix + name + ".png").toFile());
            if (Files.exists(meta)) Files.copy(meta,out.resolve(prefix + name + ".png.mcmeta"),StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int channel(int pixel, int shift) { return (pixel >>> shift) & 255; }

    private static int blend(int a, int b, int step) {
        int wa = channel(a,24) * (3-step), wb = channel(b,24) * step;
        int total = wa + wb;
        if (total == 0) return a;
        int pixel = ((total + 1) / 3) << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            pixel |= ((channel(a,shift)*wa + channel(b,shift)*wb + total/2) / total) << shift;
        }
        return pixel;
    }
}
