package org.ktximageio.ktx;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.eclipse.jdt.annotation.NonNull;

import com.luciad.imageio.webp.WebPImageReaderSpi;

/**
 * Imagereader for webp
 */
public class WebpImageIOReader extends ImageIOReader {

    @Override
    public ImageHeader read(@NonNull Path filePath) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new FileInputStream(filePath.toFile()));
        return super.read(iis, new WebPImageReaderSpi().createReaderInstance());
    }

    @Override
    public ImageHeader read(@NonNull ByteBuffer buffer) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteBufferInputStream(buffer));
        return super.read(iis, new WebPImageReaderSpi().createReaderInstance());
    }

    @Override
    public MimeFormat[] getMime() {
        return new MimeFormat[] { MimeFormat.WEBP };
    }

    @Override
    public String getReaderName() {
        return getClass().getCanonicalName();
    }

}
