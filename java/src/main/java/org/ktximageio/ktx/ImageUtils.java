package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ImageFormat;

public class ImageUtils {

    private ImageUtils() {
    }

    public static final float GAMMA = 2.4f;
    public static final float ONE_BY_GAMMA = 1f / GAMMA;

    public enum ImageType {
        TYPE_3BYTE_BGR(5, 3, 3, ImageFormat.VK_FORMAT_B8G8R8_UNORM),
        TYPE_4BYTE_ABGR(6, 4, 4, ImageFormat.VK_FORMAT_A8B8G8R8_UNORM_PACK32),
        TYPE_4BYTE_ABGR_PRE(7, 4, 4, ImageFormat.VK_FORMAT_A8B8G8R8_UNORM_PACK32),
        TYPE_BYTE_BINARY(12, -1, -1, ImageFormat.VK_FORMAT_UNDEFINED),
        TYPE_BYTE_GRAY(10, 1, 1, ImageFormat.VK_FORMAT_R8_UNORM),
        TYPE_BYTE_INDEXED(13, -1, 1, ImageFormat.VK_FORMAT_UNDEFINED),
        TYPE_CUSTOM(0, -1, -1, ImageFormat.VK_FORMAT_UNDEFINED),
        TYPE_INT_ARGB(2, 4, 4, ImageFormat.VK_FORMAT_UNDEFINED),
        TYPE_INT_ARGB_PRE(3, 4, 4, ImageFormat.VK_FORMAT_UNDEFINED),
        TYPE_INT_BGR(4, 3, 4, ImageFormat.VK_FORMAT_R8G8B8_UNORM),
        TYPE_INT_RGB(1, 3, 4, ImageFormat.VK_FORMAT_B8G8R8_UNORM),
        TYPE_USHORT_555_RGB(9, 3, 2, ImageFormat.VK_FORMAT_R5G5B5A1_UNORM_PACK16),
        TYPE_USHORT_565_RGB(8, 3, 2, ImageFormat.VK_FORMAT_R5G6B5_UNORM_PACK16),
        TYPE_USHORT_GRAY(11, 1, 2, ImageFormat.VK_FORMAT_R16_UNORM);

        public final int value;
        public final int components;
        public final int sizeInBytes;
        public final ImageFormat format;

        ImageType(int val, int count, int bytes, @NonNull ImageFormat format) {
            value = val;
            this.format = format;
            components = count;
            sizeInBytes = bytes;
        }

        public static ImageType getImageType(int type) {
            for (ImageType it : values()) {
                if (it.value == type) {
                    return it;
                }
            }
            return null;
        }

        public static ImageType get(ImageFormat imageFormat) {
            for (ImageType it : values()) {
                if (it.format == imageFormat) {
                    return it;
                }
            }
            return null;
        }
    }

    /**
     * Converts SRGB 8 bit (ubyte) values to linear, can be issued using executor for multithreading
     *
     */
    public static class ArrayToLinearRunnable implements Runnable {

        final byte[] data;
        final int length;
        final int offset;
        volatile Semaphore lock;

        /**
         * Creates an instance that will convert length bytes at offset within data to linear.
         * 
         * @param len
         * @param offs
         * @param pixels
         * @param semaphore The semaphore that will be released when length bytes have been converted.
         */
        public ArrayToLinearRunnable(int len, int offs, byte[] pixels, @NonNull Semaphore semaphore) {
            data = pixels;
            length = len;
            offset = offs;
            lock = semaphore;
        }

