package org.ktximageio.ktx;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import org.ktximageio.CommandLineApp;
import org.ktximageio.Options;
import org.ktximageio.Options.Option;
import org.ktximageio.Options.OptionResolver;
import org.ktximageio.Orientation;
import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MimeFormat;
import org.ktximageio.output.TonemappWindow.WindowListener;

/**
 * Application entrypoint for conversion of HDR or increased range image formats to KTX V2
 *
 */
public class ToKTX2 extends CommandLineApp implements WindowListener {

    public enum KTXOption implements Option {
        DISPLAYKTX("displayktx"),
        TOCUBEMAP("tocubemap"),
        BINARY("binary");

        public final String value;

        KTXOption(String val) {
            value = val;
        }

        @Override
        public String getKey() {
            return value;
        }
    }

    public ToKTX2(String[] args) throws IOException {
        super(args, new OptionResolver(KTXOption.values()));
    }

    public static void main(String[] args) throws IOException {
        ToKTX2 main = new ToKTX2(args);
        main.run();
    }

    @Override
    protected boolean hasArgs() {
        return getArgumentCount() >= 2;
    }

    @Override
    protected void run() {
        try {
            if (options.isOptionSet(KTXOption.DISPLAYKTX)) {
                displayKTX(outfile);
            } else if (options.isOptionSet(KTXOption.TOCUBEMAP)) {
                toCubemap();
            } else if (options.isOptionSet(KTXOption.BINARY)) {
                Path filePath = Paths.get(infiles[0]);
                FileChannel fileChannel = (FileChannel) Files.newByteChannel(filePath,
                        EnumSet.of(StandardOpenOption.READ));
                MappedByteBuffer bb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                bb.load();
                fileChannel.close();
                byte[] byteArray = new byte[bb.capacity()];
                bb.position(0);
                bb.get(byteArray);
                throw new IllegalArgumentException("Not implemented");
            } else {
                ImageBuffer[] buffers = packInfiles(infiles, outfile, options);
                writeOutputImage(outfile, buffers);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void toCubemap() {
        if (infiles.length > 1) {
            exit("Invalid number of infiles");
        }
        PanoramaToCubemap ptc = new PanoramaToCubemap();
        ImageBuffer[] buffers = new ImageBuffer[6];
        int index = 0;
        ImageBuffer input = readInputImage(null, infiles[0]);
        ImageFormat format = input.getFormat().isFloatFormat() ? ImageFormat.VK_FORMAT_R16G16B16_SFLOAT
                : ImageFormat.VK_FORMAT_R8G8B8_UNORM;
        for (Orientation orientation : Orientation.values()) {
            buffers[index++] = ptc.createCubeMapFace(input, orientation, format);
        }
        writeOutputImage(outfile, buffers);
    }

    private void displayKTX(String outfile) throws IOException {
        KTXDeserializer deserializer = new KTXDeserializer();
        ImageHeader header = deserializer.read(Paths.get(outfile));
        ImageBuffer buffer = header.getData();
        header.destroy();
        AwtImageUtils.displayBuffer(buffer, this);
    }

    private ImageBuffer[] packInfiles(String[] infiles, String outfile, Options options) {
        MimeFormat inputFormat = getFormat(infiles);
        ImageReader imageReader = ImageReader.getImageReader(inputFormat);
        ImageBuffer[] buffers = new ImageBuffer[infiles.length];
        for (int i = 0; i < infiles.length; i++) {
            buffers[i] = readInputImage(imageReader, infiles[i]);
        }
        return buffers;
    }

    private void writeOutputImage(String outfile, ImageBuffer... buffers) {
        KTXSerializer serializer = new KTXSerializer();
        try {
            serializer.serializeFaces(Paths.get(outfile), buffers);
        } catch (IOException e) {
            e.printStackTrace();
            exit("Error writing to " + outfile);
        }
    }

    private ImageBuffer readInputImage(ImageReader imageReader, String infile) {
        imageReader = imageReader == null ? ImageReader.getImageReader(infile, ImageFormat.VK_FORMAT_R16G16B16_SFLOAT)
                : imageReader;
        if (imageReader == null) {
            exit("Could not find imagereader for format " + infile + ", did you forget to set an option?");
        }
        try {
            ImageHeader header = imageReader.read(Paths.get(infile));
            ImageBuffer buffer = header.getData();
            System.out.println("Read image " + infile + ", into buffer with format " + buffer.getFormat());
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            exit("Error reading file " + infile);
        }
        throw new IllegalArgumentException("Should never happen");
    }

    private MimeFormat getFormat(String[] filenames) {
        MimeFormat mimeFormat = null;
        for (String name : filenames) {
            MimeFormat f = MimeFormat.get(name);
            if (mimeFormat == null) {
                mimeFormat = f;
            } else if (f != mimeFormat) {
                System.err.println(
                        "Image format of inputfiles does not match, inputfiles must contain mime file extension");
                return null;
            }
        }
        return mimeFormat;
    }

    @Override
    protected void validateParameters(String[] args) {
        if (outfile == null) {
            exit("Missing outfile");
        }
        if (!outfile.endsWith("." + MimeFormat.KTX2.extensions[0])) {
            outfile = MimeFormat.KTX2.getFilename(outfile);
        }
        if (!options.isOptionSet(KTXOption.DISPLAYKTX)) {
            checkInfiles();
        }
    }

    @Override
    public boolean windowEvent(WindowEvent event) {
        System.out.println("Event: " + event.action);
        return true;
    }

}
