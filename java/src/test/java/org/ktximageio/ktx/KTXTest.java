package org.ktximageio.ktx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import org.junit.jupiter.api.Test;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MimeFormat;
import org.ktximageio.ktx.KTX.DataFormatDescriptorBlock;
import org.ktximageio.ktx.KTX.KTXFormat;
import org.ktximageio.ktx.KTX.KTXHeader;
import org.ktximageio.ktx.KTX.KeyValue;
import org.ktximageio.ktx.KTX.KeyValueData;
import org.ktximageio.ktx.KTX.TextureType;
import org.ktximageio.ktx.KTXSerializer.Settings;

public class KTXTest extends org.ktximageio.ktx.Test {

    private byte[] createRandomData(Settings settings) {
        return createRandomData(settings.width, settings.height * settings.faceCount, settings.getFormat());
    }

    private byte[] createRandomData(int width, int height, KTXFormat format) {
        byte[] data = new byte[width * height * format.sizeInBytes];
        byte d = 5;
        for (int i = 0; i < data.length; i++) {
            data[i] = d;
            d += d * 31 + d;
        }
        return data;
    }

    @Test
    public void testKTXDeserializer() throws IOException {

        KTXDeserializer deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(getPath("cubemapsdr.ktx2")));
        header.destroy();
        header = null;

        // Make sure file not in use
        FileChannel fc = FileChannel.open(Paths.get(getPath("cubemapsdr.ktx2")), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        fc.close();
    }

    @Test
    public void testKTX() throws IOException {
        int width = 1000;
        int height = 900;
        Settings settings = new Settings(ImageFormat.VK_FORMAT_R16G16B16_SFLOAT, 0, 1, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, null, MimeFormat.KTX2.getFilename("testktx"));

        settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 1, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, null, MimeFormat.KTX2.getFilename("testktx"));
    }

    @Test
    public void testKTXWithMetaData() throws IOException {
        int width = 500;
        int height = 499;
        Settings settings = new Settings(ImageFormat.VK_FORMAT_R16G16B16_SFLOAT, 0, 6, 0, width, height, 0);
        KeyValue[] data = new KeyValue[5];
        for (int i = 0; i < data.length; i++) {
            data[i] = new KeyValue("Value" + i, Integer.toString(i));
        }
        KeyValueData metaData = new KeyValueData(data);
        saveLoadAssert(createRandomData(settings), settings, metaData, MimeFormat.KTX2.getFilename("testktx"));

        settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 6, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, metaData, MimeFormat.KTX2.getFilename("testktx"));
    }

    @Test
    public void testKTXR32G32B32SFLOAT() throws IOException {
        int width = 100;
        int height = 100;
        Settings settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 1, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, null, MimeFormat.KTX2.getFilename("testktxr32g32b32"));
    }

    @Test
    public void testKTXFaces() throws IOException {
        int width = 100;
        int height = 100;
        Settings settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 2, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, null, MimeFormat.KTX2.getFilename("testktx2faces"));

        settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 6, 0, width, height, 0);
        saveLoadAssert(createRandomData(settings), settings, null, MimeFormat.KTX2.getFilename("testktx6faces"));
    }

    private void saveLoadAssert(byte[] data, Settings settings, KeyValueData metaData, String filename)
            throws IOException {
        KTXSerializer serializer = new KTXSerializer();
        Path filepath = Paths.get(getPath(filename));
        serializer.serialize(filepath, metaData, settings, data);

        KTXDeserializer deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(filepath);

        // Check some implementation details
        KTXHeader ktxHeader = (KTXHeader) header;
        DataFormatDescriptorBlock[] dfdBlocks = ktxHeader.getDFDBlocks();
        assertTrue(dfdBlocks[0].getVersionNumber() == 2);
        assertTrue(dfdBlocks[0].getVendorID() == 0);
        assertTrue(dfdBlocks[0].getDescriptorType() == 0);

        // Check header
        assertTrue(header.getFaceCount() == settings.faceCount);
        assertTrue(header.getLevelCount() == settings.levelCount);
        assertTrue(header.getLayerCount() == settings.layerCount);
        assertTrue(header.getFormat().value == settings.getFormat().value);
        assertTrue(header.getWidth() == settings.width);
        assertTrue(header.getHeight() == settings.height);
        ImageBuffer buffer = header.getData();
        assertTrue(buffer.depth == settings.depth);
        assertTrue(buffer.width == settings.width);
        assertTrue(buffer.height == settings.height);
        assertTrue(buffer.layerCount == settings.layerCount);
        assertTrue(buffer.faceCount == settings.faceCount);
        ByteBuffer bb = buffer.getBuffer();
        byte[] readData = new byte[bb.capacity()];
        bb.get(readData);
        assertTrue(Arrays.equals(data, readData));
        if (settings.faceCount == 6) {
            assertTrue(buffer.getTextureType() == TextureType.CUBEMAP);
        }
        header.destroy();
    }

    @Test
    public void testLoadHDRSaveKTX() throws IOException, DataFormatException {
        RadianceHDRReader hdr = new RadianceHDRReader(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT);
        ImageBuffer buffer = hdr.read(Paths.get(getPath("wide_street_01_1k.hdr"))).getData();
        int width = buffer.width;
        int height = buffer.height;
        Settings settings = new Settings(ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 1, 0, width, height, 0);
        ByteBuffer bb = buffer.getBuffer();
        byte[] data = new byte[bb.capacity()];
        bb.get(data);
        saveLoadAssert(data, settings, null, MimeFormat.KTX2.getFilename("wide_street_01_1k"));
    }

    public static void main(String[] args) throws IOException {
        ImageReader reader = new KTXDeserializer();
        String filePath = getPath("Neutral2.ktx");
        ImageHeader header = reader.read(Paths.get(filePath));
        ImageBuffer buffer = header.getData();
        BufferedImage image = AwtImageUtils.toBufferedImage(buffer, 0);
        AwtImageUtils.displayImageWindow(filePath, image, 0, 0);
    }

}
