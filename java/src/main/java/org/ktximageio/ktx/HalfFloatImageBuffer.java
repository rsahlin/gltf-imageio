package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ImageFormat;

/**
 * Half float version of ImageBuffer, this will store half precision (fp16) float values in a Short buffer.
 */
public class HalfFloatImageBuffer extends FloatImageBuffer {

    /**
     * Used when converting from float precision to half float
     *
     */
    public static class FP16Convert {

        public static final float MAX_VALUE = 65504;
        public static final float MAX_SUBNORMAL = 0.000060975552f;

        public final short[] result;
        int index = 0;
        public final float[] source = new float[3];
        public final float[] halfFloat = new float[3];
        float max = Float.MIN_NORMAL;
        float min = Float.MAX_VALUE;

        public FP16Convert(short[] res) {
            result = res;
        }

        private void add(float f32, int componentOffset) {
            short converted = toHalfFloat(f32);
            halfFloat[componentOffset] = getFloat16(converted);
            source[componentOffset] = f32;
            max = Math.max(max, halfFloat[componentOffset]);
            min = Math.min(min, halfFloat[componentOffset]);
            if (result != null) {
                result[index++] = converted;
            }
        }

        /**
         * Converts an array of floats
         * 
         * @param floats
         */
        public void convert(float[][] floats) {
            for (int i = 0; i < floats.length; i++) {
                for (float f : floats[i]) {
                    convert(f);
                }
            }
        }

        /**
         * Converts an array of floats
         * 
         * @param floats
         */
        public void convert(float... floats) {
            for (float f : floats) {
                convert(f);
            }
        }

        /**
         * Converts float val to half float and stores in result array at index, index is post incremented.
         * 
         * @param val
         */
        public void convert(float val) {
            result[index++] = toHalfFloat(val);
        }

        /**
         * Sets the index to 0
         */
        public void reset() {
            index = 0;
        }

        private int getExponent(short fp16) {
            return ((fp16 >>> 10) & 0x01f);
        }

        private float getMax(int exponent) {
            return (float) (Math.pow(2, exponent - 15));
        }

        private short toHalfFloat(float val32) {
            short signBit = (short) ((Float.floatToRawIntBits(val32) >> 16) & 0x08000);
            if (val32 < 0) {
                val32 = -val32;
            } else if (val32 == 0) {
                return signBit;
            }
            int log2 = (int) (Math.log(val32) / Math.log(2));
            if (log2 < 0) {
                log2--;
            }
            int exponent = log2 < -15 ? 0 : log2 > 15 ? 30 : (byte) (log2 + 15);
            float m = (float) (Math.pow(2, exponent - 15));
            if (m > val32) {
                if (exponent > 0) {
                    exponent--;
                    m = (float) (Math.pow(2, exponent - 15));
                } else {
                    // Subnormal value
                    m = (float) (Math.pow(2, exponent - 14));
                }
            }
            int fraction16 = (int) ((val32 / m) * 1024) - 1024;
            if (fraction16 >= 1024) {
                if (exponent < 30) {
                    exponent++;
                    fraction16 -= 1024;
                } else {
                    fraction16 = 1023;
                }
            } else if (fraction16 < 0) {
                fraction16 += 1024;
            }
            return (short) (exponent << 10 | fraction16 | signBit);
        }

        static void toHalfFloat(int val, byte exponent, FP16Convert convert, int componentIndex) {
            float byteVal = (val & 0x0ff);
            float f = (float) Math.pow(2, exponent);
            float val32 = ((byteVal / 255)) * f;
            convert.add(val32, componentIndex);
        }

        static void rgbeToHalfFloat(FP16Convert convert, byte[] hdr, int readIndex, int componentStride) {
            if (hdr[readIndex + componentStride * 3] == 0.0f) {
                convert.index += 3;
                convert.halfFloat[0] = 0;
                convert.halfFloat[1] = 0;
                convert.halfFloat[2] = 0;
            } else {
                int e = (hdr[readIndex + componentStride * 3] & 0x0ff) - (128);
                convert.max = 0;
                FP16Convert.toHalfFloat(hdr[readIndex + componentStride * 0], (byte) e, convert, 0);
                FP16Convert.toHalfFloat(hdr[readIndex + componentStride * 1], (byte) e, convert, 1);
                FP16Convert.toHalfFloat(hdr[readIndex + componentStride * 2], (byte) e, convert, 2);
                if (convert.max > MAX_VALUE) {
                    float normalize = MAX_VALUE / convert.max;
                    convert.max = 0;
                    convert.index -= 3;
                    convert.add(convert.source[0] * normalize, 0);
                    convert.add(convert.source[1] * normalize, 1);
                    convert.add(convert.source[2] * normalize, 2);
                }
            }
        }

        private static float getFloat16(short fp16) {
            return getFloat16(((fp16 >>> 10) & 0x01f), (fp16 & 0x03ff), Float.floatToRawIntBits(fp16));
        }

        private static float getFloat16(int exponent, int fraction, int sign) {
            float val = 0;
            if (exponent != 0) {
                val = ((float) Math.pow(2, exponent - 15) * ((float) (fraction | 0x0400) / 1024));
            } else {
                val = ((float) Math.pow(2, -14) * ((float) (fraction) / 1024));
            }
            return sign < 0 ? -val : val;
        }

        public static float[] expandFP16(short[] halfFloats) {
            float[] floats = new float[halfFloats.length];
            int len = halfFloats.length;
            for (int i = 0; i < len; i++) {
                floats[i] = getFloat16(halfFloats[i]);
            }
            return floats;
        }
    }

    HalfFloatImageBuffer(@NonNull ByteBuffer bitmap, int layerCount, int faceCount,
            int[] dimension) {
        super(bitmap, ImageFormat.VK_FORMAT_R16G16B16_SFLOAT, layerCount, faceCount, dimension);
    }

    @Override
    public float[] getAsFloatArray(int index) {
        if (index >= getImageCount()) {
            return null;
        }
        short[] data = new short[width * height * format.typeSize];
        ShortBuffer sb = getImageBuffer(index).asShortBuffer();
        sb.limit(data.length);
        sb.get(data);
        return FP16Convert.expandFP16(data);
    }

}
