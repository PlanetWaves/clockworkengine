
package com.clockwork.app;

import com.clockwork.app.state.AppState;
import com.clockwork.font.BitmapFont;
import com.clockwork.font.BitmapText;
import com.clockwork.input.FlyByCamera;
import com.clockwork.input.KeyInput;
import com.clockwork.input.controls.ActionListener;
import com.clockwork.input.controls.KeyTrigger;
import com.clockwork.renderer.RenderManager;
import com.clockwork.renderer.queue.RenderQueue.Bucket;
import com.clockwork.scene.Node;
import com.clockwork.scene.Spatial.CullHint;
import com.clockwork.system.AppSettings;
import com.clockwork.system.CWContext.Type;
import com.clockwork.system.CWSystem;

/**
 * SimpleApplication is the base class for all applications.
 * SimpleApplication will display a statistics view
 * using the com.clockwork.app.StatsAppState AppState. It will display
 * the current frames-per-second value on-screen in addition to the statistics.
 * Several keys have special functionality in SimpleApplication:
 *
 * <table>
 * <tr><td>Esc</td><td>- Close the application</td></tr>
 * <tr><td>C</td><td>- Display the camera position and rotation in the console.</td></tr>
 * <tr><td>M</td><td>- Display memory usage in the console.</td></tr>
 * </table>
 * 
 * A com.clockwork.app.FlyCamAppState is by default attached as well and can
 * be removed by calling stateManager.detach( stateManager.getState(FlyCamAppState.class) );
 */
public abstract class SimpleApplication extends Application {

    public static final String INPUT_MAPPING_EXIT = "SIMPLEAPP_Exit";
    public static final String INPUT_MAPPING_CAMERA_POS = DebugKeysAppState.INPUT_MAPPING_CAMERA_POS;
    public static final String INPUT_MAPPING_MEMORY = DebugKeysAppState.INPUT_MAPPING_MEMORY;
    public static final String INPUT_MAPPING_HIDE_STATS = "SIMPLEAPP_HideStats";
                                                                         
    protected Node rootNode = new Node("Root Node");
    protected Node guiNode = new Node("Gui Node");
    protected BitmapText fpsText;
    protected BitmapFont guiFont;
    protected FlyByCamera flyCam;
    protected boolean showSettings = true;
    private AppActionListener actionListener = new AppActionListener();
    
    private class AppActionListener implements ActionListener {

        public void onAction(String name, boolean value, float tpf) {
            if (!value) {
                return;
            }

            if (name.equals(INPUT_MAPPING_EXIT)) {
                stop();
            }else if (name.equals(INPUT_MAPPING_HIDE_STATS)){
                if (stateManager.getState(StatsAppState.class) != null) {
                    stateManager.getState(StatsAppState.class).toggleStats();
                }
            }
        }
    }

    public SimpleApplication() {
        this( new StatsAppState(), new FlyCamAppState(), new DebugKeysAppState() );
    }

    public SimpleApplication( AppState... initialStates ) {
        super();
        
        if (initialStates != null) {
            for (AppState a : initialStates) {
                if (a != null) {
                    stateManager.attach(a);
                }
            }
        }
    }

    @Override
    public void start() {
        // set some default settings in-case
        // settings dialog is not shown
        boolean loadSettings = false;
        if (settings == null) {
            setSettings(new AppSettings(true));
            loadSettings = true;
        }

        // show settings dialog
        if (showSettings) {
            if (!CWSystem.showSettingsDialog(settings, loadSettings)) {
                return;
            }
        }
        // re-setting settings they can have been merged from the registry.
        setSettings(settings);
        super.start();
    }

    /**
     * Retrieves flyCam
     * @return flyCam Camera object
     *
     */
    public FlyByCamera getFlyByCamera() {
        return flyCam;
    }

    /**
     * Retrieves guiNode
     * @return guiNode Node object
     *
     */
    public Node getGuiNode() {
        return guiNode;
    }

    /**
     * Retrieves rootNode
     * @return rootNode Node object
     *
     */
    public Node getRootNode() {
        return rootNode;
    }

    public boolean isShowSettings() {
        return showSettings;
    }

    /**
     * Toggles settings window to display at start-up
     * @param showSettings Sets true/false
     *
     */
    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
    }

    /**
     *  Creates the font that will be set to the guiFont field
     *  and subsequently set as the font for the stats text.
     */
    protected BitmapFont loadGuiFont() {
        return assetManager.loadFont("Interface/Fonts/Console.fnt");
    }

    @Override
    public void initialize() {
        super.initialize();

        // Several things rely on having this
        guiFont = loadGuiFont();

        guiNode.setQueueBucket(Bucket.Gui);
        guiNode.setCullHint(CullHint.Never);
        viewPort.attachScene(rootNode);
        guiViewPort.attachScene(guiNode);

        if (inputManager != null) {
        
            // We have to special-case the FlyCamAppState because too
            // many SimpleApplication subclasses expect it to exist in
            // simpleInit().  But at least it only gets initialized if
            // the app state is added.
            if (stateManager.getState(FlyCamAppState.class) != null) {
                flyCam = new FlyByCamera(cam);
                flyCam.setMoveSpeed(1f); // odd to set this here but it did it before
                stateManager.getState(FlyCamAppState.class).setCamera( flyCam ); 
            }

            if (context.getType() == Type.Display) {
                inputManager.addMapping(INPUT_MAPPING_EXIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
            }

            if (stateManager.getState(StatsAppState.class) != null) {
                inputManager.addMapping(INPUT_MAPPING_HIDE_STATS, new KeyTrigger(KeyInput.KEY_F5));
                inputManager.addListener(actionListener, INPUT_MAPPING_HIDE_STATS);            
            }
            
            inputManager.addListener(actionListener, INPUT_MAPPING_EXIT);            
        }

        if (stateManager.getState(StatsAppState.class) != null) {
            // Some of the tests rely on having access to fpsText
            // for quick display.  Maybe a different way would be better.
            stateManager.getState(StatsAppState.class).setFont(guiFont);
            fpsText = stateManager.getState(StatsAppState.class).getFpsText();
        }

        // call user code
        simpleInitApp();
    }

    @Override
    public void update() {
        super.update(); // makes sure to execute AppTasks
        if (speed == 0 || paused) {
            return;
        }

        float tpf = timer.getTimePerFrame() * speed;

        // update states
        stateManager.update(tpf);

        // simple update and root node
        simpleUpdate(tpf);
 
        rootNode.updateLogicalState(tpf);
        guiNode.updateLogicalState(tpf);
        
        rootNode.updateGeometricState();
        guiNode.updateGeometricState();

        // render states
        stateManager.render(renderManager);
        renderManager.render(tpf, context.isRenderable());
        simpleRender(renderManager);
        stateManager.postRender();        
    }

    public void setDisplayFps(boolean show) {
        if (stateManager.getState(StatsAppState.class) != null) {
            stateManager.getState(StatsAppState.class).setDisplayFps(show);
        }
    }

    public void setDisplayStatView(boolean show) {
        if (stateManager.getState(StatsAppState.class) != null) {
            stateManager.getState(StatsAppState.class).setDisplayStatView(show);
        }
    }

    public abstract void simpleInitApp();

    public void simpleUpdate(float tpf) {
    }

    public void simpleRender(RenderManager rm) {
    }
}
