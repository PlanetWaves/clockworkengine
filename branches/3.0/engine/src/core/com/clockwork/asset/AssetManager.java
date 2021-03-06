
package com.clockwork.asset;

import com.clockwork.asset.plugins.ClasspathLocator;
import com.clockwork.asset.plugins.FileLocator;
import com.clockwork.audio.AudioData;
import com.clockwork.audio.AudioKey;
import com.clockwork.font.BitmapFont;
import com.clockwork.material.Material;
import com.clockwork.post.FilterPostProcessor;
import com.clockwork.renderer.Caps;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.plugins.OBJLoader;
import com.clockwork.shader.Shader;
import com.clockwork.shader.ShaderGenerator;
import com.clockwork.shader.ShaderKey;
import com.clockwork.texture.Texture;
import com.clockwork.texture.plugins.TGALoader;
import java.util.EnumSet;
import java.util.List;

/**
 * AssetManager provides an interface for managing the data assets
 * of a CW application.
 * 
 * The asset manager provides a means to register AssetLocator}s,
 * which are used to find asset data on disk, network, or other file system.
 * The asset locators are invoked in order of addition to find the asset data.
 * Use the #registerLocator(java.lang.String, java.lang.Class) } method
 * to add new AssetLocator}s. 
 * Some examples of locators:
 * 
 * FileLocator} - Used to find assets on the local file system.
 * ClasspathLocator} - Used to find assets in the Java classpath
 * 
 * 
 * The asset data is represented by the AssetInfo} class, this
 * data is passed into the registered AssetLoader}s in order to 
 * convert the data into a usable object. Use the
 * #registerLoader(java.lang.Class, java.lang.String[]) } method
 * to add loaders.
 * Some examples of loaders:
 * 
 * OBJLoader} - Used to load Wavefront .OBJ model files
 * TGALoader} - Used to load Targa image files
 * 
 * 
 * Once the asset has been loaded, 
 */
public interface AssetManager {

    /**
     * Adds a ClassLoader} that is used to load Class classes}
     * that are needed for finding and loading Assets. 
     * This does <strong>not</strong> allow loading assets from that classpath, 
     * use registerLocator for that.
     * 
     * @param loader A ClassLoader that Classes in asset files can be loaded from.
     */
    public void addClassLoader(ClassLoader loader);

    /**
     * Remove a ClassLoader} from the list of registered ClassLoaders
     */
    public void removeClassLoader(ClassLoader loader);

    /**
     * Retrieve the list of registered ClassLoaders that are used for loading 
     * Class classes} from asset files.
     */
    public List<ClassLoader> getClassLoaders();
    
    /**
     * Registers a loader for the given extensions.
     * 
     * @param loaderClassName
     * @param extensions
     * 
     * @deprecated Please use #registerLoader(java.lang.Class, java.lang.String[]) }
     * together with Class#forName(java.lang.String) } to find a class
     * and then register it.
     * 
     * @deprecated Please use #registerLoader(java.lang.Class, java.lang.String[]) }
     * with Class#forName(java.lang.String) } instead.
     */
    @Deprecated
    public void registerLoader(String loaderClassName, String ... extensions);

    /**
     * Registers an AssetLocator} by using a class name. 
     * See the AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * method for more information.
     *
     * @param rootPath The root path from which to locate assets, this 
     * depends on the implementation of the asset locator. 
     * A URL based locator will expect a url folder such as "http://www.example.com/"
     * while a File based locator will expect a file path (OS dependent).
     * @param locatorClassName The full class name of the AssetLocator}
     * implementation.
     * 
     * @deprecated Please use #registerLocator(java.lang.String, java.lang.Class)  }
     * together with Class#forName(java.lang.String) } to find a class
     * and then register it.
     */
    @Deprecated
    public void registerLocator(String rootPath, String locatorClassName);

    /**
     * Register an AssetLoader} by using a class object.
     * 
     * @param loaderClass
     * @param extensions
     */
    public void registerLoader(Class<? extends AssetLoader> loaderClass, String ... extensions);
    
    /**
     * Unregister a AssetLoader} from loading its assigned extensions.
     * This undoes the effect of calling 
     * #registerLoader(java.lang.Class, java.lang.String[]) }.
     * 
     * @param loaderClass The loader class to unregister.
     * see #registerLoader(java.lang.Class, java.lang.String[]) 
     */
    public void unregisterLoader(Class<? extends AssetLoader> loaderClass);

