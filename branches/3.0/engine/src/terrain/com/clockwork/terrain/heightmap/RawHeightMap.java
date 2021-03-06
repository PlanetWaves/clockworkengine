
package com.clockwork.terrain.heightmap;

import com.clockwork.math.FastMath;
import com.clockwork.util.LittleEndien;
import java.io.*;
import java.net.URL;
import java.util.logging.Logger;

/**
 * RawHeightMap creates a height map from a RAW image file. The
 * greyscale image denotes height based on the value of the pixel for each
 * point. Where pure black the lowest point and pure white denotes the highest.
 *
 * 
 * @version $Id$
 */
public class RawHeightMap extends AbstractHeightMap {

    private static final Logger logger = Logger.getLogger(RawHeightMap.class.getName());
    /**
     * Format specification for 8 bit precision heightmaps
     */
    public static final int FORMAT_8BIT = 0;
    /**
     * Format specification for 16 bit little endian heightmaps
     */
    public static final int FORMAT_16BITLE = 1;
    /**
     * Format specification for 16 bit big endian heightmaps
     */
    public static final int FORMAT_16BITBE = 2;
    private int format;
    private boolean swapxy;
    private InputStream stream;

    /**
     * Constructor creates a new RawHeightMap object and loads a
     * RAW image file to use as a height field. The greyscale image denotes the
     * height of the terrain, where dark is low point and bright is high point.
     * The values of the RAW correspond directly with the RAW values or 0 - 255.
     *
     * @param filename
     *            the RAW file to use as the heightmap.
     * @param size
     *            the size of the RAW (must be square).
     * @throws CWException
     *             if the filename is null or not RAW, and if the size is 0 or
     *             less.
     */
    public RawHeightMap(String filename, int size) throws Exception {
        this(filename, size, FORMAT_8BIT, false);
    }

    public RawHeightMap(float heightData[]) {
        this.heightData = heightData;
        this.size = (int) FastMath.sqrt(heightData.length);
        this.format = FORMAT_8BIT;
    }

    public RawHeightMap(String filename, int size, int format, boolean swapxy) throws Exception {
        // varify that filename and size are valid.
        if (null == filename || size <= 0) {
            throw new Exception("Must supply valid filename and "
                    + "size (> 0)");
        }
        try {
            setup(new FileInputStream(filename), size, format, swapxy);
        } catch (FileNotFoundException e) {
            throw new Exception("height file not found: " + filename);
        }
    }

    public RawHeightMap(InputStream stream, int size, int format, boolean swapxy) throws Exception {
        setup(stream, size, format, swapxy);
    }

    public RawHeightMap(URL resource, int size, int format, boolean swapxy) throws Exception {
        // varify that resource and size are valid.
        if (null == resource || size <= 0) {
            throw new Exception("Must supply valid resource and "
                    + "size (> 0)");
        }


        try {
            setup(resource.openStream(), size, format, swapxy);
        } catch (IOException e) {
            throw new Exception("Unable to open height url: " + resource);
        }
    }

    private void setup(InputStream stream, int size, int format, boolean swapxy) throws Exception {
        // varify that filename and size are valid.
        if (null == stream || size <= 0) {
            throw new Exception("Must supply valid stream and "
                    + "size (> 0)");
        }


        this.stream = stream;
        this.size = size;
        this.format = format;
        this.swapxy = swapxy;
        load();
    }

    /**
     * load fills the height data array with the appropriate data
     * from the set RAW image. If the RAW image has not been set a CWException
     * will be thrown.
     *
     * @return true if the load is successfull, false otherwise.
     */
    @Override
    public boolean load() {
        // confirm data has been set. Redundant check...
        if (null == stream || size <= 0) {
            throw new RuntimeException("Must supply valid stream and "
                    + "size (> 0)");
        }


        // clean up
        if (null != heightData) {
            unloadHeightMap();
        }


        // initialize the height data attributes
        heightData = new float[size * size];


        // attempt to connect to the supplied file.
        BufferedInputStream bis = null;


        try {
            bis = new BufferedInputStream(stream);
            if (format == RawHeightMap.FORMAT_16BITLE) {
                LittleEndien dis = new LittleEndien(bis);
                int index;
                // read the raw file
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (swapxy) {
                            index = i + j * size;
                        } else {
                            index = (i * size) + j;
                        }
                        heightData[index] = dis.readUnsignedShort();
                    }
                }
                dis.close();
            } else {
                DataInputStream dis = new DataInputStream(bis);
                // read the raw file
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        int index;
                        if (swapxy) {
                            index = i + j * size;
                        } else {
                            index = (i * size) + j;
                        }
                        if (format == RawHeightMap.FORMAT_16BITBE) {
                            heightData[index] = dis.readUnsignedShort();
                        } else {
                            heightData[index] = dis.readUnsignedByte();
                        }
                    }
                }
                dis.close();
            }
            bis.close();
        } catch (IOException e1) {
            logger.warning("Error reading height data from stream.");
            return false;
        }
        return true;
    }

    /**
     * setFilename sets the file to use for the RAW data. A call
     * to load is required to put the changes into effect.
     *
     * @param filename
     *            the new file to use for the height data.
     * @throws CWException
     *             if the file is null or not RAW.
     */
    public void setFilename(String filename) throws Exception {
        if (null == filename) {
            throw new Exception("Must supply valid filename.");
        }
        try {
            this.stream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new Exception("height file not found: " + filename);
        }
    }

    /**
     * setHeightStream sets the stream to use for the RAW data. A call
     * to load is required to put the changes into effect.
     *
     * @param stream
     *            the new stream to use for the height data.
     * @throws CWException
     *             if the stream is null or not RAW.
     */
    public void setHeightStream(InputStream stream) throws Exception {
        if (null == stream) {
            throw new Exception("Must supply valid stream.");
        }
        this.stream = stream;
    }
}
