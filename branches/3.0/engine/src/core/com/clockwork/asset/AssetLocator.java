
package com.clockwork.asset;

/**
 * AssetLocator is used to locate a resource based on an AssetKey.
 *
 */
public interface AssetLocator {
    /**
     * @param rootPath The root path where to look for assets.
     * Typically this method will only be called once per
     * instance of an asset locator.
     */
    public void setRootPath(String rootPath);

    /**
     * Request to locate an asset. The asset key
     * contains a name identifying the asset.
     * If an asset was not found, null should be returned.
     * The AssetInfo} implementation provided should have a proper
     * return value for its AssetInfo#openStream() } method.
     * 
     * @param manager
     * @param key
     * @return The AssetInfo} that was located, or null if not found.
     */
    public AssetInfo locate(AssetManager manager, AssetKey key);
}
