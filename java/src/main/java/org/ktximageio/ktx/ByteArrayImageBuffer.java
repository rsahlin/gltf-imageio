package org.ktximageio.ktx;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.TransferFunction;

/**
 * ImageBuffer for source where the pixel data is in a byte array
 * Use this for instance when the image source is from an image decoder where the data is fetched in a byte array.
 *
 */
public class ByteArrayImageBuffer extends ImageBuffer {

    private byte[] arrayBitmap;

    /**
     * Do NOT use - use static create methods in ImageBuffer
     */
    ByteArrayImageBuffer(@NonNull byte[] bitmap, @NonNull ImageFormat format, int layerCount, int faceCount, int width,
            int height, int depth, TransferFunction transferFunction) {
        super(null, format, layerCount, faceCount, width, height, depth, transferFunction);
        if (bitmap.length != getImageSizeInBytes() * getImageCount()) {
            throw new IllegalArgumentException("Wrong size of input bitmap byte array: " + bitmap.length
                    + ", should be " + getImageSizeInBytes() * getImageCount());
        }
        this.arrayBitmap = bitmap;
    }

    private void internalCreateBuffer() {
        if (bitmap == null) {
            createBuffer(arrayBitmap.length);
            bitmap.put(arrayBitmap);
        }
    }

    @Override
    public ByteBuffer getBuffer() {
        internalCreateBuffer();
        return super.getBuffer();
    }

    @Override
    public ByteBuffer getImageBuffer(int index) {
        internalCreateBuffer();
        return super.getImageBuffer(index);
    }

    @Override
    public byte[] getAsByteArray() {
        return arrayBitmap;
    }

    /**
     * Release resources
     */
    @Override
    public void destroy() {
        super.destroy();
        arrayBitmap = null;
    }

}
