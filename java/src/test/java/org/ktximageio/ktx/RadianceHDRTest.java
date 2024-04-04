package org.ktximageio.ktx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.ktximageio.ktx.FloatImageBuffer.Tonemap;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.output.TonemappWindow;

public class RadianceHDRTest extends org.ktximageio.ktx.Test {

    public static void main(String[] args) throws IOException {
        RadianceHDRTest main = new RadianceHDRTest();
        ImageFormat format = ImageFormat.VK_FORMAT_R16G16B16_SFLOAT;
        String filename = getPath("wide_street_01_1k.hdr");
        RadianceHDRReader hdr = new RadianceHDRReader(format);
        ImageBuffer buffer = hdr.read(Paths.get(filename)).getData();
        TonemappWindow window = new TonemappWindow(filename, null, main);
        window.setFloatImage((FloatImageBuffer) buffer, 0, Tonemap.PERCEPTUAL_QUANTIZER);
        AwtImageUtils.displayImageWindow(window, 0, 0);
    }

    private BufferedImage loadHDRTonemapped(String filename, ImageFormat format) throws IOException {
        RadianceHDRReader hdr = new RadianceHDRReader(format);
        ImageBuffer buffer = hdr.read(Paths.get(filename)).getData();
        BufferedImage image = AwtImageUtils.toBufferedImage(buffer, 0, Tonemap.PERCEPTUAL_QUANTIZER);
        return image;
    }

    @Test
    public void testRadianceLoad() throws IOException {
        String filename = getPath("wide_street_01_1k.hdr");
        RadianceHDRReader hdr = new RadianceHDRReader(ImageFormat.VK_FORMAT_R16G16B16_SFLOAT);
        ImageBuffer buffer = hdr.read(Paths.get(filename)).getData();
        TonemappWindow window = new TonemappWindow(filename, null, this);
        window.setFloatImage((FloatImageBuffer) buffer, 0, Tonemap.PERCEPTUAL_QUANTIZER);
        window.setVisible(true);
    }

}
