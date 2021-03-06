
package com.clockwork.terrain.geomipmap;

import com.clockwork.export.InputCapsule;
import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.math.Vector3f;
import com.clockwork.renderer.Camera;
import com.clockwork.renderer.RenderManager;
import com.clockwork.renderer.ViewPort;
import com.clockwork.scene.Node;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.control.AbstractControl;
import com.clockwork.scene.control.Control;
import com.clockwork.terrain.Terrain;
import com.clockwork.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.clockwork.terrain.geomipmap.lodcalc.LodCalculator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tells the terrain to update its Level of Detail.
 * It needs the cameras to do this, and there could possibly
 * be several cameras in the scene, so it accepts a list
 * of cameras.
 * NOTE: right now it just uses the first camera passed in,
 * in the future it will use all of them to determine what
 * LOD to set.
 *
 * This control serialises, but it does not save the Camera reference.
 * This camera reference has to be manually added in when you load the
 * terrain to the scene!
 * 
 * 
 */
public class TerrainLodControl extends AbstractControl {

    private Terrain terrain;
    protected List<Camera> cameras;
    private List<Vector3f> cameraLocations = new ArrayList<Vector3f>();
    protected LodCalculator lodCalculator;
    private boolean hasResetLod = false; // used when enabled is set to false

    private HashMap<String,UpdatedTerrainPatch> updatedPatches;
    private final Object updatePatchesLock = new Object();
    
    protected List<Vector3f> lastCameraLocations; // used for LOD calc
    private AtomicBoolean lodCalcRunning = new AtomicBoolean(false);
    private int lodOffCount = 0;
    
    protected ExecutorService executor;
    protected Future<HashMap<String, UpdatedTerrainPatch>> indexer;
    private boolean forceUpdate = true;
    
    public TerrainLodControl() {
    }

    public TerrainLodControl(Terrain terrain, Camera camera) {
        List<Camera> cams = new ArrayList<Camera>();
        cams.add(camera);
        this.terrain = terrain;
        this.cameras = cams;
        lodCalculator = new DistanceLodCalculator(65, 2.7f); // a default calculator
    }
    
