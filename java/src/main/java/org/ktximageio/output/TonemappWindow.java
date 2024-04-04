package org.ktximageio.output;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

import org.ktximageio.ktx.AwtImageUtils;
import org.ktximageio.ktx.FloatImageBuffer;
import org.ktximageio.ktx.FloatImageBuffer.Tonemap;
import org.ktximageio.ktx.ImageBuffer;

@SuppressWarnings("serial")
public class TonemappWindow extends Frame implements WindowListener {

    public interface WindowListener {

        class WindowEvent {
            public final Action action;

            WindowEvent(Action a) {
                action = a;
            }

            public static void dispatchEvent(WindowListener listener, Action action) {
                if (listener != null) {
                    listener.windowEvent(new WindowEvent(action));
                }
            }

        }

        enum Action {
            CLOSING(),
            RESIZED(),
            ACTIVATED(),
            DEACTIVATED();
        }

        /**
         * Dispatch a window event to listeners, listeners shall return true if the event was handled otherwise false.
         * 
         * @param event
         * @return True if the event was handled
         */
        boolean windowEvent(WindowEvent event);

    }

    private BufferedImage bufferedImage;
    private ImageBuffer image;
    private int index;
    private Tonemap tonemap;
    private final WindowListener listener;
    private int xOffset;
    private int yOffset;

    public TonemappWindow(String title, Frame owner, WindowListener l) {
        owner = (owner != null ? owner : new Frame());
        this.listener = l;
        addWindowListener(this);
        setTitle(title);
    }

    /**
     * Sets a float image buffer as the source of the window
     * 
     * @param img The imagebuffer containing one or more images.
     * @param i Index to the image to use
     * @param map Optional tonemapping operator
     */
    public void setFloatImage(FloatImageBuffer img, int i, Tonemap map) {
        image = img;
        index = i;
        tonemap = map;
    }

    /**
     * Sets a float image buffer as the source of the window
     * 
     * @param img The imagebuffer containing one or more images.
     * @param i Index to the image to use
     */
    public void setImage(ImageBuffer img, int i) {
        image = img;
        index = i;
        tonemap = null;
    }

    /**
     * Sets the buffered image
     * 
     * @param img
     */
    public void setImage(BufferedImage img) {
        this.bufferedImage = img;
    }

    @Override
    public void paint(Graphics g) {
        if (bufferedImage == null && image != null) {
            this.bufferedImage = AwtImageUtils.toBufferedImage(image, index, tonemap);
        }
        if (bufferedImage != null) {
            g.drawImage(bufferedImage, xOffset, yOffset, bufferedImage.getWidth(), bufferedImage.getHeight(), this);
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowClosing(WindowEvent e) {
        WindowListener.WindowEvent.dispatchEvent(listener, WindowListener.Action.CLOSING);
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

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && bufferedImage != null) {
            Insets insets = getInsets();
            setSize(bufferedImage.getWidth() + insets.left + insets.right, bufferedImage.getHeight() + insets.top
                    + insets.bottom);
            xOffset = insets.left;
            yOffset = insets.top;
        }
    }

}