        @Override
        public void run() {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                float val = ((float) (data[i] & 0x0ff)) / 255;
                data[i] = (byte) (Math.pow((val + 0.055f) / 1.055f, GAMMA) * 255);
            }
            lock.release();
        }
    }

    /**
     * Converts SRGB 16 bit (short) values to linear, can be issued using executor for multithreading
     *
     */
    public static class ArrayToLinearRunnableShort extends ArrayToLinearRunnable {

        final short[] data;

        /**
         * Creates an instance that will convert length bytes at offset within data to linear.
         * 
         * @param len
         * @param offs
         * @param pixels
         * @param semaphore The semaphore that will be released when length bytes have been converted.
         */
        public ArrayToLinearRunnableShort(int len, int offs, short[] pixels, @NonNull Semaphore semaphore) {
            super(len, offs, null, semaphore);
            data = pixels;
        }

        @Override
        public void run() {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                float val = ((float) (data[i] & 0x0ffff)) / 65536;
                data[i] = (byte) (Math.pow((val + 0.055f) / 1.055f, GAMMA) * 65536);
            }
            lock.release();
        }
    }

    /**
     * Converts SRGB 24 bit (int) values to linear, can be issued using executor for multithreading
     *
     */
    public static class ArrayToLinearRunnableInt extends ArrayToLinearRunnable {

        final int[] data;

        /**
         * Creates an instance that will convert length bytes at offset within data to linear.
         * 
         * @param len
         * @param offs
         * @param pixels
         * @param semaphore The semaphore that will be released when length bytes have been converted.
         */
        public ArrayToLinearRunnableInt(int len, int offs, int[] pixels, @NonNull Semaphore semaphore) {
            super(len, offs, null, semaphore);
            data = pixels;
        }

        @Override
        public void run() {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                int pixels = data[i];
                int val1 = (int) (Math.pow(((float) (pixels & 0x0ff) / 255 + 0.055f) / 1.055f, GAMMA) * 255);
                int val2 = (int) (Math.pow(((float) ((pixels >>> 8) & 0x0ff) / 255 + 0.055f) / 1.055f, GAMMA) * 255);
                int val3 = (int) (Math.pow(((float) ((pixels >>> 16) & 0x0ff) / 255 + 0.055f) / 1.055f, GAMMA) * 255);
                data[i] = val1 | (val2 << 8) | (val3 << 16);
            }
            lock.release();
        }
    }

    public static byte[] floatToByteArray(@NonNull float[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asFloatBuffer().put(data);
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.position(0);
        byteBuffer.get(bytes);
        return bytes;
    }

    public static byte[] byteBufferToArray(@NonNull ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return bytes;
    }

    static float[] copyABGRToRGB(@NonNull byte[] source, float divisor, float pow, float multiplier) {
        float[] destination = new float[(source.length / 4) * 3];
        int count = source.length;
        int index = 0;
        int destIndex = 0;
        while (index < count) {
            destination[destIndex++] = ((float) Math.pow((source[index + 3] & 0x0ff) / divisor, pow)) * multiplier;
            destination[destIndex++] = ((float) Math.pow((source[index + 2] & 0x0ff) / divisor, pow)) * multiplier;
            destination[destIndex++] = ((float) Math.pow((source[index + 1] & 0x0ff) / divisor, pow)) * multiplier;
            index += 4;
        }
        return destination;
    }

    static float[] copyReverse(@NonNull byte[] source, int components, float pow, float divisor) {
        float[] destination = new float[source.length];
        int count = source.length;
        int index = 0;
        while (count > 0) {
            for (int i = 0; i < components; i++) {
                destination[index + i] = ((float) Math.pow((source[index + components - i] & 0x0ff), pow)) / divisor;
            }
            index += components;
            count -= components;
        }
        return destination;
    }

    static float[] copyARGBToFloatRGB(@NonNull byte[] source, float pow, float divisor) {
        float[] destination = new float[(source.length / 4) * 3];
        int count = destination.length;
        int index = 0;
        int destIndex = 0;
        while (destIndex < count) {
            destination[destIndex++] = ((float) Math.pow((source[index++] & 0x0ff), pow)) / divisor;
            destination[destIndex++] = ((float) Math.pow((source[index++] & 0x0ff), pow)) / divisor;
            destination[destIndex++] = ((float) Math.pow((source[index++] & 0x0ff), pow)) / divisor;
            index += 1;
        }
        return destination;
    }

    /**
     * Converts an array of srgb values, in the range 0 - 255 to linear.
     * 
     * @param srgb
     */
    public static void toSRGB(int... srgb) {
        for (int i = 0; i < srgb.length; i++) {
            float val = (float) srgb[i] / 255;
            srgb[i] = (byte) (Math.max(0, 1.055f * Math.pow(val, ONE_BY_GAMMA) - 0.055f) * 255) & 0x0ff;
        }
    }

    public static void toLinear(int... rgb) {
        for (int i = 0; i < rgb.length; i++) {
            float val = (float) rgb[i] / 255;
            rgb[i] = (byte) (Math.max(0, Math.pow((val + 0.055f) / 1.055f, GAMMA)) * 255) & 0x0ff;
        }
    }

    /**
     * Colorconverts the pixels using the specified color matrix. Typesize of format must be 3
     * 
     * @param pixels
     * @param format
     * @param colorMatrix
     * @throws IllegalArgumentException If format typesize is not 3
     */
    public static void convertRGB(byte[] pixels, ImageFormat format, float[] colorMatrix) {
        if (format.typeSize == 2) {
            throw new IllegalArgumentException("Exception: , not implemented for format " + format);
        }
        int count = pixels.length;
        int[] indexes = new int[] { 0, 1, 2 };
        if (format.isReverseOrder()) {
            indexes = new int[] { 2, 1, 0 };
        }
        if (format.typeSize == 1) {
            indexes = new int[] { 0, 0, 0 };
        }
        float r;
        float g;
        float b;
        for (int i = 0; i < count; i += format.typeSize) {
            r = (float) (pixels[indexes[0] + i] & 0x0ff) / 255;
            g = (float) (pixels[indexes[1] + i] & 0x0ff) / 255;
            b = (float) (pixels[indexes[2] + i] & 0x0ff) / 255;

            pixels[indexes[0] + i] = (byte) Math.min(255, ((r * colorMatrix[0] + g * colorMatrix[1]
                    + b * colorMatrix[2])) * 255);
            pixels[indexes[1] + i] = (byte) Math.min(255, ((r * colorMatrix[3] + g * colorMatrix[4]
                    + b * colorMatrix[5])) * 255);
            pixels[indexes[2] + i] = (byte) Math.min(255, ((r * colorMatrix[6] + g * colorMatrix[7]
                    + b * colorMatrix[8])) * 255);
        }
    }

    /**
     * Converts rgb, must be in range 0 - 1, to hue-saturation-lightness according to wikipedia specification
     * 
     * @param rgb
     * @return
     */
    public static float[] convertRGBToHSL(float... rgb) {
        final float[] toHSL = new float[3];
        float min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        float max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        float add = min + max;
        float delta = max - min;
        toHSL[2] = (max + min) / 2.0f;
        toHSL[0] = min == max ? 0
                : rgb[0] == max ? ((60 * (rgb[1] - rgb[2]) / delta) + 360) % 360
                : rgb[1] == max ? (60 * (rgb[2] - rgb[0]) / delta) + 120
                : (60 * (rgb[0] - rgb[1]) / delta) + 240;
        toHSL[1] = toHSL[2] == 0 ? 0 : toHSL[2] == 1 ? 1 : toHSL[2] <= 0.5 ? delta / add : delta / (2 - add);
        toHSL[0] = Math.round(toHSL[0]);
        toHSL[1] = Math.round(toHSL[1] * 100);
        toHSL[2] = Math.round(toHSL[2] * 100);
        return toHSL;
    }

}
