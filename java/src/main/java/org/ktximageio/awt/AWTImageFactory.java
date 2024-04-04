package org.ktximageio.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ColorSpace;

/**
 * Singelton factory to help loading awt BufferedImages
 *
 */
public class AWTImageFactory {

    public enum GLComponentType {
        // VK_FORMAT_R8_SINT = 14,
        BYTE(5120, 1),
        // VK_FORMAT_R8_UINT = 13,
        UNSIGNED_BYTE(5121, 1),
        // VK_FORMAT_R16_SINT = 75,
        SHORT(5122, 2),
        // VK_FORMAT_R16_UINT = 74,
        UNSIGNED_SHORT(5123, 2),
        // VK_FORMAT_R32_UINT = 98,
        UNSIGNED_INT(5125, 4),
        // VK_FORMAT_R32_SFLOAT = 100,
        FLOAT(5126, 4);

        /**
         * The glTF value for the component type
         */
        public final int value;
        /**
         * Size in bytes
         */
        public final int size;

        GLComponentType(int val, int s) {
            size = s;
            value = val;
        }

        /**
         * Returns the component type for the glTF value
         * 
         * @param value glTF component type value
         * @return
         */
        public static GLComponentType getFromValue(int value) {
            for (GLComponentType cp : values()) {
                if (cp.value == value) {
                    return cp;
                }
            }
            return null;
        }

    }

    private static AWTImageFactory factoryInstance;

    public static AWTImageFactory getInstance() {
        if (factoryInstance == null) {
            factoryInstance = new AWTImageFactory();
        }
        return factoryInstance;
    }

    public interface MetadataParser<R> {
        void parseMetadata(R reader);
    }

    /**
     *
     * Image pixel (source) formats - these are the formats that decoded images (from jpeg,png etc) are in.
     */
    public enum AWTImageFormat {

        /**
         * Image type RGB 888, 8 bits per component, 24 bit format.
         */
        RGB(3, 24),
        /**
         * Image type BGR 888, 8 bits per component, 24 bit format.
         */
        BGR(3, 24),
        /**
         * Image type RGBA 8888, 8 bits per component, 32 bit format.
         */
        RGBA(4, 32),
        /**
         * Image type ABGR 8888, 8 bits per component, 32 bit format.
         */
        ABGR(4, 32),
        /**
         * Image type RG 88, 8 bits per component 16 bit format
         */
        RG(2, 16),
        /**
         * Image type GB 88, 8 bits per component 16 bit format
         */
        GB(2, 16),
        /**
         * Image type R 8, 8 bit for Red component
         */
        R(1, 8),
        /**
         * Type rgb with each component 16 bit unsigned short
         */
        RGB_USHORT(3, 48),
        /**
         * Type rgba with each component 16 bit unsigned short
         */
        RGBA_USHORT(4, 64);

        /**
         * The number of components per pixel
         */
        public final int components;
        /**
         * Number of bits per pixel
         */
        public final int bitsPerPixel;

        AWTImageFormat(int count, int bpp) {
            components = count;
            bitsPerPixel = bpp;
        }

        /**
         * Returns the imageformat from number of bits per component
         * 
         * @param bits Number of bits per component
         * @return
         */
        public static AWTImageFormat get(int[] bits) {
            int bitsPerPixel = 0;
            for (int b : bits) {
                bitsPerPixel += b;
            }
            for (AWTImageFormat format : values()) {
                if (bits.length == format.components && format.bitsPerPixel == bitsPerPixel) {
                    return format;
                }
            }
            return null;
        }
    }

