package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.itu.BT2100;
import org.ktximageio.ktx.HalfFloatImageBuffer.FP16Convert;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.TransferFunction;
import org.ktximageio.output.PerceptualQuantizer;

/**
 * Float version of image buffer
 *
 */
public class FloatImageBuffer extends ImageBuffer {

    public enum Tonemap {
        NONE(),
        ADAPTED_ACES(),
        PERCEPTUAL_QUANTIZER();
    }

    public static class BufferHDRProperties {
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        float minLuminance = Float.MAX_VALUE;
        float maxLuminance = Float.MIN_VALUE;
        private double totalLuminance;
        private int count;

        /**
         * Returns the minimum luminance value in the image
         * 
         * @return Minimum luminance value
         */
        public float getMinLuminance() {
            return minLuminance;
        }

        /**
         * Returns the maximum luminance value in the image
         * 
         * @return Maximum luminance value
         */
        public float getMaxLuminance() {
            return maxLuminance;
        }

        /**
         * Returns the minimum RGB component value in the image
         * 
         * @return
         */
        public float getMinValue() {
            return minVal;
        }

        /**
         * Returns the maximum RGB component value in the image, this can be used to find the max component value of
         * pixels in the image.
         * 
         * @return
         */
        public float getMaxValue() {
            return maxVal;
        }

        /**
         * Returns the average luminance value for the image.
         * 
         * @return
         */
        public float getMeanLuminance() {
            return (float) (totalLuminance / count);
        }

        /**
         * Returns an array with [minimum, maximum, mean] luminance values
         * 
         * @return
         */
        public float[] getLuminance() {
            return new float[] { getMinLuminance(), getMaxLuminance(), getMeanLuminance() };
        }

        /**
         * Adds a pixel value to the properties for a given image, call this method for each contributing pixel in the
         * image.
         * 
         * @param r
         * @param g
         * @param b
         */
        public void addData(float r, float g, float b) {
            float l = BT2100.toLuminance(r, g, b);
            minLuminance = Math.min(minLuminance, l);
            maxLuminance = Math.max(maxLuminance, l);
            maxVal = Math.max(Math.max(Math.max(r, g), b), maxVal);
            minVal = Math.min(Math.min(Math.min(r, g), b), minVal);
            totalLuminance += l;
            count++;
        }

        /**
         * Returns the HDR properties for an float buffer image.
         * If the buffer contains multiple images, a specific image properties can be calculated by specifying the index
         * parameter.
         * If the index parameter is -1 then all images in the buffer will be used to calculate the properties.
         * 
         * @param floatImage Float buffer containing one or more images
         * @param index Index of image to calculate properties for, or -1 to calculate for all images in the buffer.
         * @param properties
         * @return
         */
        public static BufferHDRProperties get(@NonNull FloatImageBuffer floatImage, int index,
                @NonNull BufferHDRProperties properties) {
            int start = index >= 0 ? index : 0;
            int end = index >= 0 ? index + 1 : floatImage.getImageCount();
            for (int i = start; i < end; i++) {
                float[] data = floatImage.getAsFloatArray(i);
                properties = get(data, properties);
            }
            return properties;
        }

        /**
         * Get the properties for a float buffer with pixel RGB data, values are added to the properties.
         * 
         * @param data Float array with RGB pixel data
         * @param properties
         * @return
         */
        public static BufferHDRProperties get(@NonNull float[] data, @NonNull BufferHDRProperties properties) {
            if (properties == null) {
                properties = new BufferHDRProperties();
            }
            for (int i = 0; i < data.length; i = i + 3) {
                properties.addData(data[i], data[i + 1], data[i + 2]);
            }
            return properties;
        }
    }

    public static class FloatImageBufferInfo extends ImageBufferInfo {

        BufferHDRProperties properties;

        public FloatImageBufferInfo(int width, int height, int arrayLayers, @NonNull ImageFormat format,
                @NonNull BufferHDRProperties props) {
            super(width, height, arrayLayers, format);
            properties = props;
        }

        /**
         * Returns the hdr properties
         * 
         * @return
         */
        public BufferHDRProperties getProperties() {
            return properties;
        }

    }

    /**
     * Internal constructor - do NOT use - use static methods in {@link ImageBuffer}
     */
    FloatImageBuffer(@NonNull ByteBuffer bitmap, @NonNull ImageFormat format, int layerCount, int faceCount,
            int[] dimension) {
        super(bitmap, format, layerCount, faceCount, dimension, TransferFunction.LINEAR);
        System.out.println("Created float image buffer, format: " + format + ", size " + dimension[0] + ", "
                + dimension[1] + ", " + dimension[2] + ", facecount " + faceCount);
    }

    @Override
    public float[] getAsFloatArray(int index) {
        if (index >= getImageCount()) {
            return null;
        }
        float[] data = new float[width * height * format.typeSize];
        FloatBuffer fb = getImageBuffer(index).asFloatBuffer();
        fb.limit(data.length);
        fb.get(data);
        return data;
    }

    /**
     * Returns a tonemapped buffer of the specified image index, using the algorithm specified.
     * 
     * @param index The index of the image, this is only valid if layerCount > 1 | faceCount > 1 | depth > 1
     * @param tonemap
     * @return The tonemapped image, or null if index is not valid
     */
    public float[] tonemap(int index, Tonemap tonemap) {
        if (index >= getImageCount()) {
            return null;
        }
        float[] bitmap = getAsFloatArray(index);
        float[] normalized = new float[bitmap.length];
        tonemap = tonemap == null ? Tonemap.NONE : tonemap;
        switch (tonemap) {
            case ADAPTED_ACES:
                float a = 2.51f;
                float b = 0.03f;
                float c = 2.43f;
                float d = 0.59f;
                float e = 0.14f;
                for (int i = 0; i < normalized.length; i++) {
                    float x = bitmap[i];
                    normalized[i] = Math.min((x * (a * x + b)) / (x * (c * x + d) + e), 1.0f);
                }
                return normalized;
            case NONE:
                for (int i = 0; i < normalized.length; i++) {
                    float val = bitmap[i];
                    normalized[i] = val > 1.0f ? 1.0f : val;
                }
                return normalized;
            case PERCEPTUAL_QUANTIZER:
                PerceptualQuantizer pq = new PerceptualQuantizer(this, index);
                return pq.applyEOTF(normalized);
            default:
                throw new IllegalArgumentException("NOT IMPLEMENTED " + tonemap);
        }
    }

    @Override
    public FloatImageBufferInfo getInfo() {
        // Create new instance otherwise changes to format may be lost
        BufferHDRProperties properties = BufferHDRProperties.get(this, -1, null);
        return new FloatImageBufferInfo(width, height, faceCount, format, properties);
    }

    /**
     * Returns a FP16 converted copy of this buffer
     * 
     * @return
     */
    public FP16Convert convertToFloat16(int index) {
        float[] floats = getAsFloatArray(index);
        FP16Convert convert = new FP16Convert(new short[floats.length]);
        convert.convert(floats);
        return convert;
    }

}
