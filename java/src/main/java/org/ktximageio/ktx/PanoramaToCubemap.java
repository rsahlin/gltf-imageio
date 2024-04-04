package org.ktximageio.ktx;

import org.ktximageio.Orientation;
import org.ktximageio.ktx.FloatImageBuffer.BufferHDRProperties;
import org.ktximageio.ktx.ImageReader.ImageFormat;

public class PanoramaToCubemap {

    public enum Interpolation {
        NEAREST();
    }

    float[][] faceTransform = new float[][] {
            { (float) Math.PI / 2, 0 }, // right
            { -(float) Math.PI / 2, 0 }, // left
            { 0, -(float) Math.PI / 2 }, // top
            { 0, (float) Math.PI / 2 }, // bottom
            { 0, 0 }, // front
            { (float) Math.PI, 0 } // back
    };

    /**
     * Creates one cubemapface from a panorama (equirectangular) image
     * 
     * @param input The source panorama image
     * @param face The side to create a cubemap face for
     * @param destFormat The cubemap destination format
     * @return
     */
    public ImageBuffer createCubeMapFace(ImageBuffer input, Orientation face, ImageFormat destFormat) {
        BufferHDRProperties props = null;
        if (input.getFormat() == destFormat) {
            props = new BufferHDRProperties();
        }
        int width = input.width / 4;
        int height = width;

        // Allocate map
        float[] floatResult = null;
        byte[] byteResult = null;
        if (destFormat.isFloatFormat()) {
            floatResult = new float[width * height * 3];
        } else {
            byteResult = new byte[width * height * 3];
        }

        // Calculate adjacent (ak) and opposite (an) of the
        // triangle that is spanned from the sphere center
        // to our cube face.
        final float opposite = (float) Math.sin(Math.PI / 4);
        final float adjacent = (float) Math.cos(Math.PI / 4);

        float ftu = faceTransform[face.face][0];
        float ftv = faceTransform[face.face][1];

        float[] sourceData = input.getAsFloatArray(0);
        // For each point in the target image,
        // calculate the corresponding source coordinates.
        float r;
        float g;
        float b;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // Map face pixel coordinates to [-1, 1] on plane
                float nx = ((float) x / width - 0.5f) * 2 * opposite;
                float ny = ((float) y / height - 0.5f) * 2 * opposite;

                // Map [-1, 1] plane coords to [-an, an]
                // thats the coordinates in respect to a unit sphere
                // that contains our box.

                float u;
                float v;
                float d;

                // Project from plane to sphere surface.
                switch (face) {
                    case LEFT:
                    case RIGHT:
                    case FRONT:
                    case BACK:
                        u = (float) Math.atan2(nx, adjacent);
                        v = (float) Math.atan2(ny * Math.cos(u), adjacent);
                        u += ftu;
                        break;
                    case BOTTOM:
                        d = (float) Math.sqrt(nx * nx + ny * ny);
                        v = (float) (Math.PI / 2 - Math.atan2(d, adjacent));
                        u = (float) Math.atan2(nx, -ny);
                        break;
                    case TOP:
                        d = (float) Math.sqrt(nx * nx + ny * ny);
                        v = (float) (-Math.PI / 2 + Math.atan2(d, adjacent));
                        u = (float) Math.atan2(nx, ny);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid face " + face);
                }
                u = (float) (u / (Math.PI));
                v = (float) (v / (Math.PI / 2));
                while (v < -1) {
                    v += 2;
                    u += 1;
                }
                while (v > 1) {
                    v -= 2;
                    u += 1;
                }

                while (u < -1) {
                    u += 2;
                }
                while (u > 1) {
                    u -= 2;
                }
                u = (u / 2.0f + 0.5f) * (input.width - 1);
                v = (v / 2.0f + 0.5f) * (input.height - 1);

                int destIndex = (y * width + x) * 3;
                int sourceIndex = ((int) u + ((int) v) * input.width) * input.format.typeSize;
                r = sourceData[sourceIndex++];
                g = sourceData[sourceIndex++];
                b = sourceData[sourceIndex++];
                if (destFormat.isFloatFormat()) {
                    floatResult[destIndex++] = r;
                    floatResult[destIndex++] = g;
                    floatResult[destIndex++] = b;
                } else {
                    byteResult[destIndex++] = (byte) (r * 255);
                    byteResult[destIndex++] = (byte) (g * 255);
                    byteResult[destIndex++] = (byte) (b * 255);
                }
                if (props != null) {
                    props.addData(r, g, b);
                }
            }
        }
        float[] luminance = null;
        if (props != null) {
            luminance = props.getLuminance();
        }
        switch (destFormat) {
            case VK_FORMAT_R8G8B8_UNORM:
                return ImageBuffer.create(byteResult, destFormat, 1, width, height, null);
            case VK_FORMAT_R16G16B16_SFLOAT:
            case VK_FORMAT_R32G32B32_SFLOAT:
                return ImageBuffer.createFloatBuffer(floatResult, destFormat, 0, 1,
                        new int[] { width, height, 0 }, luminance);
            default:
                throw new IllegalArgumentException();
        }
    }
}
