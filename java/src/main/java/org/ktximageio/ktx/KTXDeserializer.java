package org.ktximageio.ktx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class KTXDeserializer extends KTX implements ImageReader {

    private KTXHeader header;

    @Override
    public ImageHeader read(Path filePath) throws IOException {
        System.out.println("URL: " + filePath.toUri().toURL());
        FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ);
        // FileChannel fileChannel = (FileChannel) Files.newByteChannel(filePath, EnumSet.of(StandardOpenOption.READ));
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        bb.load();
        ImageHeader h = read(bb);
        fc.close();
        return h;
    }

    @Override
    public ImageHeader read(ByteBuffer buffer) {
        return createKTXHeader(buffer);
    }

    private KTXHeader createKTXHeader(ByteBuffer byteBuffer) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        header = new KTXHeader(byteBuffer);
        return header;
    }

    @Override
    public MimeFormat[] getMime() {
        return new MimeFormat[] { MimeFormat.KTX2 };
    }

    @Override
    public String getReaderName() {
        return getClass().getCanonicalName();
    }

}
