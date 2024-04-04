package org.ktximageio.sh;

import java.io.IOException;
import java.nio.file.Paths;

import org.ktximageio.Orientation;
import org.ktximageio.ktx.ImageBuffer;
import org.ktximageio.ktx.ImageHeader;
import org.ktximageio.ktx.ImageReader;
import org.ktximageio.ktx.ImageReader.MimeFormat;

public class SphericalHarmonics {

    static final float PI = (float) Math.PI;
    static final float C1 = 0.429043f;
    static final float C2 = 0.511664f;
    static final float C3 = 0.743125f;
    static final float C4 = 0.886227f;
    static final float C5 = 0.247708f;

    static final int[][] INDEXES = new int[][] { { 2, 1, 0 }, { 2, 1, 0 }, { 0, 2, 1 }, { 0, 2, 1 }, { 0, 1, 2 },
            { 0, 1, 2 } };

    /**
     * Returns the order of the faces for a given orientation, to lookup the correct axis to use take the index
     * for the x axis from the offset 0, y axis from offset 1 and the z axis from offset 2
     * 
     * @param face
     * @return
     */
    public static int[] getIndexes(Orientation face) {
        return INDEXES[face.face];
    }

    public static float[][] toCoefficientArray(float[] array) {
        float[][] result = new float[9][3];
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            result[i][0] = array[index++];
            result[i][1] = array[index++];
            result[i][2] = array[index++];
        }
        return result;
    }

    /**
     * Renders one face of a cubemap representation of the spherical harmonics coefficients
     * 
     * @param face
     * @param coefficients
     * @param size
     * @param image
     * @param destOffset
     */
    public void renderImage(Orientation face, float[][] coefficients, int size, byte[] image, int destOffset) {
        float[] xyz = new float[3];
        int pixelIndex;
        float[] pixels = new float[3];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                face.getNormal(x, y, size, xyz);
                pixelIndex = y * size * 3 + x * 3 + destOffset;
                getIrradiance(coefficients, xyz, pixels);
                // Vec3.normalize(pixels, 0);
                image[pixelIndex++] = (byte) (Math.max(0, pixels[2]) * 10 * 255);
                image[pixelIndex++] = (byte) (Math.max(0, pixels[1]) * 10 * 255);
                image[pixelIndex++] = (byte) (Math.max(0, pixels[0]) * 10 * 255);
                // image[pixelIndex++] = (byte) ((xyz[2] / 2 + 0.5f) * 255);
                // image[pixelIndex++] = (byte) ((xyz[2] / 2 + 0.5f) * 255);
                // image[pixelIndex++] = (byte) ((xyz[2] / 2 + 0.5f) * 255);
            }
        }
    }

    /**
     * col = c1*L22*(x2-y2)
     * + c3*L20*z2
     * + c4*L00
     * - c5*L20
     * + 2*c1*(L2_2*xy + L21*xz + L2_1*yz)
     * + 2*c2*(L11*x+L1_1*y+L10*z) ;
     *
     * @param coefficients
     * @param normal
     * @param result
     */
    private void getIrradiance(float[][] coefficients, float[] normal, float[] result) {
        result[0] = coefficients[0][0] * C4;
        result[1] = coefficients[0][1] * C4;
        result[2] = coefficients[0][2] * C4;

        add(coefficients[1], result, normal[1] * 2 * C2); // L1_1
        add(coefficients[2], result, normal[2] * 2 * C2); // L10
        add(coefficients[3], result, normal[0] * 2 * C2); // L11

        add(coefficients[4], result, normal[1] * normal[0] * 2 * C1); // L2_2
        add(coefficients[5], result, normal[1] * normal[2] * 2 * C1); // L2_1

        add(coefficients[6], result, -C5); // L20
        add(coefficients[7], result, normal[0] * normal[2]); // L21
        add(coefficients[8], result, (normal[0] * normal[0] - normal[1] * normal[1]) * C1); // L22

    }

    private void add(float[] coefficients, float[] result, float factor) {
        result[0] += coefficients[0] * factor;
        result[1] += coefficients[1] * factor;
        result[2] += coefficients[2] * factor;
    }

    /**
     * Loads a KTX2 cubemap image using {@link ImageReader#getImageReader(MimeFormat)}
     * 
     * @param cubemap Name of KTX2 cubemap file.
     * @return
     * @throws IOException
     */
    public ImageBuffer loadImage(String cubemap) throws IOException {
        ImageReader reader = ImageReader.getImageReader(MimeFormat.KTX2);
        ImageHeader header = reader.read(Paths.get(cubemap));
        return header.getData();
    }

    /**
     * Calculates the spherical harmonics from a cubemap imagebuffer
     * 
     * @param cubemap Must contain cubemap
     * @return
     * @throws IOException
     */
    public float[][] fromCubeMap(ImageBuffer cubemap) throws IOException {
        SphericalHarmonics sh = new SphericalHarmonics();
        float[][] coefficients = sh.fromCubeMap(cubemap, 1);
        return coefficients;
    }

    /**
     * Normalizes the vector at index
     *
     * @param values
     * @param index
     */
    private void normalize(float[] values, int index) {
        float len = (float) Math
                .sqrt((values[index] * values[index])
                        + (values[index + 1] * values[index + 1])
                        + (values[index + 2] * values[index + 2]));
        values[index] = values[index] / len;
        values[index + 1] = values[index + 1] / len;
        values[index + 2] = values[index + 2] / len;
    }

    private float fSchlick(float u, float n) {
        float m = 1 - u;
        float m2 = m * m;
        return n + (1 - n) * m2 * m2 * m; // pow(m,5)
    }

    /**
     * returns ax * bx + ay * by + az * bz
     *
     * @param vec1 ax, ay, az
     * @param vec2 bx, by, bz
     * @return ax * bx + ay * by + az * bz
     */
    private float dot(float[] vec1, int index1, float[] vec2, int index2) {
        return vec1[index1++] * vec2[index2++] + vec1[index1++] * vec2[index2++] + vec1[index1] * vec2[index2];
    }

    /**
     * Calculates spherical harnmonics coefficients from the cubemap and returns as float array.
     * 
     * @param cubeMap
     * @param maxIntensity
     * @return float array with SH coefficients
     */
    public float[][] fromCubeMap(ImageBuffer cubeMap, float maxIntensity) {
        float[] col = new float[3];
        float[] total = new float[3];
        float[][] coeffs = new float[9][3];
        int width = cubeMap.width;
        int pixelIndex = 0;
        float[] xyz = new float[3];

        Orientation[] faces = new Orientation[] { Orientation.BOTTOM };
        faces = Orientation.values();
        for (Orientation face : faces) {
            float[] pixels = cubeMap.getAsFloatArray(face.face);
            for (int y = 0; y < cubeMap.height; y++) {
                for (int x = 0; x < width; x++) {
                    pixelIndex = y * width * 3 + x * 3;
                    // v = (float) ((width / 2.0 - y) / (width / 2.0)); /* v ranges from -1 to 1 */
                    // u = (float) ((x - width / 2.0) / (width / 2.0)); /* u ranges from -1 to 1 */
                    // r = (float) Math.sqrt(u * u + v * v); /* The "radius" */
                    // if (r <= 1.0) {
                    // /* Consider only circle with r<1 */
                    // theta = PI * r; /* theta parameter of (i,j) */
                    // phi = (float) Math.atan2(v, u); /* phi parameter */
                    // xyz[indexes[0]] = (float) (Math.sin(theta) * Math.cos(phi)) * align[0]; /*
                    // * Cartesian components
                    // */
                    // xyz[indexes[1]] = (float) (Math.sin(theta) * Math.sin(phi)) * align[1];
                    // xyz[indexes[2]] = (float) Math.cos(theta) * align[2];
                    // Vec3.normalize(xyz, 0);
                    // domega = ((PI / 2) / width) * ((PI / 2) / width) * sinc(theta);
                    // updatecoeffs(coeffs, col, domega, xyz);
                    // }
                    face.getNormal(x, y, width, xyz);
                    normalize(xyz, 0);
                    float dot = dot(face.axis, 0, xyz, 0);
                    float power = 1f - fSchlick(dot, 0);
                    // float power = 1f;
                    col[0] = power * pixels[pixelIndex++];
                    col[1] = power * pixels[pixelIndex++];
                    col[2] = power * pixels[pixelIndex++];
                    total[0] += col[0];
                    total[1] += col[1];
                    total[2] += col[2];
                    // updatecoeffs(coeffs, col, ((2 * (float) Math.PI) / (width * width * faces.length)), xyz);
                    updatecoeffs(coeffs, col, ((PI / 2) / width) * ((PI / 2) / width), xyz);
                }
            }
        }
        System.out.println("Total " + total[0] + ", " + total[1] + ", " + total[2]);
        System.out.println("Avg " + total[0] / (width * width * faces.length) + ", "
                + total[1] / (width * width * faces.length) + ", "
                + total[2] / (width * width * faces.length));
        // normalize(coeffs, width, height);
        // Print out to be used in java
        System.out.println("Java formatted irradiance coefficients [R, G, B] * 9 values");
        for (int i = 0; i < coeffs.length; i++) {
            System.out.println(coeffs[i][0] + "f, " + coeffs[i][1] + "f, " + coeffs[i][2]
                    + (i < coeffs.length - 1 ? "f," : "f"));
        }
        System.out.println("-----------------------------------------------------------");
        // Print out to be used in json
        System.out.println("JSON formatted irradiance coefficients [R, G, B] * 9 values");
        System.out.println("[");
        for (int i = 0; i < coeffs.length; i++) {
            System.out.println("[" + coeffs[i][0] + ", " + coeffs[i][1] + ", " + coeffs[i][2]
                    + (i < coeffs.length - 1 ? "]," : "]"));
        }
        System.out.println("]");
        System.out.println("-----------------------------------------------------------");
        return coeffs;
    }

    private float sinc(float x) { /* Supporting sinc function */
        if (Math.abs(x) < 1.0e-4) {
            return 1.0f;
        } else {
            return (float) (Math.abs(Math.sin(x) / x));
        }
    }

    private void normalize(float[][] coeffs, int width, int height) {
        float factor = 1.0f / ((width * height));
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i][0] *= factor;
            coeffs[i][1] *= factor;
            coeffs[i][2] *= factor;
        }
    }

    private void updatecoeffs(float[][] coeffs, float[] hdr, float domega, float[] xyz) {

        /******************************************************************
         * Update the coefficients (i.e. compute the next term in the
         * integral) based on the lighting value hdr[3], the differential
         * solid angle domega and cartesian components of surface normal x,y,z
         * 
         * Inputs: hdr = L(x,y,z) [note that x^2+y^2+z^2 = 1]
         * i.e. the illumination at position (x,y,z)
         * 
         * domega = The solid angle at the pixel corresponding to
         * (x,y,z). For these light probes, this is given by
         * 
         * x,y,z = Cartesian components of surface normal
         * 
         * Notes: Of course, there are better numerical methods to do
         * integration, but this naive approach is sufficient for our
         * purpose.
         * 
         *********************************************************************/

        int col;
        for (col = 0; col < 3; col++) {
            float c; /* A different constant for each coefficient */

            /* L_{00}. Note that Y_{00} = 0.282095 */
            // c = 0.282095f;
            c = 0.282095f;
            coeffs[0][col] += hdr[col] * c * domega;

            /* L_{1m}. -1 <= m <= 1. The linear terms */
            c = 0.488603f;
            coeffs[1][col] += hdr[col] * (c * xyz[1]) * domega; /* Y_{1-1} = 0.488603 y */
            coeffs[2][col] += hdr[col] * (c * xyz[2]) * domega; /* Y_{10} = 0.488603 z */
            coeffs[3][col] += hdr[col] * (c * xyz[0]) * domega; /* Y_{11} = 0.488603 x */

            /* The Quadratic terms, L_{2m} -2 <= m <= 2 */

            /* First, L_{2-2}, L_{2-1}, L_{21} corresponding to xy,yz,xz */
            c = 1.092548f;
            coeffs[4][col] += hdr[col] * (c * xyz[0] * xyz[1]) * domega; /* Y_{2-2} = 1.092548 xy */
            coeffs[5][col] += hdr[col] * (c * xyz[1] * xyz[2]) * domega; /* Y_{2-1} = 1.092548 yz */
            coeffs[7][col] += hdr[col] * (c * xyz[0] * xyz[2]) * domega; /* Y_{21} = 1.092548 xz */

            /* L_{20}. Note that Y_{20} = 0.315392 (3z^2 - 1) */
            c = 0.315392f;
            coeffs[6][col] += hdr[col] * (c * (3 * xyz[2] * xyz[2] - 1)) * domega;

            /* L_{22}. Note that Y_{22} = 0.546274 (x^2 - y^2) */
            c = 0.546274f;
            coeffs[8][col] += hdr[col] * (c * (xyz[0] * xyz[0] - xyz[1] * xyz[1])) * domega;
        }
    }

    public static void tomatrix(float[][] coeffs, float[][] rgbMatrix) {
        /* Form the quadratic form matrix (see equations 11 and 12 in paper) */
        int col;
        float c1 = 0.429043f;
        float c2 = 0.511664f;
        float c3 = 0.743125f;
        float c4 = 0.886227f;
        float c5 = 0.247708f;

        for (col = 0; col < 3; col++) { /* Equation 12 */
            float[] matrix = rgbMatrix[col];
            matrix[0] = c1 * coeffs[8][col]; /* c1 L_{22} */
            matrix[1] = c1 * coeffs[4][col]; /* c1 L_{2-2} */
            matrix[2] = c1 * coeffs[7][col]; /* c1 L_{21} */
            matrix[3] = c2 * coeffs[3][col]; /* c2 L_{11} */

            matrix[4] = c1 * coeffs[4][col]; /* c1 L_{2-2} */
            matrix[5] = -c1 * coeffs[8][col]; /*-c1 L_{22}  */
            matrix[6] = c1 * coeffs[5][col]; /* c1 L_{2-1} */
            matrix[7] = c2 * coeffs[1][col]; /* c2 L_{1-1} */

            matrix[8] = c1 * coeffs[7][col]; /* c1 L_{21} */
            matrix[9] = c1 * coeffs[5][col]; /* c1 L_{2-1} */
            matrix[10] = c3 * coeffs[6][col]; /* c3 L_{20} */
            matrix[11] = c2 * coeffs[2][col]; /* c2 L_{10} */

            matrix[12] = c2 * coeffs[3][col]; /* c2 L_{11} */
            matrix[13] = c2 * coeffs[1][col]; /* c2 L_{1-1} */
            matrix[14] = c2 * coeffs[2][col]; /* c2 L_{10} */
            matrix[15] = c4 * coeffs[0][col] - c5 * coeffs[6][col]; /* c4 L_{00} - c5 L_{20} */
        }
    }

    public static void tomatrix(float[] coeffs, float[] rgbMatrix, int matrixOffset) {
        /* Form the quadratic form matrix (see equations 11 and 12 in paper) */
        int col;
        float c1 = 0.429043f;
        float c2 = 0.511664f;
        float c3 = 0.743125f;
        float c4 = 0.886227f;
        float c5 = 0.247708f;

        for (col = 0; col < 3; col++) { /* Equation 12 */
            int matrixIndex = 16 * col + matrixOffset;
            rgbMatrix[matrixIndex + 0] = c1 * coeffs[8 * 3 + col]; /* c1 L_{22} */
            rgbMatrix[matrixIndex + 1] = c1 * coeffs[4 * 3 + col]; /* c1 L_{2-2} */
            rgbMatrix[matrixIndex + 2] = c1 * coeffs[7 * 3 + col]; /* c1 L_{21} */
            rgbMatrix[matrixIndex + 3] = c2 * coeffs[3 * 3 + col]; /* c2 L_{11} */

            rgbMatrix[matrixIndex + 4] = c1 * coeffs[4 * 3 + col]; /* c1 L_{2-2} */
            rgbMatrix[matrixIndex + 5] = -c1 * coeffs[8 * 3 + col]; /*-c1 L_{22}  */
            rgbMatrix[matrixIndex + 6] = c1 * coeffs[5 * 3 + col]; /* c1 L_{2-1} */
            rgbMatrix[matrixIndex + 7] = c2 * coeffs[1 * 3 + col]; /* c2 L_{1-1} */

            rgbMatrix[matrixIndex + 8] = c1 * coeffs[7 * 3 + col]; /* c1 L_{21} */
            rgbMatrix[matrixIndex + 9] = c1 * coeffs[5 * 3 + col]; /* c1 L_{2-1} */
            rgbMatrix[matrixIndex + 10] = c3 * coeffs[6 * 3 + col]; /* c3 L_{20} */
            rgbMatrix[matrixIndex + 11] = c2 * coeffs[2 * 3 + col]; /* c2 L_{10} */

            rgbMatrix[matrixIndex + 12] = c2 * coeffs[3 * 3 + col]; /* c2 L_{11} */
            rgbMatrix[matrixIndex + 13] = c2 * coeffs[1 * 3 + col]; /* c2 L_{1-1} */
            rgbMatrix[matrixIndex + 14] = c2 * coeffs[2 * 3 + col]; /* c2 L_{10} */
            rgbMatrix[matrixIndex + 15] = c4 * coeffs[0 * 3 + col] - c5 * coeffs[6 * 3 + col]; /*
                                                                                                * c4 L_{00} - c5 L_{20}
                                                                                                */
        }
    }

}