    /**
     * Loaded image formats
     *
     */
    public enum AWTSourceFormat {
        /**
         * From java.awt.image.BufferedImage
         */
        TYPE_4BYTE_ABGR(06, 4, AWTImageFormat.ABGR, GLComponentType.UNSIGNED_BYTE),
        TYPE_3BYTE_BGR(05, 3, AWTImageFormat.BGR, GLComponentType.UNSIGNED_BYTE),
        TYPE_INT_ARGB(02, 4, AWTImageFormat.RGBA, GLComponentType.UNSIGNED_BYTE),
        // TODO - use ImageFormat.RGB565 instead?
        TYPE_BYTE_INDEXED(13, 1, AWTImageFormat.RGB, GLComponentType.UNSIGNED_BYTE),
        TYPE_BYTE_GRAY(10, 1, AWTImageFormat.R, GLComponentType.UNSIGNED_BYTE),
        /**
         * Bitmap with RGBA 8888 (eg Android)
         */
        TYPE_RGBA(-1, 4, AWTImageFormat.RGBA, GLComponentType.UNSIGNED_BYTE),
        /**
         * Bitmap with RGB 888 (eg Android)
         */
        TYPE_RGB(-1, 3, AWTImageFormat.RGB, GLComponentType.UNSIGNED_BYTE),
        TYPE_RGB_USHORT(-1, 6, AWTImageFormat.RGB_USHORT, GLComponentType.UNSIGNED_SHORT),
        TYPE_RGBA_USHORT(-1, 8, AWTImageFormat.RGBA_USHORT, GLComponentType.UNSIGNED_SHORT),
        TYPE_2BYTE(-1, 2, AWTImageFormat.RG, GLComponentType.UNSIGNED_BYTE);

        /**
         * The AWT BufferedImage type
         */
        public final int type;
        /**
         * The size in bytes of each pixel
         */
        public final int size;
        /**
         * Dataformat of components
         */
        public final GLComponentType componentType;
        /**
         * The most closely matching imageformat that can be used when loading
         */
        public final AWTImageFormat imageFormat;

        AWTSourceFormat(int t, int s, @NonNull AWTImageFormat format, GLComponentType glType) {
            type = t;
            size = s;
            imageFormat = format;
            componentType = glType;
        }

        public static AWTSourceFormat get(BufferedImage image) {
            AWTSourceFormat sourceFormat = get(image.getType());
            return sourceFormat == null ? getFromColorModel(image.getColorModel()) : sourceFormat;
        }

        public static AWTSourceFormat get(int type) {
            for (AWTSourceFormat format : AWTSourceFormat.values()) {
                if (format.type == type) {
                    return format;
                }
            }
            return null;
        }

        public static AWTSourceFormat getFromColorModel(java.awt.image.ColorModel colorModel) {
            java.awt.color.ColorSpace colorSpace = colorModel.getColorSpace();
            if (colorSpace.getType() == java.awt.color.ColorSpace.CS_GRAY
                    || colorSpace.getType() == java.awt.color.ColorSpace.TYPE_GRAY) {
                if (colorModel.getComponentSize()[0] == 8) {
                    return AWTSourceFormat.TYPE_BYTE_GRAY;
                } else {
                    throw new IllegalArgumentException(
                            "Exception: Not implemented for colorspace GRAY and bits per component= "
                                    + colorModel.getComponentSize()[0]);
                }
            }
            int components = colorModel.getNumComponents();
            int[] bits = colorModel.getComponentSize();
            int bitsPerComponent = bits[0];
            switch (components) {
                case 1:
                    return AWTSourceFormat.TYPE_BYTE_GRAY;
                case 2:
                    return AWTSourceFormat.TYPE_2BYTE;
                case 3:
                    return bitsPerComponent == 8 ? AWTSourceFormat.TYPE_RGB
                            : bitsPerComponent == 16 ? AWTSourceFormat.TYPE_RGB_USHORT : null;
                case 4:
                    return bitsPerComponent == 8 ? AWTSourceFormat.TYPE_RGBA
                            : bitsPerComponent == 16 ? AWTSourceFormat.TYPE_RGBA_USHORT : null;
                default:
                    throw new IllegalArgumentException("Exception: Not implemented for components=" + components);
            }
        }

    }

    enum AWTColorSpace {
        CS_CIEXYZ(1001),
        CS_GRAY(1003),
        CS_LINEAR_RGB(1004),
        CS_PYCC(1002),
        CS_sRGB(1000),
        TYPE_2CLR(12),
        TYPE_3CLR(13),
        TYPE_4CLR(14),
        TYPE_5CLR(15),
        TYPE_6CLR(16),
        TYPE_7CLR(17),
        TYPE_8CLR(18),
        TYPE_9CLR(19),
        TYPE_ACLR(20),
        TYPE_BCLR(21),
        TYPE_CCLR(22),
        TYPE_CMY(11),
        TYPE_CMYK(9),
        TYPE_DCLR(23),
        TYPE_ECLR(24),
        TYPE_FCLR(25),
        TYPE_GRAY(6),
        TYPE_HLS(8),
        TYPE_HSV(7),
        TYPE_Lab(1),
        TYPE_Luv(2),
        TYPE_RGB(5),
        TYPE_XYZ(0),
        TYPE_YCbCr(3),
        TYPE_Yxy(4);

