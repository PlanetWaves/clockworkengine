
package com.clockwork.terrain.heightmap;

/**
 * A heightmap that is built off an image.
 * If you want to be able to supply different Image types to 
 * ImageBaseHeightMapGrid, you need to implement this interface,
 * and have that class extend Abstract heightmap.
 * 
 * @deprecated
 */
public interface ImageHeightmap {
    
    /**
     * Set the image to use for this heightmap
     */
    //public void setImage(Image image);
    
    /**
     * The BufferedImage.TYPE_ that is supported
     * by this ImageHeightmap
     */
    //public int getSupportedImageType();
}
