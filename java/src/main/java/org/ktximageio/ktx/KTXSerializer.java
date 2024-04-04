package org.ktximageio.ktx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.ktximageio.ktx.FloatImageBuffer.BufferHDRProperties;
import org.ktximageio.ktx.FloatImageBuffer.FloatImageBufferInfo;
import org.ktximageio.ktx.ImageReader.ImageFormat;

/**
 * Provides the ability to serialize image data to KTX v2.0
 *
 */
public class KTXSerializer extends KTX {

    public static class Settings {

        private final ImageFormat format;
        public final int layerCount;
        public final int faceCount;
        public final int levelCount;
        public final int width;
        public final int height;
        public final int depth;

        public Settings(@NonNull ImageFormat f, int layers, int faces, int levels, int w, int h, int d) {
            format = f;
            layerCount = layers;
            faceCount = faces;
            levelCount = levels;
            width = w;
            height = h;
            depth = d;
        }

        /**
         * Returns the KTX format, this is fetched using the (image) format value.
         * 
         * @return
         */
        public KTXFormat getFormat() {
            KTXFormat ktxFormat = KTXFormat.get(format.value);
            if (ktxFormat == null) {
                // Convert to closest matching format
                throw new IllegalArgumentException("Not implemented for format " + format);
            }
            return ktxFormat;
        }

    }

    /**
     * Zlib compresses the data and writes out
     * 
     * @param path Where to serialize KTX
     * @param metaData Optional metadata
     * @param settings
     * @param data
     * @throws IOException
     */
    public void serialize(@NonNull Path path, KeyValueData metaData, @NonNull Settings settings, byte[] data)
            throws IOException {
        byte[] zipped = compressData(data);
        System.out.println("Compressed data to " + zipped.length + " bytes, from " + data.length + ", reduction %"
                + ((float) (data.length - zipped.length) / data.length) * 100);
        FileChannel out = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        ByteBuffer buffer = ByteBuffer.allocateDirect(zipped.length).order(ByteOrder.nativeOrder());
        buffer.put(zipped);
        buffer.position(0);
        writeKTX(out, metaData, settings, SuperCompression.ZLIB, buffer);
    }

    /**
     * Writes the buffers as faces - buffers format and sizes must match.
     * 
     * @param path
     * @param buffers
     * @throws IOException
     */
    public void serializeFaces(@NonNull Path path, @NonNull ImageBuffer[] buffers) throws IOException {
        ImageBuffer previous = null;
        ByteBuffer[] arrays = new ByteBuffer[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            ImageBuffer buffer = buffers[i];
            if (previous != null) {
                if (previous.format != buffer.format || previous.width != buffer.width
                        || previous.height != buffer.height || buffer.layerCount != 0 || buffer.faceCount != 1) {
                    System.err.println("Buffers does not match in format or size");
                    System.err.println("        Previous:                         Current:");
                    System.err.println(previous.format + " : " + buffer.format);
                    System.err.println(
                            previous.width + ", " + previous.height + " : " + buffer.width + ", " + buffer.height);
                    System.err.println(previous.layerCount + ", " + previous.faceCount + " : " + buffer.layerCount
                            + ", " + buffer.faceCount);
                    System.exit(1);
                }
            } else {
                previous = buffer;
            }
            arrays[i] = buffer.getBuffer();
        }
        byte[] zipped = compressData(arrays);
        System.out.println("Compressed data to " + zipped.length + " bytes");
        FileChannel out = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        KTXFormat format = KTXFormat.get(buffers[0].format.value);
        KeyValueData metaData = createMetadata(buffers);
        Settings settings = new Settings(buffers[0].format, 0, buffers.length, 0, buffers[0].width, buffers[0].height,
                0);
        ByteBuffer buffer = ByteBuffer.allocateDirect(zipped.length).order(ByteOrder.nativeOrder());
        buffer.put(zipped);
        buffer.position(0);
        writeKTX(out, metaData, settings, SuperCompression.ZLIB, buffer);
    }

    private void writeKTX(@NonNull FileChannel fc, KeyValueData metaData, @NonNull Settings settings,
            @NonNull SuperCompression superCompression, @NonNull ByteBuffer zipped) throws IOException {
        System.out.println("Writing KTX data.....");
        if (settings.format == null) {
            throw new IllegalArgumentException("Format is null");
        }
        ByteBuffer buffer = ByteBuffer
                .allocateDirect(
                        HEADER_SIZE + INDEX_SIZE + getDFDSize(settings.getFormat()) + getLevelSize(settings.levelCount)
                                + getKVDSize(metaData))
                .order(ByteOrder.LITTLE_ENDIAN);
        int offset = writeKTXHeader(buffer, metaData, settings, superCompression, zipped.capacity());
        int zipCount = zipped.remaining();
        System.out.println("Writing " + zipCount + " bytes of zipped data at offset " + offset);
        int written = 0;
        buffer.position(0);
        written += fc.write(buffer);
        written += fc.write(zipped);
        if (written < zipCount + offset) {
            throw new IllegalArgumentException(
                    "Did not write all data to FileChannel, " + written + ", should be " + zipCount + offset);
        }
        fc.close();
    }

