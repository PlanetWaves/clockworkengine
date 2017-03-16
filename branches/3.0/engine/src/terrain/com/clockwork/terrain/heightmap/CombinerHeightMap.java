
package com.clockwork.terrain.heightmap;

import java.util.logging.Logger;

/**
 * CombinerHeightMap generates a new height map based on
 * two provided height maps. These had maps can either be added together
 * or substracted from each other. Each heightmap has a weight to
 * determine how much one will affect the other. By default it is set to
 * 0.5, 0.5 and meaning the two heightmaps are averaged evenly. This
 * value can be adjusted at will, as long as the two factors are equal
 * to 1.0.
 *
 * 
 * @version $Id$
 */
public class CombinerHeightMap extends AbstractHeightMap {

    private static final Logger logger = Logger.getLogger(CombinerHeightMap.class.getName());
    /**
     * Constant mode to denote adding the two heightmaps.
     */
    public static final int ADDITION = 0;
    /**
     * Constant mode to denote subtracting the two heightmaps.
     */
    public static final int SUBTRACTION = 1;
    //the two maps.
    private AbstractHeightMap map1;
    private AbstractHeightMap map2;
    //the two factors
    private float factor1 = 0.5f;
    private float factor2 = 0.5f;
    //the combine mode.
    private int mode;

    /**
     * Constructor combines two given heightmaps by the specified mode.
     * The heightmaps will be evenly distributed. The heightmaps
     * must be of the same size.
     *
     * @param map1 the first heightmap to combine.
     * @param map2 the second heightmap to combine.
     * @param mode denotes whether to add or subtract the heightmaps, may
     *              be either ADDITION or SUBTRACTION.
     * @throws CWException if either map is null, their size
     *              do not match or the mode is invalid.
     */
    public CombinerHeightMap(
            AbstractHeightMap map1,
            AbstractHeightMap map2,
            int mode) throws Exception {


        //insure all parameters are valid.
        if (null == map1 || null == map2) {
            throw new Exception("Height map may not be null");
        }


        if (map1.getSize() != map2.getSize()) {
            throw new Exception("The two maps must be of the same size");
        }


        if ((factor1 + factor2) != 1.0f) {
            throw new Exception("factor1 and factor2 must add to 1.0");
        }


        this.size = map1.getSize();
        this.map1 = map1;
        this.map2 = map2;


        setMode(mode);

        load();
    }

    /**
     * Constructor combines two given heightmaps by the specified mode.
     * The heightmaps will be distributed based on the given factors.
     * For example, if factor1 is 0.6 and factor2 is 0.4, then 60% of
     * map1 will be used with 40% of map2. The two factors must add up
     * to 1.0. The heightmaps must also be of the same size.
     *
     * @param map1 the first heightmap to combine.
     * @param factor1 the factor for map1.
     * @param map2 the second heightmap to combine.
     * @param factor2 the factor for map2.
     * @param mode denotes whether to add or subtract the heightmaps, may
     *              be either ADDITION or SUBTRACTION.
     * @throws CWException if either map is null, their size
     *              do not match, the mode is invalid, or the factors do not add
     *              to 1.0.
     */
    public CombinerHeightMap(
            AbstractHeightMap map1,
            float factor1,
            AbstractHeightMap map2,
            float factor2,
            int mode) throws Exception {


        //insure all parameters are valid.
        if (null == map1 || null == map2) {
            throw new Exception("Height map may not be null");
        }


        if (map1.getSize() != map2.getSize()) {
            throw new Exception("The two maps must be of the same size");
        }


        if ((factor1 + factor2) != 1.0f) {
            throw new Exception("factor1 and factor2 must add to 1.0");
        }


        setMode(mode);


        this.size = map1.getSize();
        this.map1 = map1;
        this.map2 = map2;
        this.factor1 = factor1;
        this.factor2 = factor2;


        this.mode = mode;


        load();
    }

    /**
     * setFactors sets the distribution of heightmaps.
     * For example, if factor1 is 0.6 and factor2 is 0.4, then 60% of
     * map1 will be used with 40% of map2. The two factors must add up
     * to 1.0.
     * @param factor1 the factor for map1.
     * @param factor2 the factor for map2.
     * @throws CWException if the factors do not add to 1.0.
     */
    public void setFactors(float factor1, float factor2) throws Exception {
        if ((factor1 + factor2) != 1.0f) {
            throw new Exception("factor1 and factor2 must add to 1.0");
        }


        this.factor1 = factor1;
        this.factor2 = factor2;
    }

    /**
     * setHeightMaps sets the height maps to combine.
     * The size of the height maps must be the same.
     * @param map1 the first height map.
     * @param map2 the second height map.
     * @throws CWException if the either heightmap is null, or their
     *              sizes do not match.
     */
    public void setHeightMaps(AbstractHeightMap map1, AbstractHeightMap map2) throws Exception {
        if (null == map1 || null == map2) {
            throw new Exception("Height map may not be null");
        }


        if (map1.getSize() != map2.getSize()) {
            throw new Exception("The two maps must be of the same size");
        }


        this.size = map1.getSize();
        this.map1 = map1;
        this.map2 = map2;
    }

    /**
     * setMode sets the mode of the combiner. This may either
     * be ADDITION or SUBTRACTION.
     * @param mode the mode of the combiner.
     * @throws CWException if mode is not ADDITION or SUBTRACTION.
     */
    public void setMode(int mode) throws Exception {
        if (mode != ADDITION && mode != SUBTRACTION) {
            throw new Exception("Invalid mode");
        }
        this.mode = mode;
    }

    /**
     * load builds a new heightmap based on the combination of
     * two other heightmaps. The conditions of the combiner determine the
     * final outcome of the heightmap.
     *
     * @return boolean if the heightmap was successfully created.
     */
    public boolean load() {
        if (null != heightData) {
            unloadHeightMap();
        }


        heightData = new float[size * size];


        float[] temp1 = map1.getHeightMap();
        float[] temp2 = map2.getHeightMap();


        if (mode == ADDITION) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    heightData[i + (j * size)] =
                            (int) (temp1[i + (j * size)] * factor1
                            + temp2[i + (j * size)] * factor2);
                }
            }
        } else if (mode == SUBTRACTION) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    heightData[i + (j * size)] =
                            (int) (temp1[i + (j * size)] * factor1
                            - temp2[i + (j * size)] * factor2);
                }
            }
        }


        logger.fine("Created heightmap using Combiner");


        return true;
    }
}
