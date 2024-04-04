package org.ktximageio.ktx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MetaData;

public class KTX {

    public static final String[] METADATA = new String[] { "MIN", "MAX", "AVG" };

    public static final byte DATATYPE_FLOAT = (byte) 0x80;
    public static final byte DATATYPE_SIGNED = 0x40;
    public static final byte DATATYPE_EXPONENT = 0x20;
    public static final byte DATATYPE_LINEAR = 0x10;

    static final byte[] FILEIDENTIFIER = { (byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x32, 0x30, (byte) 0xBB, 0x0D, 0x0A,
            0x1A, 0x0A };

    public static final int KTX_VERSION = 2;
    public static final int HEADER_SIZE = 9 * 4 + 12; // size in bytes
    public static final int INDEX_OFFSET = HEADER_SIZE;
    public static final int INDEX_SIZE = 4 * 4 + 2 * 8; // size in bytes
    public static final int LEVEL_INDEX_OFFSET = INDEX_OFFSET + INDEX_SIZE;
    public static final int DFD_HEADER = 24; // size in bytes
    public static final int DFD_COMPONENT_SIZE = 16; // size per component
    public static final int LEVEL_STRUCT_SIZE = 24; // size in bytes

    public enum SuperCompression {
        None(0),
        BasisLZ(1),
        ZStandard(2),
        ZLIB(3);

        public final int value;

        SuperCompression(int val) {
            value = val;
        }

        public static SuperCompression get(int value) {
            for (SuperCompression sc : values()) {
                if (value == sc.value) {
                    return sc;
                }
            }
            return null;
        }
    }

    public enum KTXFormat {
        VK_FORMAT_A8B8G8R8_UNORM_PACK32(51, 4, DATATYPE_LINEAR, 0, 0, 4),
        VK_FORMAT_R8G8B8_UNORM(23, 3, DATATYPE_LINEAR, 0, 0, 3),
        VK_FORMAT_B8G8R8_UNORM(30, 3, DATATYPE_LINEAR, 0, 0, 3),
        VK_FORMAT_E5B9G9R9_UFLOAT_PACK32(123, 3, DATATYPE_FLOAT | DATATYPE_LINEAR, 0, 0, 3),
        VK_FORMAT_B10G11R11_UFLOAT_PACK32(122, 3, DATATYPE_FLOAT | DATATYPE_LINEAR, 0, 0, 3),
        VK_FORMAT_R16G16B16A16_SFLOAT(97, 4, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 8),
        VK_FORMAT_R16G16B16A16_UNORM(91, 4, DATATYPE_LINEAR, 0, 0, 8),
        VK_FORMAT_R32G32B32A32_SFLOAT(109, 4, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 16),
        VK_FORMAT_R16G16B16_SFLOAT(90, 3, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 6),
        VK_FORMAT_R16G16B16_UNORM(84, 3, DATATYPE_LINEAR, 0, 0, 6),
        VK_FORMAT_R32G32B32_SFLOAT(106, 3, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 12),
        VK_FORMAT_R16G16_SFLOAT(83, 2, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 4),
        VK_FORMAT_R16G16_UNORM(77, 2, DATATYPE_LINEAR, 0, 0, 4),
        VK_FORMAT_R32G32_SFLOAT(103, 2, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 8),
        VK_FORMAT_R16_SFLOAT(76, 2, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 2),
        VK_FORMAT_R16_UNORM(70, 2, DATATYPE_LINEAR, 0, 0, 2),
        VK_FORMAT_R32_SFLOAT(100, 1, DATATYPE_FLOAT | DATATYPE_LINEAR | DATATYPE_SIGNED, 0, 0, 4);

        public final int value;
        public final int typeSize;
        public final byte dataType;
        public final int sampleLower;
        public final int sampleUpper;
        public final int sizeInBytes;

        KTXFormat(int val, int size, int type, int lower, int upper, int bytes) {
            value = val;
            typeSize = size;
            dataType = (byte) type;
            sampleLower = lower;
            sampleUpper = upper;
            sizeInBytes = bytes;
        }

        public static KTXFormat get(int vkFormat) {
            for (KTXFormat f : KTXFormat.values()) {
                if (f.value == vkFormat) {
                    return f;
                }
            }
            return null;
        }

    }

    public enum ImageViewType {
        VK_IMAGE_VIEW_TYPE_1D(0),
        VK_IMAGE_VIEW_TYPE_2D(1),
        VK_IMAGE_VIEW_TYPE_3D(2),
        VK_IMAGE_VIEW_TYPE_CUBE(3),
        VK_IMAGE_VIEW_TYPE_1D_ARRAY(4),
        VK_IMAGE_VIEW_TYPE_2D_ARRAY(5),
        VK_IMAGE_VIEW_TYPE_CUBE_ARRAY(6);

        public final int value;

        ImageViewType(int val) {
            value = val;
        }

        public static ImageViewType get(int value) {
            for (ImageViewType ivt : values()) {
                if (value == ivt.value) {
                    return ivt;
                }
            }
            return null;
        }

    }

    public enum TextureType {
        TYPE_1D(0),
        TYPE_2D(1),
        TYPE_3D(2),
        CUBEMAP(3),
        TYPE_1D_ARRAY(4),
        TYPE_2D_ARRAY(5),
        TYPE_3D_ARRAY(-1),
        CUBEMAP_ARRAY(6);

        public final int vkValue;

        TextureType(int value) {
            vkValue = value;
        }

        public static TextureType getTextureType(int layerCount, int faceCount, int pixelWidth, int pixelHeight,
                int pixelDepth) {
            if (faceCount == 6) {
                return layerCount > 1 ? TextureType.CUBEMAP_ARRAY : TextureType.CUBEMAP;
            }
            if (layerCount > 0) {
                return pixelDepth > 1 ? TextureType.TYPE_3D_ARRAY
                        : pixelWidth > 0 ? TextureType.TYPE_2D_ARRAY : TextureType.TYPE_1D_ARRAY;
            }
            return pixelDepth > 1 ? TextureType.TYPE_3D : pixelWidth > 0 ? TextureType.TYPE_2D : TextureType.TYPE_1D;
        }

        public boolean isType(TextureType... types) {
            for (TextureType t : types) {
                if (this.vkValue == t.vkValue) {
                    return true;
                }
            }
            return false;
        }

    }

    public static class KTXHeader implements ImageHeader {
        private final byte[] header = new byte[FILEIDENTIFIER.length];
        final int vkFormat;
        final int typeSize;
        final int pixelWidth;
        final int pixelHeight;
        final int pixelDepth;
        final int layerCount;
        final int faceCount;
        final int levelCount;
        final int supercompressionScheme;

        private Index index;
        private DataFormatDescriptor dfd;
        // This may be a memory mapped file and may not be exposed outside of this class.
        private ByteBuffer fileData;
        private KeyValueData metaData;

        public KTXHeader(ByteBuffer data) {
            fileData = data;
            data.get(header);
            if (!Arrays.equals(FILEIDENTIFIER, header)) {
                throw new IllegalArgumentException("INVALID VALUE, KTX header not found.");
            }
            IntBuffer intBuffer = data.asIntBuffer();
            vkFormat = intBuffer.get();
            typeSize = intBuffer.get();
            pixelWidth = intBuffer.get();
            pixelHeight = intBuffer.get();
            pixelDepth = intBuffer.get();
            layerCount = intBuffer.get();
            faceCount = intBuffer.get();
            levelCount = intBuffer.get();
            supercompressionScheme = intBuffer.get();
            data.position(HEADER_SIZE);

            System.out.println("Created KTX header: Format " + ImageFormat.get(vkFormat) + ", w " + pixelWidth + ", h "
                    + pixelHeight + ", depth " + pixelDepth + ", layers " + layerCount + ", faces " + faceCount
                    + ", levels " + levelCount + ", supercompression " + SuperCompression.get(supercompressionScheme));

            index = new Index(data, levelCount);
            dfd = new DataFormatDescriptor(data, KTXFormat.get(vkFormat));
            metaData = getMetaData(data, index);
        }

        /**
         * Returns the {@link TextureType} using layerCount, faceCount, pixelWidth, pixelHeight and pixelDepth
         * 
         * @return The KTX texture type of the image(s)
         */
        TextureType getTextureType() {
            return TextureType.getTextureType(layerCount, faceCount, pixelWidth, pixelHeight, pixelDepth);
        }

        private KeyValueData getMetaData(ByteBuffer data, Index i) {
            if (i.kvdByteOffset > 0 && i.kvdByteLength > 0) {
                int startPos = data.position();
                int current = i.kvdByteOffset;
                ArrayList<KTX.KeyValue> metaDataList = new ArrayList<KTX.KeyValue>();
                while (current < i.kvdByteLength + i.kvdByteOffset) {
                    data.position(current);
                    int keyValueLength = data.asIntBuffer().get();
                    byte[] copy = new byte[keyValueLength];
                    current += 4;
                    data.position(current);
                    data.get(copy);
                    KeyValue kv = new KeyValue(copy);
                    metaDataList.add(kv);
                    current = KTX.alignTo4(current + keyValueLength);
                }
                System.out.println("Read metadata: ");
                for (KeyValue kv : metaDataList) {
                    System.out.println(kv.key + " : " + kv.value);
                }
                // Restore fileposition
                data.position(startPos);
                return new KeyValueData(metaDataList.toArray(new KeyValue[0]));
            }
            return null;
        }

        /**
         * Returns the byte offset for the level index.
         * 
         * @param level The level to return the offset for
         * @return Offset to levelindex or -1 if invalid level
         */
        public long getOffset(int level) {
            LevelIndex levelIndex = index.getLevelIndex(level);
            return levelIndex != null ? levelIndex.byteOffset : -1;
        }

        /**
         * Returns the size of the level index
         * 
         * @param level The level to return the size for
         * @return Size of levelindex or -1 if invalid level
         */
        public long getSize(int level) {
            LevelIndex levelIndex = index.getLevelIndex(level);
            return levelIndex != null ? levelIndex.byteLength : -1;
        }

        /**
         * Returns the uncompressed size of the level index
         * 
         * @param level The level to return uncompressed size for
         * @return Uncompressed size in bytes or -1 if invalid level
         */
        public long getUncompressedSize(int level) {
            LevelIndex levelIndex = index.getLevelIndex(level);
            return levelIndex != null ? levelIndex.uncompressedByteLength : -1;
        }

        /**
         * Returns the {@link DataFormatDescriptorBlock} array.
         * 
         * @return
         */
        public DataFormatDescriptorBlock[] getDFDBlocks() {
            return dfd.dfdBlock;
        }

        @Override
        public int getWidth() {
            return pixelWidth;
        }

        @Override
        public int getHeight() {
            return pixelHeight;
        }

        @Override
        public int getDepth() {
            return pixelDepth;
        }

        @Override
        public int getLevelCount() {
            return levelCount;
        }

        @Override
        public int getLayerCount() {
            return layerCount;
        }

        @Override
        public int getFaceCount() {
            return faceCount;
        }

        @Override
        public ImageFormat getFormat() {
            return ImageFormat.get(vkFormat);
        }

        @Override
        public ImageBuffer getData() {
            try {
                ImageFormat format = ImageFormat.get(vkFormat);
                if (format.isFloatFormat()) {
                    return ImageBuffer.createFloatBuffer(getImageFaceAsByteBuffer(0), format, layerCount, faceCount,
                            new int[] { pixelWidth, pixelHeight, pixelDepth });
                } else {
                    return ImageBuffer.create(getImageFaceAsByteBuffer(0), ImageFormat.get(vkFormat), layerCount,
                            faceCount, pixelWidth, pixelHeight, pixelDepth);
                }
            } catch (DataFormatException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the mip-level image data
         * 
         * @param level The mip-level to return data for.
         * @return The buffer containing the specified face and miplevel. null if invalid face/level or there
         * was an exception unpacking the data.
         */
        private byte[] getImageFace(int level) throws DataFormatException {
            if (level > levelCount) {
                return null;
            }
            Inflater inflater = new Inflater();
            long levelIndex = getOffset(0);
            int byteSize = (int) getSize(0);
            int uncompressed = (int) getUncompressedSize(level);
            fileData.position((int) levelIndex);
            byte[] zipped = new byte[byteSize];
            System.out.println("Fetching " + byteSize + " bytes at offset " + levelIndex);
            fileData.get(zipped);
            byte[] image = new byte[uncompressed];
            inflater.setInput(zipped);
            int inflated = inflater.inflate(image);
            if (inflated != uncompressed) {
                throw new IllegalArgumentException("INVALID VALUE, Not inflated whole image "
                        + inflated + " of " + uncompressed);
            }
            return image;
        }

        /**
         * Returns the mip-level image data
         * 
         * @param level The mip-level to return data for.
         * @return The buffer containing the specified face and miplevel. null if invalid face/level or there
         * was an exception unpacking the data.
         * @throws DataFormatException
         */
        private ByteBuffer getImageFaceAsByteBuffer(int level) throws DataFormatException {
            byte[] deflated = getImageFace(level);
            ByteBuffer buffer = ByteBuffer.allocateDirect(deflated.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(deflated);
            buffer.position(0);
            return buffer;
        }

        @Override
        public MetaData getMetaData() {
            return metaData;
        }

        @Override
        public void destroy() {
            fileData = null;
        }

    }

    /**
     * The levelindex contains the data from the levelImages Structure
     * for each layer
     * for each face
     * for each z
     * [imagedata]
     * 
     *
     */
    public static class LevelIndex {
        public final long byteOffset;
        public final long byteLength;
        public final long uncompressedByteLength;

        public LevelIndex(LongBuffer longBuffer) {
            byteOffset = longBuffer.get();
            byteLength = longBuffer.get();
            uncompressedByteLength = longBuffer.get();
        }

    }

    public static class Index {
        public final int dfdByteOffset;
        public final int dfdByteLength;
        public final int kvdByteOffset;
        public final int kvdByteLength;
        public final long sgdByteOffset;
        public final long sgdByteLength;
        private final LevelIndex[] levelIndex;

        public Index(ByteBuffer fileData, int levelCount) {
            int position = fileData.position();
            levelCount = Math.max(1, levelCount);
            IntBuffer intBuffer = fileData.asIntBuffer();
            dfdByteOffset = intBuffer.get();
            dfdByteLength = intBuffer.get();
            kvdByteOffset = intBuffer.get();
            kvdByteLength = intBuffer.get();
            fileData.position(position + 16);
            LongBuffer longBuffer = fileData.asLongBuffer();
            sgdByteOffset = longBuffer.get();
            sgdByteLength = longBuffer.get();
            levelIndex = new LevelIndex[levelCount];
            for (int i = 0; i < levelCount; i++) {
                levelIndex[i] = new LevelIndex(longBuffer);
            }
            fileData.position(position + INDEX_SIZE + getLevelSize(levelCount));
        }

        /**
         * Returns the {@link LevelIndex} for the specified level, or null if invalid level
         * 
         * @param level
         * @return LevelIndex or null
         */
        LevelIndex getLevelIndex(int level) {
            return level >= 0 && level < levelIndex.length ? levelIndex[level] : null;
        }

    }

    public static class KeyValueData extends MetaData {

        public final int kvdByteLength;
        public final KeyValue[] keyValues;

        public KeyValueData(KeyValue[] keyVals) {
            keyValues = keyVals;
            int l = 0;
            for (KeyValue kv : keyVals) {
                l += kv.getAlignedLength();
            }
            kvdByteLength = l + keyVals.length * 4;
        }

        @Override
        public Float getAsFloatValue(String key) {
            for (KeyValue kv : keyValues) {
                if (key.equalsIgnoreCase(kv.key)) {
                    return Float.parseFloat(kv.value);
                }
            }
            return null;
        }

        @Override
        public String getValue(String key) {
            for (KeyValue kv : keyValues) {
                if (key.equalsIgnoreCase(kv.key)) {
                    return kv.value;
                }
            }
            return null;
        }
    }

    public static class KeyValue {
        public final String key;
        public final String value;
        public final int length;

        public KeyValue(String k, String val) {
            key = k;
            value = val;
            length = k.length() + val.length() + 2;
        }

        public KeyValue(byte[] keyValue) {
            int delimit = getNulPosition(keyValue, 0);
            key = new String(keyValue, 0, delimit);
            delimit++;
            value = new String(keyValue, delimit, keyValue.length - delimit - 1);
            length = key.length() + value.length() + 2;
        }

        private int getNulPosition(byte[] data, int start) {
            while (start < data.length) {
                if (data[start] == 0) {
                    return start;
                }
                start++;
            }
            return -1;
        }

        /**
         * Returns the 32 bit word aligned length
         * 
         * @return
         */
        public int getAlignedLength() {
            return (length + 3) & 0x0fffffffc;
        }

    }

    public static class DataFormatDescriptor {
        public final int dfdTotalSize;
        final DataFormatDescriptorBlock[] dfdBlock;

        public DataFormatDescriptor(ByteBuffer fileData, KTXFormat format) {
            int position = fileData.position();
            IntBuffer intBuffer = fileData.asIntBuffer();
            dfdTotalSize = intBuffer.get();
            int blockSize = getDesriptorBlockSize(format);
            int count = (dfdTotalSize - 4) / blockSize;
            dfdBlock = new DataFormatDescriptorBlock[count];
            for (int i = 0; i < count; i++) {
                dfdBlock[i] = new DataFormatDescriptorBlock(intBuffer, format);
            }
            fileData.position(position + dfdTotalSize);
        }

    }

    public static class DataFormatDescriptorBlock {

        private final int[] descriptorBlock = new int[DFD_HEADER / 4];
        private final int[][] sampleData;

        public DataFormatDescriptorBlock(IntBuffer intBuffer, KTXFormat format) {
            int position = intBuffer.position();
            sampleData = new int[format.typeSize][DFD_COMPONENT_SIZE / 4];
            intBuffer.get(descriptorBlock);
            for (int i = 0; i < format.typeSize; i++) {
                intBuffer.get(sampleData[i]);
            }
            if (intBuffer.position() != position + getDesriptorBlockSize(format) / 4) {
                throw new IllegalArgumentException("INVALID STATE, Could not read DFD");
            }
        }

        /**
         * Returns the vendorID
         * 
         * @return
         */
        public int getVendorID() {
            return descriptorBlock[0] & 0x01ffff;
        }

        /**
         * Returns the descriptor type
         * 
         * @return
         */
        public int getDescriptorType() {
            return (descriptorBlock[0] & 0x0fff7) >>> 17;
        }

        /**
         * Returns the descriptor blocksize
         * 
         * @return
         */
        public int getDescriptorBlockSize() {
            return (descriptorBlock[1] & 0x0ffff) >>> 16;
        }

        /**
         * Returns the version number
         * 
         * @return
         */
        public int getVersionNumber() {
            return descriptorBlock[1] & 0x0ffff;
        }

    }

    static int getDesriptorBlockSize(KTXFormat format) {
        return DFD_HEADER + DFD_COMPONENT_SIZE * format.typeSize;
    }

    static int getDFDSize(KTXFormat format) {
        return getDesriptorBlockSize(format) + 4;
    }

    static int getLevelSize(int levelCount) {
        return LEVEL_STRUCT_SIZE * Math.max(1, levelCount);
    }

    static int getDFDOffset(int levelCount) {
        return HEADER_SIZE + INDEX_SIZE + getLevelSize(levelCount);
    }

    static int getKVDOffset(int levelCount, KTXFormat format) {
        return getDFDOffset(levelCount) + getDFDSize(format);
    }

    static int getKVDSize(KeyValueData metaData) {
        return metaData != null ? metaData.kvdByteLength : 0;
    }

    static int getSGDSize() {
        return 0;
    }

    static int alignTo4(int value) {
        return (value + 3) & 0x0fffffffc;
    }

    /**
     * Returns the offset to where the first miplevel is stored.
     * 
     * @param format
     * @param metaData
     * @param levelCount
     * @return
     */
    int getMipLevelOffset(KTXFormat format, int levelCount, KeyValueData metaData) {
        return HEADER_SIZE + INDEX_SIZE + getLevelSize(levelCount) + getDFDSize(format) + getKVDSize(metaData)
                + getSGDSize();
    }

}
