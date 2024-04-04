package org.ktximageio.ktx;

import org.ktximageio.ktx.ImageReader.ImageFormat;
import org.ktximageio.ktx.ImageReader.MetaData;

public interface ImageHeader {

    /**
     * Specifies the image format using Vulkan VkFormat enum values - note that if the image source is NOT
     * ktx then the imageformat SHALL be _UNORM for all png/jpeg or similar - or _SFLOAT for HDR type of images.
     * It is up to the caller to know the usage of referenced resources, this is the case for glTF where the target
     * source mandates if an image shall be in linear or SRGB colorspace.
     * 
     * @return
     */
    ImageFormat getFormat();

    /**
     * Returns the width of the image in pixels
     * 
     * @return
     */
    int getWidth();

    /**
     * Returns the height of the image in pixels.
     * 
     * @return
     */
    int getHeight();

    /**
     * Returns the depth, will be zero for all non 3D textures
     * 
     * @return
     */
    int getDepth();

    /**
     * Returns the number of mip-levels
     * levelCount=1 means that a file contains only the base level and the texture isn’t meant to have other levels.
     * E.g., this could be a LUT rather than a natural image.
     * levelCount=0 is allowed, except for block-compressed formats, and means that a file contains only the base level
     * and consumers, particularly loaders, should generate other levels if needed.
     * 
     * @return
     */
    int getLevelCount();

    /**
     * Returns the number array elements
     * If the texture is not an array texture, layerCount must equal 0.
     * 
     * @return
     */
    int getLayerCount();

    /**
     * Specifies the number of cubemap faces.
     * For cubemaps and cubemap arrays this must be 6.
     * For non cubemaps this must be 1.
     * Cubemap faces are stored in the order: +X, -X, +Y, -Y, +Z, -Z.
     * 
     * @return
     */
    int getFaceCount();

    /**
     * Returns the image contents of the file in the format, as reported by {@link #getFormat()}
     * 
     * @return Buffer containing the pixels
     */
    ImageBuffer getData();

    /**
     * Returns the metadata for the file
     * 
     * @return
     */
    MetaData getMetaData();

    /**
     * Releases all resources, the object may not be accessed after calling this method.
     */
    void destroy();

}
