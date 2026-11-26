package com.weddingevent.admin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;

/** Real images for studio logo tests: StudioLogo reads the header, so zero bytes are no longer a "PNG" */
public final class TestImages {

    private TestImages() {}

    public static byte[] image(String format, int width, int height) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A 1×1 PNG padded with zero bytes after IEND up to {@code bytes} (readers ignore trailing bytes) */
    public static byte[] pngBytes(int bytes) {
        byte[] png = image("png", 1, 1);
        return bytes <= png.length ? png : Arrays.copyOf(png, bytes);
    }

    public static String png(int bytes) {
        return dataUrl("image/png", pngBytes(bytes));
    }

    public static String dataUrl(String type, byte[] bytes) {
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
