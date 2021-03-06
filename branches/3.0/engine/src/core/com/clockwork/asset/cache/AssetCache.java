
package com.clockwork.asset.cache;

import com.clockwork.asset.AssetKey;

/**
 * AssetCache is an interface for asset caches. 
 * Allowing storage of loaded resources in order to improve their access time 
 * if they are requested again in a short period of time.
 * Depending on the asset type and how it is used, a specialized 
 * caching method can be selected that is most appropriate for that asset type.
 * The asset cache must be thread safe.
 * 
 * Some caches are used to manage cloneable assets, which track reachability
 * based on a shared key in all instances exposed in user code. 
 * E.g. WeakRefCloneAssetCache} uses this approach.
 * For those particular caches, either #registerAssetClone(com.clockwork.asset.AssetKey, java.lang.Object) }
 * or #notifyNoAssetClone() } MUST be called to avoid memory 
 * leaking following a successful #addToCache(com.clockwork.asset.AssetKey, java.lang.Object) }
 * or #getFromCache(com.clockwork.asset.AssetKey) } call!
 * 
 * 
 */
public interface AssetCache {
    /**
     * Adds an asset to the cache.
     * Once added, it should be possible to retrieve the asset
     * by using the #getFromCache(com.clockwork.asset.AssetKey) } method.
     * However the caching criteria may at some point choose that the asset
     * should be removed from the cache to save memory, in that case, 
     * #getFromCache(com.clockwork.asset.AssetKey) } will return null.
     * <font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to cache.
     * @param key The asset key that can be used to look up the asset.
     * @param obj The asset data to cache.
     */
    public <T> void addToCache(AssetKey<T> key, T obj);
    
    /**
     * This should be called by the asset manager when it has successfully
     * acquired a cached asset (with #getFromCache(com.clockwork.asset.AssetKey) })
     * and cloned it for use. 
     * <font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to register.
     * @param key The asset key of the loaded asset (used to retrieve from cache)
     * @param clone The <strong>clone</strong> of the asset retrieved from
     * the cache.
     */
    public <T> void registerAssetClone(AssetKey<T> key, T clone);
    
    /**
     * Notifies the cache that even though the methods #addToCache(com.clockwork.asset.AssetKey, java.lang.Object) }
     * or #getFromCache(com.clockwork.asset.AssetKey) } were used, there won't
     * be a call to #registerAssetClone(com.clockwork.asset.AssetKey, java.lang.Object) }
     * for some reason. For example, if an error occurred during loading
     * or if the addToCache/getFromCache were used from user code.
     */
    public void notifyNoAssetClone();
    
    /**
     * Retrieves an asset from the cache.
     * It is possible to add an asset to the cache using
     * #addToCache(com.clockwork.asset.AssetKey, java.lang.Object) }. 
     * The asset may be removed from the cache automatically even if
     * it was added previously, in that case, this method will return null.
     * <font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to retrieve
     * @param key The key used to lookup the asset.
     * @return The asset that was previously cached, or null if not found.
     */
    public <T> T getFromCache(AssetKey<T> key);
    
    /**
     * Deletes an asset from the cache.
     * <font color="red">Thread-Safe</font>
     * 
     * @param key The asset key to find the asset to delete.
     * @return True if the asset was successfully found in the cache
     * and removed.
     */
    public boolean deleteFromCache(AssetKey key);
    
    /**
     * Deletes all assets from the cache.
     * <font color="red">Thread-Safe</font>
     */
    public void clearCache();
}
