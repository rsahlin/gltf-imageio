package org.ktximageio.ktx;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

import org.ktximageio.ktx.FloatImageBuffer.Tonemap;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageUtils.ImageType;
import org.ktximageio.output.TonemappWindow;
import org.ktximageio.output.TonemappWindow.WindowListener;

public class AwtImageUtils {

    private static Frame frame = new Frame();

    private AwtImageUtils() {
    }

    @SuppressWarnings("serial")
    public static class MyWindow extends Frame implements java.awt.event.WindowListener {

        private final BufferedImage image;
        private final WindowListener listener;

        public MyWindow(String title, Frame owner, BufferedImage img,
                WindowListener l) {
            owner = (owner != null ? owner : new Frame());
            setTitle(title);
            image = img;
            listener = l;
            addWindowListener(this);
        }

        @Override
        public void paint(Graphics g) {
            if (image != null) {
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void windowClosing(WindowEvent e) {
            org.ktximageio.output.TonemappWindow.WindowListener.WindowEvent.dispatchEvent(listener,
                    WindowListener.Action.CLOSING);
            this.setVisible(false);
        }

        @Override
        public void windowClosed(WindowEvent e) {
        }

        @Override
        public void windowIconified(WindowEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void windowActivated(WindowEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            // TODO Auto-generated method stub

        }

    }

    public static BufferedImage toBufferedImage(float[] bitmap, int width, int height) {
        if (bitmap.length != 3 * width * height) {
            throw new IllegalArgumentException("INVALID VALUE, Wrong size of bitmap " + bitmap.length);
        }
        java.awt.color.ColorSpace xyzCS = java.awt.color.ColorSpace
                .getInstance(java.awt.color.ColorSpace.CS_LINEAR_RGB);
        ComponentColorModel cm = new ComponentColorModel(xyzCS, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_FLOAT);
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        raster.setDataElements(0, 0, width, height, bitmap);
        BufferedImage floatImage = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        return floatImage;
    }

    public static BufferedImage toBufferedImage(ImageBuffer buffer, int index, Tonemap tonemap) {
        if (buffer instanceof FloatImageBuffer) {
            return toBufferedImage(((FloatImageBuffer) buffer).tonemap(index, tonemap), buffer.width,
                    buffer.height);
        } else {
            if (tonemap != null && tonemap != Tonemap.NONE) {
                throw new IllegalArgumentException("Invalid tonemap for imagebuffer : " + tonemap);
            }
            return toBufferedImage(buffer.getImageBuffer(index), buffer.width, buffer.height, buffer.format);
        }
    }

    public static BufferedImage toBufferedImage(ImageBuffer buffer, int index) {
        return toBufferedImage(buffer.getImageBuffer(index), buffer.width, buffer.height, buffer.format);
    }

    public static BufferedImage toBufferedImage(ByteBuffer buffer, int width, int height, ImageFormat format) {
        byte[] array = new byte[buffer.limit() - buffer.position()];
        buffer.get(array);
        return toBufferedImage(array, width, height, format);
    }

    public static BufferedImage toBufferedImage(byte[] array, int width, int height, ImageFormat format) {
        ImageType type = ImageType.get(format);
        if (type == null) {
            throw new IllegalArgumentException("Cannot get ImageType from format " + format);
        }
        BufferedImage image = new BufferedImage(width, height, type.value);
        DataBufferByte dbb = new DataBufferByte(width * height * format.sizeInBytes);
        byte[] writeData = dbb.getData();
        System.arraycopy(array, 0, writeData, 0, writeData.length);
        int[] bandOffsets = new int[format.sizeInBytes];
        int band = format.isReverseOrder() ? format.sizeInBytes - 1 : 0;
        int step = format.isReverseOrder() ? -1 : 1;
        for (int i = 0; i < bandOffsets.length; i++) {
            bandOffsets[i] = band;
            band += step;
        }
        // Create a raster that will read the data according to the source
        Raster raster = Raster.createWritableRaster(
                new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, format.sizeInBytes,
                        width * format.sizeInBytes, bandOffsets),
                dbb, null);
        image.setData(raster);
        return image;
    }

    public static float[] convertToFloat(BufferedImage image, ImageFormat destFormat) {
        if (image.getData().getTransferType() == DataBuffer.TYPE_BYTE) {
            int type = image.getType();
            if (type == 0) {
                type = image.getColorModel().getNumComponents() == 4 ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB;
            }
            return convertToFloat((DataBufferByte) image.getData().getDataBuffer(), type, destFormat);
        }
        throw new IllegalArgumentException("NOT IMPLEMENTED, invalid source/dest");
    }

    private static float[] convertToFloat(DataBufferByte dataBuffer, int imageType, ImageFormat destFormat) {
        byte[] source = dataBuffer.getData();
        switch (imageType) {
            case BufferedImage.TYPE_3BYTE_BGR:
                if (destFormat.typeSize == 3) {
                    return ImageUtils.copyReverse(source, 3, 1f, 255);
                }
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                if (destFormat.typeSize == 4) {
                    return ImageUtils.copyReverse(source, 4, 1f, 255);
                } else if (destFormat.typeSize == 3) {
                    return ImageUtils.copyABGRToRGB(source, 255, 2.2f, 1f);
                }
            case BufferedImage.TYPE_INT_ARGB:
                if (destFormat.typeSize == 4) {
                    throw new IllegalArgumentException("NOT IMPLEMENTED AWT image type: " + imageType);
                } else if (destFormat.typeSize == 3) {
                    return ImageUtils.copyARGBToFloatRGB(source, 1f, 255);
                }
            default:
                throw new IllegalArgumentException("NOT IMPLEMENTED AWT image type: " + imageType);
        }
        throw new IllegalArgumentException("INVALID VALUE");
    }

    public static void displayImageWindow(String title, BufferedImage image, int xpos, int ypos) {
        displayImageWindow(title, image, xpos, ypos, null);
    }

    public static int displayImageWindow(String title, BufferedImage image, int xpos, int ypos,
            WindowListener listener) {
        MyWindow window = new MyWindow(title, frame, image, listener);
        window.setLocation(xpos, ypos);
        window.setVisible(true);
        Insets insets = window.getInsets();
        window.setSize(image.getWidth() + insets.left + insets.right, image.getHeight() + insets.top + insets.bottom);
        return window.getWidth();
    }

    /**
     * Display a buffer on screen
     * 
     * @param buffer
     * @param listener
     * @param width
     * @param height
     * @param format
     * @param title
     * @return
     */
    public static TonemappWindow displayBuffer(ByteBuffer buffer, WindowListener listener, int width, int height,
            ImageFormat format, String title) {
        byte[] image = new byte[buffer.remaining()];
        return displayBuffer(image, listener, width, height, format, title);
    }

    /**
     * Display a buffer on screen
     * 
     * @param buffer
     * @param listener
     * @param width
     * @param height
     * @param format
     * @param title
     * @return
     */
    public static TonemappWindow displayBuffer(byte[] image, WindowListener listener, int width, int height,
            ImageFormat format, String title) {
        BufferedImage bufferedImage = AwtImageUtils.toBufferedImage(image, width, height, format);
        TonemappWindow window = new TonemappWindow(title, null, listener);
        window.setImage(bufferedImage);
        AwtImageUtils.displayImageWindow(window, 0, 0);
        return window;
    }

    public static int displayImageWindow(TonemappWindow window, int xpos, int ypos) {
        window.setLocation(xpos, ypos);
        window.setVisible(true);
        window.paint(window.getGraphics());
        return window.getWidth();
    }

    public static void displayBuffer(ImageBuffer buffer, WindowListener listener, int windowX, int windowY, String... titles) {
        int screenWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
        for (int i = 0; i < buffer.faceCount; i++) {
            String title = titles != null && titles.length > i ? titles[i] : "Face " + i;
            TonemappWindow window = new TonemappWindow(title, null, listener);
            if (buffer instanceof FloatImageBuffer) {
                window.setFloatImage((FloatImageBuffer) buffer, i, Tonemap.PERCEPTUAL_QUANTIZER);
            } else {
                window.setImage(buffer, i);
            }
            windowX += AwtImageUtils.displayImageWindow(window, windowX, windowY);
            if (windowX >= screenWidth - buffer.width) {
                windowX = 0;
                windowY += buffer.height;
            }
        }
    }

    public static void displayBuffer(ImageBuffer buffer, WindowListener listener, String... titles) {
        displayBuffer(buffer, listener, 0, 0, titles);
    }

    /**
     * Slow java copy from source RGBA to BGR
     * 
     * @param source
     * @return
     */
    public static byte[] copyRGBAToBGR(ByteBuffer source) {
        byte[] sourceArray = new byte[source.remaining()];
        source.get(sourceArray);
        return copyRGBAToBGR(sourceArray);
    }

    /**
     * Slow java copy from source RGBA format to BGR
     * 
     * @param source
     * @return
     */
    public static byte[] copyRGBAToBGR(byte[] source) {
        byte[] result = new byte[source.length];
        int destIndex = 0;
        for (int sourceIndex = 0; sourceIndex < source.length;) {
            result[destIndex + 2] = source[sourceIndex++];
            result[destIndex + 1] = source[sourceIndex++];
            result[destIndex] = source[sourceIndex++];
            destIndex += 3;
            sourceIndex++;
        }
        return result;
    }

    /**
     * Converts RGB/BGR int pixel format to 3 byte format and returns as an array. This simply skipps the highest byte of the int.
     * 
     * @param pixels
     * @return
     */
    public static byte[] convertIntRGBtoByteArray(int[] pixels) {
        byte[] result = new byte[pixels.length * 3];
        int index = 0;
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i];
            result[index++] = (byte) (value & 0x0ff);
            result[index++] = (byte) ((value >>> 8) & 0x0ff);
            result[index++] = (byte) ((value >>> 16) & 0x0ff);
        }
        return result;
    }

    /**
     * Moves the alpha from highest byte to lowest.
     * 
     * @param pixels
     * @return
     */
    public static int[] shiftBGRAToABGR(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            int val = pixels[i];
            int alpha = val;
            pixels[i] = (val << 8) | (alpha >>> 24);
        }
        return pixels;
    }

}