        public final int type;

        AWTColorSpace(int t) {
            type = t;
        }

        public static AWTColorSpace getFromType(int type) {
            for (AWTColorSpace cs : values()) {
                if (cs.type == type) {
                    return cs;
                }
            }
            return null;
        }

    }

    private static class AwtPlatformImage extends PlatformImage<BufferedImage> {

        private final BufferedImage image;

        private AwtPlatformImage(@NonNull BufferedImage img, @NonNull ColorSpace sourceColorSpace,
                @NonNull AWTImageFormat format, int compressedSize) {
            super(sourceColorSpace, format);
            image = img;
            setCompressedSize(compressedSize);
        }

        @Override
        public BufferedImage getImage() {
            return image;
        }
    }

    public abstract static class PlatformImage<T> {

        private final AWTImageFormat format;
        private final ColorSpace sourceColorSpace;
        private long compressedSizeInBytes;
        private String uri;
        private String mime;

        protected PlatformImage(ColorSpace colorSpace, AWTImageFormat f) {
            if (f == null) {
                throw new IllegalArgumentException("Invalid value: Null");
            }
            format = f;
            sourceColorSpace = colorSpace;
        }

        public abstract T getImage();

        /**
         * Returns the awt image format
         * 
         * @return The format of the image
         */
        public AWTImageFormat getFormat() {
            return format;
        }

        /**
         * Returns the colorspace
         * 
         * @return The colorspace of the image
         */
        public ColorSpace getSourceColorSpace() {
            return sourceColorSpace;
        }

        /**
         * Sets the compressed size in bytes - this is for information purposes only
         * 
         * @param sizeInBytes
         */
        public void setCompressedSize(long sizeInBytes) {
            compressedSizeInBytes = sizeInBytes;
        }

        /**
         * Returns the compressed size in bytes, if it has been set by calling {@link #setCompressedSize(int)}
         * This is for information purposes only
         * 
         * @return
         */
        public long getCompressedSize() {
            return compressedSizeInBytes;
        }

        /**
         * Sets the uri - may only be called once
         * 
         * @param uriStr
         */
        public void setUri(String uriStr) {
            if (uri != null) {
                throw new IllegalArgumentException("Already set uri: " + uri);
            }
            uri = uriStr;
        }

        /**
         * Returns the uri of the image if set by calling {@link #setUri(String)} otherwise null
         * 
         * @return The uri of the image or null
         */
        public String getUri() {
            return uri;
        }

        /**
         * Sets the mimetype
         * 
         * @param mimeStr
         */
        public void setMimeType(String mimeStr) {
            mime = mimeStr;
        }

        /**
         * Returns the mime type of the image, if set by calling {@link #setMimeType(String)} otherwise null
         * 
         * @return The mimetype of the image or null
         */
        public String getMimeType() {
            return mime;
        }

    }

    public class ToByte {

        /**
         * Creates a new IndexedToByte for the specified format
         * 
         * @param w
         * @param h
         * @param format
         * @param pixels
         */
        ToByte(int w, int h, AWTSourceFormat format, byte[] pixels) {
            width = w;
            height = h;
            sourceFormat = format;
            resultData = pixels;
        }

        final int width;
        final int height;
        final AWTSourceFormat sourceFormat;
        private byte[] resultData;

    }

    public static BufferedImage createBufferedImage(int width, int height, int columns, int rows, float green,
            float blue) {
        java.awt.color.ColorSpace xyzCS = java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_sRGB);
        ComponentColorModel cm = new ComponentColorModel(xyzCS, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        BufferedImage xyzImage = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        float xColor = green;
        float yColor = blue;
        float xColorDelta = 1.0f / (columns - 1);
        float yColorDelta = 1.0f / (rows - 1);
        Graphics graphics = xyzImage.getGraphics();
        for (int y = 0; y < rows; y++) {
            xColor = green;
            for (int x = 0; x < columns; x++) {
                graphics.setColor(new Color(0.0f, xColor, 1.0f - yColor));
                graphics.fillRect(x * (width / columns), y * (height / rows), width / columns, height / rows);
                xColor += xColorDelta;
            }
            yColor += yColorDelta;
        }
        return xyzImage;
    }

