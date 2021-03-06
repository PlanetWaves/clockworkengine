
package com.clockwork.shadow;

import com.clockwork.asset.AssetManager;
import com.clockwork.export.InputCapsule;
import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.light.DirectionalLight;
import com.clockwork.material.Material;
import com.clockwork.math.ColorRGBA;
import com.clockwork.math.Vector2f;
import com.clockwork.math.Vector3f;
import com.clockwork.renderer.Camera;
import com.clockwork.renderer.queue.GeometryList;
import com.clockwork.scene.Node;
import java.io.IOException;

/**
 * DirectionalLightShadowRenderer renderer use Parrallel Split Shadow Mapping
 * technique (pssm) It splits the view frustum in several parts and compute
 * a shadow map for each one. splits are distributed so that the closer they
 * are from the camera, the smaller they are to maximize the resolution used of
 * the shadow map. This result in a better quality shadow than standard
 * shadow mapping. for more informations on this read this <a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
 * <p/>
 */
public class DirectionalLightShadowRenderer extends AbstractShadowRenderer {

    protected float lambda = 0.65f;
    protected float zFarOverride = 0;
    protected Camera shadowCam;
    protected ColorRGBA splits;
    protected float[] splitsArray;
    protected DirectionalLight light;
    protected Vector3f[] points = new Vector3f[8];
    //Holding the info for fading shadows in the far distance 
    protected Vector2f fadeInfo;
    protected float fadeLength;
    private boolean stabilize = true;

    /**
     * Used for serialzation use
     * DirectionalLightShadowRenderer#DirectionalLightShadowRenderer(AssetManager
     * assetManager, int shadowMapSize, int nbSplits)
     */
    public DirectionalLightShadowRenderer() {
        super();
    }

