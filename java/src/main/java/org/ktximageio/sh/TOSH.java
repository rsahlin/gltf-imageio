package org.ktximageio.sh;

import java.io.IOException;

import org.ktximageio.CommandLineApp;
import org.ktximageio.Options.Option;
import org.ktximageio.Options.OptionResolver;
import org.ktximageio.Orientation;
import org.ktximageio.ktx.AwtImageUtils;
import org.ktximageio.ktx.ImageBuffer;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.output.TonemappWindow.WindowListener;

/**
 * Application entrypoint to create an irradiance map with spherical harmonic coefficients
 */
public class TOSH extends CommandLineApp implements WindowListener {

    public static final float[] TEST_COEFFICIENTS = new float[] {
            0.2629173f, 0.24339972f, 0.017253349f,
            -0.4100555f, -0.37980413f, -0.026886208f,
            -0.0012579918f, -0.0011651911f, -8.2491344E-5f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0019890629f, 0.0018423218f, 1.3042969E-4f,
            -0.13375147f, -0.12387587f, -0.00877106f,
            0.0f, 0.0f, 0.0f,
            -0.41668287f, -0.38595766f, -0.027323356f
    };

    public static final float[] GRACE_CATHEDRAL = new float[] {
            0.078908f, 0.043710f, 0.054161f,
            0.039499f, 0.034989f, 0.060488f,
            -0.033974f, -0.018236f, -0.026940f,
            -0.029213f, -0.005562f, 0.000944f,
            -0.011141f, -0.005090f, -0.012231f,
            -0.026240f, -0.022401f, -0.047479f,
            -0.015570f, -0.009471f, -0.014733f,
            0.056014f, 0.021444f, 0.013915f,
            0.021205f, -0.005432f, -0.030374f };

    public enum TOSHOption implements Option {
        DISPLAYSH("displaysh");

        public final String value;

        TOSHOption(String val) {
            value = val;
        }

        @Override
        public String getKey() {
            return value;
        }
    }

    public TOSH(String[] args) {
        super(args, new OptionResolver(TOSHOption.values()));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        TOSH main = new TOSH(args);
        main.run();
    }

    @Override
    public boolean windowEvent(WindowEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void run() {
        String cubemapName = infiles != null ? infiles[0] : outfile;
        try {
            SphericalHarmonics sh = new SphericalHarmonics();
            ImageBuffer cubemapImage = sh.loadImage(cubemapName);
            // float[][] coefficients = sh.fromCubeMap(cubemapImage);
            // float[][] coefficients = SphericalHarmonics.toCoefficientArray(TEST_COEFFICIENTS);
            float[][] coefficients = SphericalHarmonics.toCoefficientArray(GRACE_CATHEDRAL);
            if (options.isOptionSet(TOSHOption.DISPLAYSH)) {
                displayCoefficients(cubemapImage, sh, coefficients);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void validateParameters(String[] args) {
        if (outfile == null) {
            exit("Missing outfile");
        }
        if (!options.isOptionSet(TOSHOption.DISPLAYSH)) {
            checkInfiles();
        }
    }

    @Override
    protected boolean hasArgs() {
        return getArgumentCount() >= 2;
    }

    private void displayCoefficients(ImageBuffer cubemapImage, SphericalHarmonics sh, float[][] coefficients) {
        int size = 256;
        byte[] images = new byte[size * size * 6 * 3];
        int index = 0;
        for (Orientation face : Orientation.values()) {
            sh.renderImage(face, coefficients, size, images, index);
            index += size * size * 3;
        }
        ImageBuffer ib = ImageBuffer.create(images, ImageFormat.VK_FORMAT_B8G8R8_UNORM, 6, size, size, null);
        AwtImageUtils.displayBuffer(ib, this, Orientation.getOrientations());
        AwtImageUtils.displayBuffer(cubemapImage, this, 0, size + 50, Orientation.getOrientations());

    }

}