    public static void logImage(BufferedImage image) {
        DataBuffer buffer = image.getData().getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            DataBufferByte byteBuffer = (DataBufferByte) buffer;
            byte[] values = byteBuffer.getData();
            StringBuffer sb = new StringBuffer();
            int counter = 0;
            for (byte v : values) {
                sb.append((counter++ > 0 ? ", " : "") + (v & 0x0ff) + (counter == 3 ? "\n" : ""));
                if (counter == 3) {
                    counter = 0;
                }
            }
            System.out.println(sb.toString());
        }
    }

    /**
     * Loads the image using inputstream as a source, using javax.imageio.ImageIO to get an image reader to decode the
     * inputstream.
     * 
     * @param stream
     * @param colorSpace
     * @param metadataReader
     * @return The loaded image
     * @throws IOException If there is an exception loading the image
     */
    public PlatformImage<BufferedImage> loadImage(InputStream stream, ColorSpace colorSpace,
            MetadataParser<ImageReader> metadataReader) throws IOException {
        try {
            long start = System.currentTimeMillis();
            PlatformImage<BufferedImage> image = readImage(stream, colorSpace, metadataReader);
            AWTSourceFormat sourceFormat = AWTSourceFormat.get(image.getImage());
            Raster raster = image.getImage().getData();
            int delta = (int) (System.currentTimeMillis() - start) + 1;
            int size = (int) image.getCompressedSize();
            System.out.println(
                    "Loaded image in format " + sourceFormat + " : " + raster.getWidth() + " X "
                            + raster.getHeight()
                            + " in " + delta + " millis [" + size / delta + "K/s]");
            return image;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private PlatformImage<BufferedImage> readImage(InputStream stream, ColorSpace sourceColorSpace,
            MetadataParser<ImageReader> metadataReader) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(stream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (readers.hasNext()) {
            // pick the first available ImageReader
            ImageReader reader = readers.next();
            // attach source to the reader
            reader.setInput(iis, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (metadataReader != null) {
                metadataReader.parseMetadata(reader);
            }
            ImageTypeSpecifier rawType = reader.getImageTypes(0).next();
            if (rawType == null) {
                throw new IllegalArgumentException("Exception: No ImageTypeSpecifier");
            }
            ColorModel sourceColorModel = rawType.getColorModel();
            AWTColorSpace acs = AWTColorSpace.getFromType(sourceColorModel.getColorSpace().getType());
            System.out.println(acs.name());
            ImageReadParam ird = new ImageReadParam();
            BufferedImage sourceImage = null;
            if (sourceColorModel instanceof ComponentColorModel) {
                ColorModel cm = new ComponentColorModel(sourceColorModel.getColorSpace(), null,
                        sourceColorModel.hasAlpha(),
                        sourceColorModel.isAlphaPremultiplied(), sourceColorModel.getTransparency(),
                        sourceColorModel.getTransferType());
                SampleModel sm = cm.createCompatibleSampleModel(width, height);
                ImageTypeSpecifier destinationType = new ImageTypeSpecifier(cm, sm);
                sourceImage = destinationType.createBufferedImage(width, height);
            } else if (sourceColorModel instanceof IndexColorModel) {
                sourceImage = rawType.createBufferedImage(width, height);
            }
            ird.setDestination(sourceImage);
            reader.read(0, ird);
            int components = sourceColorModel.getNumComponents();
            int numOfBands = rawType.getNumBands();
            int[] bitsPerSample = new int[components];
            for (int i = 0; i < components; i++) {
                bitsPerSample[i] = i < numOfBands ? rawType.getBitsPerBand(i) : rawType.getBitsPerBand(numOfBands - 1);
            }
            AWTImageFormat format = AWTImageFormat.get(bitsPerSample);
            return new AwtPlatformImage(sourceImage, sourceColorSpace, format, (int) iis.getStreamPosition());
        } else {
            throw new IOException("Exception: could not find suitable reader");
        }
    }

}
