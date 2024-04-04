package org.ktximageio.ktx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.ktximageio.ktx.FloatImageBuffer.BufferHDRProperties;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MetaData;
import org.ktximageio.ktx.ImageReader.MimeFormat;
import org.ktximageio.ktx.KTX.TextureType;

public class ToKTX2Test extends org.ktximageio.ktx.Test {

    public void testBinaryInput() throws IOException {
        // Not implemented
        ToKTX2 main = new ToKTX2(new String[] { "--binary", getPath("binary"), getPath("binary.bin") });
        main.run();
        ImageReader deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(MimeFormat.KTX2.getFilename(getPath("binary"))));
    }

    @Test
    public void testHDRInput() throws IOException {
        String filename = getPath("wide_street_01_1k.hdr");
        ToKTX2 main = new ToKTX2(new String[] { getPath("WideStreetHDR"), filename });
        main.run();

        ImageReader deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(MimeFormat.KTX2.getFilename(getPath("WideStreetHDR"))));
        ImageBuffer ktx2Image = header.getData();

        assertTrue(ktx2Image.depth == 0);
        assertTrue(ktx2Image.faceCount == 1);
        assertTrue(ktx2Image.layerCount == 0);
        assertNotNull(header.getMetaData());
        assertNotNull(header.getMetaData().getValue(KTX.METADATA[0]));
        assertNotNull(header.getMetaData().getValue(KTX.METADATA[1]));
        assertNotNull(header.getMetaData().getValue(KTX.METADATA[2]));

        ByteBuffer buffer = ktx2Image.getBuffer();

        ImageReader reader = ImageReader.getImageReader(filename);
        header = reader.read(Paths.get(filename));

        ByteBuffer read = header.getData().getBuffer();
        assertTrue(read.equals(buffer));
    }

    @Test
    public void testKTXCubemapHDR() throws IOException {
        ToKTX2 main = new ToKTX2(
                new String[] { getPath("cubemaphdr"), getPath("px.hdr"), getPath("nx.hdr"), getPath("py.hdr"),
                        getPath("ny.hdr"),
                        getPath("pz.hdr"), getPath("nz.hdr") });
        main.run();

        ImageReader deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(MimeFormat.KTX2.getFilename(getPath("cubemaphdr"))));
        ImageBuffer ktx2Image = header.getData();

        assertTrue(ktx2Image.depth == 0);
        assertTrue(ktx2Image.faceCount == 6);
        assertTrue(ktx2Image.layerCount == 0);

        ImageReader reader = ImageReader.getImageReader(main.getInfiles()[0]);
        for (int i = 0; i < ktx2Image.getImageCount(); i++) {
            header = reader.read(Paths.get(main.getInfiles()[i]));
            ByteBuffer readBuffer = header.getData().getBuffer();
            ByteBuffer ktxBuffer = ktx2Image.getImageBuffer(i);
            assertTrue(readBuffer.equals(ktxBuffer));
        }
    }

    @Test
    public void testKTXCubemapPNG() throws IOException {
        ToKTX2 main = new ToKTX2(
                new String[] { getPath("cubemapsdr"), getPath("left.png"), getPath("right.png"), getPath("top.png"),
                        getPath("bottom.png"),
                        getPath("front.png"), getPath("back.png") });
        main.run();
        ImageReader deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(MimeFormat.KTX2.getFilename(getPath("cubemapsdr"))));
        ImageBuffer ktx2Image = header.getData();

        assertTrue(ktx2Image.depth == 0);
        assertTrue(ktx2Image.faceCount == 6);
        assertTrue(ktx2Image.layerCount == 0);

        ImageReader reader = ImageReader.getImageReader(main.getInfiles()[0]);
        for (int i = 0; i < ktx2Image.getImageCount(); i++) {
            header = reader.read(Paths.get(main.getInfiles()[i]));
            ByteBuffer readBuffer = header.getData().getBuffer();
            ByteBuffer ktxBuffer = ktx2Image.getImageBuffer(i);
            assertTrue(readBuffer.equals(ktxBuffer));
        }
    }

    @Test
    public void testDisplayKTX() throws IOException {
        // First create ktx to display
        // ToKTX2 main = new ToKTX2(new String[] { getPath("displayktx"), getPath("wide_street_01_1k.hdr") });
        ToKTX2 main = new ToKTX2(new String[] { "--displayktx", MimeFormat.KTX2.getFilename(getPath("cubemapsdr")) });
        main.run();
        System.out.println("done");
    }

    @Test
    public void testDisplayKTXCubemap() throws IOException {
        // ToKTX2 main = new ToKTX2(new String[] { "--displayktx",
        // "C:\\source\\gltf-rita\\java\\gltf-rita-lwjgl3\\src\\test\\resources\\assets\\gltf\\EnvironmentLight\\EnvironmentLight_images\\hdrcubemap.ktx2"
        // });
        // ToKTX2 main = new ToKTX2(new String[] { "--displayktx", getPath("hdrcubemap.ktx2") });
        ToKTX2 main = new ToKTX2(new String[] { "--displayktx", getPath("chromatic_mini.ktx2") });
        main.run();
    }

    @Test
    public void testPanoramaToCubemap() throws IOException {
        // String[] names = {"hdrcubemap", "wide_street_01_1k.hdr"};
        // String[] names = {"footprint_court", "C:/source/glTF-Sample-Viewer/assets/environments/footprint_court.hdr"};
        // String[] names = {"cannon_exterior", "C:/source/3DC-Certification/models/Cannon_Exterior.hdr"};
        // String[] names = {"highres", "C:/Users/richa/Downloads/highres.hdr"};
        // String[] names = { "studio_05", "C:/Users/richa/OneDrive/Desktop/project/environmentmaps/studio_05.jpg" };
        // String[] names = { "chromatic_mini", "Chromatic_mini.jpg" };
        String[] names = { "milkyway1", "C:/Users/richa/OneDrive/Desktop/project/environmentmaps/milkyway1.jpg" };

        ToKTX2 main = new ToKTX2(new String[] { "--tocubemap", getPath(names[0]), getPath(names[1]) });
        main.run();
        ImageReader deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(MimeFormat.KTX2.getFilename(getPath(names[0]))));
        ImageBuffer ktx2Image = header.getData();
        if (ktx2Image.getFormat() == ImageFormat.VK_FORMAT_R16G16B16_SFLOAT) {
            assertTrue(HalfFloatImageBuffer.class.isInstance(ktx2Image));
        }
        if (ktx2Image.getFormat() == ImageFormat.VK_FORMAT_R32G32B32_SFLOAT) {
            assertTrue(FloatImageBuffer.class.isInstance(ktx2Image));
        }
        MetaData metaData = header.getMetaData();
        assertTrue(ktx2Image.depth == 0);
        assertTrue(ktx2Image.faceCount == 6);
        assertTrue(ktx2Image.layerCount == 0);
        assertTrue(ktx2Image.getTextureType() == TextureType.CUBEMAP);
        assertNotNull(metaData);
        if (ktx2Image.getFormat().isFloatFormat()) {
            BufferHDRProperties props = null;
            for (int i = 0; i < ktx2Image.faceCount; i++) {
                props = BufferHDRProperties.get((FloatImageBuffer) ktx2Image, i, props);
            }
            assertEquals(props.minVal, metaData.getAsFloatValue(KTX.METADATA[0]));
            assertEquals(props.maxVal, metaData.getAsFloatValue(KTX.METADATA[1]));
        }
    }

}
