
package com.clockwork.asset;

import com.clockwork.material.Material;
import com.clockwork.shader.Shader;

/**
 * <code>AssetProcessor</code> is used to apply processing to assets
 * after they have been loaded. They are assigned to a particular
 * asset type (which is represented by a {@link Class} and any assets
 * loaded that are of that class will be processed by the assigned
 * processor.
 * 
 * 
 */
public interface AssetProcessor {
    /**
     * Applies post processing to an asset.
     * The method may return an object that is not the same
     * instance as the parameter object, and it could be from a different class.
     * 
     * @param obj The asset that was loaded from an {@link AssetLoader}.
     * @return Either the same object with processing applied, or an instance
     * of a new object.
     */
    public Object postProcess(AssetKey key, Object obj);
    
    /**
     * Creates a clone of the given asset.
     * If no clone is desired, then the same instance can be returned,
     * otherwise, a clone should be created.
     * For example, a clone of a {@link Material} should have its own set
     * of unique parameters that can be changed just for that instance,
     * but it may share certain other data if it sees fit (like the {@link Shader}).
     * 
     * @param obj The asset to clone
     * @return The cloned asset, or the same as the given argument if no
     * clone is needed.
     */
    public Object createClone(Object obj);
}
