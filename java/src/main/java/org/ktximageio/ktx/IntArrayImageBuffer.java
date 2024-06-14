package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.TransferFunction;

public class IntArrayImageBuffer extends ImageBuffer {

    private int[] arrayBitmap;

    protected IntArrayImageBuffer(@NonNull int[] bitmap, @NonNull ImageFormat format, int layerCount, int faceCount, int width, int height, int depth) {
        super(null, format, layerCount, faceCount, width, height, depth, TransferFunction.LINEAR);
        arrayBitmap = bitmap;
    }

    private void internalCreateBuffer() {
        if (bitmap == null) {
            createBuffer(arrayBitmap.length * Integer.BYTES);
            IntBuffer sb = bitmap.asIntBuffer();
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
     * Returns the int array for this image - this is the array specified when creating this image.
     * 
     * @return
     */
    public int[] getAsIntArray() {
        return arrayBitmap;
    }

}
