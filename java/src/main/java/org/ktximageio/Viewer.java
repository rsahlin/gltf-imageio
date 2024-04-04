package org.ktximageio;

import java.io.IOException;
import java.nio.file.Paths;

import org.ktximageio.Options.Option;
import org.ktximageio.Options.OptionResolver;
import org.ktximageio.ktx.AwtImageUtils;
import org.ktximageio.ktx.ImageBuffer;
import org.ktximageio.ktx.ImageHeader;
import org.ktximageio.ktx.ImageReader;
import org.ktximageio.ktx.ImageReader.MimeFormat;

/**
 * Displays the known imageio image source types, for instance .HDR and .KTX
 */
public class Viewer extends CommandLineApp {

    public enum ViewerOption implements Option {
        DISPLAYSH("windowsize");

        public final String value;

        ViewerOption(String val) {
            value = val;
        }

        @Override
        public String getKey() {
            return value;
        }
    }

    /**
     * @param args
     * @param optionResolver
     */
    protected Viewer(String[] args, OptionResolver optionResolver) {
        super(args, optionResolver);
    }

    @Override
    protected boolean hasArgs() {
        return getArgumentCount() >= 1;
    }

    @Override
    public boolean windowEvent(WindowEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected String parseOutFile(String[] args) {
        return null;
    }

    @Override
    protected void run() {
        for (String filename : infiles) {
            try {
                MimeFormat mime = MimeFormat.get(filename);
                System.out.println("Loading " + filename + " using MIME " + mime);
                ImageReader reader = ImageReader.getImageReader(mime);
                ImageHeader header;
                header = reader.read(Paths.get(filename));
                ImageBuffer buffer = header.getData();
                AwtImageUtils.displayBuffer(buffer, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void validateParameters(String[] args) {
        if (infiles == null) {
            exit("Missing infile");
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Viewer main = new Viewer(args, new OptionResolver(null));
        main.run();
    }

}
