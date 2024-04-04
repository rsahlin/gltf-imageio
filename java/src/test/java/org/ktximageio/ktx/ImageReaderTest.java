package org.ktximageio.ktx;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MimeFormat;

public class ImageReaderTest extends org.ktximageio.ktx.Test {

    @Test
    public void testPNG() throws IOException, URISyntaxException {
        assertImage("equirectangle.png", ImageFormat.VK_FORMAT_A8B8G8R8_UNORM_PACK32);
    }

    @Test
    public void testJPG() throws IOException, URISyntaxException {
        assertImage("Chromatic_mini.jpg", ImageFormat.VK_FORMAT_B8G8R8_UNORM);
    }

    @Test
    public void testHDR() throws IOException {
        ImageReader reader = ImageReader.getImageReader(MimeFormat.HDR);
        String filename = getPath("Neutral2.hdr");
        assertNotNull(reader);
        reader = ImageReader.getImageReader(filename);
        // TODO - how to validate the data in the result buffer?
        assertNotNull(reader);
    }

    void assertImage(String imageName, ImageFormat format) throws IOException, URISyntaxException {
        ImageReader reader = ImageReader.getImageReader(imageName);
        assertNotNull(reader);
        String fileName = getPath(imageName);
        ImageHeader header = reader.read(Paths.get(fileName));
        assertNotNull(header);
        ClassLoader loader = getClass().getClassLoader();
        URL url = loader.getResource(imageName);
        BufferedImage source = ImageIO.read(url);
        assertTrue(header.getFormat() == format);
        assertTrue(header.getWidth() == source.getWidth());
        assertTrue(header.getHeight() == source.getHeight());
        assertImageBuffer(header.getData(), source);
    }

    void assertImageBuffer(ImageBuffer loaded, BufferedImage source) {
        Raster raster = source.getData();
        int bands = raster.getNumBands();
        ByteBuffer loadedBuffer = loaded.getBuffer();
        if (loaded.format.isReverseOrder()) {
            for (int y = 0; y < loaded.height; y++) {
                for (int x = 0; x < loaded.width; x++) {
                    for (int i = bands - 1; i >= 0; i--) {
                        assertTrue((byte) raster.getSample(x, y, i) == loadedBuffer.get());
                    }
                }
            }
        } else {
            for (int y = 0; y < loaded.height; y++) {
                for (int x = 0; x < loaded.width; x++) {
                    for (int i = 0; i < bands; i++) {
                        assertTrue((byte) raster.getSample(x, y, i) == loadedBuffer.get());
                    }
                }
            }
        }
    }

}
