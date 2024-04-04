package org.ktximageio.ktx;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import org.ktximageio.ktx.ImageUtils.ImageType;

/**
 * Class that uses ImageIO to read images
 * Loaded images using BufferedImage, the raster will be stored as is, with the matching ImageFormat.
 *
 */
public class ImageIOReader implements org.ktximageio.ktx.ImageReader {

    /**
     * Create an inputstream from a ByteBuffer
     *
     */
    public static class ByteBufferInputStream extends InputStream {

        private ByteBuffer byteBuffer;
        private int mark = -1;
        private int readLimit;

        public ByteBufferInputStream(ByteBuffer buffer) {
            if (buffer == null) {
                throw new IllegalArgumentException("ByteBuffer is Null");
            }
            byteBuffer = buffer;
        }

        @Override
        public int available() {
            return byteBuffer.limit() - byteBuffer.position();
        }

        @Override
        public void close() {
            this.byteBuffer = null;
        }

        @Override
        public void mark(int limit) {
            mark = byteBuffer.position();
            readLimit = limit;
        }

        @Override
        public int read() throws IOException {
            if (available() > 0) {
                readLimit -= mark != -1 ? 1 : 0;
                return byteBuffer.get();

            }
            return -1;
        }

        @Override
        public int read(byte[] destination) {
            return read(destination, 0, destination.length);
        }

        @Override
        public int read(byte[] destination, int offset, int length) {
            if (available() < length) {
                length = available();
                if (length == 0) {
                    return -1;
                }
            }
            readLimit -= mark != -1 ? destination.length : 0;
            byteBuffer.get(destination, offset, length);
            return length;
        }

        @Override
        public void reset() {
            if (mark != -1) {
                byteBuffer.position(mark);
            }
        }

    }

    public static class ImageIOHeader implements ImageHeader {

        private final ImageBuffer image;
        private final ImageFormat format;
        private final int width;
        private final int height;

        private ImageIOHeader(byte[] bitmap, ImageFormat f, int w, int h) {
            format = f;
            width = w;
            height = h;
            image = ImageBuffer.create(bitmap, f, 1, w, h, TransferFunction.LINEAR);
        }

        private ImageIOHeader(short[] bitmap, ImageFormat f, int w, int h) {
            format = f;
            width = w;
            height = h;
            image = ImageBuffer.create(bitmap, f, w, h);
        }

        @Override
        public ImageFormat getFormat() {
            return format;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public int getLevelCount() {
            return 0;
        }

        @Override
        public int getLayerCount() {
            return 0;
        }

        @Override
        public int getFaceCount() {
            return 1;
        }

        @Override
        public ImageBuffer getData() {
            return image;
        }

        @Override
        public MetaData getMetaData() {
            return null;
        }

        @Override
        public void destroy() {
        }

    }

    @Override
    public ImageHeader read(Path path) throws IOException {
        return read(new FileInputStream(path.toFile()));
    }

    @Override
    public ImageHeader read(ByteBuffer buffer) throws IOException {
        return read(new ByteBufferInputStream(buffer));
    }

    private ImageHeader read(InputStream stream) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(stream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (readers.hasNext()) {
            // pick the first available ImageReader
            ImageReader reader = readers.next();
            // attach source to the reader
            reader.setInput(iis, true, true);
            ImageTypeSpecifier rawType = reader.getImageTypes(0).next();
            if (rawType == null) {
                throw new IllegalArgumentException("INVALID VALUE, no ImageTypeSpecifier");
            }
            BufferedImage sourceImage = reader.read(0);
            reader.dispose();
            reader = null;
            return getPixelsAsArray(sourceImage, rawType);
        }
        return null;

    }

    private static final ImageFormat[][] NUM_BANDS_FORMATS = new ImageFormat[][] {
            { ImageFormat.VK_FORMAT_R8_UNORM, ImageFormat.VK_FORMAT_R8G8_UNORM, ImageFormat.VK_FORMAT_R8G8B8_UNORM,
                    ImageFormat.VK_FORMAT_R8G8B8A8_UNORM },
            { ImageFormat.VK_FORMAT_R16_UNORM, ImageFormat.VK_FORMAT_R16G16_UNORM,
                    ImageFormat.VK_FORMAT_R16G16B16_UNORM, ImageFormat.VK_FORMAT_R16G16B16A16_UNORM }
    };

    private ImageFormat resolveCustomFormat(ImageTypeSpecifier rawType) {
        int numOfBands = rawType.getNumBands();
        int[] bitsPerSample = new int[numOfBands];
        for (int i = 0; i < numOfBands; i++) {
            bitsPerSample[i] = i < numOfBands ? rawType.getBitsPerBand(i) : rawType.getBitsPerBand(numOfBands - 1);
            if (bitsPerSample[i] != bitsPerSample[0]) {
                throw new IllegalArgumentException("Not implemented support for different bits per sample");
            }
        }
        ImageFormat[] formats = null;
        switch (bitsPerSample[0]) {
            case 8:
                if (rawType.getColorModel().hasAlpha()) {
                    return NUM_BANDS_FORMATS[0][rawType.getColorModel().getNumComponents() - 1];
                }
                return NUM_BANDS_FORMATS[0][rawType.getColorModel().getNumColorComponents() - 1];
            case 16:
                formats = NUM_BANDS_FORMATS[1];
                break;
            case 4:
                return NUM_BANDS_FORMATS[0][rawType.getColorModel().getNumColorComponents() - 1];
            default:
                throw new IllegalArgumentException(
                        "Not implemented support for " + bitsPerSample[0] + " bits per sample");
        }
        return formats[numOfBands - 1];
    }

