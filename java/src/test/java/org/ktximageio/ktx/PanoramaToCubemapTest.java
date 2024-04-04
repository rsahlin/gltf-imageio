package org.ktximageio.ktx;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import org.ktximageio.Orientation;
import org.ktximageio.ktx.FloatImageBuffer.Tonemap;
import org.ktximageio.ktx.ImageReader.ImageFormat;

public class PanoramaToCubemapTest extends Test {

    public static void main(String[] args) throws IOException {
        String filename = getPath("wide_street_01_1k.hdr");
        // String filename = getPath("equirectangle.png");
        // String filename = getPath("chromatic_mini.jpg");
        ImageReader reader = ImageReader.getImageReader(filename);
        ImageHeader header = reader.read(Paths.get(filename));
        FloatImageBuffer buffer = null;
        ImageBuffer imageBuffer = header.getData();
        PanoramaToCubemap convert = new PanoramaToCubemap();
        int windowX = 0;
        int windowY = 0;
        int screenWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode()
                .getWidth();
        for (int i = 0; i < 6; i++) {
            ImageBuffer cubemap = convert.createCubeMapFace(imageBuffer, Orientation.get(i),
                    ImageFormat.VK_FORMAT_R16G16B16_SFLOAT);
            BufferedImage image = AwtImageUtils.toBufferedImage(cubemap, 0, Tonemap.ADAPTED_ACES);
            AwtImageUtils.displayImageWindow(filename, image, windowX, windowY);
            windowX += cubemap.width;
            if (windowX >= screenWidth - cubemap.width) {
                windowX = 0;
                windowY += cubemap.height;
            }
        }
    }

}