    private KeyValueData createMetadata(ImageBuffer[] buffers) {
        if (buffers[0] instanceof FloatImageBuffer) {
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            double totalMean = 0;
            for (ImageBuffer ib : buffers) {
                BufferHDRProperties properties = ((FloatImageBufferInfo) ib.getInfo()).getProperties();
                min = Math.min(min, properties.minVal);
                max = Math.max(max, properties.maxVal);
                totalMean += properties.getMeanLuminance();
            }
            float mean = (float) (totalMean / buffers.length);

            KeyValue[] data = new KeyValue[3];
            data[0] = new KeyValue(METADATA[0], Float.toString(min));
            data[1] = new KeyValue(METADATA[1], Float.toString(max));
            data[2] = new KeyValue(METADATA[2], Float.toString(mean));
            return new KeyValueData(data);
        } else {
            KeyValue[] data = new KeyValue[2];
            data[0] = new KeyValue(METADATA[0], "0");
            data[1] = new KeyValue(METADATA[1], "255");
            return new KeyValueData(data);
        }
    }

    private int writeKTXHeader(@NonNull ByteBuffer buffer, KeyValueData metaData, @NonNull Settings settings,
            @NonNull SuperCompression superCompression, int zippedSize) {
        int position = buffer.position();
        int offset = writeHeader(buffer, settings, superCompression);
        if (offset != INDEX_OFFSET) {
            throw new IllegalArgumentException("INVALID VALUE, offset wrong after writing header: " + offset);
        }
        buffer.position(offset);
        IntBuffer intBuffer = buffer.asIntBuffer();
        offset += writeIndex(buffer, metaData, settings.getFormat(), settings.levelCount);
        if (offset != LEVEL_INDEX_OFFSET) {
            throw new IllegalArgumentException("INVALID VALUE, offset wrong after writing index: " + offset);
        }
        buffer.position(offset);
        offset += writeLevelIndex(buffer, metaData, settings, zippedSize);
        if (offset != LEVEL_INDEX_OFFSET + getLevelSize(settings.levelCount)) {
            throw new IllegalArgumentException("INVALID VALUE, offset wrong after writing levelindex: " + offset);
        }
        buffer.position(offset);
        offset += writeDFD(buffer, settings.getFormat());
        if (offset != LEVEL_INDEX_OFFSET + getDFDSize(settings.getFormat()) + getLevelSize(settings.levelCount)) {
            throw new IllegalArgumentException("INVALID VALUE, offset wrong after writing dfd: " + offset);
        }
        buffer.position(offset);
        offset += writeKVD(buffer, metaData);
        if (offset != LEVEL_INDEX_OFFSET + getDFDSize(settings.getFormat()) + getLevelSize(settings.levelCount)
                + getKVDSize(metaData)) {
            throw new IllegalArgumentException("INVALID VALUE, offset wrong after writing kvd: " + offset);
        }
        buffer.position(position + offset);
        return offset;
    }

    private int writeHeader(@NonNull ByteBuffer buffer, @NonNull Settings settings,
            @NonNull SuperCompression superCompression) {
        buffer.put(FILEIDENTIFIER);
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(settings.format.value);
        intBuffer.put(settings.format.typeSize);
        intBuffer.put(settings.width);
        intBuffer.put(settings.height);
        intBuffer.put(settings.depth); // depth
        intBuffer.put(settings.layerCount); // layerCount
        intBuffer.put(settings.faceCount); // faceCount
        intBuffer.put(settings.levelCount); // levelCount
        intBuffer.put(superCompression.value); // supercompression
        System.out.println(
                "Written header: Format " + settings.format + " w " + settings.width + ", h " + settings.height
                        + ", depth " + settings.depth + ", layers "
                        + settings.layerCount + ", faces " + settings.faceCount + ", levels " + settings.levelCount
                        + ", supercompression " + superCompression);
        return HEADER_SIZE;
    }

    private int writeIndex(@NonNull ByteBuffer buffer, KeyValueData metaData, @NonNull KTXFormat format,
            int levelCount) {
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(getDFDOffset(levelCount)); // dfd byte offset
        intBuffer.put(getDFDSize(format));
        intBuffer.put(getKVDOffset(levelCount, format)); // kvd offset
        intBuffer.put(getKVDSize(metaData)); // kvd bytesize
        buffer.position(buffer.position() + 16);
        LongBuffer longBuffer = buffer.asLongBuffer();
        longBuffer.put(0); // sgd offset
        longBuffer.put(0); // sgd bytesize
        return INDEX_SIZE;
    }