    private ImageHeader getPixelsAsArray(BufferedImage source, ImageTypeSpecifier rawType) {
        long start = System.currentTimeMillis();
        Raster raster = source.getData();
        ImageFormat format = ImageType.getImageType(source.getType()).format[0];
        if (format == ImageFormat.VK_FORMAT_UNDEFINED) {
            format = resolveCustomFormat(rawType);
        }
        if (raster.getTransferType() == DataBuffer.TYPE_BYTE) {
            DataBufferByte dbb = (DataBufferByte) raster.getDataBuffer();
            byte[] byteBuffer = getImageDataAsBytes(source, dbb);
            System.out.println(
                    "getPixels to format " + format + ", took " + (System.currentTimeMillis() - start) + " millis");
            return new ImageIOHeader(byteBuffer, format, source.getWidth(),
                    source.getHeight());
        } else if (raster.getTransferType() == DataBuffer.TYPE_SHORT) {
            DataBufferShort shortBuffer = (DataBufferShort) raster.getDataBuffer();
            return new ImageIOHeader(shortBuffer.getData(), format,
                    source.getWidth(), source.getHeight());
        } else if (raster.getTransferType() == DataBuffer.TYPE_USHORT) {
            DataBufferUShort shortBuffer = (DataBufferUShort) raster.getDataBuffer();
            return new ImageIOHeader(shortBuffer.getData(), format,
                    source.getWidth(), source.getHeight());
        } else {
            throw new IllegalArgumentException(
                    "Not implemented support for DataBuffer type " + raster.getTransferType());
        }
    }

    private byte[] getImageDataAsBytes(BufferedImage source, DataBufferByte dataBuffer) {
        ImageType type = ImageType.getImageType(source.getType());
        if (type == ImageType.TYPE_BYTE_INDEXED) {
            return handleByteIndexed(source, dataBuffer.getData());
        }
        if (type == ImageType.TYPE_BYTE_BINARY) {
            return handleByteBinary(source, dataBuffer.getData());
        }
        return dataBuffer.getData();
    }

    private byte[] handleByteBinary(BufferedImage source, byte[] data) {
        IndexColorModel icm = (IndexColorModel) source.getColorModel();
        int length = source.getWidth() * source.getHeight();
        byte[] result = new byte[length * 3];
        int mapSize = icm.getMapSize();
        byte[] r = new byte[mapSize];
        byte[] g = new byte[mapSize];
        byte[] b = new byte[mapSize];
        icm.getReds(r);
        icm.getGreens(g);
        icm.getBlues(b);
        int index = 0;
        int value = 0;
        switch (icm.getPixelSize()) {
            case 4:
                for (int i = 0; i < length / 2; i++) {
                    value = (data[i] & 0x0ff);
                    result[index++] = r[value & 0x0f];
                    result[index++] = g[value & 0x0f];
                    result[index++] = b[value & 0x0f];
                    result[index++] = r[value >>> 8];
                    result[index++] = g[value >>> 8];
                    result[index++] = b[value >>> 8];
                }
                return result;
            default:
                throw new IllegalArgumentException("Exception: Not implemented for pixelsize " + icm.getPixelSize());
        }
    }

    private byte[] handleByteIndexed(BufferedImage source, byte[] data) {
        IndexColorModel icm = (IndexColorModel) source.getColorModel();
        if ((icm.hasAlpha())) {
            byte[] resultData = byteIndexedToRGBA(icm, data);
            // ToArray result = new ToArray(source.getWidth(), source.getHeight(), ImageFormat.VK_FORMAT_R8G8B8A8_UNORM,
            // resultData);
            return resultData;
        } else {
            byte[] resultData = byteIndexedToBGR(icm, data);
            // ToArray result = new ToArray(source.getWidth(), source.getHeight(), ImageFormat.VK_FORMAT_R8G8B8A8_UNORM,
            // resultData);
            return resultData;
        }
    }

    private byte[] byteIndexedToBGR(IndexColorModel icm, byte[] data) {
        int length = data.length;
        byte[] result = new byte[length * 3];
        int mapSize = icm.getMapSize();
        byte[] r = new byte[mapSize];
        byte[] g = new byte[mapSize];
        byte[] b = new byte[mapSize];
        icm.getReds(r);
        icm.getGreens(g);
        icm.getBlues(b);
        int index = 0;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (data[i] & 0x0ff);
            result[index++] = r[value];
            result[index++] = g[value];
            result[index++] = b[value];
        }
        return result;
    }

    private byte[] byteIndexedToRGBA(IndexColorModel icm, byte[] data) {
        int length = data.length;
        byte[] result = new byte[length * 4];
        int mapSize = icm.getMapSize();
        byte[] a = new byte[mapSize];
        byte[] r = new byte[mapSize];
        byte[] g = new byte[mapSize];
        byte[] b = new byte[mapSize];
        icm.getAlphas(a);
        icm.getReds(r);
        icm.getGreens(g);
        icm.getBlues(b);
        int index = 0;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = (data[i] & 0x0ff);
            result[index++] = r[value];
            result[index++] = g[value];
            result[index++] = b[value];
            result[index++] = a[value];
        }
        return result;
    }

    @Override
    public MimeFormat[] getMime() {
        return new MimeFormat[] { MimeFormat.JPEG, MimeFormat.PNG };
    }

    @Override
    public String getReaderName() {
        return getClass().getCanonicalName();
    }

}
