
package clockworktest.terrain;

import com.clockwork.app.SimpleApplication;
import com.clockwork.export.Savable;
import com.clockwork.export.binary.BinaryExporter;
import com.clockwork.export.binary.BinaryImporter;
import com.clockwork.font.BitmapText;
import com.clockwork.input.KeyInput;
import com.clockwork.input.controls.ActionListener;
import com.clockwork.input.controls.KeyTrigger;
import com.clockwork.light.DirectionalLight;
import com.clockwork.material.Material;
import com.clockwork.math.ColorRGBA;
import com.clockwork.math.Vector3f;
import com.clockwork.scene.Node;
import com.clockwork.terrain.Terrain;
import com.clockwork.terrain.geomipmap.TerrainLodControl;
import com.clockwork.terrain.geomipmap.TerrainQuad;
import com.clockwork.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.clockwork.terrain.heightmap.AbstractHeightMap;
import com.clockwork.terrain.heightmap.ImageBasedHeightMap;
import com.clockwork.texture.Texture;
import com.clockwork.texture.Texture.WrapMode;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves and loads terrain.
 *
 */
public class TerrainTestReadWrite extends SimpleApplication {

    private Terrain terrain;
    protected BitmapText hintText;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private Material matTerrain;
    private Material matWire;

    public static void main(String[] args) {
        TerrainTestReadWrite app = new TerrainTestReadWrite();
        app.start();
        //testHeightmapBuilding();
    }

    @Override
    public void initialize() {
        super.initialize();

        loadHintText();
    }

    @Override
    public void simpleInitApp() {


        createControls();
        createMap();
    }

    private void createMap() {
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setBoolean("WardIso", true);

        // ALPHA map (for splat textures)
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // HEIGHTMAP image (for the terrain heightmap)
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", grass);
        matTerrain.setFloat("DiffuseMap_0_scale", grassScale);


        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", dirt);
        matTerrain.setFloat("DiffuseMap_1_scale", dirtScale);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_2", rock);
        matTerrain.setFloat("DiffuseMap_2_scale", rockScale);


        Texture normalMap0 = assetManager.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
        normalMap0.setWrap(WrapMode.Repeat);
        Texture normalMap1 = assetManager.loadTexture("Textures/Terrain/splat/dirt_normal.png");
        normalMap1.setWrap(WrapMode.Repeat);
        Texture normalMap2 = assetManager.loadTexture("Textures/Terrain/splat/road_normal.png");
        normalMap2.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("NormalMap", normalMap0);
        matTerrain.setTexture("NormalMap_1", normalMap2);
        matTerrain.setTexture("NormalMap_2", normalMap2);

        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);


        // CREATE HEIGHTMAP
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 1f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (new File("terrainsave.CW").exists()) {
            loadTerrain();
        } else {
            // create the terrain as normal, and give it a control for LOD management
            TerrainQuad terrainQuad = new TerrainQuad("terrain", 65, 129, heightmap.getHeightMap());//, new LodPerspectiveCalculatorFactory(getCamera(), 4)); // add this in to see it use entropy for LOD calculations
            TerrainLodControl control = new TerrainLodControl(terrainQuad, getCamera());
            control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            terrainQuad.addControl(control);
            terrainQuad.setMaterial(matTerrain);
            terrainQuad.setLocalTranslation(0, -100, 0);
            terrainQuad.setLocalScale(4f, 0.25f, 4f);
            rootNode.attachChild(terrainQuad);
            
            this.terrain = terrainQuad;
        }

        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);
    }

    /**
     * Create the save and load actions and add them to the input listener
     */
    private void createControls() {
        flyCam.setMoveSpeed(50);
        cam.setLocation(new Vector3f(0, 100, 0));

        inputManager.addMapping("save", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(saveActionListener, "save");

        inputManager.addMapping("load", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addListener(loadActionListener, "load");

        inputManager.addMapping("clone", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addListener(cloneActionListener, "clone");
    }

    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit T to save, and Y to load");
        guiNode.attachChild(hintText);
    }
    private ActionListener saveActionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("save") && !pressed) {

                FileOutputStream fos = null;
                try {
                    long start = System.currentTimeMillis();
                    fos = new FileOutputStream(new File("terrainsave.CW"));

                    // we just use the exporter and pass in the terrain
                    BinaryExporter.getInstance().save((Savable)terrain, new BufferedOutputStream(fos));

                    fos.flush();
                    float duration = (System.currentTimeMillis() - start) / 1000.0f;
                    System.out.println("Save took " + duration + " seconds");
                } catch (IOException ex) {
                    Logger.getLogger(TerrainTestReadWrite.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Logger.getLogger(TerrainTestReadWrite.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
        }
    };

    private void loadTerrain() {
        FileInputStream fis = null;
        try {
            long start = System.currentTimeMillis();
            // remove the existing terrain and detach it from the root node.
            if (terrain != null) {
                Node existingTerrain = (Node)terrain;
                existingTerrain.removeFromParent();
                existingTerrain.removeControl(TerrainLodControl.class);
                existingTerrain.detachAllChildren();
                terrain = null;
            }

            // import the saved terrain, and attach it back to the root node
            File f = new File("terrainsave.CW");
            fis = new FileInputStream(f);
            BinaryImporter imp = BinaryImporter.getInstance();
            imp.setAssetManager(assetManager);
            terrain = (TerrainQuad) imp.load(new BufferedInputStream(fis));
            rootNode.attachChild((Node)terrain);

            float duration = (System.currentTimeMillis() - start) / 1000.0f;
            System.out.println("Load took " + duration + " seconds");

            // now we have to add back the camera to the LOD control
            TerrainLodControl lodControl = ((Node)terrain).getControl(TerrainLodControl.class);
            if (lodControl != null)
                lodControl.setCamera(getCamera());

        } catch (IOException ex) {
            Logger.getLogger(TerrainTestReadWrite.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TerrainTestReadWrite.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private ActionListener loadActionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("load") && !pressed) {
                loadTerrain();
            }
        }
    };
    private ActionListener cloneActionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("clone") && !pressed) {

                Terrain clone = (Terrain) ((Node)terrain).clone();
                ((Node)terrain).removeFromParent();
                terrain = clone;
                getRootNode().attachChild((Node)terrain);
            }
        }
    };

    // no junit tests, so this has to be hand-tested:
    private static void testHeightmapBuilding() {
        int s = 9;
        int b = 3;
        float[] hm = new float[s * s];
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                hm[(i * s) + j] = i * j;
            }
        }

        for (int i = 0; i < s; i++) {
            for (int j = 0; j < s; j++) {
                System.out.print(hm[i * s + j] + " ");
            }
            System.out.println("");
        }

        TerrainQuad terrain = new TerrainQuad("terrain", b, s, hm);
        float[] hm2 = terrain.getHeightMap();
        boolean failed = false;
        for (int i = 0; i < s * s; i++) {
            if (hm[i] != hm2[i]) {
                failed = true;
            }
        }

        System.out.println("");
        if (failed) {
            System.out.println("Terrain heightmap building FAILED!!!");
            for (int i = 0; i < s; i++) {
                for (int j = 0; j < s; j++) {
                    System.out.print(hm2[i * s + j] + " ");
                }
                System.out.println("");
            }
        } else {
            System.out.println("Terrain heightmap building PASSED");
        }
    }
}
