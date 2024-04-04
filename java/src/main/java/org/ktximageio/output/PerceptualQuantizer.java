package org.ktximageio.output;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.FloatImageBuffer;

public class PerceptualQuantizer implements OutputMapper {

    public final float m1 = 1305.0f / 8192.0f;
    public final float m2 = 2523.0f / 32.0f;
    public final float c1 = (107.0f / 128.0f);
    public final float c2 = (2413.0f / 128.0f);
    public final float c3 = (2392.0f / 128.0f);
    public final float oneByM2 = (1.0f / m2);
    public final float oneByM1 = (1.0f / m1);

    private FloatImageBuffer buffer;
    private int index;
    private float[] pixels;

    public PerceptualQuantizer(@NonNull FloatImageBuffer buf, int i) {
        if (buf.getFormat().typeSize != 3) {
            throw new IllegalArgumentException(
                    "Only support for 3 component (rgb) images : " + buf.getFormat().typeSize);
        }
        buffer = buf;
        index = i;
    }

    private void limitNormalized(float scaleFactor) {
        pixels = buffer.getAsFloatArray(index);
        int length = pixels.length;
        float peakIntensity = getPeakIntensity();
        for (int i = 0; i < length; i += 3) {
            pixels[i] *= scaleFactor;
            pixels[i + 1] *= scaleFactor;
            pixels[i + 2] *= scaleFactor;

            float luminance = Math.max(pixels[i], Math.max(pixels[i + 1], pixels[i + 2]));
            if (luminance > peakIntensity) {
                float factor = peakIntensity / luminance;
                pixels[i] *= factor;
                pixels[i + 1] *= factor;
                pixels[i + 2] *= factor;
            }
        }
    }

    /**
     * Applies the EOTF to the pixeldata in this class.
     * This will apply the perceptual quantizer on the image.
     * 
     * @param mapped Optional destination, if specified must be at least size of pixels array.
     * @return The float array containing image with PQ EOTF applied
     */
    public float[] applyEOTF(float[] mapped) {
        if (pixels == null) {
            limitNormalized(10000);
        }
        if (mapped == null) {
            mapped = new float[pixels.length];
        }
        float peak = getPeakIntensity();
        for (int i = 0; i < pixels.length; i += 3) {
            float luminance = Math.max(pixels[i], Math.max(pixels[i + 1], pixels[i + 2])) / peak;
            float eotf = (float) Math.pow((c1 + c2 * Math.pow(luminance, m1)) / (1 + c3 * Math.pow(luminance, m1)), m2);
            mapped[i] = pixels[i] / peak * eotf;
            mapped[i + 1] = pixels[i + 1] / peak * eotf;
            mapped[i + 2] = pixels[i + 2] / peak * eotf;
        }
        return mapped;
    }

    @Override
    public float getPeakIntensity() {
        return 10000;
    }

}