    private int writeLevelIndex(@NonNull ByteBuffer buffer, KeyValueData metaData, @NonNull Settings settings,
            int zippedSize) {
        int offset = getMipLevelOffset(settings.getFormat(), 0, metaData);
        LongBuffer longBuffer = buffer.asLongBuffer();
        longBuffer.put(offset);
        longBuffer.put(zippedSize);
        long uncompressed = Math.max(1, settings.layerCount) * settings.faceCount * settings.width
                * Math.max(1, settings.height) * Math.max(1, settings.depth)
                * settings.format.sizeInBytes;
        longBuffer.put(uncompressed);
        System.out.println("Written level index: leveloffset " + offset + ", compressed size " + zippedSize
                + ", uncompressed size " + uncompressed);
        return LEVEL_STRUCT_SIZE;
    }

    private int writeDFD(ByteBuffer buffer, KTXFormat format) {
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(getDFDSize(format));
        intBuffer.put(0); // descriptorType and vendorId
        intBuffer.put((short) KTX_VERSION | ((HEADER_SIZE - 4) << 16)); // descriptorBlockSize and version
        buffer.position(buffer.position() + 12);
        buffer.put((byte) 1); // colormode = KHR_DF_MODEL_RGBSDA (= 1)
        buffer.put((byte) 1); // color primaries KHR_DF_PRIMARIES_BT709 (= 1)
        buffer.put((byte) 1); // transfer function KHR_DF_TRANSFER_LINEAR (= 1)
        buffer.put((byte) 0); // flags
        buffer.put((byte) 0); // texelblockdimension
        buffer.put((byte) 0); // texelblockdimension
        buffer.put((byte) 0); // texelblockdimension
        buffer.put((byte) 0); // texelblockdimension
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        buffer.put((byte) 0); // bytesPlane
        for (int i = 0; i < format.typeSize; i++) {
            buffer.put((byte) 0); // bit offset
            buffer.put((byte) 0); // bit offset
            buffer.put((byte) 0); // bit length
            buffer.put(format.dataType); // channelType
            buffer.put((byte) 0); // sample position
            buffer.put((byte) 0); // sample position
            buffer.put((byte) 0); // sample position
            buffer.put((byte) 0); // sample position
            intBuffer = buffer.asIntBuffer();
            intBuffer.put(format.sampleLower);
            intBuffer.put(format.sampleUpper);
            buffer.position(buffer.position() + 8);
        }
        return getDFDSize(format);
    }

    private int writeKVD(ByteBuffer buffer, KeyValueData metaData) {
        if (metaData != null) {
            int startPos = buffer.position();
            int currentPosition = buffer.position();
            for (KeyValue kv : metaData.keyValues) {
                buffer.asIntBuffer().put(kv.length);
                currentPosition += 4;
                buffer.position(currentPosition);
                buffer.put(kv.key.getBytes(), 0, kv.key.length());
                buffer.put((byte) 0);
                buffer.put(kv.value.getBytes(), 0, kv.value.length());
                buffer.put((byte) 0);
                currentPosition = KTX.alignTo4(buffer.position());
                buffer.position(currentPosition);
                System.out.println("Written metadata " + kv.key + " = " + kv.value);
            }

            if (startPos + metaData.kvdByteLength != buffer.position()) {
                throw new IllegalArgumentException("Error - kvd offset not matching after writing keyvalues, is "
                        + buffer.position() + ", should be " + (startPos + metaData.kvdByteLength));
            }
            return metaData.kvdByteLength;
        }
        return 0;
    }

    private byte[] compressData(ByteBuffer... data) throws IOException {
        int size = 0;
        for (ByteBuffer buffer : data) {
            size += buffer.capacity();
        }
        byte[] result = new byte[size];
        int offset = 0;
        for (ByteBuffer buffer : data) {
            buffer.position(0);
            buffer.get(result, offset, buffer.capacity());
            offset += buffer.capacity();
        }
        return compressData(result);

    }

    private byte[] compressData(byte[]... data) throws IOException {
        int size = 0;
        for (byte[] array : data) {
            size += array.length;
        }
        byte[] result = new byte[size];
        int index = 0;
        for (byte[] array : data) {
            System.arraycopy(array, 0, result, index, array.length);
            index += array.length;
        }
        return compressData(result);
    }

    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        // deflater.setStrategy(Deflater.FILTERED);
        DeflaterOutputStream zip = new DeflaterOutputStream(baos, deflater);
        zip.write(data);
        zip.flush();
        zip.close();
        return baos.toByteArray();
    }

}