    /**
     * Registers the given locator class for locating assets with this
     * AssetManager. AssetLocator}s are invoked in the order
     * they were registered, to locate the asset by the AssetKey}.
     * Once an AssetLocator} returns a non-null AssetInfo, it is sent
     * to the AssetLoader} to load the asset.
     * Once a locator is registered, it can be removed via
     * #unregisterLocator(java.lang.String, java.lang.Class) }.
     *
     * @param rootPath Specifies the root path from which to locate assets
     * for the given AssetLocator}. The purpose of this parameter
     * depends on the type of the AssetLocator}.
     * @param locatorClass The class type of the AssetLocator} to register.
     *
     * see AssetLocator#setRootPath(java.lang.String)
     * see AssetLocator#locate(com.clockwork.asset.AssetManager, com.clockwork.asset.AssetKey) 
     * see #unregisterLocator(java.lang.String, java.lang.Class) 
     */
    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass);

    /**
     * Unregisters the given locator class. This essentially undoes the operation
     * done by #registerLocator(java.lang.String, java.lang.Class) }.
     * 
     * @param rootPath Should be the same as the root path specified in {@link
     * #registerLocator(java.lang.String, java.lang.Class) }.
     * @param locatorClass The locator class to unregister
     * 
     * see #registerLocator(java.lang.String, java.lang.Class) 
     */
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass);
    
    /**
     * Add an AssetEventListener} to receive events from this
     * AssetManager. 
     * @param listener The asset event listener to add
     */
    public void addAssetEventListener(AssetEventListener listener);
    
    /**
     * Remove an AssetEventListener} from receiving events from this
     * AssetManager
     * @param listener The asset event listener to remove
     */
    public void removeAssetEventListener(AssetEventListener listener);
    
    /**
     * Removes all asset event listeners.
     * 
     * see #addAssetEventListener(com.clockwork.asset.AssetEventListener) 
     */
    public void clearAssetEventListeners();
    
    /**
     * Set an AssetEventListener} to receive events from this
     * AssetManager. Any currently added listeners are
     * cleared and then the given listener is added.
     * 
     * @param listener The listener to set
     * @deprecated Please use #addAssetEventListener(com.clockwork.asset.AssetEventListener) }
     * to listen for asset events.
     */
    @Deprecated
    public void setAssetEventListener(AssetEventListener listener);

    /**
     * Manually locates an asset with the given AssetKey}. This method
     * should be used for debugging or internal uses. 
     * The call will attempt to locate the asset by invoking the
     * AssetLocator} that are registered with this AssetManager,
     * in the same way that the AssetManager#loadAsset(com.clockwork.asset.AssetKey) }
     * method locates assets.
     *
     * @param key The AssetKey} to locate.
     * @return The AssetInfo} object returned from the AssetLocator}
     * that located the asset, or null if the asset cannot be located.
     */
    public AssetInfo locateAsset(AssetKey<?> key);

    /**
     * Load an asset from a key, the asset will be located
     * by one of the AssetLocator} implementations provided in the
     * AssetManager#registerLocator(java.lang.String, java.lang.Class) }
     * call. If located successfully, it will be loaded via the the appropriate
     * AssetLoader} implementation based on the file's extension, as
     * specified in the call 
     * AssetManager#registerLoader(java.lang.Class, java.lang.String[]) }.
     *
     * @param <T> The object type that will be loaded from the AssetKey instance.
     * @param key The AssetKey
     * @return The loaded asset, or null if it was failed to be located
     * or loaded.
     */
    public <T> T loadAsset(AssetKey<T> key);

    /**
     * Load an asset by name, calling this method
     * is the same as calling
     * 
     * loadAsset(new AssetKey(name)).
     * 
     *
     * @param name The name of the asset to load.
     * @return The loaded asset, or null if failed to be loaded.
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Object loadAsset(String name);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA and DDS.
     *
     * @param key The TextureKey} to use for loading.
     * @return The loaded texture, or null if failed to be loaded.
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Texture loadTexture(TextureKey key);

    /**
     * Loads texture file, supported types are BMP, JPG, PNG, GIF,
     * TGA and DDS.
     *
     * @param name The name of the texture to load.
     * @return The texture that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Texture loadTexture(String name);

    /**
     * Load audio file, supported types are WAV or OGG.
     * @param key Asset key of the audio file to load
     * @return The audio data loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public AudioData loadAudio(AudioKey key);

    /**
     * Load audio file, supported types are WAV or OGG.
     * The file is loaded without stream-mode.
     * @param name Asset name of the audio file to load
     * @return The audio data loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public AudioData loadAudio(String name);

    /**
     * Loads a 3D model with a ModelKey. 
     * Models can be CW object files (J3O) or OgreXML/OBJ files.
     * @param key Asset key of the model to load
     * @return The model that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Spatial loadModel(ModelKey key);

    /**
     * Loads a 3D model. Models can be CW object files (J3O) or
     * OgreXML/OBJ files.
     * @param name Asset name of the model to load
     * @return The model that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Spatial loadModel(String name);

    /**
     * Load a material instance (J3M) file.
     * @param name Asset name of the material to load
     * @return The material that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Material loadMaterial(String name);

    /**
     * Loads shader file(s), shouldn't be used by end-user in most cases.
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public Shader loadShader(ShaderKey key);

    /**
     * Load a font file. Font files are in AngelCode text format,
     * and are with the extension "fnt".
     *
     * @param name Asset name of the font to load
     * @return The font loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey) 
     */
    public BitmapFont loadFont(String name);
    
    /**
     * Loads a filter *.j3f file with a FilterKey.
     * @param key Asset key of the filter file to load
     * @return The filter that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public FilterPostProcessor loadFilter(FilterKey key);

    /**
     * Loads a filter *.j3f file with a FilterKey.
     * @param name Asset name of the filter file to load
     * @return The filter that was loaded
     *
     * see AssetManager#loadAsset(com.clockwork.asset.AssetKey)
     */
    public FilterPostProcessor loadFilter(String name);
    
    /**
     * Sets the shaderGenerator to generate shaders based on shaderNodes.
     * @param generator the shaderGenerator 
     */    
    public void setShaderGenerator(ShaderGenerator generator);
    
    /**
     * Returns the shaderGenerator responsible for generating the shaders
     * @return the shaderGenerator 
     */
    public ShaderGenerator getShaderGenerator(EnumSet<Caps> caps);
    
}