    /**
     * Only uses the first camera right now.
     * @param terrain to act upon (must be a Spatial)
     * @param cameras one or more cameras to reference for LOD calc
     */
    public TerrainLodControl(Terrain terrain, List<Camera> cameras) {
        this.terrain = terrain;
        this.cameras = cameras;
        lodCalculator = new DistanceLodCalculator(65, 2.7f); // a default calculator
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
    
    protected ExecutorService createExecutorService() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("CW Terrain Thread");
                th.setDaemon(true);
                return th;
            }
        });
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        //list of cameras for when terrain supports multiple cameras (ie split screen)

        if (lodCalculator == null)
            return;
        
        if (!enabled) {
            if (!hasResetLod) {
                // this will get run once
                hasResetLod = true;
                lodCalculator.turnOffLod();
            }
        }
        
        if (cameras != null) {
            if (cameraLocations.isEmpty() && !cameras.isEmpty()) {
                for (Camera c : cameras) // populate them
                {
                    cameraLocations.add(c.getLocation());
                }
            }
            updateLOD(cameraLocations, lodCalculator);
        }
    }

    // do all of the LOD calculations
    protected void updateLOD(List<Vector3f> locations, LodCalculator lodCalculator) {
        if(getSpatial() == null){
            return;
        }
        
        // update any existing ones that need updating
        updateQuadLODs();

        if (lodCalculator.isLodOff()) {
            // we want to calculate the base lod at least once
            if (lodOffCount == 1)
                return;
            else
                lodOffCount++;
        } else 
            lodOffCount = 0;
        
        if (lastCameraLocations != null) {
            if (!forceUpdate && lastCameraLocationsTheSame(locations) && !lodCalculator.isLodOff())
                return; // don't update if in same spot
            else
                lastCameraLocations = cloneVectorList(locations);
            forceUpdate = false;
        }
        else {
            lastCameraLocations = cloneVectorList(locations);
            return;
        }

        if (isLodCalcRunning()) {
            return;
        }
        setLodCalcRunning(true);

        if (executor == null)
            executor = createExecutorService();
        
        prepareTerrain();
        
        UpdateLOD updateLodThread = getLodThread(locations, lodCalculator);
        indexer = executor.submit(updateLodThread);
    }

    /**
     * Force the LOD to update instantly, does not wait for the camera to move.
     * It will reset once it has updated.
     */
    public void forceUpdate() {
        this.forceUpdate = true;
    }
    
    protected void prepareTerrain() {
        TerrainQuad terrain = (TerrainQuad)getSpatial();
        terrain.cacheTerrainTransforms();// cache the terrain's world transforms so they can be accessed on the separate thread safely
    }
    
    protected UpdateLOD getLodThread(List<Vector3f> locations, LodCalculator lodCalculator) {
        return new UpdateLOD(locations, lodCalculator);
    }

    /**
     * Back on the ogl thread: update the terrain patch geometries
     */
    private void updateQuadLODs() {
        if (indexer != null) {
            if (indexer.isDone()) {
                try {
                    
                    HashMap<String, UpdatedTerrainPatch> updated = indexer.get();
                    if (updated != null) {
                        // do the actual geometry update here
                        for (UpdatedTerrainPatch utp : updated.values()) {
                            utp.updateAll();
                        }
                    }
                    
                } catch (InterruptedException ex) {
                    Logger.getLogger(TerrainLodControl.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(TerrainLodControl.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    indexer = null;
                }
            }
        }
    }
    
    private boolean lastCameraLocationsTheSame(List<Vector3f> locations) {
        boolean theSame = true;
        for (Vector3f l : locations) {
            for (Vector3f v : lastCameraLocations) {
                if (!v.equals(l) ) {
                    theSame = false;
                    return false;
                }
            }
        }
        return theSame;
    }
    
    protected synchronized boolean isLodCalcRunning() {
        return lodCalcRunning.get();
    }

    protected synchronized void setLodCalcRunning(boolean running) {
        lodCalcRunning.set(running);
    }

    private List<Vector3f> cloneVectorList(List<Vector3f> locations) {
        List<Vector3f> cloned = new ArrayList<Vector3f>();
        for(Vector3f l : locations)
            cloned.add(l.clone());
        return cloned;
    }

    
    
    
    
    
    
    public Control cloneForSpatial(Spatial spatial) {
        if (spatial instanceof Terrain) {
            List<Camera> cameraClone = new ArrayList<Camera>();
            if (cameras != null) {
                for (Camera c : cameras) {
                    cameraClone.add(c);
                }
            }
            TerrainLodControl cloned = new TerrainLodControl((Terrain) spatial, cameraClone);
            cloned.setLodCalculator(lodCalculator.clone());
            return cloned;
        }
        return null;
    }

    public void setCamera(Camera camera) {
        List<Camera> cams = new ArrayList<Camera>();
        cams.add(camera);
        setCameras(cams);
    }
    
    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
        cameraLocations.clear();
        for (Camera c : cameras) {
            cameraLocations.add(c.getLocation());
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial instanceof Terrain) {
            this.terrain = (Terrain) spatial;
        }
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public LodCalculator getLodCalculator() {
        return lodCalculator;
    }

    public void setLodCalculator(LodCalculator lodCalculator) {
        this.lodCalculator = lodCalculator;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            // reset the lod levels to max detail for the terrain
            hasResetLod = false;
        } else {
            hasResetLod = true;
            lodCalculator.turnOnLod();
        }
    }
    
    
    /**
     * Calculates the LOD of all child terrain patches.
     */
    protected class UpdateLOD implements Callable<HashMap<String,UpdatedTerrainPatch>> {
        protected List<Vector3f> camLocations;
        protected LodCalculator lodCalculator;

        protected UpdateLOD(List<Vector3f> camLocations, LodCalculator lodCalculator) {
            this.camLocations = camLocations;
            this.lodCalculator = lodCalculator;
        }

        public HashMap<String, UpdatedTerrainPatch> call() throws Exception {
            //long start = System.currentTimeMillis();
            //if (isLodCalcRunning()) {
            //    return null;
            //}
            setLodCalcRunning(true);

            TerrainQuad terrainQuad = (TerrainQuad)getSpatial();
            
            // go through each patch and calculate its LOD based on camera distance
            HashMap<String,UpdatedTerrainPatch> updated = new HashMap<String,UpdatedTerrainPatch>();
            boolean lodChanged = terrainQuad.calculateLod(camLocations, updated, lodCalculator); // 'updated' gets populated here

            if (!lodChanged) {
                // not worth updating anything else since no one's LOD changed
                setLodCalcRunning(false);
                return null;
            }
            
            
            // then calculate its neighbour LOD values for seaming in the shader
            terrainQuad.findNeighboursLod(updated);

            terrainQuad.fixEdges(updated); // 'updated' can get added to here

            terrainQuad.reIndexPages(updated, lodCalculator.usesVariableLod());

            //setUpdateQuadLODs(updated); // set back to main ogl thread

            setLodCalcRunning(false);
            
            return updated;
        }
    }

    @Override
    public void write(CWExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write((Node)terrain, "terrain", null);
        oc.write(lodCalculator, "lodCalculator", null);
    }

    @Override
    public void read(CWImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        terrain = (Terrain) ic.readSavable("terrain", null);
        lodCalculator = (LodCalculator) ic.readSavable("lodCalculator", new DistanceLodCalculator());
    }

}
