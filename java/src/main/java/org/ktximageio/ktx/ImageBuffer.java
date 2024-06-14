package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.FloatImageBuffer.BufferHDRProperties;
import org.ktximageio.ktx.HalfFloatImageBuffer.FP16Convert;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.TransferFunction;
import org.ktximageio.ktx.KTX.TextureType;

/**
 * A buffer containing pixeldata as represented by one level from a KTX v2 image or similar
 * It contains the pixeldata for the levelImages for the specified mip-level
 * All pixel data shall be stored in direct byte buffers using little endianess (same as KTX)
 * 
 */
public class ImageBuffer {

    /**
     * Class used to categorize use of imagebuffers based on dimension and format.
     *
     */
    public static class ImageBufferInfo {

        public final int width;
        public final int height;
        /**
         * 0 for a normal image, 6 for a cubemap
         */
        public final int arrayLayers;
        public final ImageFormat format;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((format == null) ? 0 : format.hashCode());
            result = prime * result + height;
            result = prime * result + width;
            result = prime * result + arrayLayers;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() != obj.getClass()) {
                return false;
            }
            ImageBufferInfo other = (ImageBufferInfo) obj;
            if (format != other.format) {
                return false;
            } else if (height != other.height) {
                return false;
            } else if (width != other.width) {
                return false;
            } else if (arrayLayers != other.arrayLayers) {
                return false;
            }
            return true;
        }

        /**
         * Returns the KTX texture type, using the number of layers as faceCount when looking up the texture type.
         * 
         * @return
         */
        public KTX.TextureType getTextureType() {
            return TextureType.getTextureType(1, arrayLayers, width, height, 1);
        }

        public ImageBufferInfo(int w, int h, int layers, @NonNull ImageFormat f) {
            width = w;
            height = h;
            format = f;
            arrayLayers = layers;
        }

        public ImageBufferInfo(int w, int h, int layers, int f) {
            width = w;
            height = h;
            format = ImageFormat.get(f);
            arrayLayers = layers;
        }

        @Override
        public String toString() {
            return "Width " + width + ", height " + height + ", arrayLayers " + arrayLayers + ", format" + format;
        }

    }

    /**
     * Same as ktx layerCount
     * layerCount specifies the number of array elements. If the texture is not an array texture, layerCount must equal
     * 0.
     */
    public final int layerCount;
    /**
     * Same as ktx faceCount
     * faceCount specifies the number of cubemap faces. For cubemaps and cubemap arrays this must be 6.
     * For non cubemaps this must be 1.
     * Cubemap faces are stored in the order: +X, -X, +Y, -Y, +Z, -Z in a left-handed coordinate system with +Y up and,
     * with the +Z face forward, +X on the on the right.
     * All faces must have the same orientation which must be rd (top-left origin)
     */
    public final int faceCount;
    /**
     * Same as ktx pixelDepth
     * For 2D and cubemap textures, pixelDepth must be 0.
     */
    public final int depth;
    public final int width;
    public final int height;
    public final ImageFormat format;
    ByteBuffer bitmap;
    /**
     * Has any transfer function been applied to the loaded data?
     */
    public final TransferFunction transferFunction;

    /**
     * Do NOT use - use static create method
     */
    protected ImageBuffer(@NonNull ByteBuffer pixels, @NonNull ImageFormat f, int layers, int faces, int w, int h, int d, TransferFunction tf) {
        if (faces <= 0) {
            throw new IllegalArgumentException("Invalid faceCount " + faces);
        }
        layerCount = layers;
        faceCount = faces;
        depth = d;
        bitmap = pixels;
        format = f;
        width = w;
        height = h;
        transferFunction = tf;

    }

    /**
     * Do NOT use - use static create method
     */
    protected ImageBuffer(@NonNull ByteBuffer pixels, @NonNull ImageFormat f, int layers, int faces, int[] dim, TransferFunction tf) {
        if (faces <= 0) {
            throw new IllegalArgumentException("Invalid faceCount " + faces);
        }
        layerCount = layers;
        faceCount = faces;
        bitmap = pixels;
        format = f;
        width = dim[0];
        height = dim[1];
        depth = dim.length > 2 ? dim[2] : 0;
        transferFunction = tf;
    }

    final void createBuffer(int sizeInBytes) {
        this.bitmap = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.nativeOrder());
        System.out.println("Allocated direct imagebuffer with " + sizeInBytes + " bytes");
    }

    /**
     * Returns the imageformat of the pixeldata in the image.
     * 
     * @return
     */
    public ImageFormat getFormat() {
        return format;
    }

    /**
     * Returns the ktx texture type of the image, using layerCount, faceCount, width, height and depth.
     * 
     * @return
     */
    public KTX.TextureType getTextureType() {
        return TextureType.getTextureType(layerCount, faceCount, width, height, depth);
    }

    /**
     * /**
     * Returns the buffer containing the pixel data, may contain multiple layers/faces and depth.
     * Data is stored according to the KTX fileformat specification:
     * for each layer in max(1,layerCount)
     * for each face in faceCount
     * for each z_slice_of_blocks in num_blocks_z
     * for each row_of_blocks in num_blocks_y
     * for each block in num_blocks_x
     * Byte data[format_specific_number_of_bytes]
     * 
     * @return
     */
    public ByteBuffer getBuffer() {
        bitmap.position(0);
        bitmap.limit(bitmap.capacity());
        return bitmap;
    }

    /**
     * Release resources
     */
    public void destroy() {
        bitmap = null;
    }

    /**
     * Returns the buffer containing the pixel data for one image at the image index
     * 
     * @param index Index of the image where the buffer position is located.
     * Must be < getImageCount() otherwise null is returned
     * @return The bitmap buffer positioned at the image, with limit set to end after the image.
     * 
     */
    public ByteBuffer getImageBuffer(int index) {
        if (index >= getImageCount()) {
            return null;
        }
        int position = getImagePosition(index);
        bitmap.position(position);
        bitmap.limit(position + getImageSizeInBytes());
        return bitmap;
    }

    /**
     * Returns the buffer position for the image with index
     * 
     * @param index Must be < getImageCount()
     * @return
     */
    private int getImagePosition(int index) {
        return index * getImageSizeInBytes();
    }

    /**
     * Returns the size, in bytes, for one image - does not take faces or layers into account. Just one single image
     * 
     * @return
     */
    public int getImageSizeInBytes() {
        return width * height * format.sizeInBytes;
    }

    /**
     * Returns the number of images in this buffer, conveniance method - adds the result of layerCount, faceCount and
     * depth.
     * 
     * @return Number of images in buffer
     */
    public int getImageCount() {
        return Math.max(1, layerCount) * faceCount * Math.max(1, depth);

    }

    /**
     * Very slow method for getting float bitmap, will create new copy of array and possibly convert data.
     * The returned data will be in RGBA format, depending on number of components in source it may be R, RG, RGB or
     * RGBA.
     * If source is BGR order the data will be flipped.
     * 
     * @param index
     * @return
     */
    public float[] getAsFloatArray(int index) {
        if (index >= getImageCount()) {
            return null;
        }
        int count = width * height * format.typeSize;
        byte[] byteData = new byte[count];
        float[] data = new float[count];
        ByteBuffer fb = getImageBuffer(index);
        fb.get(byteData);
        for (int i = 0; i < count; i++) {
            int val = byteData[i];
            data[i] = ((float) (val & 0x0ff)) / 255;
        }
        if (format.typeSize == 1 || !format.isReverseOrder()) {
            return data;
        }
        float[] reversed = new float[data.length];
        int i = 0;
        switch (format.typeSize) {
            case 2:
                while (i < count) {
                    reversed[i] = data[i + 1];
                    reversed[i + 1] = data[i];
                    i += 2;
                }
                break;
            case 3:
                while (i < count) {
                    reversed[i] = data[i + 2];
                    reversed[i + 1] = data[i + 1];
                    reversed[i + 2] = data[i];
                    i += 3;
                }
                break;
            case 4:
                while (i < count) {
                    reversed[i] = data[i + 3];
                    reversed[i + 1] = data[i + 2];
                    reversed[i + 2] = data[i + 3];
                    reversed[i + 3] = data[i];
                    i += 4;
                }
                break;
            default:
                throw new IllegalArgumentException("Not implemented for " + format);
        }
        return reversed;
    }

    /**
     * Returns pixels as byte array
     * 
     * @return
     */
    public byte[] getAsByteArray() {
        ByteBuffer pixels = getBuffer();
        byte[] result = new byte[pixels.capacity()];
        pixels.get(result);
        return result;
    }

    /**
     * Creates a new image buffer from a byte bitmap. Use this to create an imagebuffer for instance from a loaded
     * 8 bit image.
     * 
     * @param bitmap
     * @param format
     * @param faceCount
     * @param width
     * @param height
     * @param transferFunction If a transfer function has been applied to the bitmap
     * @return
     */
    public static ImageBuffer create(@NonNull byte[] bitmap, @NonNull ImageFormat format, int faceCount, int width, int height, TransferFunction transferFunction) {
        return new ByteArrayImageBuffer(bitmap, format, 0, faceCount, width, height, 0, transferFunction);
    }

    public static ImageBuffer create(@NonNull short[] bitmap, @NonNull ImageFormat format, int width, int height) {
        return new ShortArrayImageBuffer(bitmap, format, 0, 1, width, height, 0);
    }

    public static ImageBuffer create(@NonNull int[] bitmap, @NonNull ImageFormat format, int width, int height) {
        return new IntArrayImageBuffer(bitmap, format, 0, 1, width, height, 0);
    }

    public static ImageBuffer create(@NonNull ByteBuffer bitmap, @NonNull ImageFormat format, int layerCount,
            int faceCount, int width, int height, int depth) {
        if (format.isFloatFormat()) {
            throw new IllegalArgumentException("Must call createFloatBuffer for float formats");
        } else {
            return new ImageBuffer(bitmap, format, layerCount, faceCount, new int[] { width, height }, null);
        }
    }

    public static FloatImageBuffer createFloatBuffer(@NonNull ByteBuffer bitmap, ImageFormat format, int layerCount,
            int faceCount, int[] dimension) {
        switch (format) {
            case VK_FORMAT_R32G32B32_SFLOAT:
                return new FloatImageBuffer(bitmap, format, layerCount, faceCount, dimension);
            case VK_FORMAT_R16G16B16_SFLOAT:
                return new HalfFloatImageBuffer(bitmap, layerCount, faceCount, dimension);
            default:
                throw new IllegalArgumentException("Not implemented for format " + format);

        }
    }

    public static ImageBuffer createFloatBuffer(float[] bitmap, ImageFormat destFormat, int layerCount, int faceCount,
            int[] dimension, float[] luminance) {
        if (luminance == null) {
            BufferHDRProperties props = BufferHDRProperties.get(bitmap, null);
            luminance = props.getLuminance();
        }
        switch (destFormat) {
            case VK_FORMAT_R32G32B32_SFLOAT:
                ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.length * Float.BYTES)
                        .order(ByteOrder.LITTLE_ENDIAN);
                FloatBuffer fb = buffer.asFloatBuffer();
                fb.put(bitmap);
                return new FloatImageBuffer(buffer, destFormat, layerCount, faceCount, dimension);
            case VK_FORMAT_R16G16B16_SFLOAT:
                FP16Convert converted = new FP16Convert(new short[bitmap.length]);
                converted.convert(bitmap);
                return createHalfFloatBuffer(converted.result, layerCount, faceCount, dimension);
            default:
                throw new IllegalArgumentException("Not implemented for format " + destFormat);
        }
    }

    public static HalfFloatImageBuffer createHalfFloatBuffer(short[] bitmap, int layerCount,
            int faceCount, int[] dimension) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.length * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sb = buffer.asShortBuffer();
        sb.put(bitmap);
        return new HalfFloatImageBuffer(buffer, layerCount, faceCount, dimension);
    }

    /**
     * Returns the imagebuffer info
     * 
     * @return
     */
    public ImageBufferInfo getInfo() {
        // Create new instance otherwise changes to format may be lost
        return new ImageBufferInfo(width, height, faceCount, format);
    }

}
