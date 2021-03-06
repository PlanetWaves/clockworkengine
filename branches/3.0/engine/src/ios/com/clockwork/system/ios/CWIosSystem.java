
package com.clockwork.system.ios;

import com.clockwork.asset.AssetManager;
import com.clockwork.audio.AudioRenderer;
import com.clockwork.audio.android.AndroidOpenALSoftAudioRenderer;
import com.clockwork.system.AppSettings;
import com.clockwork.system.CWContext;
import com.clockwork.system.CWSystemDelegate;
import com.clockwork.system.NullContext;
import com.clockwork.texture.Image;
import com.clockwork.texture.image.ImageRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 *
 */
public class CWIosSystem extends CWSystemDelegate {

    @Override
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AssetManager newAssetManager(URL configFile) {
        return new IosAssetManager(configFile);
    }

    @Override
    public AssetManager newAssetManager() {
        return new IosAssetManager();
    }

    @Override
    public void showErrorDialog(String message) {
        showDialog(message);
        System.err.println("CW APPLICATION ERROR:" + message);
    }
    
    private native void showDialog(String message);

    @Override
    public boolean showSettingsDialog(AppSettings sourceSettings, boolean loadFromRegistry) {
        return true;
    }

    @Override
    public CWContext newContext(AppSettings settings, CWContext.Type contextType) {
        initialize(settings);
        CWContext ctx = null;
        if (settings.getRenderer() == null
                || settings.getRenderer().equals("NULL")
                || contextType == CWContext.Type.Headless) {
            ctx = new NullContext();
            ctx.setSettings(settings);
        } else {
            ctx = new IGLESContext();
            ctx.setSettings(settings);
        }
        return ctx;
    }

    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        return new AndroidOpenALSoftAudioRenderer();
    }

    @Override
    public void initialize(AppSettings settings) {
        Logger.getLogger("").addHandler(new IosLogHandler());
//                throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageRaster createImageRaster(Image image, int slice) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}