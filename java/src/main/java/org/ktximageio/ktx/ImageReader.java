package org.ktximageio.ktx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface for reading of images from filesystem.
 *
 */
public interface ImageReader {

    abstract class MetaData {

        /**
         * Returns the value for the metadata key/value pair, or null if not defined.
         * 
         * @param key
         * @return
         */
        abstract String getValue(String key);

        /**
         * Returns the value for the metadata key/value pair as a Float, or null if not defined or value cannot
         * be parsed into a float.
         * 
         * @param key
         * @return
         */
        abstract Float getAsFloatValue(String key);

    }

    enum ColorSpace {
        LINEAR(),
        SRGB(),
        PQ();
    }

    enum TransferFunction {
        /**
         * Data is as is
         */
        LINEAR(),
        /**
         * Data is 'compressed' from linear to sRGB
         */
        SRGB(),
        /**
         * Perceptual Quantizer
         */
        PQ();
    }

    enum ImageFormat {
        VK_FORMAT_UNDEFINED(0, -1, -1),
        VK_FORMAT_R4G4_UNORM_PACK8(1, 2, 1),
        VK_FORMAT_R4G4B4A4_UNORM_PACK16(2, 4, 2),
        VK_FORMAT_B4G4R4A4_UNORM_PACK16(3, 4, 2),
        VK_FORMAT_R5G6B5_UNORM_PACK16(4, 3, 2),
        VK_FORMAT_B5G6R5_UNORM_PACK16(5, 3, 2),
        VK_FORMAT_R5G5B5A1_UNORM_PACK16(6, 4, 2),
        VK_FORMAT_B5G5R5A1_UNORM_PACK16(7, 4, 2),
        VK_FORMAT_A1R5G5B5_UNORM_PACK16(8, 4, 2),
        VK_FORMAT_R8_UNORM(9, 1, 1),
        VK_FORMAT_R8_SNORM(10, 1, 1),
        VK_FORMAT_R8_USCALED(11, 1, 1),
        VK_FORMAT_R8_SSCALED(12, 1, 1),
        VK_FORMAT_R8_UINT(13, 1, 1),
        VK_FORMAT_R8_SINT(14, 1, 1),
        VK_FORMAT_R8_SRGB(15, 1, 1),
        VK_FORMAT_R8G8_UNORM(16, 2, 2),
        VK_FORMAT_R8G8_SNORM(17, 2, 2),
        VK_FORMAT_R8G8_USCALED(18, 2, 2),
        VK_FORMAT_R8G8_SSCALED(19, 2, 2),
        VK_FORMAT_R8G8_UINT(20, 2, 2),
        VK_FORMAT_R8G8_SINT(21, 2, 2),
        VK_FORMAT_R8G8_SRGB(22, 2, 2),
        VK_FORMAT_R8G8B8_UNORM(23, 3, 3),
        VK_FORMAT_R8G8B8_SNORM(24, 3, 3),
        VK_FORMAT_R8G8B8_USCALED(25, 3, 3),
        VK_FORMAT_R8G8B8_SSCALED(26, 3, 3),
        VK_FORMAT_R8G8B8_UINT(27, 3, 3),
        VK_FORMAT_R8G8B8_SINT(28, 3, 3),
        VK_FORMAT_R8G8B8_SRGB(29, 3, 3),
        VK_FORMAT_B8G8R8_UNORM(30, 3, 3),
        VK_FORMAT_B8G8R8_SNORM(31, 3, 3),
        VK_FORMAT_B8G8R8_USCALED(32, 3, 3),
        VK_FORMAT_B8G8R8_SSCALED(33, 3, 3),
        VK_FORMAT_B8G8R8_UINT(34, 3, 3),
        VK_FORMAT_B8G8R8_SINT(35, 3, 3),
        VK_FORMAT_B8G8R8_SRGB(36, 3, 3),
        VK_FORMAT_R8G8B8A8_UNORM(37, 4, 4),
        VK_FORMAT_R8G8B8A8_SNORM(38, 4, 4),
        VK_FORMAT_R8G8B8A8_USCALED(39, 4, 4),
        VK_FORMAT_R8G8B8A8_SSCALED(40, 4, 4),
        VK_FORMAT_R8G8B8A8_UINT(41, 4, 4),
        VK_FORMAT_R8G8B8A8_SINT(42, 4, 4),
        VK_FORMAT_R8G8B8A8_SRGB(43, 4, 4),
        VK_FORMAT_B8G8R8A8_UNORM(44, 4, 4),
        VK_FORMAT_B8G8R8A8_SNORM(45, 4, 4),
        VK_FORMAT_B8G8R8A8_USCALED(46, 4, 4),
        VK_FORMAT_B8G8R8A8_SSCALED(47, 4, 4),
        VK_FORMAT_B8G8R8A8_UINT(48, 4, 4),
        VK_FORMAT_B8G8R8A8_SINT(49, 4, 4),
        VK_FORMAT_B8G8R8A8_SRGB(50, 4, 4),
        VK_FORMAT_A8B8G8R8_UNORM_PACK32(51, 4, 4),
        VK_FORMAT_A8B8G8R8_SNORM_PACK32(52, 4, 4),
        VK_FORMAT_A8B8G8R8_USCALED_PACK32(53, 4, 4),
        VK_FORMAT_A8B8G8R8_SSCALED_PACK32(54, 4, 4),
        VK_FORMAT_A8B8G8R8_UINT_PACK32(55, 4, 4),
        VK_FORMAT_A8B8G8R8_SINT_PACK32(56, 4, 4),
        VK_FORMAT_A8B8G8R8_SRGB_PACK32(57, 4, 4),
        VK_FORMAT_A2R10G10B10_UNORM_PACK32(58, 4, 4),
        VK_FORMAT_A2R10G10B10_SNORM_PACK32(59, 4, 4),
        VK_FORMAT_A2R10G10B10_USCALED_PACK32(60, 4, 4),
        VK_FORMAT_A2R10G10B10_SSCALED_PACK32(61, 4, 4),
        VK_FORMAT_A2R10G10B10_UINT_PACK32(62, 4, 4),
        VK_FORMAT_A2R10G10B10_SINT_PACK32(63, 4, 4),
        VK_FORMAT_A2B10G10R10_UNORM_PACK32(64, 4, 4),
        VK_FORMAT_A2B10G10R10_SNORM_PACK325(65, 4, 4),
        VK_FORMAT_A2B10G10R10_USCALED_PACK32(66, 4, 4),
        VK_FORMAT_A2B10G10R10_SSCALED_PACK32(67, 4, 4),
        VK_FORMAT_A2B10G10R10_UINT_PACK32(68, 4, 4),
        VK_FORMAT_A2B10G10R10_SINT_PACK32(69, 4, 4),
        VK_FORMAT_R16_UNORM(70, 1, 2),
        VK_FORMAT_R16_SNORM(71, 1, 2),
        VK_FORMAT_R16_USCALED(72, 1, 2),
        VK_FORMAT_R16_SSCALED(73, 1, 2),
        VK_FORMAT_R16_UINT(74, 1, 2),
        VK_FORMAT_R16_SINT(75, 1, 2),
        VK_FORMAT_R16_SFLOAT(76, 1, 2),
        VK_FORMAT_R16G16_UNORM(77, 2, 4),
        VK_FORMAT_R16G16_SNORM8(78, 2, 4),
        VK_FORMAT_R16G16_USCALED(79, 2, 4),
        VK_FORMAT_R16G16_SSCALED(80, 2, 4),
        VK_FORMAT_R16G16_UINT(81, 2, 4),
        VK_FORMAT_R16G16_SINT(82, 2, 4),
        VK_FORMAT_R16G16_SFLOAT(83, 2, 4),
        VK_FORMAT_R16G16B16_UNORM(84, 3, 6),
        VK_FORMAT_R16G16B16_SNORM(85, 3, 6),
        VK_FORMAT_R16G16B16_USCALED(86, 3, 6),
        VK_FORMAT_R16G16B16_SSCALED(87, 3, 6),
        VK_FORMAT_R16G16B16_UINT(88, 3, 6),
        VK_FORMAT_R16G16B16_SINT(89, 3, 6),
        VK_FORMAT_R16G16B16_SFLOAT(90, 3, 6),
        VK_FORMAT_R16G16B16A16_UNORM(91, 4, 8),
        VK_FORMAT_R16G16B16A16_SNORM(92, 4, 8),
        VK_FORMAT_R16G16B16A16_USCALED(93, 4, 8),
        VK_FORMAT_R16G16B16A16_SSCALED(94, 4, 8),
        VK_FORMAT_R16G16B16A16_UINT(95, 4, 8),
        VK_FORMAT_R16G16B16A16_SINT(96, 4, 8),
        VK_FORMAT_R16G16B16A16_SFLOAT(97, 4, 8),
        VK_FORMAT_R32_UINT(98, 1, 4),
        VK_FORMAT_R32_SINT(99, 1, 4),
        VK_FORMAT_R32_SFLOAT(100, 1, 4),
        VK_FORMAT_R32G32_UINT(101, 2, 8),
        VK_FORMAT_R32G32_SINT(102, 2, 8),
        VK_FORMAT_R32G32_SFLOAT(103, 2, 8),
        VK_FORMAT_R32G32B32_UINT(104, 3, 12),
        VK_FORMAT_R32G32B32_SINT(105, 3, 12),
        VK_FORMAT_R32G32B32_SFLOAT(106, 3, 12),
        VK_FORMAT_R32G32B32A32_UINT(107, 4, 16),
        VK_FORMAT_R32G32B32A32_SINT(108, 4, 16),
        VK_FORMAT_R32G32B32A32_SFLOAT(109, 4, 16),
        VK_FORMAT_R64_UINT(110, 1, 8),
        VK_FORMAT_R64_SINT(111, 1, 8),
        VK_FORMAT_R64_SFLOAT(112, 1, 8),
        VK_FORMAT_R64G64_UINT(113, 2, 16),
        VK_FORMAT_R64G64_SINT(114, 2, 16),
        VK_FORMAT_R64G64_SFLOAT(115, 2, 16),
        VK_FORMAT_R64G64B64_UINT(116, 3, 24),
        VK_FORMAT_R64G64B64_SINT(117, 3, 24),
        VK_FORMAT_R64G64B64_SFLOAT(118, 3, 24),
        VK_FORMAT_R64G64B64A64_UINT(119, 4, 32),
        VK_FORMAT_R64G64B64A64_SINT(120, 4, 32),
        VK_FORMAT_R64G64B64A64_SFLOAT(121, 4, 32),
        VK_FORMAT_B10G11R11_UFLOAT_PACK32(122, 3, 4),
        VK_FORMAT_E5B9G9R9_UFLOAT_PACK32(123, 3, 4),
        VK_FORMAT_D16_UNORM(124, 1, 2),
        VK_FORMAT_X8_D24_UNORM_PACK32(125, 2, 4),
        VK_FORMAT_D32_SFLOAT(126, 1, 4),
        VK_FORMAT_S8_UINT(127, 1, 1),
        VK_FORMAT_D16_UNORM_S8_UINT(128, 1, 2),
        VK_FORMAT_D24_UNORM_S8_UINT(129, 1, 3),
        VK_FORMAT_D32_SFLOAT_S8_UINT(130, 1, 4),
        VK_FORMAT_BC1_RGB_UNORM_BLOCK(131, -1, -1),
        VK_FORMAT_BC1_RGB_SRGB_BLOCK(132, -1, -1),
        VK_FORMAT_BC1_RGBA_UNORM_BLOCK(133, -1, -1),
        VK_FORMAT_BC1_RGBA_SRGB_BLOCK(134, -1, -1),
        VK_FORMAT_BC2_UNORM_BLOCK(135, -1, -1),
        VK_FORMAT_BC2_SRGB_BLOCK(136, -1, -1),
        VK_FORMAT_BC3_UNORM_BLOCK(137, -1, -1),
        VK_FORMAT_BC3_SRGB_BLOCK(138, -1, -1),
        VK_FORMAT_BC4_UNORM_BLOCK(139, -1, -1),
        VK_FORMAT_BC4_SNORM_BLOCK(140, -1, -1),
        VK_FORMAT_BC5_UNORM_BLOCK(141, -1, -1),
        VK_FORMAT_BC5_SNORM_BLOCK(142, -1, -1),
        VK_FORMAT_BC6H_UFLOAT_BLOCK(143, -1, -1),
        VK_FORMAT_BC6H_SFLOAT_BLOCK(144, -1, -1),
        VK_FORMAT_BC7_UNORM_BLOCK(145, -1, -1),
        VK_FORMAT_BC7_SRGB_BLOCK(146, -1, -1),
        VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK(147, -1, -1),
        VK_FORMAT_ETC2_R8G8B8_SRGB_BLOCK(148, -1, -1),
        VK_FORMAT_ETC2_R8G8B8A1_UNORM_BLOCK(149, -1, -1),
        VK_FORMAT_ETC2_R8G8B8A1_SRGB_BLOCK(150, -1, -1),
        VK_FORMAT_ETC2_R8G8B8A8_UNORM_BLOCK(151, -1, -1),
        VK_FORMAT_ETC2_R8G8B8A8_SRGB_BLOCK(152, -1, -1),
        VK_FORMAT_EAC_R11_UNORM_BLOCK(153, -1, -1),
        VK_FORMAT_EAC_R11_SNORM_BLOCK(154, -1, -1),
        VK_FORMAT_EAC_R11G11_UNORM_BLOCK(155, -1, -1),
        VK_FORMAT_EAC_R11G11_SNORM_BLOCK(156, -1, -1),
        VK_FORMAT_ASTC_4x4_UNORM_BLOCK(157, -1, -1),
        VK_FORMAT_ASTC_4x4_SRGB_BLOCK(158, -1, -1),
        VK_FORMAT_ASTC_5x4_UNORM_BLOCK(159, -1, -1),
        VK_FORMAT_ASTC_5x4_SRGB_BLOCK(160, -1, -1),
        VK_FORMAT_ASTC_5x5_UNORM_BLOCK(161, -1, -1),
        VK_FORMAT_ASTC_5x5_SRGB_BLOCK(162, -1, -1),
        VK_FORMAT_ASTC_6x5_UNORM_BLOCK(163, -1, -1),
        VK_FORMAT_ASTC_6x5_SRGB_BLOCK(164, -1, -1),
        VK_FORMAT_ASTC_6x6_UNORM_BLOCK(165, -1, -1),
        VK_FORMAT_ASTC_6x6_SRGB_BLOCK(166, -1, -1),
        VK_FORMAT_ASTC_8x5_UNORM_BLOCK(167, -1, -1),
        VK_FORMAT_ASTC_8x5_SRGB_BLOCK(168, -1, -1),
        VK_FORMAT_ASTC_8x6_UNORM_BLOCK(169, -1, -1),
        VK_FORMAT_ASTC_8x6_SRGB_BLOCK(170, -1, -1),
        VK_FORMAT_ASTC_8x8_UNORM_BLOCK(171, -1, -1),
        VK_FORMAT_ASTC_8x8_SRGB_BLOCK(172, -1, -1),
        VK_FORMAT_ASTC_10x5_UNORM_BLOCK(173, -1, -1),
        VK_FORMAT_ASTC_10x5_SRGB_BLOCK(174, -1, -1),
        VK_FORMAT_ASTC_10x6_UNORM_BLOCK(175, -1, -1),
        VK_FORMAT_ASTC_10x6_SRGB_BLOCK(176, -1, -1),
        VK_FORMAT_ASTC_10x8_UNORM_BLOCK(177, -1, -1),
        VK_FORMAT_ASTC_10x8_SRGB_BLOCK(178, -1, -1),
        VK_FORMAT_ASTC_10x10_UNORM_BLOCK(179, -1, -1),
        VK_FORMAT_ASTC_10x10_SRGB_BLOCK(180, -1, -1),
        VK_FORMAT_ASTC_12x10_UNORM_BLOCK(181, -1, -1),
        VK_FORMAT_ASTC_12x10_SRGB_BLOCK(182, -1, -1),
        VK_FORMAT_ASTC_12x12_UNORM_BLOCK(183, -1, -1),
        VK_FORMAT_ASTC_12x12_SRGB_BLOCK(184, -1, -1);

