package org.ktximageio.ktx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.FloatImageBuffer.BufferHDRProperties;
import org.ktximageio.ktx.HalfFloatImageBuffer.FP16Convert;

public class RadianceHDRReader implements ImageReader {

    private static final String IDENTIFIER = "#?RADIANCE";
    private final ImageFormat format;

    /**
     * Creates a new hdr reader with optional format conversion.
     * 
     * @param f The image format
     */
    RadianceHDRReader(ImageFormat f) {
        format = f;
    }

    public enum Variables {
        FORMAT();
    }

    public static class RadianceHeader implements ImageHeader {

        private final byte[] header = new byte[IDENTIFIER.length()];
        private final HashMap<String, String> variables = new HashMap();
        private String orientation;
        private int width;
        private int height;
        private final ByteBuffer fileData;
        private byte[] scanlineBuffer;
        private float maxValue;
        private boolean oldVersion = false;
        private final ImageFormat format;

        public RadianceHeader(ByteBuffer data, ImageFormat f) {
            format = f;
            data.get(header);
            if (!IDENTIFIER.contentEquals(new String(header))) {
                throw new IllegalArgumentException("INVALID VALUE, not radiance HDR header: " + new String(header));
            }
            // Skip newline
            if ((data.get()) != 10) {
                throw new IllegalArgumentException("INVALID VALUE");
            }
            setVariables(data);
            setResolution(data);
            this.fileData = data;
        }

        private void setVariables(ByteBuffer data) {
            String str = null;
            while ((str = getLine(data)) != null) {
                StringTokenizer st = new StringTokenizer(str, "=");
                if (st.countTokens() > 1) {
                    variables.put(st.nextToken().toUpperCase(), st.nextToken());
                }
            }
        }

        private String getLine(ByteBuffer data) {
            byte read = 0;
            String str = null;
            while ((read = data.get()) != 10) {
                if (str == null) {
                    str = "";
                }
                str += Character.toString((char) read);
            }
            return str;
        }

        private void setResolution(ByteBuffer data) {
            String resolution = getLine(data);
            StringTokenizer st = new StringTokenizer(resolution);
            if (st.countTokens() < 4) {
                throw new IllegalArgumentException("INVALID VALUE, resolution string: " + resolution);
            }
            String setOrient = st.nextToken();
            height = Integer.parseInt(st.nextToken());
            setOrient += st.nextToken();
            width = Integer.parseInt(st.nextToken());
            System.out.println("Set resolution to " + width + ", " + height);
            orientation = setOrient;
        }

        private byte[] getScanlines() {
            if (scanlineBuffer == null) {
                scanlineBuffer = new byte[4 * width * height];
                int destOffset = 0;
                for (int i = 0; i < height; i++) {
                    destOffset = getScanLine(fileData, destOffset, scanlineBuffer);
                }
                if (fileData.position() != fileData.capacity()) {
                    throw new IllegalArgumentException("INVALID STATE, not at end of file");
                }
                System.out.println("Read radiance data");
            }
            return scanlineBuffer;
        }

        private int getScanLine(ByteBuffer data, int destOffset, byte[] radianceData) {
            byte[] run = new byte[4];
            data.get(run);
            if (run[0] != 2) {
                data.position(data.position() - run.length);
                oldVersion = true;
                return getScanLineOld(data, destOffset, radianceData);
            }
            if (oldVersion) {
                throw new IllegalArgumentException("INVALID STATE, mix of old and new style scanlines");
            }
            int scanlineLength = ((run[2] << 8) & 0x0ff00) | (run[3] & 0x0ff);
            if (scanlineLength != width) {
                throw new IllegalArgumentException(
                        "INVALID VALUE, scanline length does not match width: " + scanlineLength);
            }

            int scanlineOffset = 0;
            for (int i = 0; i < 4; i++) {
                scanlineOffset = 0;
                while (scanlineOffset < scanlineLength) {
                    int code = data.get() & 0x0ff;
                    if (code > 128) {
                        code &= 127;
                        byte val = (byte) (data.get() & 0x0ff);
                        scanlineOffset += code;
                        while (code-- > 0) {
                            radianceData[destOffset++] = val;
                        }
                    } else {
                        scanlineOffset += code;
                        while (code-- > 0) {
                            radianceData[destOffset++] = (byte) (data.get() & 0x0ff);
                        }
                    }
                }
                if (scanlineOffset > scanlineLength) {
                    throw new IllegalArgumentException("INVALID VALUE, scanline overrun");
                }
            }
            return destOffset;
        }