    /**
     * Create a DirectionalLightShadowRenderer More info on the technique at <a
     * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     */
    public DirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbSplits) {
        super(assetManager, shadowMapSize, nbSplits);
        init(nbSplits, shadowMapSize);
    }

    private void init(int nbSplits, int shadowMapSize) {
        nbShadowMaps = Math.max(Math.min(nbSplits, 4), 1);
        if (nbShadowMaps != nbSplits) {
            throw new IllegalArgumentException("Number of splits must be between 1 and 4. Given value : " + nbSplits);
        }
        splits = new ColorRGBA();
        splitsArray = new float[nbSplits + 1];
        shadowCam = new Camera(shadowMapSize, shadowMapSize);
        shadowCam.setParallelProjection(true);
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    /**
     * return the light used to cast shadows
     *
     * @return the DirectionalLight
     */
    public DirectionalLight getLight() {
        return light;
    }

    /**
     * Sets the light to use to cast shadows
     *
     * @param light a DirectionalLight
     */
    public void setLight(DirectionalLight light) {
        this.light = light;
    }

    @Override
    protected void updateShadowCams(Camera viewCam) {

        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);

        //shadowCam.setDirection(direction);
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();

        PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);

        // in parallel projection shadow position goe from 0 to 1
        if(viewCam.isParallelProjection()){
            for (int i = 0; i < nbShadowMaps; i++) {
                splitsArray[i] = splitsArray[i]/(zFar- frustumNear);
            }
        }

        switch (splitsArray.length) {
            case 5:
                splits.a = splitsArray[4];
            case 4:
                splits.b = splitsArray[3];
            case 3:
                splits.g = splitsArray[2];
            case 2:
            case 1:
                splits.r = splitsArray[1];
                break;
        }

    }
    
    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList sceneOccluders, GeometryList sceneReceivers, GeometryList shadowMapOccluders) {

        // update frustum points based on current camera and split
        ShadowUtil.updateFrustumPoints(viewPort.getCamera(), splitsArray[shadowMapIndex], splitsArray[shadowMapIndex + 1], 1.0f, points);

        //Updating shadow cam with curent split frustra        
        ShadowUtil.updateShadowCamera(sceneOccluders, sceneReceivers, shadowCam, points, shadowMapOccluders, stabilize?shadowMapSize:0);

        return shadowMapOccluders;
    }

    @Override
    GeometryList getReceivers(GeometryList sceneReceivers, GeometryList lightReceivers) {
        return sceneReceivers;
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCam;
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, shadowMapIndex));
        ShadowUtil.updateFrustumPoints2(shadowCam, points);
        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, shadowMapIndex));
    }

    @Override
    protected void setMaterialParameters(Material material) {
        material.setColor("Splits", splits);
    }

    /**
     * returns the labda parameter see #setLambda(float lambda)
     *
     * @return lambda
     */
    public float getLambda() {
        return lambda;
    }

    /*
     * Adjust the repartition of the different shadow maps in the shadow extend
     * usualy goes from 0.0 to 1.0
     * a low value give a more linear repartition resulting in a constant quality in the shadow over the extends, but near shadows could look very jagged
     * a high value give a more logarithmic repartition resulting in a high quality for near shadows, but the quality quickly decrease over the extend.
     * the default value is set to 0.65f (theoric optimal value).
     * @param lambda the lambda value.
     */
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    /**
     * How far the shadows are rendered in the view
     *
     * see #setShadowZExtend(float zFar)
     * @return shadowZExtend
     */
    public float getShadowZExtend() {
        return zFarOverride;
    }

    /**
     * Set the distance from the eye where the shadows will be rendered default
     * value is dynamicaly computed to the shadow casters/receivers union bound
     * zFar, capped to view frustum far value.
     *
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        if (fadeInfo != null) {
            fadeInfo.set(zFar - fadeLength, 1f / fadeLength);
        }
        this.zFarOverride = zFar;

    }

    /**
     * Define the length over which the shadow will fade out when using a
     * shadowZextend This is useful to make dynamic shadows fade into baked
     * shadows in the distance.
     *
     * @param length the fade length in world units
     */
    public void setShadowZFadeLength(float length) {
        if (length == 0) {
            fadeInfo = null;
            fadeLength = 0;
            postshadowMat.clearParam("FadeInfo");
        } else {
            if (zFarOverride == 0) {
                fadeInfo = new Vector2f(0, 0);
            } else {
                fadeInfo = new Vector2f(zFarOverride - length, 1.0f / length);
            }
            fadeLength = length;
            postshadowMat.setVector2("FadeInfo", fadeInfo);
        }
    }

    /**
     * get the length over which the shadow will fade out when using a
     * shadowZextend
     *
     * @return the fade length in world units
     */
    public float getShadowZFadeLength() {
        if (fadeInfo != null) {
            return zFarOverride - fadeInfo.x;
        }
        return 0f;
    }

    /**
     * retruns true if stabilization is enabled
     * @return 
     */
    public boolean isEnabledStabilization() {
        return stabilize;
    }
    
    /**
     * Enables the stabilization of the shadows's edges. (default is true)
     * This prevents shadows' edges to flicker when the camera moves
     * However it can lead to some shadow quality loss in some particular scenes.
     * @param stabilize 
     */
    public void setEnabledStabilization(boolean stabilize) {
        this.stabilize = stabilize;
    }
    
    @Override
    public void read(CWImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = (InputCapsule) im.getCapsule(this);
        lambda = ic.readFloat("lambda", 0.65f);
        zFarOverride = ic.readInt("zFarOverride", 0);
        light = (DirectionalLight) ic.readSavable("light", null);
        fadeInfo = (Vector2f) ic.readSavable("fadeInfo", null);
        fadeLength = ic.readFloat("fadeLength", 0f);
        init(nbShadowMaps, (int) shadowMapSize);
    }

    @Override
    public void write(CWExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
        oc.write(lambda, "lambda", 0.65f);
        oc.write(zFarOverride, "zFarOverride", 0);
        oc.write(light, "light", null);
        oc.write(fadeInfo, "fadeInfo", null);
        oc.write(fadeLength, "fadeLength", 0f);
    }
}
