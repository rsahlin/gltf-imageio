package org.ktximageio.ktx;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.ktximageio.sh.SphericalHarmonics;

public class TestSphericalHarmonics extends org.ktximageio.ktx.Test {

    public static final float[] CATHEDRAL = new float[] {

            0.078908f, 0.043710f, 0.054161f,
            0.039499f, 0.034989f, 0.060488f,
            -0.033974f, -0.018236f, -0.026940f,
            -0.029213f, -0.005562f, 0.000944f,
            -0.011141f, -0.005090f, -0.012231f,
            -0.026240f, -0.022401f, -0.047479f,
            -0.015570f, -0.009471f, -0.014733f,
            0.056014f, 0.021444f, 0.013915f,
            0.021205f, -0.005432f, -0.030374f
    };

    public static final float[] DEFAULT_COEFFICIENTS = new float[] {
            0.8966792f, 0.86376095f, 0.86376095f,
            0.6904614f, 0.6904614f, 0.61035883f,
            0.78916955f, 0.7829518f, 0.73402005f,
            0.7644828f, 0.7644828f, 0.6609855f,
            0.680011f, 0.680011f, 0.56443167f,
            0.6072222f, 0.5962319f, 0.5465285f,
            0.13119966f, 0.13119966f, 0.13119966f,
            0.7624938f, 0.7624938f, 0.7172956f,
            0.009687597f, -0.009687597f, 0.009687597f
    };

    private float[][] getCoefficients(float[] values) {
        int offset = 0;
        float[][] coefficients = new float[9][3];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i][0] = values[offset++];
            coefficients[i][1] = values[offset++];
            coefficients[i][2] = values[offset++];
        }
        return coefficients;
    }

    @Test
    public void testSH() throws IOException {
        SphericalHarmonics sh = new SphericalHarmonics();
        String fileName = getPath("cubemap.ktx2");
        ImageBuffer cubemap = sh.loadImage(fileName);
        float[][] coefficients = sh.fromCubeMap(cubemap);
    }

}
