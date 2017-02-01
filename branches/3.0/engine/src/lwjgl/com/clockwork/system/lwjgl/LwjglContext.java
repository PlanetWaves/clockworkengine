

package com.clockwork.system.lwjgl;

import com.clockwork.input.lwjgl.JInputJoyInput;
import com.clockwork.input.lwjgl.LwjglKeyInput;
import com.clockwork.input.lwjgl.LwjglMouseInput;
import com.clockwork.math.FastMath;
import com.clockwork.renderer.Renderer;
import com.clockwork.renderer.lwjgl.LwjglGL1Renderer;
import com.clockwork.renderer.lwjgl.LwjglRenderer;
import com.clockwork.system.AppSettings;
import com.clockwork.system.JmeContext;
import com.clockwork.system.JmeSystem;
import com.clockwork.system.SystemListener;
import com.clockwork.system.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.*;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(LwjglContext.class.getName());

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected LwjglKeyInput keyInput;
    protected LwjglMouseInput mouseInput;
    protected JInputJoyInput joyInput;
    protected Timer timer;
    protected SystemListener listener;

    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    protected void printContextInitInfo() {
        logger.log(Level.INFO, "Lwjgl {0} context running on thread {1}",
                new Object[]{Sys.getVersion(), Thread.currentThread().getName()});

        logger.log(Level.INFO, "Adapter: {0}", Display.getAdapter());
        logger.log(Level.INFO, "Driver Version: {0}", Display.getVersion());

        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        logger.log(Level.INFO, "Vendor: {0}", vendor);

        String version = GL11.glGetString(GL11.GL_VERSION);
        logger.log(Level.INFO, "OpenGL Version: {0}", version);

        String renderGl = GL11.glGetString(GL11.GL_RENDERER);
        logger.log(Level.INFO, "Renderer: {0}", renderGl);

        if (GLContext.getCapabilities().OpenGL20){
            String shadingLang = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
            logger.log(Level.INFO, "GLSL Ver: {0}", shadingLang);
        }
    }

    protected ContextAttribs createContextAttribs() {
        if (settings.getBoolean("GraphicsDebug") || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
            ContextAttribs attr;
            if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
                attr = new ContextAttribs(3, 2);
                attr = attr.withProfileCore(true).withForwardCompatible(true).withProfileCompatibility(false);
            } else {
                attr = new ContextAttribs();
            }
            if (settings.getBoolean("GraphicsDebug")) {
                attr = attr.withDebug(true);
            }
            return attr;
        } else {
            return null;
        }
    }
    
    protected int determineMaxSamples(int requestedSamples) {
        boolean displayWasCurrent = false;
        try {
            // If we already have a valid context, determine samples using current
            // context.
            if (Display.isCreated() && Display.isCurrent()) {
                if (GLContext.getCapabilities().GL_ARB_framebuffer_object) {
                    return GL11.glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
                } else if (GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
                    return GL11.glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
                }
                // Doesn't support any of the needed extensions .. continue down.
                displayWasCurrent = true;
            }
        } catch (LWJGLException ex) {
            listener.handleError("Failed to check if display is current", ex);
        }
        
        if ((Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0) {
            // No pbuffer, assume everything is supported.
            return Integer.MAX_VALUE;
        } else {
            Pbuffer pb = null;
            
            if (!displayWasCurrent) {
                // OpenGL2 method: Create pbuffer and query samples
                // from GL_ARB_framebuffer_object or GL_EXT_framebuffer_multisample.
                try {
                    pb = new Pbuffer(1, 1, new PixelFormat(0, 0, 0), null);
                    pb.makeCurrent();

                    if (GLContext.getCapabilities().GL_ARB_framebuffer_object) {
                        return GL11.glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
                    } else if (GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
                        return GL11.glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
                    }

                    // OpenGL2 method failed.
                } catch (LWJGLException ex) {
                    // Something else failed.
                    return Integer.MAX_VALUE;
                } finally { 
                    if (pb != null) {
                        pb.destroy();
                        pb = null;
                    }
                }
            }
            
            // OpenGL1 method (DOESNT WORK RIGHT NOW ..)
            requestedSamples = FastMath.nearestPowerOfTwo(requestedSamples);
            try {
                requestedSamples = Integer.MAX_VALUE;
                /*
                while (requestedSamples > 1) {
                    try {
                        pb = new Pbuffer(1, 1, new PixelFormat(16, 0, 8, 0, requestedSamples), null);
                    } catch (LWJGLException ex) {
                        if (ex.getMessage().startsWith("Failed to find ARB pixel format")) {
                            // Unsupported format, so continue.
                            requestedSamples = FastMath.nearestPowerOfTwo(requestedSamples / 2);
                        } else {
                            // Something else went wrong ..
                            return Integer.MAX_VALUE;
                        }
                    } finally {
                        if (pb != null){
                            pb.destroy();
                            pb = null;
                        }
                    }
                }*/
            } finally {
                if (displayWasCurrent) {
                    try {
                        Display.makeCurrent();
                    } catch (LWJGLException ex) {
                        listener.handleError("Failed to make display current after checking samples", ex);
                    }
                }
            }
            
            return requestedSamples;
        }
    }
    
    protected int getNumSamplesToUse() {
        int samples = 0;
        if (settings.getSamples() > 1){
            samples = settings.getSamples();
            int supportedSamples = determineMaxSamples(samples);
            if (supportedSamples < samples) {
                samples = supportedSamples;
            }
        }
        return samples;
    }

    protected void initContextFirstTime(){
        if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL2)
         || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)){
            renderer = new LwjglRenderer();
        }else if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL1)){
            renderer = new LwjglGL1Renderer();
        }else if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL_ANY)){
            // Choose an appropriate renderer based on capabilities
            if (GLContext.getCapabilities().OpenGL20){
                renderer = new LwjglRenderer();
            }else{
                renderer = new LwjglGL1Renderer();
            }
        }else{
            throw new UnsupportedOperationException("Unsupported renderer: " + settings.getRenderer());
        }
        
        // Init renderer
        if (renderer instanceof LwjglRenderer){
            ((LwjglRenderer)renderer).initialize();
        }else if (renderer instanceof LwjglGL1Renderer){
            ((LwjglGL1Renderer)renderer).initialize();
        }else{
            assert false;
        }

        // Init input
        if (keyInput != null) {
            keyInput.initialize();
        }

        if (mouseInput != null) {
            mouseInput.initialize();
        }

        if (joyInput != null) {
            joyInput.initialize();
        }
    }

    public void internalDestroy(){
        renderer = null;
        timer = null;
        renderable.set(false);
        synchronized (createdLock){
            created.set(false);
            createdLock.notifyAll();
        }
    }
    
    public void internalCreate(){
        timer = new LwjglTimer();
        
        synchronized (createdLock){
            created.set(true);
            createdLock.notifyAll();
        }
        
        if (renderable.get()){
            initContextFirstTime();
        }else{
            assert getType() == Type.Canvas;
        }
    }

    public void create(){
        create(false);
    }

    public void destroy(){
        destroy(false);
    }

    protected void waitFor(boolean createdVal){
        synchronized (createdLock){
            while (created.get() != createdVal){
                try {
                    createdLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public boolean isCreated(){
        return created.get();
    }
    
    public boolean isRenderable(){
        return renderable.get();
    }

    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    public AppSettings getSettings(){
        return settings;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Timer getTimer() {
        return timer;
    }

}