        public final int value;
        public final int typeSize;
        public final int sizeInBytes;

        ImageFormat(int val, int size, int bytes) {
            value = val;
            typeSize = size;
            sizeInBytes = bytes;
        }

        /**
         * Returns true if this format is BGR order
         * 
         * @return
         */
        public boolean isReverseOrder() {
            switch (this) {
                case VK_FORMAT_B5G6R5_UNORM_PACK16:
                case VK_FORMAT_R5G5B5A1_UNORM_PACK16:
                case VK_FORMAT_B5G5R5A1_UNORM_PACK16:
                case VK_FORMAT_B8G8R8_UNORM:
                case VK_FORMAT_B8G8R8_SNORM:
                case VK_FORMAT_B8G8R8_USCALED:
                case VK_FORMAT_B8G8R8_SSCALED:
                case VK_FORMAT_B8G8R8_UINT:
                case VK_FORMAT_B8G8R8_SINT:
                case VK_FORMAT_B8G8R8_SRGB:
                case VK_FORMAT_A8B8G8R8_UNORM_PACK32:
                case VK_FORMAT_A8B8G8R8_SNORM_PACK32:
                case VK_FORMAT_A8B8G8R8_USCALED_PACK32:
                case VK_FORMAT_A8B8G8R8_SSCALED_PACK32:
                case VK_FORMAT_A8B8G8R8_UINT_PACK32:
                case VK_FORMAT_A8B8G8R8_SINT_PACK32:
                case VK_FORMAT_A8B8G8R8_SRGB_PACK32:
                case VK_FORMAT_A2B10G10R10_UNORM_PACK32:
                case VK_FORMAT_A2B10G10R10_SNORM_PACK325:
                case VK_FORMAT_A2B10G10R10_USCALED_PACK32:
                case VK_FORMAT_A2B10G10R10_SSCALED_PACK32:
                case VK_FORMAT_A2B10G10R10_UINT_PACK32:
                case VK_FORMAT_A2B10G10R10_SINT_PACK32:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isFloatFormat() {
            switch (this) {
                case VK_FORMAT_BC6H_SFLOAT_BLOCK:
                case VK_FORMAT_BC6H_UFLOAT_BLOCK:
                case VK_FORMAT_B10G11R11_UFLOAT_PACK32:
                case VK_FORMAT_D32_SFLOAT:
                case VK_FORMAT_D32_SFLOAT_S8_UINT:
                case VK_FORMAT_E5B9G9R9_UFLOAT_PACK32:
                case VK_FORMAT_R16_SFLOAT:
                case VK_FORMAT_R16G16_SFLOAT:
                case VK_FORMAT_R16G16B16_SFLOAT:
                case VK_FORMAT_R16G16B16A16_SFLOAT:
                case VK_FORMAT_R32_SFLOAT:
                case VK_FORMAT_R32G32_SFLOAT:
                case VK_FORMAT_R32G32B32_SFLOAT:
                case VK_FORMAT_R32G32B32A32_SFLOAT:
                case VK_FORMAT_R64_SFLOAT:
                case VK_FORMAT_R64G64_SFLOAT:
                case VK_FORMAT_R64G64B64_SFLOAT:
                case VK_FORMAT_R64G64B64A64_SFLOAT:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isSRGB() {
            // TODO - add block compressed formats
            switch (this) {
                case VK_FORMAT_A8B8G8R8_SRGB_PACK32:
                case VK_FORMAT_B8G8R8_SRGB:
                case VK_FORMAT_B8G8R8A8_SRGB:
                case VK_FORMAT_R8_SRGB:
                case VK_FORMAT_R8G8_SRGB:
                case VK_FORMAT_R8G8B8_SRGB:
                case VK_FORMAT_R8G8B8A8_SRGB:
                    return true;
                default:
                    return false;
            }
        }

        public static ImageFormat get(String name) {
            for (ImageFormat sf : values()) {
                if (sf.name().contentEquals(name)) {
                    return sf;
                }
            }
            return null;
        }

        public static ImageFormat get(int value) {
            for (ImageFormat sf : values()) {
                if (sf.value == value) {
                    return sf;
                }
            }
            return null;
        }

        /**
         * Returns the srgb format - only for _UNORM values
         * 
         * @param format
         * @return
         */
        public static ImageFormat toSRGB(ImageFormat format) {
            switch (format) {
                case VK_FORMAT_A8B8G8R8_UNORM_PACK32:
                    return VK_FORMAT_A8B8G8R8_SRGB_PACK32;
                case VK_FORMAT_B8G8R8_UNORM:
                    return VK_FORMAT_B8G8R8_SRGB;
                case VK_FORMAT_B8G8R8A8_UNORM:
                    return VK_FORMAT_B8G8R8A8_SRGB;
                case VK_FORMAT_R8_UNORM:
                    return VK_FORMAT_R8_SRGB;
                case VK_FORMAT_R8G8_UNORM:
                    return VK_FORMAT_R8G8_SRGB;
                case VK_FORMAT_R8G8B8_UNORM:
                    return VK_FORMAT_R8G8B8_SRGB;
                case VK_FORMAT_R8G8B8A8_UNORM:
                    return VK_FORMAT_R8G8B8A8_SRGB;
                default:
                    throw new IllegalArgumentException("Not implemented toSRGB() for " + format);
            }
        }
    }

    enum MimeFormat {
        PNG(new String[] { "png" }),
        JPEG(new String[] { "jpg", "jpeg" }),
        HDR(new String[] { "hdr" }),
        KTX2(new String[] { "ktx2" }),
        BIN(new String[] { "bin" });

        public final String[] extensions;

        MimeFormat(String[] ext) {
            extensions = ext;
        }

        public boolean isFormat(String filename) {
            for (String extension : extensions) {
                if (filename.toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }

        public static MimeFormat get(String filename) {
            for (MimeFormat f : values()) {
                if (f.isFormat(filename)) {
                    return f;
                }
            }
            return null;
        }

        /**
         * Returns the filename with file extension
         * 
         * @param name Name, excluding file extension
         * @return Name including file extension
         */
        public String getFilename(String name) {
            return name + "." + extensions[0];
        }

        public static String toString(MimeFormat... formats) {
            String result = "";
            for (MimeFormat ff : formats) {
                result += result.length() > 0 ? "/" + ff.name() : ff.name();
            }
            return result;
        }

    }

    /**
     * Reads the header information and pixel data for the image file
     * If header information is wrong or not understood an IOException is thrown, otherwise the header is returned.
     * This can be used to query the data and to get the pixeldata.
     * 
     * Use this method when the contents to be read reside on filesystem so that a filemapping may be done.
     * 
     * @param filePath
     */
    ImageHeader read(@NonNull Path filePath) throws IOException;

    /**
     * Reads the header information and pixel data for the image file.
     * If header information is wrong or not understood an IOException is thrown, otherwise the header is returned.
     * This can be used to query the data and to get the pixeldata.
     * 
     * Use this when the image data resides in a buffer that is already loaded.
     * 
     * @param buffer Buffer containing the image data, buffer MUST be at position where image data begins and
     * limit MUST be set to end of image data.
     */
    ImageHeader read(@NonNull ByteBuffer buffer) throws IOException;

    /**
     * Returns the mime formats that this reader supports
     * 
     * @return
     */
    MimeFormat[] getMime();

    /**
     * Returns the implementation name
     * 
     * @return
     */
    String getReaderName();

    /**
     * Returns an image reader for the specified filename, using filename extension to lookup mime
     * 
     * @param filename
     * @return
     */
    static ImageReader getImageReader(@NonNull String filename) {
        MimeFormat format = MimeFormat.get(filename);
        return format != null ? getImageReader(format) : null;
    }

    static ImageReader getImageReader(@NonNull String filename, @NonNull ImageFormat imageFormat) {
        MimeFormat format = MimeFormat.get(filename);
        return format != null ? getImageReader(format, imageFormat) : null;
    }

    /**
     * Returns image reader for the specified mime
     * 
     * @param mime
     * @return
     */

    static ImageReader getImageReader(@NonNull MimeFormat mime) {
        switch (mime) {
            case HDR:
                return new RadianceHDRReader(null);
            case JPEG:
            case PNG:
                return new ImageIOReader();
            case KTX2:
                return new KTXDeserializer();
            default:
                return null;
        }
    }

    /**
     * Returns image reader for the specified mime and imageformat
     * 
     * @param mime
     * @param imageFormat format that the content will be converted to
     * @return
     */
    static ImageReader getImageReader(@NonNull MimeFormat mime, @NonNull ImageFormat imageFormat) {
        switch (mime) {
            case HDR:
                return new RadianceHDRReader(imageFormat);
            case JPEG:
            case PNG:
                return new ImageIOReader();
            case KTX2:
                return new KTXDeserializer();
            default:
                return null;
        }
    }

}
