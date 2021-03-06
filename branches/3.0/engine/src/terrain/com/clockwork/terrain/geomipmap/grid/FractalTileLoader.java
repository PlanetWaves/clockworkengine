
package com.clockwork.terrain.geomipmap.grid;


import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.math.Vector3f;
import com.clockwork.terrain.geomipmap.TerrainGridTileLoader;
import com.clockwork.terrain.geomipmap.TerrainQuad;
import com.clockwork.terrain.heightmap.AbstractHeightMap;
import com.clockwork.terrain.heightmap.HeightMap;
import com.clockwork.terrain.noise.Basis;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 *
 */
public class FractalTileLoader implements TerrainGridTileLoader{
            
    public class FloatBufferHeightMap extends AbstractHeightMap {

        private final FloatBuffer buffer;

        public FloatBufferHeightMap(FloatBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean load() {
            this.heightData = this.buffer.array();
            return true;
        }

    }

    private int patchSize;
    private int quadSize;
    private final Basis base;
    private final float heightScale;

    public FractalTileLoader(Basis base, float heightScale) {
        this.base = base;
        this.heightScale = heightScale;
    }

    private HeightMap getHeightMapAt(Vector3f location) {
        AbstractHeightMap heightmap = null;
        
        FloatBuffer buffer = this.base.getBuffer(location.x * (this.quadSize - 1), location.z * (this.quadSize - 1), 0, this.quadSize);

        float[] arr = buffer.array();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] * this.heightScale;
        }
        heightmap = new FloatBufferHeightMap(buffer);
        heightmap.load();
        return heightmap;
    }

    public TerrainQuad getTerrainQuadAt(Vector3f location) {
        HeightMap heightMapAt = getHeightMapAt(location);
        TerrainQuad q = new TerrainQuad("Quad" + location, patchSize, quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap());
        return q;
    }

    public void setPatchSize(int patchSize) {
        this.patchSize = patchSize;
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }

    public void write(CWExporter ex) throws IOException {
        //TODO: serialisation
    }

    public void read(CWImporter im) throws IOException {
        //TODO: serialisation
    }    
}