        /**
         * The source for this is in radiance: ray\src\common\color.c - oldreadcolrs()
         * 
         * @param data
         * @param destOffset
         * @param radianceData
         * @return
         */
        private int getScanLineOld(ByteBuffer data, int destOffset, byte[] radianceData) {
            byte[] scanline = new byte[4];
            int length = width;
            while (length > 0) {
                data.get(scanline);
                if (scanline[0] == 1 && scanline[1] == 1 && scanline[2] == 1) {
                    throw new IllegalArgumentException("Not implemented");
                } else {
                    radianceData[destOffset++] = scanline[0];
                    radianceData[destOffset++] = scanline[1];
                    radianceData[destOffset++] = scanline[2];
                    radianceData[destOffset++] = scanline[3];
                }
                length--;
            }
            return destOffset;
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
        public ImageFormat getFormat() {
            return format;
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
            return convertToFloat(getScanlines(), oldVersion ? 1 : width, oldVersion ? 4 : 1, format);
        }

        private ImageBuffer convertToFloat(byte[] scanlines, int componentStride, int pixelStride,
                ImageFormat destFormat) {
            destFormat = destFormat == null ? ImageFormat.VK_FORMAT_R32G32B32_SFLOAT : destFormat;
            switch (destFormat) {
                case VK_FORMAT_R32G32B32_SFLOAT:
                    return toBufferFloat32(scanlines, componentStride, pixelStride);
                case VK_FORMAT_R16G16B16_SFLOAT:
                    return toBufferFloat16(scanlines, componentStride, pixelStride);
                default:
                    throw new IllegalArgumentException("Invalid format " + format);
            }
        }

        private ImageBuffer toBufferFloat16(byte[] scanlines, int componentStride, int pixelStride) {
            BufferHDRProperties props = new BufferHDRProperties();
            int pixelCount = scanlines.length / 4;
            int readIndex = 0;
            short[] expanded = new short[pixelCount * 3];
            FP16Convert convert = new FP16Convert(expanded);
            for (int y = 0; y < height; y++) {
                readIndex = width * 4 * y;
                for (int x = 0; x < width; x++) {
                    FP16Convert.rgbeToHalfFloat(convert, scanlines, readIndex, componentStride);
                    readIndex += pixelStride;
                    props.addData(convert.halfFloat[0], convert.halfFloat[1], convert.halfFloat[2]);
                    if (convert.max > FP16Convert.MAX_VALUE) {
                        throw new IllegalArgumentException("Invalid value " + convert.max);
                    }
                }
            }
            System.out.println("Converted scanlines to float data " + format + ", minLuminance=" + props.minLuminance
                    + ", maxLuminance="
                    + props.maxLuminance);
            return ImageBuffer.createHalfFloatBuffer(expanded, 0, 1, new int[] { width, height, 0 });
        }

        private ImageBuffer toBufferFloat32(byte[] scanlines, int componentStride, int pixelStride) {
            BufferHDRProperties props = new BufferHDRProperties();
            int pixelCount = scanlines.length / 4;
            int destIndex = 0;
            int readIndex = 0;
            final float[] frgb = new float[4];
            float f;
            float r;
            float g;
            float b;
            float[] expanded = new float[pixelCount * 3];
            for (int y = 0; y < height; y++) {
                readIndex = width * 4 * y;
                for (int x = 0; x < width; x++) {
                    if (scanlines[readIndex + componentStride * 3] == 0.0f) {
                        expanded[destIndex++] = 0;
                        expanded[destIndex++] = 0;
                        expanded[destIndex++] = 0;
                        readIndex += pixelStride;
                    } else {
                        int e = (scanlines[readIndex + componentStride * 3] & 0x0ff) - (128);
                        f = (float) Math.pow(2, e);
                        r = (((float) ((scanlines[readIndex + componentStride * 0]) & 0x0ff) / 255)) * f;
                        g = (((float) ((scanlines[readIndex + componentStride * 1]) & 0x0ff) / 255)) * f;
                        b = (((float) ((scanlines[readIndex + componentStride * 2]) & 0x0ff) / 255)) * f;
                        // From ITU-R BT.2020
                        expanded[destIndex++] = r;
                        expanded[destIndex++] = g;
                        expanded[destIndex++] = b;
                        props.addData(r, g, b);
                        readIndex += pixelStride;
                    }
                }
            }
            System.out.println("Converted scanlines to float data " + format + ", minLuminance=" + props.minLuminance
                    + ", maxLuminance="
                    + props.maxLuminance);
            return ImageBuffer.createFloatBuffer(expanded, ImageFormat.VK_FORMAT_R32G32B32_SFLOAT, 0, 1,
                    new int[] { width, height, 0 },
                    new float[] { props.minLuminance, props.maxLuminance, props.getMeanLuminance() });
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
    public RadianceHeader read(Path filePath) throws IOException {
        System.out.println("URL: " + filePath.toUri().toURL());
        FileChannel fileChannel = (FileChannel) Files.newByteChannel(filePath, EnumSet.of(StandardOpenOption.READ));
        ByteBuffer bb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        return createRadianceHeader(bb, format);
    }

    @Override
    public ImageHeader read(@NonNull ByteBuffer buffer) {
        return createRadianceHeader(buffer, format);
    }

    private RadianceHeader createRadianceHeader(ByteBuffer fileData, ImageFormat f) {
        RadianceHeader header = new RadianceHeader(fileData, f);
        return header;
    }

    @Override
    public MimeFormat[] getMime() {
        return new MimeFormat[] { MimeFormat.HDR };
    }

    @Override
    public String getReaderName() {
        return getClass().getCanonicalName();
    }

}
