package com.benji.oasiso.client.gui;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class GifFrameDecoder {
    private static final int MAX_FRAMES = 1000;
    private static final int MAX_CANVAS_SIZE = 8192;

    private GifFrameDecoder() {
    }

    static Decoded decode(InputStream stream) throws IOException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            if (imageStream == null) {
                throw new IOException("Cannot open GIF stream");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                throw new IOException("No GIF ImageIO reader is available");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, false, false);
                int frameCount = reader.getNumImages(true);
                if (frameCount <= 0 || frameCount > MAX_FRAMES) {
                    throw new IOException("Invalid GIF frame count: " + frameCount);
                }

                int[] logicalSize = logicalSize(reader);
                int logicalWidth = logicalSize[0];
                int logicalHeight = logicalSize[1];
                if (logicalWidth <= 0 || logicalHeight <= 0 || logicalWidth > MAX_CANVAS_SIZE || logicalHeight > MAX_CANVAS_SIZE) {
                    throw new IOException("Invalid GIF canvas: " + logicalWidth + "x" + logicalHeight);
                }

                BufferedImage canvas = new BufferedImage(logicalWidth, logicalHeight, BufferedImage.TYPE_INT_ARGB);
                List<RawFrame> frames = new ArrayList<>(frameCount);
                BoundsAccumulator allBounds = new BoundsAccumulator();

                for (int index = 0; index < frameCount; index++) {
                    BufferedImage frame = reader.read(index);
                    FrameInfo info = frameInfo(reader.getImageMetadata(index));
                    BufferedImage previous = "restoreToPrevious".equals(info.disposal) ? copyImage(canvas) : null;

                    Graphics2D graphics = canvas.createGraphics();
                    graphics.setComposite(AlphaComposite.SrcOver);
                    int drawX = frame.getWidth() == logicalWidth ? 0 : info.left;
                    int drawY = frame.getHeight() == logicalHeight ? 0 : info.top;
                    graphics.drawImage(frame, drawX, drawY, null);
                    graphics.dispose();

                    Rectangle visible = alphaBounds(canvas);
                    if (visible != null) {
                        allBounds.include(visible);
                    }
                    frames.add(new RawFrame(visible, copyArgb(canvas, visible), Math.min(60_000, Math.max(10, info.delayHundredths * 10))));

                    if ("restoreToBackgroundColor".equals(info.disposal)) {
                        Graphics2D clear = canvas.createGraphics();
                        clear.setComposite(AlphaComposite.Clear);
                        clear.fillRect(info.left, info.top, info.width, info.height);
                        clear.dispose();
                    } else if (previous != null) {
                        canvas = previous;
                    }
                }

                Rectangle crop = allBounds.toRectangle();
                if (crop == null) {
                    crop = new Rectangle(0, 0, 1, 1);
                }
                return new Decoded(logicalWidth, logicalHeight, crop, List.copyOf(frames));
            } finally {
                reader.dispose();
            }
        }
    }

    private static int[] logicalSize(ImageReader reader) throws IOException {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        IIOMetadata metadata = reader.getStreamMetadata();
        if (metadata == null) {
            return new int[]{width, height};
        }

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_stream_1.0");
        IIOMetadataNode descriptor = child(root, "LogicalScreenDescriptor");
        if (descriptor != null) {
            width = integerAttribute(descriptor, "logicalScreenWidth", width);
            height = integerAttribute(descriptor, "logicalScreenHeight", height);
        }
        return new int[]{width, height};
    }

    private static FrameInfo frameInfo(IIOMetadata metadata) {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
        IIOMetadataNode descriptor = child(root, "ImageDescriptor");
        IIOMetadataNode control = child(root, "GraphicControlExtension");

        int left = integerAttribute(descriptor, "imageLeftPosition", 0);
        int top = integerAttribute(descriptor, "imageTopPosition", 0);
        int width = integerAttribute(descriptor, "imageWidth", 1);
        int height = integerAttribute(descriptor, "imageHeight", 1);
        int delay = integerAttribute(control, "delayTime", 1);
        String disposal = control != null ? control.getAttribute("disposalMethod") : "none";
        return new FrameInfo(left, top, width, height, delay, disposal);
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        if (root == null) {
            return null;
        }
        for (int index = 0; index < root.getLength(); index++) {
            if (name.equals(root.item(index).getNodeName())) {
                return (IIOMetadataNode) root.item(index);
            }
        }
        return null;
    }

    private static int integerAttribute(IIOMetadataNode node, String name, int fallback) {
        if (node == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(node.getAttribute(name));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static Rectangle alphaBounds(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int width = image.getWidth();
        int minX = width;
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                if ((pixels[row + x] >>> 24) == 0) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        return maxX >= minX && maxY >= minY ? new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1) : null;
    }

    private static int[] copyArgb(BufferedImage source, Rectangle bounds) {
        if (bounds == null) {
            return new int[0];
        }

        int[] sourcePixels = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        int[] result = new int[bounds.width * bounds.height];
        for (int y = 0; y < bounds.height; y++) {
            System.arraycopy(sourcePixels, (bounds.y + y) * source.getWidth() + bounds.x, result, y * bounds.width, bounds.width);
        }
        return result;
    }

    record Decoded(int canvasWidth, int canvasHeight, Rectangle crop, List<RawFrame> frames) {
    }

    record RawFrame(Rectangle bounds, int[] argb, int delayMillis) {
    }

    private record FrameInfo(int left, int top, int width, int height, int delayHundredths, String disposal) {
    }

    private static final class BoundsAccumulator {
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = -1;
        private int maxY = -1;

        void include(Rectangle bounds) {
            minX = Math.min(minX, bounds.x);
            minY = Math.min(minY, bounds.y);
            maxX = Math.max(maxX, bounds.x + bounds.width - 1);
            maxY = Math.max(maxY, bounds.y + bounds.height - 1);
        }

        Rectangle toRectangle() {
            return maxX >= minX && maxY >= minY ? new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1) : null;
        }
    }
}
