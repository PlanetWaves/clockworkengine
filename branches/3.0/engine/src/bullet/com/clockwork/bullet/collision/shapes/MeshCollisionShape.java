
package com.clockwork.bullet.collision.shapes;

import com.clockwork.bullet.util.NativeMeshUtil;
import com.clockwork.export.InputCapsule;
import com.clockwork.export.JmeExporter;
import com.clockwork.export.JmeImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.scene.Mesh;
import com.clockwork.scene.VertexBuffer.Type;
import com.clockwork.scene.mesh.IndexBuffer;
import com.clockwork.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic mesh collision shape
 * 
 */
public class MeshCollisionShape extends CollisionShape {

    protected int numVertices, numTriangles, vertexStride, triangleIndexStride;
    protected ByteBuffer triangleIndexBase, vertexBase;
    protected long meshId = 0;

    public MeshCollisionShape() {
    }

    /**
     * creates a collision shape from the given TriMesh
     * @param mesh the TriMesh to use
     */
    public MeshCollisionShape(Mesh mesh) {
        createCollisionMesh(mesh);
    }

    private void createCollisionMesh(Mesh mesh) {
        triangleIndexBase = BufferUtils.createByteBuffer(mesh.getTriangleCount() * 3 * 4);
        vertexBase = BufferUtils.createByteBuffer(mesh.getVertexCount() * 3 * 4);
        numVertices = mesh.getVertexCount();
        vertexStride = 12; //3 verts * 4 bytes per.
        numTriangles = mesh.getTriangleCount();
        triangleIndexStride = 12; //3 index entries * 4 bytes each.

        IndexBuffer indices = mesh.getIndicesAsList();
        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        vertices.rewind();

        int verticesLength = mesh.getVertexCount() * 3;
        for (int i = 0; i < verticesLength; i++) {
            float tempFloat = vertices.get();
            vertexBase.putFloat(tempFloat);
        }

        int indicesLength = mesh.getTriangleCount() * 3;
        for (int i = 0; i < indicesLength; i++) {
            triangleIndexBase.putInt(indices.get(i));
        }
        vertices.rewind();
        vertices.clear();

        createShape();
    }

    /**
     * creates a jme mesh from the collision shape, only needed for debugging
     */
//    public Mesh createJmeMesh(){
//        return Converter.convert(bulletMesh);
//    }
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(numVertices, "numVertices", 0);
        capsule.write(numTriangles, "numTriangles", 0);
        capsule.write(vertexStride, "vertexStride", 0);
        capsule.write(triangleIndexStride, "triangleIndexStride", 0);

        capsule.write(triangleIndexBase.array(), "triangleIndexBase", new byte[0]);
        capsule.write(vertexBase.array(), "vertexBase", new byte[0]);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        numVertices = capsule.readInt("numVertices", 0);
        numTriangles = capsule.readInt("numTriangles", 0);
        vertexStride = capsule.readInt("vertexStride", 0);
        triangleIndexStride = capsule.readInt("triangleIndexStride", 0);

        triangleIndexBase = ByteBuffer.wrap(capsule.readByteArray("triangleIndexBase", new byte[0]));
        vertexBase = ByteBuffer.wrap(capsule.readByteArray("vertexBase", new byte[0])).order(ByteOrder.nativeOrder());
        createShape();
    }

    protected void createShape() {
//        bulletMesh = new IndexedMesh();
//        bulletMesh.numVertices = numVertices;
//        bulletMesh.numTriangles = numTriangles;
//        bulletMesh.vertexStride = vertexStride;
//        bulletMesh.triangleIndexStride = triangleIndexStride;
//        bulletMesh.triangleIndexBase = triangleIndexBase;
//        bulletMesh.vertexBase = vertexBase;
//        bulletMesh.triangleIndexBase = triangleIndexBase;
//        TriangleIndexVertexArray tiv = new TriangleIndexVertexArray(numTriangles, triangleIndexBase, triangleIndexStride, numVertices, vertexBase, vertexStride);
//        objectId = new BvhTriangleMeshShape(tiv, true);
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        meshId = NativeMeshUtil.createTriangleIndexVertexArray(triangleIndexBase, vertexBase, numTriangles, numVertices, vertexStride, triangleIndexStride);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Mesh {0}", Long.toHexString(meshId));
        objectId = createShape(meshId);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(long meshId);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Mesh {0}", Long.toHexString(meshId));
        finalizeNative(meshId);
    }

    private native void finalizeNative(long objectId);
}
