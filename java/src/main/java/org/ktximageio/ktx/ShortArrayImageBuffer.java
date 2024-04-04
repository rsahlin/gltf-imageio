package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.TransferFunction;

public class ShortArrayImageBuffer extends ImageBuffer {

    private final short[] arrayBitmap;

    /**
     * Do NOT use - use static create methods in ImageBuffer
     */
    protected ShortArrayImageBuffer(@NonNull short[] bitmap, @NonNull ImageFormat format, int layerCount,
            int faceCount, int width, int height, int depth) {
        super(null, format, layerCount, faceCount, width, height, depth, TransferFunction.LINEAR);
        arrayBitmap = bitmap;
    }

    private void internalCreateBuffer() {
        if (bitmap == null) {
            createBuffer(arrayBitmap.length * Short.BYTES);
            ShortBuffer sb = bitmap.asShortBuffer();
            sb.put(arrayBitmap);
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

    /**
     * Returns the short array for this image - this is the array specified when creating this image.
     * 
     * @return
     */
    public short[] getAsShortArray() {
        return arrayBitmap;
    }

}
